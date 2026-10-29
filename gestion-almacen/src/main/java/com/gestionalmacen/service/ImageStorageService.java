package com.gestionalmacen.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.exception.BusinessRuleException;

// Guarda, trae y borra las imagenes del sistema: la foto de un producto (RF-14 / CU-22)
// y la de una factura de proveedor (RF-15 / CU-23).
//
// El archivo se guarda en una carpeta del servidor y en la base queda solo el nombre: un BLOB
// engorda los respaldos y servir un archivo estatico es mas rapido que sacar el binario de la base.
//
// Cada foto se guarda dos veces: la original y una miniatura. Los listados y la grilla de la
// venta muestran la foto a 40 o a 110 pixeles, y una original de 600x600 obliga al navegador a
// decodificar y achicar seis veces mas pixeles de los que se ven, por cada producto y cada vez.
// En una computadora de mostrador vieja eso se nota al desplazar.
//
// ApplicationRunner: al arrancar se completan las miniaturas que falten. Hace falta porque las
// fotos cargadas antes de esto no tienen, y porque si alguna vez falla la creacion, el proximo
// arranque la recupera sola.
@Service
public class ImageStorageService implements IImageStorage, ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

	// Formatos que acepta, con la extension que le corresponde a cada uno. Lo que no este aca
	// se rechaza: asi no se guarda cualquier archivo con nombre de imagen.
	private static final Map<String, String> TYPES = Map.of(
			MediaType.IMAGE_JPEG_VALUE, "jpg",
			MediaType.IMAGE_PNG_VALUE, "png",
			"image/webp", "webp");

	// 2 MB alcanza de sobra para la foto de un producto y evita llenar el disco del servidor.
	private static final long MAX_SIZE = 2L * 1024 * 1024;

	// La miniatura va como "producto-7-mini.jpg", al lado de la original. Siempre JPG, sea cual
	// sea el formato de la original: es el mas liviano de los tres y la foto de un producto no
	// necesita transparencia.
	private static final String MINI = "-mini";
	private static final String MINI_EXTENSION = "jpg";

	// Lo mas grande que se dibuja una foto en el sistema son 140 pixeles. Con 240 queda nitida
	// tambien en una pantalla de mayor densidad, y sigue siendo seis veces mas chica que la original.
	private static final int MINI_SIDE = 240;

	// De donde se acepta descargar una imagen. El enlace llega desde el navegador, asi que sin esta
	// lista alguien podria hacer que el servidor pida cualquier direccion, incluso de la red interna.
	private static final String[] ORIGINS = {
			"https://images.openfoodfacts.org/",
			"https://images.openbeautyfacts.org/",
			"https://images.openproductsfacts.org/",
			"https://images.openpetfoodfacts.org/",
	};

	// Carpeta donde quedan las imagenes. Va fuera del proyecto: son datos del comercio, no codigo.
	@Value("${app.images.folder}")
	private String folder;

	@Override
	public String save(String baseName, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("No se recibió ninguna imagen.");
		}
		if (file.getSize() > MAX_SIZE) {
			throw new BusinessRuleException("La imagen no puede superar los 2 MB.");
		}

		String extension = TYPES.get(file.getContentType());
		if (extension == null) {
			throw new BusinessRuleException("La imagen tiene que ser JPG, PNG o WEBP.");
		}

		try {
			return this.write(baseName, extension, file.getBytes());
		} catch (IOException e) {
			log.error("No se pudo guardar la imagen {}", baseName, e);
			throw new BusinessRuleException("No se pudo guardar la imagen. Intente de nuevo.");
		}
	}

	@Override
	public String downloadFromCatalog(String baseName, String url) {
		if (url == null || url.isBlank() || !this.isAllowed(url)) {
			return null;
		}

		try {
			ResponseEntity<byte[]> respuesta = this.client().get().uri(url).retrieve().toEntity(byte[].class);
			byte[] contenido = respuesta.getBody();
			MediaType tipo = respuesta.getHeaders().getContentType();
			String extension = tipo == null ? null : TYPES.get(tipo.toString());

			if (contenido == null || contenido.length == 0 || contenido.length > MAX_SIZE || extension == null) {
				log.warn("La imagen sugerida para {} no sirve: {}", baseName, url);
				return null;
			}
			return this.write(baseName, extension, contenido);
		} catch (RestClientException | IOException e) {
			// No poder traer la foto no es un error: el producto se da de alta igual y la foto se carga despues.
			log.warn("No se pudo descargar la imagen de {}: {}", baseName, e.getMessage());
			return null;
		}
	}

	@Override
	public void delete(String baseName) {
		// Se borran todas las extensiones posibles: si antes era un JPG y ahora un PNG, el viejo
		// quedaria ocupando lugar para siempre. La miniatura se va con la original.
		for (String extension : TYPES.values()) {
			try {
				Files.deleteIfExists(this.folder().resolve(this.fileName(baseName, extension)));
			} catch (IOException e) {
				log.warn("No se pudo borrar la imagen {} de {}", extension, baseName);
			}
		}
		try {
			Files.deleteIfExists(this.folder().resolve(this.fileName(baseName + MINI, MINI_EXTENSION)));
		} catch (IOException e) {
			log.warn("No se pudo borrar la miniatura de {}", baseName);
		}
	}

	// Completa las miniaturas que falten, al arrancar.
	@Override
	public void run(ApplicationArguments args) {
		try (Stream<Path> archivos = Files.list(this.folder())) {
			List<Path> pendientes = archivos.filter(this::needsThumbnail).toList();
			for (Path archivo : pendientes) {
				String nombre = archivo.getFileName().toString();
				this.writeThumbnail(nombre.substring(0, nombre.lastIndexOf('.')), Files.readAllBytes(archivo));
			}
			if (!pendientes.isEmpty()) {
				log.info("Se crearon {} miniaturas que faltaban", pendientes.size());
			}
		} catch (IOException e) {
			// Sin miniaturas el sistema funciona igual, solo que las listas pesan mas.
			log.warn("No se pudieron completar las miniaturas: {}", e.getMessage());
		}
	}

	// Una imagen original a la que todavia no se le hizo la miniatura.
	private boolean needsThumbnail(Path archivo) {
		String nombre = archivo.getFileName().toString();
		int punto = nombre.lastIndexOf('.');
		if (punto < 0 || !TYPES.containsValue(nombre.substring(punto + 1).toLowerCase())) {
			return false;
		}
		String base = nombre.substring(0, punto);
		if (base.endsWith(MINI)) {
			return false;
		}
		return !Files.exists(archivo.resolveSibling(this.fileName(base + MINI, MINI_EXTENSION)));
	}

	// El nombre lo arma quien llama a partir de un id, nunca lo manda el cliente: asi no hay
	// forma de que un nombre con ".." termine escribiendo fuera de la carpeta.
	private String fileName(String baseName, String extension) {
		return baseName + "." + extension;
	}

	private String write(String baseName, String extension, byte[] contenido) throws IOException {
		this.delete(baseName);
		String nombre = this.fileName(baseName, extension);
		Files.write(this.folder().resolve(nombre), contenido);
		this.writeThumbnail(baseName, contenido);
		return nombre;
	}

	// La version chica, la que se muestra en los listados y en la grilla de la venta.
	//
	// Si falla no se corta nada: la pantalla se da cuenta de que no hay miniatura y usa la foto
	// original. El producto se ve igual, solo que la pantalla pesa mas. Pasa, por ejemplo, con
	// los WEBP: Java no sabe leerlos.
	private void writeThumbnail(String baseName, byte[] contenido) {
		try {
			BufferedImage original = ImageIO.read(new ByteArrayInputStream(contenido));
			if (original == null) {
				log.info("No se pudo leer el formato de {} para hacer la miniatura", baseName);
				return;
			}

			// Se achica por el lado mas largo para no deformarla. Una foto ya chica no se agranda:
			// agrandarla solo la haria pesar mas y verse peor.
			double escala = Math.min((double) MINI_SIDE / original.getWidth(),
					(double) MINI_SIDE / original.getHeight());
			int ancho = escala < 1 ? Math.max(1, (int) Math.round(original.getWidth() * escala))
					: original.getWidth();
			int alto = escala < 1 ? Math.max(1, (int) Math.round(original.getHeight() * escala))
					: original.getHeight();

			// SCALE_SMOOTH promedia los pixeles en lugar de descartarlos: achicar a la mitad sin
			// eso deja los bordes dentados. Es mas lento, pero se hace una sola vez por foto.
			Image achicada = original.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);

			// Fondo blanco: un PNG con transparencia quedaria con el fondo negro al pasarlo a JPG.
			BufferedImage mini = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
			Graphics2D lienzo = mini.createGraphics();
			lienzo.setColor(Color.WHITE);
			lienzo.fillRect(0, 0, ancho, alto);
			lienzo.drawImage(achicada, 0, 0, null);
			lienzo.dispose();

			Path destino = this.folder().resolve(this.fileName(baseName + MINI, MINI_EXTENSION));
			ImageIO.write(mini, MINI_EXTENSION, destino.toFile());
		} catch (IOException | RuntimeException e) {
			log.warn("No se pudo crear la miniatura de {}: {}", baseName, e.getMessage());
		}
	}

	private Path folder() throws IOException {
		return Files.createDirectories(Paths.get(folder));
	}

	private boolean isAllowed(String url) {
		for (String origen : ORIGINS) {
			if (url.startsWith(origen)) {
				return true;
			}
		}
		log.warn("No se descarga la imagen: el enlace no es de los catalogos que consulta el sistema");
		return false;
	}

	// Con tiempo de espera: el alta de un producto no puede quedar colgada porque el catalogo no responda.
	private RestClient client() {
		SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(Duration.ofSeconds(10));
		return RestClient.builder().requestFactory(fabrica).build();
	}
}

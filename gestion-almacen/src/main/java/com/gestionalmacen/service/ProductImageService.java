package com.gestionalmacen.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.exception.BusinessRuleException;

// Guarda, trae y borra la imagen de un producto (RF-12 / CU-21).
//
// El archivo se guarda en una carpeta del servidor y en la base queda solo el nombre: un BLOB
// engorda los respaldos y servir un archivo estatico es mas rapido que sacar el binario de la base.
@Service
public class ProductImageService implements IProductImageService {

	private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

	// Formatos que acepta, con la extension que le corresponde a cada uno. Lo que no este aca
	// se rechaza: asi no se guarda cualquier archivo con nombre de imagen.
	private static final Map<String, String> TYPES = Map.of(
			MediaType.IMAGE_JPEG_VALUE, "jpg",
			MediaType.IMAGE_PNG_VALUE, "png",
			"image/webp", "webp");

	// 2 MB alcanza de sobra para la foto de un producto y evita llenar el disco del servidor.
	private static final long MAX_SIZE = 2L * 1024 * 1024;

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
	public String save(Long productId, MultipartFile file) {
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
			return this.write(productId, extension, file.getBytes());
		} catch (IOException e) {
			log.error("No se pudo guardar la imagen del producto {}", productId, e);
			throw new BusinessRuleException("No se pudo guardar la imagen. Intente de nuevo.");
		}
	}

	@Override
	public String download(Long productId, String url) {
		if (url == null || url.isBlank() || !this.isAllowed(url)) {
			return null;
		}

		try {
			ResponseEntity<byte[]> respuesta = this.client().get().uri(url).retrieve().toEntity(byte[].class);
			byte[] contenido = respuesta.getBody();
			MediaType tipo = respuesta.getHeaders().getContentType();
			String extension = tipo == null ? null : TYPES.get(tipo.toString());

			if (contenido == null || contenido.length == 0 || contenido.length > MAX_SIZE || extension == null) {
				log.warn("La imagen sugerida para el producto {} no sirve: {}", productId, url);
				return null;
			}
			return this.write(productId, extension, contenido);
		} catch (RestClientException | IOException e) {
			// No poder traer la foto no es un error: el producto se da de alta igual y la foto se carga despues.
			log.warn("No se pudo descargar la imagen del producto {}: {}", productId, e.getMessage());
			return null;
		}
	}

	@Override
	public void delete(Long productId) {
		// Se borran todas las extensiones posibles: si antes era un JPG y ahora un PNG, el viejo
		// quedaria ocupando lugar para siempre.
		for (String extension : TYPES.values()) {
			try {
				Files.deleteIfExists(this.folder().resolve(this.fileName(productId, extension)));
			} catch (IOException e) {
				log.warn("No se pudo borrar la imagen {} del producto {}", extension, productId);
			}
		}
	}

	// El nombre sale del id del producto y nunca de lo que mando el cliente: asi no hay forma
	// de que un nombre con ".." termine escribiendo fuera de la carpeta.
	private String fileName(Long productId, String extension) {
		return "producto-" + productId + "." + extension;
	}

	private String write(Long productId, String extension, byte[] contenido) throws IOException {
		this.delete(productId);
		String nombre = this.fileName(productId, extension);
		Files.write(this.folder().resolve(nombre), contenido);
		return nombre;
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

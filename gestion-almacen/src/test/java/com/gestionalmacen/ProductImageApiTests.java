package com.gestionalmacen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Imagen de un producto: carga, reemplazo y baja (RF-14 / CU-22), y la foto que sugiere
// el catalogo publico al escanear un codigo (CU-03 paso 6).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class ProductImageApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private FakeProductCatalog catalog;

	// La misma carpeta que usa el servicio. En las pruebas apunta a una temporal.
	@Value("${app.images.folder}")
	private String folder;

	// @Transactional deshace lo de la base, pero no borra archivos: la carpeta se limpia a mano.
	@BeforeEach
	void prepare() throws IOException {
		catalog.reset();
		userRepository.save(user("admin"));
		this.emptyFolder();
	}

	@Test
	void savesTheImageOfAProduct() throws Exception {
		long id = this.createProduct();

		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("producto-" + id + ".jpg"));

		assertTrue(Files.exists(this.file("producto-" + id + ".jpg")), "la imagen tiene que quedar en la carpeta");
	}

	// El nombre del archivo sale del id del producto, nunca del que mando el cliente: con un
	// nombre elegido por el cliente se podria escribir fuera de la carpeta.
	@Test
	void ignoresTheFileNameSentByTheClient() throws Exception {
		long id = this.createProduct();

		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("../../atacante.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("producto-" + id + ".jpg"));
	}

	@Test
	void rejectsAFileThatIsNotAnImage() throws Exception {
		long id = this.createProduct();

		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("lista.txt", MediaType.TEXT_PLAIN_VALUE)).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La imagen tiene que ser JPG, PNG o WEBP."));
	}

	@Test
	void rejectsAnImageOverTwoMegabytes() throws Exception {
		long id = this.createProduct();
		byte[] pesada = new byte[3 * 1024 * 1024];

		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(new MockMultipartFile("file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, pesada)).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La imagen no puede superar los 2 MB."));
	}

	@Test
	void answersNotFoundWhenTheProductDoesNotExist() throws Exception {
		mvc.perform(multipart("/api/products/{id}/image", 9999)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isNotFound());
	}

	// Al cambiar de formato el archivo viejo tiene que irse: si no, queda ocupando lugar para siempre.
	@Test
	void replacesTheImageThatTheProductAlreadyHad() throws Exception {
		long id = this.createProduct();
		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk());

		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("otra.png", MediaType.IMAGE_PNG_VALUE)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("producto-" + id + ".png"));

		assertFalse(Files.exists(this.file("producto-" + id + ".jpg")), "la imagen anterior tiene que borrarse");
		assertTrue(Files.exists(this.file("producto-" + id + ".png")));
	}

	@Test
	void removesTheImageOfAProduct() throws Exception {
		long id = this.createProduct();
		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk());

		mvc.perform(delete("/api/products/{id}/image", id).with(csrf()))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/products/{id}", id)).andExpect(jsonPath("$.image").doesNotExist());
		assertFalse(Files.exists(this.file("producto-" + id + ".jpg")), "el archivo tiene que borrarse");
	}

	// ---------- CU-03 paso 6: la foto que sugiere el catalogo publico ----------

	@Test
	void theSuggestionCarriesTheLinkToThePhoto() throws Exception {
		catalog.add("7791813444411", "Seven Up 1.5 lts", "https://images.openfoodfacts.org/images/7up.jpg");

		mvc.perform(get("/api/products/suggestion/7791813444411"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imageUrl").value("https://images.openfoodfacts.org/images/7up.jpg"));
	}

	// El enlace llega desde el navegador: si se descargara cualquier direccion, se podria hacer
	// que el servidor pida paginas de la red interna del comercio.
	@Test
	void doesNotDownloadAnImageFromOutsideTheCatalogs() throws Exception {
		String json = product() + ",\"imageUrl\":\"http://localhost:8080/api/users\"}";

		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.image").doesNotExist());
	}

	// CU-22: un producto que ya estaba cargado tambien puede pedir la foto al catalogo.
	// Como la descarga sale a internet, aca se comprueba lo que se puede sin red: que el
	// enlace viaje en la modificacion y que uno de afuera de los catalogos no se descargue.
	@Test
	void doesNotDownloadAnImageFromOutsideTheCatalogsWhenEditing() throws Exception {
		long id = this.createProduct();
		String json = product() + ",\"imageUrl\":\"http://localhost:8080/api/users\"}";

		mvc.perform(put("/api/products/{id}", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").doesNotExist());
	}

	// Si la descarga no sale bien, el producto no puede quedarse sin la foto que ya tenia.
	@Test
	void keepsThePreviousImageWhenTheDownloadFails() throws Exception {
		long id = this.createProduct();
		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk());

		// Un enlace que no es de los catalogos: no se descarga nada.
		String json = product() + ",\"imageUrl\":\"http://localhost:8080/api/users\"}";
		mvc.perform(put("/api/products/{id}", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("producto-" + id + ".jpg"));

		assertTrue(Files.exists(this.file("producto-" + id + ".jpg")), "la foto anterior tiene que seguir estando");
	}

	// Modificar un producto sin pedir foto no toca la que ya tenia.
	@Test
	void anEditWithoutAPhotoLeavesTheOneItHad() throws Exception {
		long id = this.createProduct();
		mvc.perform(multipart("/api/products/{id}/image", id)
				.file(image("foto.jpg", MediaType.IMAGE_JPEG_VALUE)).with(csrf()))
				.andExpect(status().isOk());

		mvc.perform(put("/api/products/{id}", id).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(product() + "}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("producto-" + id + ".jpg"));
	}

	@Test
	void aNewProductStartsWithoutImage() throws Exception {
		mvc.perform(get("/api/products/{id}", this.createProduct()))
				.andExpect(jsonPath("$.image").doesNotExist());
	}

	// ---------- helpers ----------

	// Los datos minimos de un producto, sin cerrar la llave: asi cada prueba le suma lo suyo.
	private String product() {
		return "{\"name\":\"Yerba 1kg\",\"price\":4850.00,\"stock\":40,\"minimumStock\":10,\"onOffer\":false";
	}

	private long createProduct() throws Exception {
		String response = mvc.perform(post("/api/products").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(product() + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private MockMultipartFile image(String name, String contentType) {
		return new MockMultipartFile("file", name, contentType, new byte[] { 1, 2, 3 });
	}

	private Path file(String name) {
		return Paths.get(folder).resolve(name);
	}

	private void emptyFolder() throws IOException {
		Path carpeta = Paths.get(folder);
		if (!Files.exists(carpeta)) {
			return;
		}
		try (Stream<Path> archivos = Files.walk(carpeta)) {
			archivos.sorted(Comparator.reverseOrder()).forEach(archivo -> archivo.toFile().delete());
		}
	}

	private User user(String username) {
		User user = new User();
		user.setName("Prueba");
		user.setLastName("Prueba");
		user.setUsername(username);
		user.setPasswordHash("sin-uso"); // estas pruebas no inician sesion con contraseña
		user.setRole(Role.ADMIN);
		return user;
	}
}

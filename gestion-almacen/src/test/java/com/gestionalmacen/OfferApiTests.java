package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.ISaleRepository;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Sugerencias y gestion de ofertas (RF-10 / CU-19).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN") // las ofertas son del administrador
class OfferApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private ISaleRepository saleRepository;

	@BeforeEach
	void loadUser() {
		userRepository.save(newUser("admin", Role.ADMIN));
	}

	// ---------- RF-10: sugerencia por baja rotacion ----------

	// Primero los que menos se vendieron. Uno sin ventas aparece con cero.
	@Test
	void listsTheLeastSoldProductsFirst() throws Exception {
		long yerba = createProduct("Yerba", "4850.00", 50);
		long fideos = createProduct("Fideos", "1200.00", 50);
		createProduct("Arroz", "900.00", 50);
		sell(yerba, 5);
		sell(fideos, 1);

		mvc.perform(get("/api/offers/least-sold"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].product.name").value("Arroz"))
				.andExpect(jsonPath("$[0].unitsSold").value(0))
				.andExpect(jsonPath("$[1].product.name").value("Fideos"))
				.andExpect(jsonPath("$[1].unitsSold").value(1))
				.andExpect(jsonPath("$[2].product.name").value("Yerba"))
				.andExpect(jsonPath("$[2].unitsSold").value(5));
	}

	// Una venta de hace 40 dias no cuenta en un periodo de 30.
	@Test
	void onlyCountsTheSalesOfThePeriod() throws Exception {
		long yerba = createProduct("Yerba", "4850.00", 50);
		long saleId = sell(yerba, 5);

		Sale sale = saleRepository.findById(saleId).orElseThrow();
		sale.setDateTime(LocalDateTime.now().minusDays(40));
		saleRepository.flush();

		mvc.perform(get("/api/offers/least-sold").param("days", "30"))
				.andExpect(jsonPath("$[0].unitsSold").value(0));
		mvc.perform(get("/api/offers/least-sold").param("days", "60"))
				.andExpect(jsonPath("$[0].unitsSold").value(5));
	}

	// No se sugiere un producto dado de baja ni uno sin stock: no hay nada que promocionar.
	@Test
	void ignoresInactiveProductsAndProductsWithoutStock() throws Exception {
		createProduct("Con stock", "100.00", 10);
		createProduct("Sin stock", "100.00", 0);
		long inactive = createProduct("Dado de baja", "100.00", 10);
		mvc.perform(patch("/api/products/" + inactive + "/deactivate").with(csrf())).andExpect(status().isNoContent());

		mvc.perform(get("/api/offers/least-sold"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].product.name").value("Con stock"));
	}

	// ---------- CU-19: poner y quitar ofertas ----------

	@Test
	void putsAProductOnOffer() throws Exception {
		long aceite = createProduct("Aceite", "3400.00", 25);

		mvc.perform(offer(aceite, "2990"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onOffer").value(true))
				.andExpect(jsonPath("$.offerPrice").value(2990))
				.andExpect(jsonPath("$.salePrice").value(2990));

		mvc.perform(get("/api/offers"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Aceite"));
	}

	// CU-19 exc. 4a: la oferta tiene que ser mas barata que el precio normal.
	@Test
	void rejectsAnOfferPriceThatIsNotLower() throws Exception {
		long aceite = createProduct("Aceite", "3400.00", 25);

		mvc.perform(offer(aceite, "3400"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El precio de oferta tiene que ser menor al precio normal."));
	}

	@Test
	void theOfferPriceIsRequired() throws Exception {
		long aceite = createProduct("Aceite", "3400.00", 25);

		mvc.perform(patch("/api/offers/" + aceite).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.offerPrice").value("El precio de oferta es obligatorio"));
	}

	@Test
	void rejectsAnOfferOnAnInactiveProduct() throws Exception {
		long aceite = createProduct("Aceite", "3400.00", 25);
		mvc.perform(patch("/api/products/" + aceite + "/deactivate").with(csrf())).andExpect(status().isNoContent());

		mvc.perform(offer(aceite, "2990")).andExpect(status().isConflict());
	}

	// CU-19 paso 6: al quitar la oferta vuelve el precio normal.
	@Test
	void endsAnOffer() throws Exception {
		long aceite = createProduct("Aceite", "3400.00", 25);
		mvc.perform(offer(aceite, "2990")).andExpect(status().isOk());

		mvc.perform(patch("/api/offers/" + aceite + "/end").with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onOffer").value(false))
				.andExpect(jsonPath("$.offerPrice").isEmpty())
				.andExpect(jsonPath("$.salePrice").value(3400.00));
		mvc.perform(get("/api/offers")).andExpect(jsonPath("$.length()").value(0));
	}

	// RF-10: las ofertas son decision del administrador.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void theEmployeeCannotManageOffers() throws Exception {
		mvc.perform(get("/api/offers")).andExpect(status().isForbidden());
		mvc.perform(get("/api/offers/least-sold")).andExpect(status().isForbidden());
		mvc.perform(offer(1, "100")).andExpect(status().isForbidden());
	}

	private RequestBuilder offer(long productId, String offerPrice) {
		return patch("/api/offers/" + productId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"offerPrice\":" + offerPrice + "}");
	}

	// Registra una venta de un solo producto y devuelve su numero.
	private long sell(long productId, int quantity) throws Exception {
		return create(post("/api/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}],"
						+ "\"paymentMethod\":\"EFECTIVO\"}"));
	}

	private long createProduct(String name, String price, int stock) throws Exception {
		return create(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"price\":" + price + ",\"stock\":" + stock
						+ ",\"minimumStock\":0,\"onOffer\":false}"));
	}

	// Manda el pedido de alta y devuelve el id que le asigno la base.
	private long create(RequestBuilder request) throws Exception {
		String response = mvc.perform(request)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private User newUser(String username, Role role) {
		User user = new User();
		user.setName("Prueba");
		user.setLastName("Prueba");
		user.setUsername(username);
		user.setPasswordHash("sin-uso"); // estas pruebas no inician sesion con contraseña
		user.setRole(role);
		return user;
	}
}

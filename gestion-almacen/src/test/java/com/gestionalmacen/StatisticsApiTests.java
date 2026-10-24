package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Panel principal: los numeros del dia (RF-15 / CU-06).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class StatisticsApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	@BeforeEach
	void loadUsers() {
		userRepository.save(user("admin", Role.ADMIN));
		userRepository.save(user("vendedor", Role.EMPLEADO));
	}

	// ---------- Lo que ve cualquiera ----------

	@Test
	void showsWhatWasSoldToday() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", "3000.00");
		sell(yerba, 2);
		sell(yerba, 1);

		mvc.perform(get("/api/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.soldToday").value(14550.00))
				.andExpect(jsonPath("$.ticketsToday").value(2))
				.andExpect(jsonPath("$.averageTicket").value(7275.00))
				.andExpect(jsonPath("$.byMethod.EFECTIVO").value(14550.00));
	}

	// Un dia sin ventas no deja los numeros en null: la pantalla muestra cero y no se rompe.
	@Test
	void aDayWithoutSalesShowsZero() throws Exception {
		mvc.perform(get("/api/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.soldToday").value(0))
				.andExpect(jsonPath("$.ticketsToday").value(0))
				.andExpect(jsonPath("$.averageTicket").doesNotExist());
	}

	// ---------- La ganancia (RF-15) ----------

	@Test
	void calculatesTheProfitWithTheCostOfWhatWasSold() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", "3000.00");
		sell(yerba, 2);

		// Dos unidades a 4850 que costaron 3000 cada una.
		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.profitToday").value(3700.00))
				.andExpect(jsonPath("$.linesWithoutCost").value(0));
	}

	// Un producto sin costo cargado no entra en la ganancia y la pantalla avisa cuantos quedaron
	// afuera: contarlo como costo cero daria una ganancia inventada.
	@Test
	void leavesOutTheLinesWithoutCost() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", "3000.00");
		long fideos = createProduct("Fideos 500g", "1200.00", null);
		sell(yerba, 2);
		sell(fideos, 3);

		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.soldToday").value(13300.00))
				.andExpect(jsonPath("$.profitToday").value(3700.00))
				.andExpect(jsonPath("$.linesWithoutCost").value(1));
	}

	// Lo que hace que la ganancia de un periodo cerrado no se mueva: el costo que cuenta es el
	// del dia de la venta, no el que tiene el producto hoy.
	@Test
	void theProfitDoesNotChangeWhenTheCostGoesUpLater() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", "3000.00");
		sell(yerba, 2);

		// El proveedor aumenta y se actualiza el costo del producto.
		mvc.perform(put("/api/products/" + yerba).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(product("Yerba 1kg", "4850.00", "4000.00")))
				.andExpect(status().isOk());

		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.profitToday").value(3700.00));
	}

	// ---------- El grafico y lo mas vendido ----------

	// Una barra por dia, tambien los dias en que no se vendio nada: si los dias vacios no
	// vinieran, una semana con un feriado se dibujaria mas corta de lo que es.
	@Test
	void theChartHasOneBarPerDayIncludingTheEmptyOnes() throws Exception {
		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.lastDays.length()").value(14))
				.andExpect(jsonPath("$.lastDays[0].total").value(0))
				.andExpect(jsonPath("$.lastDays[13].total").value(0));
	}

	@Test
	void showsTheBestSellingProductsByAmount() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", "3000.00");
		long fideos = createProduct("Fideos 500g", "1200.00", "800.00");
		sell(yerba, 2);
		sell(fideos, 3);

		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.topProducts.length()").value(2))
				.andExpect(jsonPath("$.topProducts[0].name").value("Yerba 1kg"))
				.andExpect(jsonPath("$.topProducts[0].total").value(9700.00))
				.andExpect(jsonPath("$.topProducts[1].name").value("Fideos 500g"));
	}

	// ---------- Que ve cada rol ----------

	// La ganancia y la deuda a proveedores son del comercio, no del turno. No es que la pantalla
	// las esconda: el servidor no las manda.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void theEmployeeOnlyGetsWhatIsNeededToCloseTheTill() throws Exception {
		mvc.perform(get("/api/statistics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.soldToday").exists())
				.andExpect(jsonPath("$.byMethod").exists())
				.andExpect(jsonPath("$.profitToday").doesNotExist())
				.andExpect(jsonPath("$.linesWithoutCost").doesNotExist())
				.andExpect(jsonPath("$.supplierDebt").doesNotExist())
				.andExpect(jsonPath("$.monthPurchases").doesNotExist())
				.andExpect(jsonPath("$.lastDays").doesNotExist())
				.andExpect(jsonPath("$.topProducts").doesNotExist());
	}

	// El administrador si los recibe.
	@Test
	void theAdministratorGetsTheMoneyOfTheBusiness() throws Exception {
		mvc.perform(get("/api/statistics"))
				.andExpect(jsonPath("$.profitToday").exists())
				.andExpect(jsonPath("$.supplierDebt").exists())
				.andExpect(jsonPath("$.monthPurchases").exists())
				.andExpect(jsonPath("$.lastDays").exists());
	}

	// Sin sesion no se ven los numeros del comercio.
	@Test
	@WithMockUser(username = "nadie", roles = "NINGUNO")
	void aRoleWithoutPermissionsStillNeedsASession() throws Exception {
		mvc.perform(get("/api/statistics")).andExpect(status().isOk());
	}

	// ---------- Ayudantes ----------

	// Un producto con su precio de venta y, si se indica, el de costo.
	private long createProduct(String name, String price, String costPrice) throws Exception {
		String response = mvc.perform(post("/api/products").with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(product(name, price, costPrice)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private String product(String name, String price, String costPrice) {
		return "{\"name\":\"" + name + "\",\"price\":" + price
				+ ",\"costPrice\":" + (costPrice == null ? "null" : costPrice)
				+ ",\"stock\":100,\"minimumStock\":1,\"onOffer\":false}";
	}

	// Una venta en efectivo, que queda confirmada al registrarse.
	private void sell(long productId, int quantity) throws Exception {
		String json = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]"
				+ ",\"discount\":null,\"paymentMethod\":\"EFECTIVO\"}";
		RequestBuilder request = post("/api/sales").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(json);
		mvc.perform(request).andExpect(status().isCreated());
	}

	private User user(String username, Role role) {
		User user = new User();
		user.setName("Prueba");
		user.setLastName("Prueba");
		user.setUsername(username);
		user.setPasswordHash("sin-uso"); // estas pruebas no inician sesion con contraseña
		user.setRole(role);
		return user;
	}
}

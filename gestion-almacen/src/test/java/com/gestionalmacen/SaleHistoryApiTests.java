package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Historial de ventas con filtros (RF-07 / CU-16).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN") // el historial es del administrador
class SaleHistoryApiTests {

	private static final String TODAY = LocalDate.now().toString();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	// Dos ventas de hoy: una del empleado con yerba y otra del administrador con leche.
	@BeforeEach
	void loadSales() throws Exception {
		userRepository.save(newUser("admin", Role.ADMIN));
		userRepository.save(newUser("vendedor", Role.EMPLEADO));

		long yerba = createProduct("Yerba Mate 1kg", "4850.00");
		long leche = createProduct("Leche entera 1L", "1650.00");

		sell("vendedor", "EMPLEADO", yerba, 2);
		sell("admin", "ADMIN", leche, 1);
	}

	// CU-16 pasos 2 y 3: las del periodo, de la mas nueva a la mas vieja.
	@Test
	void listsTheSalesOfThePeriodNewestFirst() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].username").value("admin"))
				.andExpect(jsonPath("$.content[1].username").value("vendedor"))
				.andExpect(jsonPath("$.content[1].details[0].productName").value("Yerba Mate 1kg"))
				.andExpect(jsonPath("$.content[1].payments[0].method").value("EFECTIVO"));
	}

	@Test
	void filtersByEmployee() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY).param("username", "vendedor"))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].total").value(9700.00));
	}

	// El producto se busca por su nombre en los renglones de la venta.
	@Test
	void filtersByProduct() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY).param("product", "leche"))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].username").value("admin"));
	}

	// CU-10: el numero de operacion encuentra la venta de un cobro del reporte de MercadoPago.
	@Test
	void filtersByOperationNumber() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY).param("reference", "ORD-PRUEBA"))
				.andExpect(status().isOk())
				// Las dos ventas de la preparacion son en efectivo, asi que no tienen numero de operacion.
				.andExpect(jsonPath("$.content.length()").value(0));

		// Sin filtro siguen apareciendo las dos.
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY))
				.andExpect(jsonPath("$.content.length()").value(2));
	}

	// CU-16 exc. 3a: no hay ventas en el periodo.
	@Test
	void anotherPeriodHasNoSales() throws Exception {
		String yesterday = LocalDate.now().minusDays(1).toString();

		mvc.perform(get("/api/sales").param("from", yesterday).param("to", yesterday))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void rejectsAnInvertedPeriod() throws Exception {
		String tomorrow = LocalDate.now().plusDays(1).toString();

		mvc.perform(get("/api/sales").param("from", tomorrow).param("to", TODAY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La fecha desde no puede ser posterior a la fecha hasta."));
	}

	// Las fechas son obligatorias y tienen que estar bien escritas.
	@Test
	void rejectsAMissingOrInvalidDate() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/sales").param("from", "21/09/2026").param("to", TODAY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Falta un dato de la búsqueda o tiene un formato inválido."));
	}

	// ---------- RF-13: el historial de a paginas ----------

	@Test
	void bringsTheHistoryOnePageAtATime() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY)
						.param("page", "0").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].username").value("admin"))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));

		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY)
						.param("page", "1").param("size", "1"))
				.andExpect(jsonPath("$.content[0].username").value("vendedor"));
	}

	// Aunque la pagina traiga una sola venta, los renglones y el pago siguen viniendo: la
	// pantalla los muestra en el listado.
	@Test
	void eachPageStillBringsTheDetailsAndThePayment() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY)
						.param("page", "1").param("size", "1"))
				.andExpect(jsonPath("$.content[0].details[0].productName").value("Yerba Mate 1kg"))
				.andExpect(jsonPath("$.content[0].payments[0].method").value("EFECTIVO"));
	}

	// ---------- CU-16 paso 6: el resumen del periodo ----------

	// Lo cobrado y como se reparte por forma de pago.
	@Test
	void summarisesWhatWasCharged() throws Exception {
		mvc.perform(get("/api/sales/summary").param("from", TODAY).param("to", TODAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.charged").value(2))
				.andExpect(jsonPath("$.total").value(11350.00))
				.andExpect(jsonPath("$.byMethod.EFECTIVO").value(11350.00));
	}

	// El resumen es del periodo entero y no de la pagina que se esta viendo: si fuera de la
	// pagina, el total de la pantalla no serviria para controlar la caja.
	@Test
	void theSummaryDoesNotDependOnThePage() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY)
						.param("page", "0").param("size", "1"))
				.andExpect(jsonPath("$.content.length()").value(1));

		mvc.perform(get("/api/sales/summary").param("from", TODAY).param("to", TODAY))
				.andExpect(jsonPath("$.charged").value(2))
				.andExpect(jsonPath("$.total").value(11350.00));
	}

	// El resumen respeta los mismos filtros que el listado.
	@Test
	void theSummaryUsesTheSameFilters() throws Exception {
		mvc.perform(get("/api/sales/summary").param("from", TODAY).param("to", TODAY)
						.param("username", "vendedor"))
				.andExpect(jsonPath("$.charged").value(1))
				.andExpect(jsonPath("$.total").value(9700.00));
	}

	// Un periodo sin ventas no deja los importes en null: la pantalla muestra cero.
	@Test
	void theSummaryOfAPeriodWithoutSalesIsZero() throws Exception {
		String yesterday = LocalDate.now().minusDays(1).toString();

		mvc.perform(get("/api/sales/summary").param("from", yesterday).param("to", yesterday))
				.andExpect(jsonPath("$.charged").value(0))
				.andExpect(jsonPath("$.total").value(0))
				.andExpect(jsonPath("$.byMethod").isEmpty());
	}

	// RF-07: el historial es del administrador. El empleado igual puede ver el ticket de una venta.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void theEmployeeCannotSeeTheHistory() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY))
				.andExpect(status().isForbidden());
	}

	// Registra una venta en nombre del usuario indicado.
	private void sell(String username, String role, long productId, int quantity) throws Exception {
		mvc.perform(post("/api/sales").with(csrf()).with(user(username).roles(role))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}],"
						+ "\"paymentMethod\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated());
	}

	private long createProduct(String name, String price) throws Exception {
		String response = mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"price\":" + price + ",\"stock\":50,\"minimumStock\":1,\"onOffer\":false}"))
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

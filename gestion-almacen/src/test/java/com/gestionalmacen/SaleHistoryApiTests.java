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
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].username").value("admin"))
				.andExpect(jsonPath("$[1].username").value("vendedor"))
				.andExpect(jsonPath("$[1].details[0].productName").value("Yerba Mate 1kg"))
				.andExpect(jsonPath("$[1].payments[0].method").value("EFECTIVO"));
	}

	@Test
	void filtersByEmployee() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY).param("username", "vendedor"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].total").value(9700.00));
	}

	// El producto se busca por su nombre en los renglones de la venta.
	@Test
	void filtersByProduct() throws Exception {
		mvc.perform(get("/api/sales").param("from", TODAY).param("to", TODAY).param("product", "leche"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].username").value("admin"));
	}

	// CU-16 exc. 3a: no hay ventas en el periodo.
	@Test
	void anotherPeriodHasNoSales() throws Exception {
		String yesterday = LocalDate.now().minusDays(1).toString();

		mvc.perform(get("/api/sales").param("from", yesterday).param("to", yesterday))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
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

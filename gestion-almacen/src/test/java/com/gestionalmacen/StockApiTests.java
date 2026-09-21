package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Ajuste de stock, movimientos y alertas (RF-02, RF-09 / CU-04, CU-06).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "vendedor", roles = "EMPLEADO") // el stock tambien lo maneja el empleado
class StockApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	// El usuario de la sesion tiene que existir en la base: cada movimiento guarda quien lo hizo.
	@BeforeEach
	void loadUser() {
		userRepository.save(user("vendedor", Role.EMPLEADO));
	}

	// ---------- RF-02 / CU-04: ajuste y movimientos ----------

	// El alta del producto deja registrada la carga inicial.
	@Test
	void creatingAProductRecordsTheInitialLoad() throws Exception {
		long id = createProduct("Yerba 1kg", 40, 10, null);

		mvc.perform(get("/api/stock/" + id + "/movements"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].type").value("CARGA_INICIAL"))
				.andExpect(jsonPath("$[0].previousStock").value(0))
				.andExpect(jsonPath("$[0].newStock").value(40))
				.andExpect(jsonPath("$[0].username").value("vendedor"));
	}

	@Test
	void adjustsTheStockAndRecordsTheMovement() throws Exception {
		long id = createProduct("Fideos", 60, 15, null);

		mvc.perform(adjust(id, 48, "Rotura de 12 paquetes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(48));

		// El mas nuevo primero.
		mvc.perform(get("/api/stock/" + id + "/movements"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].type").value("AJUSTE_MANUAL"))
				.andExpect(jsonPath("$[0].previousStock").value(60))
				.andExpect(jsonPath("$[0].newStock").value(48))
				.andExpect(jsonPath("$[0].reason").value("Rotura de 12 paquetes"))
				.andExpect(jsonPath("$[0].username").value("vendedor"))
				.andExpect(jsonPath("$[0].dateTime").exists());
	}

	// CU-04 paso 7: si el stock queda en el minimo o por debajo, aparece la alerta.
	@Test
	void anAdjustmentCanLeaveTheProductWithLowStock() throws Exception {
		long id = createProduct("Leche 1L", 30, 20, null);

		mvc.perform(adjust(id, 8, "Conteo del día"))
				.andExpect(jsonPath("$.lowStock").value(true));
		mvc.perform(get("/api/stock/low-stock"))
				.andExpect(jsonPath("$[0].name").value("Leche 1L"));
	}

	// CU-04 exc. 5a: el valor no es valido.
	@Test
	void rejectsANegativeStock() throws Exception {
		long id = createProduct("Arroz", 10, 5, null);

		mvc.perform(adjust(id, -3, "Conteo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.newStock").value("El stock no puede ser negativo"));
	}

	@Test
	void theReasonIsRequired() throws Exception {
		long id = createProduct("Arroz", 10, 5, null);

		mvc.perform(adjust(id, 12, " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.reason").value("El motivo es obligatorio"));
	}

	@Test
	void rejectsAnAdjustmentThatChangesNothing() throws Exception {
		long id = createProduct("Arroz", 10, 5, null);

		mvc.perform(adjust(id, 10, "Conteo"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El stock nuevo es igual al actual."));
	}

	@Test
	void rejectsAnAdjustmentOnAnInactiveProduct() throws Exception {
		long id = createProduct("Producto viejo", 10, 5, null);
		deactivate(id);

		mvc.perform(adjust(id, 0, "Descarte"))
				.andExpect(status().isConflict());
	}

	@Test
	void anUnknownProductReturns404() throws Exception {
		mvc.perform(adjust(9999, 5, "Conteo")).andExpect(status().isNotFound());
		mvc.perform(get("/api/stock/9999/movements")).andExpect(status().isNotFound());
	}

	// ---------- RF-09 / CU-06: alertas ----------

	// Solo los activos en el minimo o por debajo, del stock mas bajo al mas alto.
	@Test
	void listsTheLowStockProducts() throws Exception {
		createProduct("Normal", 50, 10, null);
		createProduct("En el mínimo", 10, 10, null);
		createProduct("Sin stock", 0, 5, null);
		deactivate(createProduct("Dado de baja", 1, 5, null));

		mvc.perform(get("/api/stock/low-stock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Sin stock"))
				.andExpect(jsonPath("$[1].name").value("En el mínimo"));
	}

	// Los que vencen dentro del plazo, incluidos los vencidos. Sin fecha, sin stock o dados de baja no entran.
	@Test
	void listsTheProductsAboutToExpire() throws Exception {
		LocalDate today = LocalDate.now();
		createProduct("Vencido", 5, 1, today.minusDays(2));
		createProduct("Vence en 10 días", 5, 1, today.plusDays(10));
		createProduct("Vence en 60 días", 5, 1, today.plusDays(60));
		createProduct("Sin fecha", 5, 1, null);
		createProduct("Sin stock", 0, 0, today.plusDays(3));
		deactivate(createProduct("Dado de baja", 5, 1, today.plusDays(5)));

		// Sin indicar el plazo, son 30 dias.
		mvc.perform(get("/api/stock/expiring"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Vencido"))
				.andExpect(jsonPath("$[1].name").value("Vence en 10 días"));

		mvc.perform(get("/api/stock/expiring").param("days", "90"))
				.andExpect(jsonPath("$.length()").value(3));
	}

	// Arma el pedido de ajuste de stock.
	private RequestBuilder adjust(long productId, int newStock, String reason) {
		return patch("/api/stock/" + productId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"newStock\":" + newStock + ",\"reason\":\"" + reason + "\"}");
	}

	// Da de alta un producto y devuelve su id. La fecha de vencimiento puede ir en null.
	private long createProduct(String name, int stock, int minimumStock, LocalDate expirationDate) throws Exception {
		String json = "{\"name\":\"" + name + "\",\"price\":100.00,\"stock\":" + stock
				+ ",\"minimumStock\":" + minimumStock
				+ ",\"expirationDate\":" + (expirationDate == null ? "null" : "\"" + expirationDate + "\"") + "}";

		String response = mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private void deactivate(long productId) throws Exception {
		mvc.perform(patch("/api/products/" + productId + "/deactivate").with(csrf()))
				.andExpect(status().isNoContent());
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

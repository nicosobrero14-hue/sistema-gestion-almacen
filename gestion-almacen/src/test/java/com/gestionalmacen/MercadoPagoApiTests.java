package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

// Cobro con el codigo QR de MercadoPago (RF-05 / CU-10).
// La venta no se registra hasta que el dinero entra: hasta entonces el stock no se toca.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "vendedor", roles = "EMPLEADO")
class MercadoPagoApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	// La pasarela falsa: desde aca se simula que el cliente pago o que el cobro vencio.
	@Autowired
	private FakePaymentGateway gateway;

	@BeforeEach
	void prepare() {
		gateway.reset();
		userRepository.save(newUser("vendedor", Role.EMPLEADO));
	}

	// ---------- CU-10: la venta espera el pago ----------

	@Test
	void theSaleWaitsForThePaymentAndTheStockIsNotTouched() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);

		mvc.perform(sale("MERCADOPAGO", item(yerba, 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDIENTE_PAGO"))
				.andExpect(jsonPath("$.total").value(9700.00))
				.andExpect(jsonPath("$.payments[0].method").value("MERCADOPAGO"))
				.andExpect(jsonPath("$.payments[0].status").value("PENDIENTE"))
				// El identificador del cobro queda guardado para poder seguirlo.
				.andExpect(jsonPath("$.payments[0].externalReference").value("ORD-PRUEBA-1"));

		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(40));
	}

	@Test
	void whileNobodyPaysTheSaleKeepsWaiting() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 1)));

		mvc.perform(get("/api/sales/" + saleId + "/payment"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDIENTE_PAGO"));
		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(40));
	}

	// ---------- CU-10 paso 6: el dinero entra ----------

	@Test
	void confirmsTheSaleWhenThePaymentArrives() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 2)));
		gateway.approve("ORD-PRUEBA-1");

		mvc.perform(get("/api/sales/" + saleId + "/payment"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.payments[0].status").value("APROBADO"))
				.andExpect(jsonPath("$.payments[0].confirmedAt").exists());

		// Recien ahora se descuenta el stock y queda el movimiento (CU-12).
		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(38));
		mvc.perform(get("/api/stock/" + yerba + "/movements"))
				.andExpect(jsonPath("$[0].type").value("VENTA"))
				.andExpect(jsonPath("$[0].newStock").value(38))
				.andExpect(jsonPath("$[0].reason").value("Venta N° " + saleId));
	}

	// Preguntar de nuevo despues de confirmada no tiene que descontar el stock dos veces.
	@Test
	void checkingTwiceDoesNotDiscountTheStockTwice() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 2)));
		gateway.approve("ORD-PRUEBA-1");

		mvc.perform(get("/api/sales/" + saleId + "/payment")).andExpect(jsonPath("$.status").value("CONFIRMADA"));
		mvc.perform(get("/api/sales/" + saleId + "/payment")).andExpect(jsonPath("$.status").value("CONFIRMADA"));

		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(38));
	}

	// ---------- CU-10 exc. 5a: el cobro no se completa ----------

	@Test
	void cancelsTheSaleWhenTheChargeExpires() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 1)));
		gateway.expire("ORD-PRUEBA-1");

		mvc.perform(get("/api/sales/" + saleId + "/payment"))
				.andExpect(jsonPath("$.status").value("CANCELADA"))
				.andExpect(jsonPath("$.payments[0].status").value("CANCELADO"));
		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(40));
	}

	@Test
	void theCashierCancelsTheCharge() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 1)));

		mvc.perform(post("/api/sales/" + saleId + "/payment/cancel").with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADA"));
		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(40));
	}

	// Una venta ya cobrada no se puede dar de baja: el dinero ya entro.
	@Test
	void cannotCancelASaleAlreadyPaid() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("MERCADOPAGO", item(yerba, 1)));
		gateway.approve("ORD-PRUEBA-1");
		mvc.perform(get("/api/sales/" + saleId + "/payment")).andExpect(jsonPath("$.status").value("CONFIRMADA"));

		mvc.perform(post("/api/sales/" + saleId + "/payment/cancel").with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La venta N° " + saleId + " ya no está esperando un pago."));
	}

	// ---------- Que el resto siga igual ----------

	@Test
	void cashSalesAreStillConfirmedOnTheSpot() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);

		mvc.perform(sale("EFECTIVO", item(yerba, 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.payments[0].status").value("APROBADO"))
				.andExpect(jsonPath("$.payments[0].externalReference").doesNotExist());
		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(39));
	}

	// ---------- ayudas ----------

	private RequestBuilder sale(String paymentMethod, String... items) {
		String json = "{\"items\":[" + String.join(",", items) + "],\"paymentMethod\":\"" + paymentMethod + "\"}";
		return post("/api/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json);
	}

	private String item(long productId, int quantity) {
		return "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
	}

	private long createProduct(String name, String price, int stock) throws Exception {
		return create(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"price\":" + price + ",\"stock\":" + stock
						+ ",\"minimumStock\":1,\"onOffer\":false}"));
	}

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
		user.setPasswordHash("sin-uso");
		user.setRole(role);
		return user;
	}
}

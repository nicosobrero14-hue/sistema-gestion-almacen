package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

// Venta, descuento de stock y ticket (RF-03, RF-06 / CU-07 a CU-15).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "vendedor", roles = "EMPLEADO") // vender es la tarea principal del empleado
class SaleApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	// Los usuarios de la sesion tienen que existir en la base: la venta guarda quien la hizo.
	@BeforeEach
	void loadUsers() {
		userRepository.save(user("admin", Role.ADMIN));
		userRepository.save(user("vendedor", Role.EMPLEADO));
	}

	// ---------- CU-11: confirmar la venta ----------

	@Test
	void confirmsACashSale() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long fideos = createProduct("Fideos 500g", "1200.00", 60);

		mvc.perform(sale("EFECTIVO", "100", item(yerba, 2), item(fideos, 3)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"))
				.andExpect(jsonPath("$.username").value("vendedor"))
				.andExpect(jsonPath("$.subtotal").value(13300.00))
				.andExpect(jsonPath("$.discount").value(100))
				.andExpect(jsonPath("$.total").value(13200.00))
				.andExpect(jsonPath("$.details.length()").value(2))
				.andExpect(jsonPath("$.details[0].productName").value("Yerba 1kg"))
				.andExpect(jsonPath("$.details[0].unitPrice").value(4850.00))
				.andExpect(jsonPath("$.details[0].quantity").value(2))
				.andExpect(jsonPath("$.details[0].subtotal").value(9700.00))
				.andExpect(jsonPath("$.payments[0].method").value("EFECTIVO"))
				.andExpect(jsonPath("$.payments[0].status").value("APROBADO"))
				.andExpect(jsonPath("$.payments[0].amount").value(13200.00));
	}

	// CU-12: al confirmar se descuenta el stock y queda el movimiento con el numero de venta.
	@Test
	void discountsTheStockAndRecordsTheMovement() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("EFECTIVO", null, item(yerba, 2)));

		mvc.perform(get("/api/products/" + yerba)).andExpect(jsonPath("$.stock").value(38));
		mvc.perform(get("/api/stock/" + yerba + "/movements"))
				.andExpect(jsonPath("$[0].type").value("VENTA"))
				.andExpect(jsonPath("$[0].previousStock").value(40))
				.andExpect(jsonPath("$[0].newStock").value(38))
				.andExpect(jsonPath("$[0].reason").value("Venta N° " + saleId))
				.andExpect(jsonPath("$[0].username").value("vendedor"));
	}

	@Test
	void recordsATransfer() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);

		mvc.perform(sale("TRANSFERENCIA", null, item(yerba, 1)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.discount").value(0))
				.andExpect(jsonPath("$.payments[0].method").value("TRANSFERENCIA"));
	}

	// Un producto en oferta se cobra al precio de oferta (RF-10).
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void chargesTheOfferPrice() throws Exception {
		long aceite = create(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Aceite 900ml\",\"price\":3400.00,\"stock\":25,\"minimumStock\":10,"
						+ "\"onOffer\":true,\"offerPrice\":2990.00}"));

		mvc.perform(sale("EFECTIVO", null, item(aceite, 1)))
				.andExpect(jsonPath("$.details[0].unitPrice").value(2990.00))
				.andExpect(jsonPath("$.total").value(2990.00));
	}

	// El ticket guarda el precio del momento: si despues cambia el precio, la venta no cambia.
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void theTicketKeepsThePriceOfTheSale() throws Exception {
		long arroz = createProduct("Arroz 1kg", "900.00", 20);
		long saleId = create(sale("EFECTIVO", null, item(arroz, 1)));

		mvc.perform(put("/api/products/" + arroz).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Arroz 1kg\",\"price\":1100.00,\"stock\":19,\"minimumStock\":5,\"onOffer\":false}"))
				.andExpect(status().isOk());

		mvc.perform(get("/api/sales/" + saleId))
				.andExpect(jsonPath("$.details[0].unitPrice").value(900.00))
				.andExpect(jsonPath("$.total").value(900.00));
	}

	// ---------- Excepciones ----------

	// CU-07 exc. 5a y CU-08 exc. 4a: no se vende mas de lo que hay, y el stock queda igual.
	@Test
	void rejectsMoreThanTheAvailableStock() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);

		mvc.perform(sale("EFECTIVO", null, item(leche, 6)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No hay stock suficiente de Leche 1L. Disponible: 5."));
		mvc.perform(get("/api/products/" + leche)).andExpect(jsonPath("$.stock").value(5));
	}

	// CU-11 exc. 1a: el carrito esta vacio.
	@Test
	void rejectsAnEmptySale() throws Exception {
		mvc.perform(sale("EFECTIVO", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.items").value("Agregue al menos un producto a la venta"));
	}

	// CU-08 exc. 3a: la cantidad es cero.
	@Test
	void rejectsAZeroQuantity() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);

		mvc.perform(sale("EFECTIVO", null, item(leche, 0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors['items[0].quantity']").value("La cantidad tiene que ser mayor a cero"));
	}

	@Test
	void theSaleNeedsAPaymentMethod() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);

		mvc.perform(sale(null, null, item(leche, 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.paymentMethod").value("Elija la forma de pago"));
	}

	@Test
	void rejectsADiscountEqualToTheSubtotal() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);

		mvc.perform(sale("EFECTIVO", "1650", item(leche, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El descuento tiene que ser menor al subtotal."));
	}

	@Test
	void rejectsAnInactiveProduct() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);
		mvc.perform(patch("/api/products/" + leche + "/deactivate").with(csrf())).andExpect(status().isNoContent());

		mvc.perform(sale("EFECTIVO", null, item(leche, 1)))
				.andExpect(status().isConflict());
	}

	// Repetido, cada renglon pasaria el control de stock por separado y se venderia de mas.
	@Test
	void rejectsARepeatedProduct() throws Exception {
		long leche = createProduct("Leche 1L", "1650.00", 5);

		mvc.perform(sale("EFECTIVO", null, item(leche, 3), item(leche, 3)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El producto Leche 1L está repetido en la venta."));
	}

	// ---------- CU-14: ticket ----------

	@Test
	void findsTheSaleForTheTicket() throws Exception {
		long yerba = createProduct("Yerba 1kg", "4850.00", 40);
		long saleId = create(sale("EFECTIVO", null, item(yerba, 1)));

		mvc.perform(get("/api/sales/" + saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(saleId))
				.andExpect(jsonPath("$.dateTime").exists())
				.andExpect(jsonPath("$.details[0].productName").value("Yerba 1kg"))
				.andExpect(jsonPath("$.payments[0].method").value("EFECTIVO"));
		mvc.perform(get("/api/sales/999999")).andExpect(status().isNotFound());
	}

	// Arma el pedido de una venta. paymentMethod y discount pueden ir en null.
	private RequestBuilder sale(String paymentMethod, String discount, String... items) {
		String json = "{\"items\":[" + String.join(",", items) + "]"
				+ ",\"discount\":" + discount
				+ ",\"paymentMethod\":" + (paymentMethod == null ? "null" : "\"" + paymentMethod + "\"") + "}";
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

	// Manda el pedido de alta y devuelve el id que le asigno la base.
	private long create(RequestBuilder request) throws Exception {
		String response = mvc.perform(request)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
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

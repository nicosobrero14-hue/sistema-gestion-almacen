package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Carga de las facturas de los proveedores (RF-15 / CU-23).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class PurchaseApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	private long supplierId;

	@BeforeEach
	void prepare() throws Exception {
		userRepository.save(user("admin", Role.ADMIN));
		userRepository.save(user("vendedor", Role.EMPLEADO));
		supplierId = create("/api/suppliers", "{\"name\":\"Distribuidora Fogolin\"}");
	}

	@Test
	void loadsAnInvoice() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "125878.05", null, null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.supplier.name").value("Distribuidora Fogolin"))
				.andExpect(jsonPath("$.voucherNumber").value("0003-00004046"))
				.andExpect(jsonPath("$.invoicedTotal").value(125878.05))
				// Si no se aclara otra cosa, lo acordado es lo facturado.
				.andExpect(jsonPath("$.agreedTotal").value(125878.05))
				.andExpect(jsonPath("$.adjustmentReason").doesNotExist());
	}

	// Una factura recien cargada no tiene pagos, asi que debe todo.
	@Test
	void aNewInvoiceOwesEverything() throws Exception {
		long id = this.createInvoice();

		mvc.perform(get("/api/purchases/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paid").value(0))
				.andExpect(jsonPath("$.balance").value(125878.05))
				.andExpect(jsonPath("$.status").value("PENDIENTE"));
	}

	// El caso de la factura real: el papel decia un total y se pago otro porque falto mercaderia.
	@Test
	void theAgreedTotalCanBeLessThanTheInvoicedOne() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "125878.05", "120209.00", "Faltó un producto del pedido")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoicedTotal").value(125878.05))
				.andExpect(jsonPath("$.agreedTotal").value(120209.00))
				.andExpect(jsonPath("$.balance").value(120209.00));
	}

	@Test
	void rejectsAnAgreedTotalOverTheInvoicedOne() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "1000.00", "1500.00", "Se me ocurrió")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El total acordado no puede ser mayor al facturado."));
	}

	// Si se corrige el total hay que decir por que: sin motivo, el numero no se puede auditar.
	@Test
	void rejectsACorrectedTotalWithoutAReason() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "1000.00", "900.00", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Si el total acordado no es el facturado, hay que indicar el motivo."));
	}

	// Con fotos y reparto diario, cargar la misma factura dos veces es un error que va a pasar.
	@Test
	void rejectsTheSameVoucherTwice() throws Exception {
		this.createInvoice();

		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "999.00", null, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("El comprobante 0003-00004046 de Distribuidora Fogolin ya está cargado."));
	}

	// El mismo numero de otro proveedor no es el mismo comprobante.
	@Test
	void theSameNumberFromAnotherSupplierIsAllowed() throws Exception {
		this.createInvoice();
		long otro = create("/api/suppliers", "{\"name\":\"Lácteos del Centro\"}");

		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"supplierId\":" + otro + ",\"voucherType\":6,\"salesPoint\":3,\"number\":4046"
						+ ",\"date\":\"2026-10-20\",\"invoicedTotal\":500.00}"))
				.andExpect(status().isCreated());
	}

	@Test
	void rejectsASupplierThatDoesNotExist() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"supplierId\":9999,\"voucherType\":6,\"salesPoint\":3,\"number\":1"
						+ ",\"date\":\"2026-10-20\",\"invoicedTotal\":500.00}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsASupplierThatWasDeactivated() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/suppliers/{id}/deactivate", supplierId).with(csrf()))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "500.00", null, null)))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectsATotalOfZero() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "0", null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.invoicedTotal").exists());
	}

	// Una factura que todavia no se emitio no existe.
	@Test
	void rejectsAnInvoiceDatedInTheFuture() throws Exception {
		String manana = LocalDate.now().plusDays(1).toString();

		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"supplierId\":" + supplierId + ",\"voucherType\":6,\"salesPoint\":3,\"number\":77"
						+ ",\"date\":\"" + manana + "\",\"invoicedTotal\":500.00}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.date").exists());
	}

	// ---------- el periodo ----------

	@Test
	void listsOnlyThePurchasesOfThePeriod() throws Exception {
		this.createInvoiceOn("2026-10-20", 4046);
		this.createInvoiceOn("2026-09-15", 4000);

		mvc.perform(get("/api/purchases").param("from", "2026-10-01").param("to", "2026-10-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].voucherNumber").value("0003-00004046"));
	}

	@Test
	void filtersBySupplier() throws Exception {
		this.createInvoiceOn("2026-10-20", 4046);
		long otro = create("/api/suppliers", "{\"name\":\"Lácteos del Centro\"}");
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"supplierId\":" + otro + ",\"voucherType\":6,\"salesPoint\":1,\"number\":9"
						+ ",\"date\":\"2026-10-21\",\"invoicedTotal\":500.00}"))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/purchases").param("from", "2026-10-01").param("to", "2026-10-31")
				.param("supplierId", String.valueOf(otro)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].supplier.name").value("Lácteos del Centro"));
	}

	// ---------- la foto del comprobante ----------

	@Test
	void savesThePhotoOfTheVoucher() throws Exception {
		long id = this.createInvoice();

		mvc.perform(multipart("/api/purchases/{id}/image", id)
				.file(new MockMultipartFile("file", "factura.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] { 1, 2, 3 }))
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.image").value("compra-" + id + ".jpg"));
	}

	@Test
	void rejectsAVoucherPhotoThatIsNotAnImage() throws Exception {
		long id = this.createInvoice();

		mvc.perform(multipart("/api/purchases/{id}/image", id)
				.file(new MockMultipartFile("file", "factura.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[] { 1 }))
				.with(csrf()))
				.andExpect(status().isConflict());
	}

	// ---------- permisos ----------

	// El empleado carga la factura cuando llega el reparto, que es cuando la tiene en la mano.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void anEmployeeCanLoadAnInvoice() throws Exception {
		mvc.perform(post("/api/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(invoice(6, 3, 4046, "125878.05", null, null)))
				.andExpect(status().isCreated());
	}

	// Pero los saldos son plata del comercio y los ve el administrador.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void anEmployeeCannotSeeThePurchases() throws Exception {
		mvc.perform(get("/api/purchases").param("from", "2026-10-01").param("to", "2026-10-31"))
				.andExpect(status().isForbidden());
	}

	// ---------- helpers ----------

	private String invoice(int type, int point, long number, String invoiced, String agreed, String reason) {
		return "{\"supplierId\":" + supplierId
				+ ",\"voucherType\":" + type
				+ ",\"salesPoint\":" + point
				+ ",\"number\":" + number
				+ ",\"date\":\"2026-10-20\""
				+ ",\"invoicedTotal\":" + invoiced
				+ (agreed == null ? "" : ",\"agreedTotal\":" + agreed)
				+ (reason == null ? "" : ",\"adjustmentReason\":\"" + reason + "\"")
				+ "}";
	}

	private long createInvoice() throws Exception {
		return create("/api/purchases", invoice(6, 3, 4046, "125878.05", null, null));
	}

	private long createInvoiceOn(String date, long number) throws Exception {
		return create("/api/purchases", "{\"supplierId\":" + supplierId
				+ ",\"voucherType\":6,\"salesPoint\":3,\"number\":" + number
				+ ",\"date\":\"" + date + "\",\"invoicedTotal\":125878.05}");
	}

	private long create(String url, String json) throws Exception {
		String response = mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private User user(String username, Role role) {
		User user = new User();
		user.setName("Prueba");
		user.setLastName("Prueba");
		user.setUsername(username);
		user.setPasswordHash("sin-uso");
		user.setRole(role);
		return user;
	}
}

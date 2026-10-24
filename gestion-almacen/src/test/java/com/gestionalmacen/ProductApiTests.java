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
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Alta, baja, modificacion y busqueda de productos (RF-01 / CU-02, CU-20).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN") // con la seguridad activa, cada pedido necesita un usuario con sesion
class ProductApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	// El catalogo publico de prueba: desde aca se simula que conoce o no un codigo.
	@Autowired
	private FakeProductCatalog catalog;

	// Los usuarios de la sesion tienen que existir en la base: el alta registra quien cargo el stock.
	@BeforeEach
	void loadUsers() {
		catalog.reset();
		userRepository.save(user("admin", Role.ADMIN));
		userRepository.save(user("vendedor", Role.EMPLEADO));
	}

	// ---------- CU-03 paso 6: datos sugeridos para un codigo desconocido ----------

	@Test
	void suggestsTheProductDataFromThePublicCatalog() throws Exception {
		catalog.add("7791813444411", "Seven Up 1.5 lts");

		mvc.perform(get("/api/products/suggestion/7791813444411"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.barcode").value("7791813444411"))
				.andExpect(jsonPath("$.name").value("Seven Up 1.5 lts"));
	}

	// Que el catalogo no conozca el codigo es lo normal: se carga a mano y listo.
	@Test
	void answersNotFoundWhenThePublicCatalogDoesNotKnowTheCode() throws Exception {
		mvc.perform(get("/api/products/suggestion/9999999999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No hay datos públicos para el código 9999999999999"));
	}

	// La sugerencia no crea nada: el producto sigue sin existir hasta que alguien lo guarde.
	@Test
	void theSuggestionDoesNotCreateTheProduct() throws Exception {
		catalog.add("7791813444411", "Seven Up 1.5 lts");
		mvc.perform(get("/api/products/suggestion/7791813444411")).andExpect(status().isOk());

		mvc.perform(get("/api/products/barcode/7791813444411")).andExpect(status().isNotFound());
	}

	// ---------- RF-12 / RF-14: las pestanias de la pantalla de venta ----------

	// La venta separa lo que se cuenta de lo que se pesa, porque el circuito es distinto:
	// uno se toca y va al carrito, el otro pasa antes por la balanza.
	@Test
	void listsTheProductsOfEachSaleUnit() throws Exception {
		create("/api/products", product("Yerba 1kg", "4850.00", 40, 10, "7790010001234", null));
		create("/api/products", byWeight("Queso cremoso", "12000.00"));
		create("/api/products", byWeight("Jamón cocido", "14500.00"));

		mvc.perform(get("/api/products/by-sale-unit/{unit}", "KG"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Jamón cocido"))  // alfabetico: se buscan por nombre
				.andExpect(jsonPath("$[1].name").value("Queso cremoso"));

		mvc.perform(get("/api/products/by-sale-unit/{unit}", "UNIDAD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Yerba 1kg"));
	}

	// Un producto dado de baja no se puede vender, asi que tampoco aparece en la pestania.
	@Test
	void theTabLeavesOutProductsThatAreDeactivated() throws Exception {
		long id = create("/api/products", byWeight("Queso cremoso", "12000.00"));
		mvc.perform(patch("/api/products/{id}/deactivate", id).with(csrf())).andExpect(status().isNoContent());

		mvc.perform(get("/api/products/by-sale-unit/{unit}", "KG"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// Una unidad de venta que no existe no puede devolver un listado vacio como si nada.
	@Test
	void rejectsASaleUnitThatDoesNotExist() throws Exception {
		mvc.perform(get("/api/products/by-sale-unit/{unit}", "LITROS"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createsAProductWithItsSupplier() throws Exception {
		long supplierId = create("/api/suppliers", "{\"name\":\"Distribuidora Sur\"}");

		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Yerba 1kg", "4850.00", 40, 10, "7790010001234", supplierId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Yerba 1kg"))
				.andExpect(jsonPath("$.supplier.name").value("Distribuidora Sur"))
				.andExpect(jsonPath("$.lowStock").value(false));
	}

	// RF-09: el stock en el minimo o por debajo se marca como bajo.
	@Test
	void marksLowStock() throws Exception {
		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Leche 1L", "1650.00", 5, 20, null, null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lowStock").value(true));
	}

	// CU-02 exc. 5a: datos invalidos, con el campo marcado.
	@Test
	void rejectsANegativePrice() throws Exception {
		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Malo", "-5", 1, 1, null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.price").value("El precio debe ser mayor a cero"));
	}

	@Test
	void rejectsARepeatedBarcode() throws Exception {
		create("/api/products", product("Original", "100.00", 1, 1, "7790000000001", null));

		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Copia", "100.00", 1, 1, "7790000000001", null)))
				.andExpect(status().isConflict());
	}

	// CU-19 exc. 4a: la oferta tiene que ser mas barata que el precio normal.
	@Test
	void rejectsAnOfferMoreExpensiveThanThePrice() throws Exception {
		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Oferta mala\",\"price\":100.00,\"stock\":1,\"minimumStock\":1,"
						+ "\"onOffer\":true,\"offerPrice\":150.00}"))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectsAnInactiveSupplier() throws Exception {
		long supplierId = create("/api/suppliers", "{\"name\":\"Proveedor dado de baja\"}");
		mvc.perform(patch("/api/suppliers/" + supplierId + "/deactivate").with(csrf())).andExpect(status().isNoContent());

		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Sin proveedor valido", "100.00", 1, 1, null, supplierId)))
				.andExpect(status().isConflict());
	}

	// El stock solo se carga en el alta: editar el producto no lo cambia.
	@Test
	void editingDoesNotChangeTheStock() throws Exception {
		long id = create("/api/products", product("Fideos", "1200.00", 60, 15, null, null));

		mvc.perform(put("/api/products/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Fideos guiseros", "1300.00", 999, 15, null, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Fideos guiseros"))
				.andExpect(jsonPath("$.price").value(1300.00))
				.andExpect(jsonPath("$.stock").value(60));
	}

	@Test
	void searchesByNameOrBarcode() throws Exception {
		create("/api/products", product("Gaseosa cola", "3100.00", 45, 12, "7790895667788", null));
		create("/api/products", product("Azúcar 1kg", "1890.00", 30, 10, null, null));

		mvc.perform(get("/api/products").param("search", "gaseosa"))
				.andExpect(jsonPath("$.length()").value(1));
		mvc.perform(get("/api/products").param("search", "7790895"))
				.andExpect(jsonPath("$[0].name").value("Gaseosa cola"));
	}

	// CU-03: el lector manda el codigo exacto.
	@Test
	void findsAProductByItsBarcode() throws Exception {
		create("/api/products", product("Gaseosa cola", "3100.00", 45, 12, "7790895667788", null));

		mvc.perform(get("/api/products/barcode/7790895667788"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Gaseosa cola"));
	}

	// CU-03 paso 6: el codigo no existe.
	@Test
	void anUnknownBarcodeReturns404() throws Exception {
		mvc.perform(get("/api/products/barcode/7790000099999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No hay ningún producto con el código 7790000099999"));
	}

	@Test
	void deactivatesAProduct() throws Exception {
		long id = create("/api/products", product("Producto viejo", "100.00", 0, 0, null, null));

		mvc.perform(patch("/api/products/" + id + "/deactivate").with(csrf())).andExpect(status().isNoContent());
		mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.active").value(false));
	}

	// CU-20: el empleado puede editar el producto mientras no cambie el precio.
	// 900 y 900.00 son el mismo precio: no cuenta como cambio.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void anEmployeeCanEditAProductWithoutChangingThePrice() throws Exception {
		long id = create("/api/products", product("Arroz", "900.00", 20, 5, null, null));

		mvc.perform(put("/api/products/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Arroz largo fino", "900", 20, 5, null, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Arroz largo fino"));
	}

	// CU-20: solo el administrador cambia precios.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void anEmployeeCannotChangeThePrice() throws Exception {
		long id = create("/api/products", product("Arroz", "900.00", 20, 5, null, null));

		mvc.perform(put("/api/products/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(product("Arroz", "1000.00", 20, 5, null, null)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Solo el administrador puede modificar el precio y la oferta."));
	}

	// CU-19: poner un producto en oferta es decision del administrador.
	@Test
	@WithMockUser(username = "vendedor", roles = "EMPLEADO")
	void anEmployeeCannotPutAProductOnOffer() throws Exception {
		mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Oferta\",\"price\":100.00,\"stock\":1,\"minimumStock\":1,"
						+ "\"onOffer\":true,\"offerPrice\":80.00}"))
				.andExpect(status().isForbidden());
	}

	// Arma el JSON de un producto. barcode y supplierId pueden ir en null.
	// Un producto que se vende por kilo, con el stock fraccionado.
	private String byWeight(String name, String price) {
		return "{\"name\":\"" + name + "\",\"price\":" + price
				+ ",\"stock\":3.250,\"minimumStock\":1,\"saleUnit\":\"KG\",\"onOffer\":false}";
	}

	private String product(String name, String price, int stock, int minimumStock, String barcode, Long supplierId) {
		return "{\"name\":\"" + name + "\",\"price\":" + price + ",\"stock\":" + stock
				+ ",\"minimumStock\":" + minimumStock + ",\"onOffer\":false"
				+ ",\"barcode\":" + (barcode == null ? "null" : "\"" + barcode + "\"")
				+ ",\"supplierId\":" + supplierId + "}";
	}

	// Da de alta un registro y devuelve el id que le asigno la base.
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
		user.setPasswordHash("sin-uso"); // estas pruebas no inician sesion con contraseña
		user.setRole(role);
		return user;
	}
}

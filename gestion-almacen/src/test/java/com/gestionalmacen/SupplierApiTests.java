package com.gestionalmacen;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

// Alta, baja, modificacion y busqueda de proveedores (RF-08 / CU-05).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional // cada prueba deshace lo que guardo
@WithMockUser(roles = "EMPLEADO") // con la seguridad activa, cada pedido necesita un usuario con sesion
class SupplierApiTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void createsASupplier() throws Exception {
		mvc.perform(post("/api/suppliers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Distribuidora Norte\",\"email\":\"norte@mail.com\",\"phone\":\"351555\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Distribuidora Norte"))
				.andExpect(jsonPath("$.active").value(true));
	}

	// CU-05 exc. 5a: faltan datos obligatorios.
	@Test
	void rejectsASupplierWithoutName() throws Exception {
		mvc.perform(post("/api/suppliers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sin-nombre@mail.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").value("El nombre es obligatorio"));
	}

	@Test
	void rejectsARepeatedEmail() throws Exception {
		createSupplier("{\"name\":\"Primero\",\"email\":\"repetido@mail.com\"}");

		mvc.perform(post("/api/suppliers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Segundo\",\"email\":\"repetido@mail.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void editsASupplier() throws Exception {
		long id = createSupplier("{\"name\":\"Nombre viejo\",\"email\":\"editar@mail.com\"}");

		// Guardar sin cambiar el email no cuenta como repetido.
		mvc.perform(put("/api/suppliers/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Nombre nuevo\",\"email\":\"editar@mail.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Nombre nuevo"));
	}

	@Test
	void searchesByName() throws Exception {
		createSupplier("{\"name\":\"Lácteos del Valle\"}");
		createSupplier("{\"name\":\"Bebidas Centro\"}");

		mvc.perform(get("/api/suppliers").param("search", "lácteos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Lácteos del Valle"));
	}

	@Test
	void deactivatesAndActivatesASupplier() throws Exception {
		long id = createSupplier("{\"name\":\"Proveedor temporal\"}");

		mvc.perform(patch("/api/suppliers/" + id + "/deactivate").with(csrf())).andExpect(status().isNoContent());

		// Dado de baja: no aparece entre los activos, pero sigue existiendo.
		mvc.perform(get("/api/suppliers").param("search", "temporal"))
				.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/suppliers").param("search", "temporal").param("activeOnly", "false"))
				.andExpect(jsonPath("$.length()").value(1));

		mvc.perform(patch("/api/suppliers/" + id + "/activate").with(csrf())).andExpect(status().isNoContent());
		mvc.perform(get("/api/suppliers").param("search", "temporal"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void returns404ForAnUnknownSupplier() throws Exception {
		mvc.perform(get("/api/suppliers/999999")).andExpect(status().isNotFound());
	}

	// Da de alta un proveedor y devuelve el id que le asigno la base.
	private long createSupplier(String json) throws Exception {
		String response = mvc.perform(post("/api/suppliers").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}
}

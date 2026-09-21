package com.gestionalmacen;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;

// Alta, baja y modificacion de usuarios (RF-11 / CU-18).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// RNF-02: la contraseña nunca sale por la API.
	@Test
	void createsAUserWithoutExposingThePassword() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content(user("maria", "Empleado1234", "EMPLEADO")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("maria"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	// RNF-02: se guarda el hash BCrypt, no la contraseña.
	@Test
	void storesThePasswordHashed() throws Exception {
		long id = create(user("juan", "Clave1234", "EMPLEADO"));

		User saved = userRepository.findById(id).orElseThrow();
		assertThat(saved.getPasswordHash()).startsWith("$2a$");
		assertThat(passwordEncoder.matches("Clave1234", saved.getPasswordHash())).isTrue();
	}

	// CU-18 exc. 5a: el nombre de usuario ya existe.
	@Test
	void rejectsARepeatedUsername() throws Exception {
		create(user("repetido", "Clave1234", "EMPLEADO"));

		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content(user("repetido", "Otra12345", "EMPLEADO")))
				.andExpect(status().isConflict());
	}

	// RNF-02: minimo 8 caracteres, con letras y numeros.
	@Test
	void rejectsAWeakPassword() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content(user("debil", "corta", "EMPLEADO")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void requiresAPasswordToCreate() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
				.content(user("sinclave", null, "EMPLEADO")))
				.andExpect(status().isConflict());
	}

	@Test
	void editingWithoutPasswordKeepsTheCurrentOne() throws Exception {
		long id = create(user("pedro", "Clave1234", "EMPLEADO"));

		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(user("pedro", null, "EMPLEADO")))
				.andExpect(status().isOk());

		User saved = userRepository.findById(id).orElseThrow();
		assertThat(passwordEncoder.matches("Clave1234", saved.getPasswordHash())).isTrue();
	}

	// CU-18 exc. 3a: no se puede dar de baja al unico administrador.
	@Test
	void cannotDeactivateTheLastAdmin() throws Exception {
		userRepository.deleteAll();
		long id = create(user("jefe", "Admin1234", "ADMIN"));

		mvc.perform(patch("/api/users/" + id + "/deactivate"))
				.andExpect(status().isConflict());
	}

	// Con un segundo administrador activo, ya se puede.
	@Test
	void canDeactivateAnAdminIfThereIsAnother() throws Exception {
		userRepository.deleteAll();
		long id = create(user("jefe", "Admin1234", "ADMIN"));
		create(user("jefe2", "Admin1234", "ADMIN"));

		mvc.perform(patch("/api/users/" + id + "/deactivate"))
				.andExpect(status().isNoContent());
	}

	// Tampoco se le puede quitar el rol al unico administrador.
	@Test
	void cannotRemoveTheRoleFromTheLastAdmin() throws Exception {
		userRepository.deleteAll();
		long id = create(user("jefe", "Admin1234", "ADMIN"));

		mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(user("jefe", null, "EMPLEADO")))
				.andExpect(status().isConflict());
	}

	// Arma el JSON de un usuario. password puede ir en null.
	private String user(String username, String password, String role) {
		return "{\"name\":\"Nombre\",\"lastName\":\"Apellido\",\"username\":\"" + username + "\""
				+ ",\"password\":" + (password == null ? "null" : "\"" + password + "\"")
				+ ",\"role\":\"" + role + "\"}";
	}

	// Da de alta un usuario y devuelve el id que le asigno la base.
	private long create(String json) throws Exception {
		String response = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}
}

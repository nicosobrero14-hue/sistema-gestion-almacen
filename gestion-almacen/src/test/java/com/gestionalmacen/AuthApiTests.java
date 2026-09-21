package com.gestionalmacen;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;
import com.gestionalmacen.repository.IUserRepository;

// Inicio y cierre de sesion (CU-01) y permisos por rol (RF-11).
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void loadUsers() {
		userRepository.deleteAll();
		userRepository.save(user("admin", "Admin1234", Role.ADMIN, true));
		userRepository.save(user("vendedor", "Empleado1234", Role.EMPLEADO, true));
		userRepository.save(user("retirado", "Retirado123", Role.EMPLEADO, false));
	}

	// ---------- CU-01: flujo principal ----------

	@Test
	void withoutASessionTheApiReturns401() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/suppliers")).andExpect(status().isUnauthorized());
	}

	// El estado del sistema es publico: sirve para verificar la instalacion.
	@Test
	void theStatusIsPublic() throws Exception {
		mvc.perform(get("/api/status")).andExpect(status().isOk());
	}

	@Test
	void theLoginReturnsTheUserWithoutThePassword() throws Exception {
		mvc.perform(login("admin", "Admin1234"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	// Despues del login, los pedidos siguientes entran sin volver a mandar la contraseña.
	@Test
	void theSessionIsKeptBetweenRequests() throws Exception {
		MockHttpSession session = (MockHttpSession) mvc.perform(login("vendedor", "Empleado1234"))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);

		mvc.perform(get("/api/products").session(session)).andExpect(status().isOk());
		mvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("vendedor"));
	}

	// El cierre de sesion la invalida en el servidor: aunque alguien guarde la cookie, ya no sirve.
	@Test
	void theLogoutInvalidatesTheSession() throws Exception {
		MockHttpSession session = (MockHttpSession) mvc.perform(login("admin", "Admin1234"))
				.andReturn().getRequest().getSession(false);

		mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
				.andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
	}

	// ---------- CU-01: excepciones ----------

	// CU-01 exc. 4a: credenciales incorrectas.
	@Test
	void aWrongPasswordReturns401() throws Exception {
		mvc.perform(login("admin", "EstaNoEs123"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
	}

	// Mismo mensaje que con la contraseña incorrecta, a proposito.
	@Test
	void anUnknownUserReturnsTheSameMessage() throws Exception {
		mvc.perform(login("noexiste", "Cualquiera123"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
	}

	// CU-01 exc. 3a: algun campo vacio.
	@Test
	void emptyFieldsReturn400() throws Exception {
		mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void anInactiveUserCannotLogIn() throws Exception {
		mvc.perform(login("retirado", "Retirado123"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("El usuario está dado de baja. Consulte con el administrador."));
	}

	// ---------- RF-11: permisos por rol ----------

	// Los usuarios son solo del administrador. Productos y proveedores son parte del trabajo del empleado.
	@Test
	@WithMockUser(roles = "EMPLEADO")
	void anEmployeeCannotAccessUsers() throws Exception {
		mvc.perform(get("/api/users")).andExpect(status().isForbidden());
		mvc.perform(get("/api/products")).andExpect(status().isOk());
		mvc.perform(get("/api/suppliers")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void anAdminCanAccessUsers() throws Exception {
		mvc.perform(get("/api/users")).andExpect(status().isOk());
	}

	private RequestBuilder login(String username, String password) {
		return post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
	}

	private User user(String username, String password, Role role, boolean active) {
		User user = new User();
		user.setName("Prueba");
		user.setLastName("Prueba");
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(role);
		user.setActive(active);
		return user;
	}
}

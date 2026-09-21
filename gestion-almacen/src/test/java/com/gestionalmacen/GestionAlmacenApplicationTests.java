package com.gestionalmacen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Arranque de la aplicacion y estado del sistema.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GestionAlmacenApplicationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void theApplicationStarts() {
		// Si el contexto no levanta, el test falla antes de llegar aca.
	}

	@Test
	void theStatusEndpointAnswers() throws Exception {
		mvc.perform(get("/api/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.application").value("ok"))
				.andExpect(jsonPath("$.database").value("ok"));
	}
}

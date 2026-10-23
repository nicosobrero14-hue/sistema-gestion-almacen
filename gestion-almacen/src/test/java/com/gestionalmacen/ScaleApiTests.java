package com.gestionalmacen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.gestionalmacen.service.ScaleMessage;

// Lectura del peso de la balanza (RF-12 / CU-21).
//
// Lo que se prueba de verdad es el armado del mensaje, que es la parte que se puede verificar
// sin tener la balanza enchufada y donde es mas facil equivocarse. El manejo del puerto serie
// se prueba recien con el equipo conectado.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithMockUser(username = "vendedor", roles = "EMPLEADO")
class ScaleApiTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private FakeScale scale;

	// Arma el mensaje tal como lo manda la balanza: byte de inicio, peso y retorno de carro.
	private byte[] message(String peso) {
		byte[] numero = peso.getBytes(StandardCharsets.US_ASCII);
		byte[] mensaje = new byte[numero.length + 2];
		mensaje[0] = ScaleMessage.BOT;
		System.arraycopy(numero, 0, mensaje, 1, numero.length);
		mensaje[mensaje.length - 1] = '\r';
		return mensaje;
	}

	// ---------- el mensaje de la balanza (manual, seccion 14.4) ----------

	@Test
	void readsTheWeightOfAMessage() {
		assertEquals(new BigDecimal("12.345"), ScaleMessage.weightOf(message("12.345")));
	}

	// 120 gramos, que es el caso que motivo toda la fase de los decimales.
	@Test
	void readsAWeightUnderOneKilo() {
		assertEquals(new BigDecimal("00.120"), ScaleMessage.weightOf(message("00.120")));
	}

	@Test
	void readsAnEmptyPlate() {
		assertEquals(new BigDecimal("00.000"), ScaleMessage.weightOf(message("00.000")));
	}

	// El peso puede venir con espacios de relleno adelante.
	@Test
	void ignoresThePaddingSpaces() {
		assertEquals(new BigDecimal("1.250"), ScaleMessage.weightOf(message("  1.250")));
	}

	// En los modos que ademas mandan precio e importe, el primer bloque sigue siendo el peso.
	@Test
	void readsTheWeightWhenPriceAndAmountAlsoCome() {
		byte[] completo = "\u00022.500\r0150.00\r00375.00\r".getBytes(StandardCharsets.ISO_8859_1);
		assertEquals(new BigDecimal("2.500"), ScaleMessage.weightOf(completo));
	}

	// Si antes del mensaje quedo basura en el puerto, se busca el byte de inicio.
	@Test
	void skipsTheGarbageBeforeTheStartByte() {
		byte[] conBasura = "xy\u00023.000\r".getBytes(StandardCharsets.ISO_8859_1);
		assertEquals(new BigDecimal("3.000"), ScaleMessage.weightOf(conBasura));
	}

	// ---------- lo que no es un peso valido ----------

	@Test
	void rejectsAMessageWithoutTheStartByte() {
		assertNull(ScaleMessage.weightOf("12.345\r".getBytes(StandardCharsets.US_ASCII)));
	}

	@Test
	void rejectsAMessageThatIsNotANumber() {
		assertNull(ScaleMessage.weightOf(message("ERROR")));
	}

	@Test
	void rejectsAnEmptyMessage() {
		assertNull(ScaleMessage.weightOf(new byte[0]));
		assertNull(ScaleMessage.weightOf(null));
	}

	// Un peso negativo es una balanza descalibrada o con tara puesta: mejor no inventar un numero.
	@Test
	void rejectsANegativeWeight() {
		assertNull(ScaleMessage.weightOf(message("-1.500")));
	}

	// ---------- el endpoint ----------

	@Test
	void answersTheWeightOnThePlate() throws Exception {
		scale.put("0.320");

		mvc.perform(get("/api/scale/weight"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.weight").value(0.320));
	}

	// Sin balanza responde 200 con el peso vacio y no un error: este equipo no tiene una
	// conectada, el peso se escribe a mano y el comercio sigue vendiendo (RNF-04).
	@Test
	void answersEmptyWhenThereIsNoScale() throws Exception {
		scale.put(null);

		mvc.perform(get("/api/scale/weight"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.weight").doesNotExist());
	}
}

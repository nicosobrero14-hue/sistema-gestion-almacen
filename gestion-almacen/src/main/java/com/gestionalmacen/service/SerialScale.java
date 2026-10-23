package com.gestionalmacen.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;

// Balanza Kretz Delta ECO 2 conectada por puerto serie (RF-12 / CU-21).
//
// Los parametros salen del manual, seccion 14.5, y no son los habituales: son 9600 baudios,
// 8 bits de datos, sin paridad y DOS bits de stop. Casi todas las librerias usan uno solo por
// defecto, y con ese valor los caracteres llegan con basura.
//
// El sistema le pide el peso y la balanza responde una vez (modo "a pedido", seccion 14.3.3).
// Se usa ese modo y no el continuo porque el peso se lee cuando el empleado lo necesita, no
// dos veces por segundo contra un puerto que puede no estar.
//
// @Profile("!test"): las pruebas usan una balanza falsa y no dependen de que haya uuna conectada.
@Service
@Profile("!test")
public class SerialScale implements IScale {

	private static final Logger log = LoggerFactory.getLogger(SerialScale.class);

	// Lo que se espera de vuelta: byte de inicio, seis caracteres de peso y el retorno de carro.
	private static final int RESPUESTA = 16;

	// El puerto donde esta la balanza, por ejemplo COM3. Vacio significa que no hay balanza
	// configurada en este equipo, que es lo normal mientras no se instale.
	@Value("${scale.port:}")
	private String port;

	@Value("${scale.timeout-ms:1500}")
	private int timeoutMs;

	@Override
	public BigDecimal readWeight() {
		if (port.isBlank()) {
			return null;
		}

		SerialPort puerto = SerialPort.getCommPort(port);
		puerto.setComPortParameters(9600, 8, SerialPort.TWO_STOP_BITS, SerialPort.NO_PARITY);
		puerto.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0);

		if (!puerto.openPort()) {
			// Con el cable desenchufado o el puerto tomado por otro programa. No es un error
			// del sistema: el empleado escribe el peso a mano y sigue vendiendo.
			log.warn("No se pudo abrir el puerto {} de la balanza", port);
			return null;
		}

		try {
			puerto.writeBytes(new byte[] { ScaleMessage.REQUEST }, 1);
			byte[] respuesta = new byte[RESPUESTA];
			int leidos = puerto.readBytes(respuesta, respuesta.length);
			if (leidos <= 0) {
				log.warn("La balanza no respondio en {}", port);
				return null;
			}

			byte[] recibido = new byte[leidos];
			System.arraycopy(respuesta, 0, recibido, 0, leidos);
			BigDecimal peso = ScaleMessage.weightOf(recibido);
			if (peso == null) {
				log.warn("La balanza respondio algo que no se entiende en {}", port);
			}
			return peso;
		} catch (RuntimeException e) {
			log.warn("Fallo la lectura de la balanza en {}: {}", port, e.getMessage());
			return null;
		} finally {
			puerto.closePort();
		}
	}
}

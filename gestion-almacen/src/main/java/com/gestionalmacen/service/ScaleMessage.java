package com.gestionalmacen.service;

import java.math.BigDecimal;

// Interpreta el mensaje que manda la balanza Kretz Delta ECO 2 (manual, seccion 14.4).
//
// En los modos de transmision de peso el mensaje es:
//
//     0x02   XX.XXX   0x0D
//      BOT    peso      CR
//
// Un byte de inicio, el peso neto en kilos con tres decimales, y un retorno de carro.
// En los modos que ademas mandan precio e importe vienen dos bloques mas separados por CR;
// el primero sigue siendo el peso, asi que se lee igual.
//
// Esta clase esta aparte del puerto serie a proposito: es la parte que se puede probar sin
// tener la balanza enchufada, que es justo donde es mas facil equivocarse.
public final class ScaleMessage {

	// Byte de inicio del mensaje.
	public static final byte BOT = 0x02;

	// Caracter de pedido de transmision. La balanza acepta P, p, W y w, y los cuatro
	// significan lo mismo (manual, seccion 14.3.3).
	public static final byte REQUEST = 'P';

	private ScaleMessage() {
	}

	// El peso del mensaje, o null si lo que llego no es un mensaje de peso valido.
	public static BigDecimal weightOf(byte[] received) {
		if (received == null) {
			return null;
		}

		// El inicio puede traer basura de antes: se busca el byte de inicio.
		int desde = -1;
		for (int i = 0; i < received.length; i++) {
			if (received[i] == BOT) {
				desde = i + 1;
				break;
			}
		}
		if (desde < 0) {
			return null;
		}

		StringBuilder numero = new StringBuilder();
		for (int i = desde; i < received.length; i++) {
			char caracter = (char) received[i];
			if (caracter == '\r' || caracter == '\n') {
				break;
			}
			// El peso puede venir con espacios de relleno adelante.
			if (caracter != ' ') {
				numero.append(caracter);
			}
		}

		try {
			BigDecimal peso = new BigDecimal(numero.toString());
			// Un peso negativo significa que la balanza esta descalibrada o con tara puesta:
			// no es una venta valida y conviene no inventar un numero.
			return peso.signum() < 0 ? null : peso;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

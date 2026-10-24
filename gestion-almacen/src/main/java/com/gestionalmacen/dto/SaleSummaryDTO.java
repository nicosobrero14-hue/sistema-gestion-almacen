package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

// Resumen del historial de ventas (CU-16 paso 6): cuantas se cobraron, cuanto entro y cuanto por
// cada forma de pago.
//
// Viaja aparte del listado porque corresponde a todas las ventas que cumplen el filtro y no a la
// pagina que se esta mirando. Sumar lo que llego a la pantalla daria el total de veinte ventas y
// no el del periodo, que es el numero con el que se controla la caja.
@Getter
public class SaleSummaryDTO {

	private long charged;

	private BigDecimal total;

	// La clave es el nombre de la forma de pago. Se completa aparte porque sale de otra consulta.
	@Setter
	private Map<String, BigDecimal> byMethod = new LinkedHashMap<>();

	public SaleSummaryDTO(Long charged, BigDecimal total) {
		this.charged = charged;
		this.total = total;
	}
}

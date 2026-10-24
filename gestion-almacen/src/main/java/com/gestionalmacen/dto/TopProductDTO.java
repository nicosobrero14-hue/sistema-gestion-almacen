package com.gestionalmacen.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Un producto de los que mas se vendieron en el periodo (RF-15).
//
// Se agrupa por el nombre que quedo en el renglon y no por el producto: un producto que se dio
// de baja igual tiene que figurar en lo vendido, porque la plata entro.
@Getter
@AllArgsConstructor
public class TopProductDTO {

	private String name;

	// Decimal: lo que se vende por peso se suma en kilos (RF-12).
	private BigDecimal quantity;

	private BigDecimal total;
}

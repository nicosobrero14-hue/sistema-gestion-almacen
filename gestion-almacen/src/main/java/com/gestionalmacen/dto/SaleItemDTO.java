package com.gestionalmacen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Un producto del carrito: cual y cuantos (CU-08). El precio no viaja: lo pone el servidor.
@Getter @Setter
public class SaleItemDTO {

	@NotNull(message = "Falta el producto")
	private Long productId;

	// CU-08 exc. 3a: la cantidad tiene que ser mayor a cero.
	// Decimal: en los productos que se venden por peso son kilos, 0,120 son 120 gramos (RF-12).
	@NotNull(message = "La cantidad es obligatoria")
	@DecimalMin(value = "0", inclusive = false, message = "La cantidad tiene que ser mayor a cero")
	@Digits(integer = 7, fraction = 3, message = "La cantidad admite hasta 3 decimales")
	private BigDecimal quantity;
}

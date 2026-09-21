package com.gestionalmacen.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al poner un producto en oferta (CU-19).
@Getter @Setter
public class OfferDTO {

	@NotNull(message = "El precio de oferta es obligatorio")
	@DecimalMin(value = "0.01", message = "El precio de oferta debe ser mayor a cero")
	@Digits(integer = 8, fraction = 2, message = "El precio de oferta admite hasta 8 enteros y 2 decimales")
	private BigDecimal offerPrice;
}

package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.util.List;

import com.gestionalmacen.entity.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al confirmar una venta (CU-11): el carrito, el descuento y la forma de pago.
@Getter @Setter
public class SaleDTO {

	// CU-11 exc. 1a: no se confirma una venta sin productos.
	// @Valid: valida tambien cada producto de la lista.
	@NotEmpty(message = "Agregue al menos un producto a la venta")
	@Valid
	private List<SaleItemDTO> items;

	// Descuento en pesos sobre el total (RF-03). Si no viene, es cero.
	@DecimalMin(value = "0", message = "El descuento no puede ser negativo")
	@Digits(integer = 8, fraction = 2, message = "El descuento admite hasta 8 enteros y 2 decimales")
	private BigDecimal discount;

	@NotNull(message = "Elija la forma de pago")
	private PaymentMethod paymentMethod;
}

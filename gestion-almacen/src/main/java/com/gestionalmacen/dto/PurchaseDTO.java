package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al cargar la factura de un proveedor (CU-23).
// Varios de estos campos los completa el lector al escanear el codigo QR del comprobante.
@Getter @Setter
public class PurchaseDTO {

	@NotNull(message = "El proveedor es obligatorio")
	private Long supplierId;

	// El que viene en el QR. Sirve para dejar constancia de quien emitio el comprobante.
	@Size(max = 13, message = "El CUIT no puede superar los 13 caracteres")
	private String issuerCuit;

	// Codigo de ARCA: 1 factura A, 6 factura B, 11 factura C.
	@NotNull(message = "El tipo de comprobante es obligatorio")
	@Min(value = 1, message = "El tipo de comprobante no es válido")
	private Integer voucherType;

	@NotNull(message = "El punto de venta es obligatorio")
	@Min(value = 1, message = "El punto de venta no es válido")
	private Integer salesPoint;

	@NotNull(message = "El número de comprobante es obligatorio")
	@Min(value = 1, message = "El número de comprobante no es válido")
	private Long number;

	// La del comprobante. No puede ser futura: una factura que todavia no se emitio no existe.
	@NotNull(message = "La fecha es obligatoria")
	@PastOrPresent(message = "La fecha no puede ser futura")
	private LocalDate date;

	@NotNull(message = "El total es obligatorio")
	@DecimalMin(value = "0.01", message = "El total debe ser mayor a cero")
	@Digits(integer = 10, fraction = 2, message = "El total admite hasta 10 enteros y 2 decimales")
	private BigDecimal invoicedTotal;

	// Lo que realmente se debe. Si no viene, es igual al facturado, que es el caso normal.
	@DecimalMin(value = "0.01", message = "El total acordado debe ser mayor a cero")
	@Digits(integer = 10, fraction = 2, message = "El total acordado admite hasta 10 enteros y 2 decimales")
	private BigDecimal agreedTotal;

	@Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
	private String adjustmentReason;

	@Size(max = 20, message = "El código de autorización no puede superar los 20 caracteres")
	private String authorizationCode;
}

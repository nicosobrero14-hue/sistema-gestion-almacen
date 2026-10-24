package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gestionalmacen.entity.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al registrar un pago a un proveedor (CU-24).
@Getter @Setter
public class SupplierPaymentDTO {

	// La del pago, que puede no ser la de hoy: se carga despues de haber pagado.
	@NotNull(message = "La fecha es obligatoria")
	@PastOrPresent(message = "La fecha no puede ser futura")
	private LocalDate date;

	@NotNull(message = "El monto es obligatorio")
	@DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
	@Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 enteros y 2 decimales")
	private BigDecimal amount;

	@NotNull(message = "La forma de pago es obligatoria")
	private PaymentMethod method;
}

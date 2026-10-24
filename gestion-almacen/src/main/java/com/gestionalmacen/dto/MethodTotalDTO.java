package com.gestionalmacen.dto;

import java.math.BigDecimal;

import com.gestionalmacen.entity.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Lo cobrado por una forma de pago, que es como se controla la caja contra el efectivo que hay
// en el cajon (CU-16 paso 6). Sale de agrupar los pagos de las ventas confirmadas.
@Getter
@AllArgsConstructor
public class MethodTotalDTO {

	private PaymentMethod method;

	private BigDecimal amount;
}

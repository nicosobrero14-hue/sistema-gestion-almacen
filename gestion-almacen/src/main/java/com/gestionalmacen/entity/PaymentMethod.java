package com.gestionalmacen.entity;

// Formas de pago que acepta el sistema (CU-09). La base tambien admite MERCADOPAGO, que no esta implementado.
public enum PaymentMethod {
	EFECTIVO,
	TRANSFERENCIA // el empleado verifica que la transferencia haya llegado antes de confirmar
}

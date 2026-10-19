package com.gestionalmacen.entity;

// Formas de pago que acepta el sistema (CU-09).
public enum PaymentMethod {
	EFECTIVO,
	TRANSFERENCIA, // el empleado verifica que la transferencia haya llegado antes de confirmar
	MERCADOPAGO // el cliente escanea el QR del mostrador y paga desde su telefono (CU-10)
}

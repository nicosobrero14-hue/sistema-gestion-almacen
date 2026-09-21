package com.gestionalmacen.entity;

// Estado de un pago. Los valores son los mismos que se guardan en la base.
public enum PaymentStatus {
	PENDIENTE,
	APROBADO,
	RECHAZADO,
	CANCELADO
}

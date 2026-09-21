package com.gestionalmacen.entity;

// Estado de una venta. Los valores son los mismos que se guardan en la base.
public enum SaleStatus {
	PENDIENTE_PAGO, // esperando un pago digital
	CONFIRMADA,     // cobrada: el stock ya se desconto
	CANCELADA       // el pago no se completo
}

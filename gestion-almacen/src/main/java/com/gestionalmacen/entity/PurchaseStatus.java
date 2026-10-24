package com.gestionalmacen.entity;

// Como esta una factura de proveedor respecto de lo que se le pago (RF-15).
// No se guarda en la base: se calcula con la suma de los pagos.
public enum PurchaseStatus {

	// Sin ningun pago registrado.
	PENDIENTE,

	// Se pago una parte y queda saldo.
	PARCIAL,

	// Se pago todo lo acordado.
	PAGADA
}

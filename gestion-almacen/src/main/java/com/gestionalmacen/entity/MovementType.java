package com.gestionalmacen.entity;

// Origen de cada cambio de stock (RF-02). Los valores son los mismos que se guardan en la base.
public enum MovementType {
	CARGA_INICIAL, // el stock con el que se da de alta el producto
	AJUSTE_MANUAL, // ingreso de mercaderia, rotura, perdida o correccion de un conteo
	VENTA          // descuento automatico al confirmar una venta (fase 5)
}

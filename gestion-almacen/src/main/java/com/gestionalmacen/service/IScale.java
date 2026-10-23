package com.gestionalmacen.service;

import java.math.BigDecimal;

// Lectura del peso desde la balanza (RF-12 / CU-21).
// Detras de una interfaz por lo mismo que la pasarela de pago: las pruebas no pueden depender
// de que haya una balanza conectada.
public interface IScale {

	// El peso neto en kilos, o null si no hay balanza configurada, no responde o lo que mando
	// no se entiende. Devuelve null en vez de fallar: sin balanza se escribe el peso a mano y
	// el comercio sigue vendiendo (RNF-04).
	BigDecimal readWeight();
}

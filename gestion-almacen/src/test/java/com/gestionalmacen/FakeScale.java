package com.gestionalmacen;

import java.math.BigDecimal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestionalmacen.service.IScale;

// Balanza de prueba: reemplaza a la de verdad para que las pruebas no dependan de que haya
// una conectada. Por defecto no hay balanza, que es el caso de cualquier equipo sin instalar.
@Service
@Profile("test")
public class FakeScale implements IScale {

	private BigDecimal weight;

	@Override
	public BigDecimal readWeight() {
		return weight;
	}

	// ---------- para las pruebas ----------

	// Simula que hay una balanza con ese peso arriba. null es "no hay balanza".
	public void put(String kilos) {
		weight = kilos == null ? null : new BigDecimal(kilos);
	}
}

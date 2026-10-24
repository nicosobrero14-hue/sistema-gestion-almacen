package com.gestionalmacen.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.service.IScale;

// Lectura del peso de la balanza (RF-12 / CU-21).
// La pantalla de venta la consulta mientras el empleado pesa, para mostrar el numero en vivo.
@RestController
@RequestMapping("/api/scale")
public class ScaleController {

	@Autowired
	private IScale scale;

	//1- el peso que hay ahora en la balanza
	// Responde 200 con el peso en null cuando no hay balanza: no es un error, es que este
	// equipo no tiene una conectada y el peso se escribe a mano. HashMap y no Map.of porque
	// este ultimo no admite valores nulos.
	@GetMapping("/weight")
	public Map<String, BigDecimal> readWeight() {
		Map<String, BigDecimal> respuesta = new HashMap<>();
		respuesta.put("weight", scale.readWeight());
		return respuesta;
	}
}

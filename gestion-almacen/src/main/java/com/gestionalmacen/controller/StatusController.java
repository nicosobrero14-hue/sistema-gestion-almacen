package com.gestionalmacen.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Estado del sistema: sirve para comprobar que el frontend llega al backend y el backend a la base.
@RestController
@RequestMapping("/api/status")
public class StatusController {

	@Autowired
	private JdbcTemplate jdbc;

	//1- estado de la aplicacion y de la base de datos
	@GetMapping
	public Map<String, String> getStatus() {
		Map<String, String> status = new HashMap<>();
		status.put("application", "ok");
		status.put("database", getDatabaseStatus());
		return status;
	}

	// Una consulta minima: si la base responde, la conexion esta bien.
	private String getDatabaseStatus() {
		try {
			jdbc.queryForObject("SELECT 1", Integer.class);
			return "ok";
		} catch (Exception e) {
			return "sin conexion";
		}
	}
}

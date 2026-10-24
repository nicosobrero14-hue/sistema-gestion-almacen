package com.gestionalmacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.dto.StatisticsDTO;
import com.gestionalmacen.service.IStatisticsService;

import jakarta.servlet.http.HttpServletRequest;

// API del panel principal (RF-15 / CU-06).
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

	@Autowired
	private IStatisticsService statisticsService;

	//1- los numeros del dia. Que numeros se devuelven depende del rol
	// isUserInRole: el rol sale de la sesion, no de lo que manda el cliente.
	@GetMapping
	public StatisticsDTO getStatistics(HttpServletRequest request) {
		return statisticsService.getStatistics(request.isUserInRole("ADMIN"));
	}
}

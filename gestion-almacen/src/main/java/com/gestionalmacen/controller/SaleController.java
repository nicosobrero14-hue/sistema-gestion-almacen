package com.gestionalmacen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.entity.Sale;
import com.gestionalmacen.service.ISaleService;

import jakarta.validation.Valid;

// API de ventas (RF-03, RF-06 / CU-07 a CU-15).
@RestController
@RequestMapping("/api/sales")
public class SaleController {

	@Autowired
	private ISaleService saleService;

	//1- confirmar una venta: la registra con su pago y descuenta el stock (CU-11)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Sale saveSale(@Valid @RequestBody SaleDTO saleDTO) {
		return saleService.saveSale(saleDTO);
	}

	//2- traer una venta con sus renglones y su pago: es lo que muestra el ticket (CU-14)
	@GetMapping("/{id}")
	public Sale findSale(@PathVariable Long id) {
		return saleService.findSale(id);
	}
}

package com.gestionalmacen.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestionalmacen.dto.OfferDTO;
import com.gestionalmacen.dto.ProductSalesDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.service.IOfferService;

import jakarta.validation.Valid;

// API de ofertas (RF-10 / CU-19). Solo el administrador.
// La sugerencia por vencimiento usa /api/stock/expiring, la misma consulta de la alerta.
@RestController
@RequestMapping("/api/offers")
public class OfferController {

	@Autowired
	private IOfferService offerService;

	//1- ofertas vigentes
	@GetMapping
	public List<Product> getOffers() {
		return offerService.getOffers();
	}

	//2- sugerencia por baja rotacion: los que menos se vendieron en los ultimos dias
	@GetMapping("/least-sold")
	public List<ProductSalesDTO> getLeastSoldProducts(@RequestParam(defaultValue = "30") int days) {
		return offerService.getLeastSoldProducts(days);
	}

	//3- poner un producto en oferta con su precio especial (CU-19 paso 4)
	@PatchMapping("/{productId}")
	public Product putOnOffer(@PathVariable Long productId, @Valid @RequestBody OfferDTO offerDTO) {
		return offerService.putOnOffer(productId, offerDTO);
	}

	//4- quitar la oferta (CU-19 paso 6)
	@PatchMapping("/{productId}/end")
	public Product endOffer(@PathVariable Long productId) {
		return offerService.endOffer(productId);
	}
}

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

import com.gestionalmacen.dto.StockAdjustmentDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.StockMovement;
import com.gestionalmacen.service.IStockService;

import jakarta.validation.Valid;

// API de stock y alertas (RF-02, RF-09 / CU-04, CU-06).
@RestController
@RequestMapping("/api/stock")
public class StockController {

	@Autowired
	private IStockService stockService;

	//1- ajustar el stock de un producto, con su motivo (CU-04)
	@PatchMapping("/{productId}")
	public Product adjustStock(@PathVariable Long productId, @Valid @RequestBody StockAdjustmentDTO adjustmentDTO) {
		return stockService.adjustStock(productId, adjustmentDTO);
	}

	//2- movimientos de un producto, del mas nuevo al mas viejo
	@GetMapping("/{productId}/movements")
	public List<StockMovement> getMovements(@PathVariable Long productId) {
		return stockService.getMovements(productId);
	}

	//3- alerta de stock bajo (CU-06)
	@GetMapping("/low-stock")
	public List<Product> getLowStockProducts() {
		return stockService.getLowStockProducts();
	}

	//4- alerta de vencimiento: los que vencen en los proximos dias (CU-06)
	@GetMapping("/expiring")
	public List<Product> getExpiringProducts(@RequestParam(defaultValue = "30") int days) {
		return stockService.getExpiringProducts(days);
	}
}

package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.StockAdjustmentDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.StockMovement;

// Control de stock y alertas (RF-02, RF-09 / CU-04, CU-06).
public interface IStockService {

	// Ajuste manual: cambia el stock y deja el movimiento registrado.
	Product adjustStock(Long productId, StockAdjustmentDTO adjustmentDTO);

	List<StockMovement> getMovements(Long productId);

	List<Product> getLowStockProducts();

	// Los que vencen dentro de los proximos dias, incluidos los ya vencidos.
	List<Product> getExpiringProducts(int days);
}

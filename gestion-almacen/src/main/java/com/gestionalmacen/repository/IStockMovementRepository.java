package com.gestionalmacen.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.StockMovement;

// Acceso a la tabla movimientos_stock.
@Repository
public interface IStockMovementRepository extends JpaRepository<StockMovement, Long> {

	// Los movimientos de un producto, del mas nuevo al mas viejo. El id crece con cada movimiento.
	List<StockMovement> findByProductIdOrderByIdDesc(Long productId);
}

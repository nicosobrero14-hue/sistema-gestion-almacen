package com.gestionalmacen.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.dto.StockAdjustmentDTO;
import com.gestionalmacen.entity.MovementType;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.StockMovement;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.repository.IProductRepository;
import com.gestionalmacen.repository.IStockMovementRepository;

@Service
public class StockService implements IStockService {

	@Autowired
	private IProductRepository productRepository;

	@Autowired
	private IStockMovementRepository movementRepository;

	// Para buscar el producto, con el mismo 404 que el resto de la aplicacion.
	@Autowired
	private IProductService productService;

	// Para saber quien hace el ajuste.
	@Autowired
	private IUserService userService;

	// @Transactional: el stock y su movimiento se guardan juntos. Si uno falla, no se guarda ninguno.
	@Override
	@Transactional
	public Product adjustStock(Long productId, StockAdjustmentDTO adjustmentDTO) {
		Product product = productService.findProduct(productId);

		if (!product.isActive()) {
			throw new BusinessRuleException("El producto " + product.getName() + " está dado de baja.");
		}
		// Un ajuste que no cambia nada solo ensuciaria el historial.
		if (adjustmentDTO.getNewStock() == product.getStock()) {
			throw new BusinessRuleException("El stock nuevo es igual al actual.");
		}

		int previousStock = product.getStock();
		product.setStock(adjustmentDTO.getNewStock());
		productRepository.save(product);

		movementRepository.save(new StockMovement(product, userService.getSessionUser(),
				MovementType.AJUSTE_MANUAL, previousStock, adjustmentDTO.getReason().trim()));
		return product;
	}

	@Override
	public List<StockMovement> getMovements(Long productId) {
		// Si el producto no existe, responde 404 en lugar de una lista vacia.
		productService.findProduct(productId);
		return movementRepository.findByProductIdOrderByIdDesc(productId);
	}

	@Override
	public List<Product> getLowStockProducts() {
		return productRepository.findLowStock();
	}

	@Override
	public List<Product> getExpiringProducts(int days) {
		return productRepository.findExpiring(LocalDate.now().plusDays(days));
	}
}

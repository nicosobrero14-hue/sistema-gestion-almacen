package com.gestionalmacen.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gestionalmacen.dto.OfferDTO;
import com.gestionalmacen.dto.ProductSalesDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.repository.IProductRepository;

@Service
public class OfferService implements IOfferService {

	@Autowired
	private IProductRepository productRepository;

	// Para buscar el producto, con el mismo 404 que el resto de la aplicacion.
	@Autowired
	private IProductService productService;

	@Override
	public List<Product> getOffers() {
		return productRepository.findByActiveTrueAndOnOfferTrueOrderByName();
	}

	@Override
	public List<ProductSalesDTO> getLeastSoldProducts(int days) {
		// El periodo empieza a las 0 horas de hace tantos dias.
		return productRepository.findLeastSold(LocalDate.now().minusDays(days).atStartOfDay());
	}

	@Override
	public Product putOnOffer(Long productId, OfferDTO offerDTO) {
		Product product = productService.findProduct(productId);

		if (!product.isActive()) {
			throw new BusinessRuleException("El producto " + product.getName() + " está dado de baja.");
		}
		// CU-19 exc. 4a: la oferta tiene que ser mas barata que el precio normal.
		if (offerDTO.getOfferPrice().compareTo(product.getPrice()) >= 0) {
			throw new BusinessRuleException("El precio de oferta tiene que ser menor al precio normal.");
		}

		product.setOnOffer(true);
		product.setOfferPrice(offerDTO.getOfferPrice());
		return productRepository.save(product);
	}

	// CU-19 paso 6: la oferta se puede quitar en cualquier momento. El producto vuelve a su precio normal.
	@Override
	public Product endOffer(Long productId) {
		Product product = productService.findProduct(productId);
		product.setOnOffer(false);
		product.setOfferPrice(null);
		return productRepository.save(product);
	}
}

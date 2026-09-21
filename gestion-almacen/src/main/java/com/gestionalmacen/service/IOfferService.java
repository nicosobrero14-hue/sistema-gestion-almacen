package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.OfferDTO;
import com.gestionalmacen.dto.ProductSalesDTO;
import com.gestionalmacen.entity.Product;

// Ofertas y sugerencias (RF-10 / CU-19). Las sugerencias no cambian nada: decide el administrador.
public interface IOfferService {

	// Los productos que estan en oferta ahora.
	List<Product> getOffers();

	// Los que menos se vendieron en los ultimos dias.
	List<ProductSalesDTO> getLeastSoldProducts(int days);

	Product putOnOffer(Long productId, OfferDTO offerDTO);

	Product endOffer(Long productId);
}

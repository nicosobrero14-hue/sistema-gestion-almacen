package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.entity.Product;

// Operaciones sobre el catalogo de productos (RF-01 / CU-02, CU-20).
public interface IProductService {

	List<Product> getProducts(String search, boolean activeOnly);

	Product findProduct(Long id);

	Product saveProduct(ProductDTO productDTO);

	Product editProduct(Long id, ProductDTO productDTO);

	void deactivateProduct(Long id);

	void activateProduct(Long id);
}

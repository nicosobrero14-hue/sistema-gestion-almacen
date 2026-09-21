package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.entity.Product;

// Operaciones sobre el catalogo de productos (RF-01 / CU-02, CU-20).
public interface IProductService {

	List<Product> getProducts(String search, boolean activeOnly);

	Product findProduct(Long id);

	// Busca por el codigo exacto, como lo manda el lector de codigo de barras (CU-03).
	Product findProductByBarcode(String barcode);

	// isAdmin: si quien lo hace es administrador. Solo el administrador maneja precios y ofertas.
	Product saveProduct(ProductDTO productDTO, boolean isAdmin);

	Product editProduct(Long id, ProductDTO productDTO, boolean isAdmin);

	void deactivateProduct(Long id);

	void activateProduct(Long id);
}

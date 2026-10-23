package com.gestionalmacen.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.dto.ProductSuggestionDTO;
import com.gestionalmacen.entity.Product;

// Operaciones sobre el catalogo de productos (RF-01 / CU-02, CU-20).
public interface IProductService {

	List<Product> getProducts(String search, boolean activeOnly);

	Product findProduct(Long id);

	// Los productos sin codigo de barras, que son los que no se pueden escanear (RF-14 / CU-07).
	List<Product> getProductsWithoutBarcode();

	// Los que se venden por peso, que son los que pasan por la balanza (RF-12 / CU-21).
	List<Product> getProductsByWeight();

	// Busca por el codigo exacto, como lo manda el lector de codigo de barras (CU-03).
	Product findProductByBarcode(String barcode);

	// CU-03 paso 6: datos sugeridos desde un catalogo publico para un codigo que no esta en el comercio.
	ProductSuggestionDTO suggestProduct(String barcode);

	// isAdmin: si quien lo hace es administrador. Solo el administrador maneja precios y ofertas.
	Product saveProduct(ProductDTO productDTO, boolean isAdmin);

	Product editProduct(Long id, ProductDTO productDTO, boolean isAdmin);

	// CU-22: la foto del producto. La puede cargar cualquiera que pueda editarlo.
	Product saveImage(Long id, MultipartFile file);

	void deleteImage(Long id);

	void deactivateProduct(Long id);

	void activateProduct(Long id);
}

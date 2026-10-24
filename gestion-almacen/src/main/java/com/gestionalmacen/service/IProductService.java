package com.gestionalmacen.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.dto.ProductSort;
import com.gestionalmacen.dto.ProductSuggestionDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.entity.SaleUnit;

// Operaciones sobre el catalogo de productos (RF-01 / CU-02, CU-20).
public interface IProductService {

	// RF-13: el catalogo se trae de a paginas y ordenado por la columna que elija la pantalla.
	// Con cientos de productos, traerlo entero no cumple el tiempo de respuesta del RNF-01.
	// lowStockOnly: solo los que estan en el minimo o por debajo. El filtro va en la consulta y
	// no en la pantalla porque con el listado paginado filtrar lo que ya llego esconde resultados.
	Page<Product> getProducts(String search, boolean activeOnly, boolean lowStockOnly, int page, int size,
			ProductSort sort, Sort.Direction direction);

	Product findProduct(Long id);

	// Los activos de una unidad de venta, para las pestanias de la pantalla de venta.
	// Los que se cuentan van en una y los que se pesan en la otra (RF-12 / RF-14).
	List<Product> getProductsBySaleUnit(SaleUnit saleUnit);

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

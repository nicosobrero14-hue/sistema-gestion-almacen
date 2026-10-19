package com.gestionalmacen.service;

import com.gestionalmacen.dto.ProductSuggestionDTO;

// Consulta de un codigo de barras en un catalogo publico de productos (CU-03).
// Detras de una interfaz para que las pruebas no salgan a internet.
public interface IProductCatalog {

	// Los datos del producto con ese codigo, o null si no esta en el catalogo o no se pudo consultar.
	// Devuelve null en vez de fallar: no poder sugerir no es un error, se carga a mano y listo.
	ProductSuggestionDTO findByBarcode(String barcode);
}

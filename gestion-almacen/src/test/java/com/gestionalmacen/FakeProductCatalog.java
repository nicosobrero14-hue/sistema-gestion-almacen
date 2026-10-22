package com.gestionalmacen;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestionalmacen.dto.ProductSuggestionDTO;
import com.gestionalmacen.service.IProductCatalog;

// Catalogo de prueba: reemplaza al catalogo publico para que las pruebas no salgan a internet.
// Por defecto no conoce ningun codigo, que es el caso mas comun en un almacen.
@Service
@Profile("test")
public class FakeProductCatalog implements IProductCatalog {

	private final Map<String, String> products = new HashMap<>();
	private final Map<String, String> images = new HashMap<>();

	@Override
	public ProductSuggestionDTO findByBarcode(String barcode) {
		String name = products.get(barcode);
		return name == null ? null : new ProductSuggestionDTO(barcode, name, images.get(barcode));
	}

	// ---------- para las pruebas ----------

	// Simula que el catalogo publico conoce ese codigo.
	public void add(String barcode, String name) {
		products.put(barcode, name);
	}

	// Lo mismo, pero la ficha del catalogo ademas trae una foto.
	public void add(String barcode, String name, String imageUrl) {
		products.put(barcode, name);
		images.put(barcode, imageUrl);
	}

	// El bean es unico para toda la ejecucion: cada prueba arranca sin lo que cargo la anterior.
	public void reset() {
		products.clear();
		images.clear();
	}
}

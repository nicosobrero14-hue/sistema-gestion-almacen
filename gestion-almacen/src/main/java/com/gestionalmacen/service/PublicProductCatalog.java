package com.gestionalmacen.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gestionalmacen.dto.ProductSuggestionDTO;

// Catalogos publicos de productos: se consulta el codigo de barras para sugerir los datos
// de un producto que el comercio todavia no tiene cargado (CU-03).
//
// Es una base abierta y gratuita, sin credenciales. La cobertura es parcial: los productos
// de marca suelen estar, lo que se vende suelto no. Por eso no sugerir es normal y no es un error.
//
// @Profile("!test"): las pruebas usan un catalogo falso y no dependen de internet.
@Service
@Profile("!test")
public class PublicProductCatalog implements IProductCatalog {

	private static final Logger log = LoggerFactory.getLogger(PublicProductCatalog.class);
	// Las bases del mismo proyecto, una por rubro. Un almacen vende de todas: la de alimentos
	// no conoce un desodorante y la de cosmetica no conoce una yerba. Se consultan en orden
	// y gana la primera que lo tenga.
	private static final String[] BASES = {
			"https://world.openfoodfacts.org",    // alimentos y bebidas
			"https://world.openbeautyfacts.org",  // higiene y cosmetica
			"https://world.openproductsfacts.org", // el resto
			"https://world.openpetfoodfacts.org", // alimento para mascotas
	};
	private static final ParameterizedTypeReference<Map<String, Object>> RESPUESTA = new ParameterizedTypeReference<>() {
	};

	@Override
	public ProductSuggestionDTO findByBarcode(String barcode) {
		for (String base : BASES) {
			ProductSuggestionDTO suggestion = this.search(base, barcode);
			if (suggestion != null) {
				return suggestion;
			}
		}
		return null;
	}

	private ProductSuggestionDTO search(String base, String barcode) {
		Map<String, Object> respuesta;
		try {
			respuesta = RestClient.builder()
					.baseUrl(base)
					// El catalogo pide identificar a quien consulta.
					.defaultHeader("User-Agent", "SistemaGestionAlmacen/1.0 (proyecto academico)")
					.build()
					.get()
					.uri("/api/v2/product/{barcode}.json?fields=product_name,brands,quantity", barcode)
					.retrieve()
					.body(RESPUESTA);
		} catch (RestClientException e) {
			// Sin internet o con una base caida se sigue con la que viene, y si no se carga a mano.
			log.warn("No se pudo consultar {}: {}", base, e.getMessage());
			return null;
		}

		// status 1 es "encontrado". Cualquier otra cosa es que ese codigo no esta cargado.
		if (respuesta == null || !Integer.valueOf(1).equals(respuesta.get("status"))) {
			return null;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> producto = (Map<String, Object>) respuesta.get("product");
		String name = this.buildName(producto);
		return name.isEmpty() ? null : new ProductSuggestionDTO(barcode, name);
	}

	// Arma un nombre presentable con lo que devuelve el catalogo, que viene en minusculas
	// y a veces repetido: marca "seven up" y nombre "seven" son el mismo producto.
	private String buildName(Map<String, Object> producto) {
		// El catalogo puede traer varias marcas separadas por coma: alcanza con la primera.
		String brand = this.clean(producto.get("brands")).split(",")[0].trim();
		String product = this.clean(producto.get("product_name"));
		String quantity = this.clean(producto.get("quantity"));

		String base;
		if (brand.isEmpty() || product.isEmpty()) {
			base = brand.isEmpty() ? product : brand;
		} else if (this.contains(brand, product)) {
			base = brand; // el nombre no agrega nada: "seven" dentro de "seven up"
		} else if (this.contains(product, brand)) {
			base = product;
		} else {
			base = brand + " " + product;
		}

		// Solo se capitaliza la marca y el nombre: el tamanio se deja como viene ("1.5 lts", "2,25 L").
		return (this.capitalize(base) + " " + quantity).trim();
	}

	// La coma no se toca: el tamanio puede venir como "2,25 L".
	private String clean(Object valor) {
		return valor == null ? "" : String.valueOf(valor).trim().replaceAll(" +", " ");
	}

	// Compara sin tildes, sin guiones y sin espacios: "Coca-Cola" y "coca cola" son lo mismo.
	private boolean contains(String mayor, String menor) {
		return this.simplify(mayor).contains(this.simplify(menor));
	}

	private String simplify(String texto) {
		return texto.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	private String capitalize(String texto) {
		StringBuilder salida = new StringBuilder();
		for (String palabra : texto.split(" ")) {
			if (palabra.isEmpty()) {
				continue;
			}
			if (!salida.isEmpty()) {
				salida.append(" ");
			}
			// Lo que ya trae mayusculas se respeta: "Coca-Cola" no pasa a "Coca-cola".
			salida.append(Character.isLetter(palabra.charAt(0)) && palabra.equals(palabra.toLowerCase())
					? Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1)
					: palabra);
		}
		return salida.toString();
	}
}

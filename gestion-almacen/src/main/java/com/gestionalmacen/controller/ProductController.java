package com.gestionalmacen.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.ProductDTO;
import com.gestionalmacen.dto.ProductSuggestionDTO;
import com.gestionalmacen.entity.Product;
import com.gestionalmacen.service.IProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// API de productos (RF-01 / CU-02, CU-20).
@RestController
@RequestMapping("/api/products")
public class ProductController {

	@Autowired
	private IProductService productService;

	//1- listar productos, con busqueda por nombre o codigo de barras
	@GetMapping
	public List<Product> getProducts(@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "true") boolean activeOnly) {
		return productService.getProducts(search, activeOnly);
	}

	//2- los productos que no se pueden escanear, para la grilla de la pantalla de venta (CU-07)
	// Va antes de /{id} no por el orden del archivo sino porque Spring prefiere la ruta literal.
	@GetMapping("/without-barcode")
	public List<Product> getProductsWithoutBarcode() {
		return productService.getProductsWithoutBarcode();
	}

	//3- los que se venden por peso, para la pestania de balanza de la venta (CU-21)
	@GetMapping("/by-weight")
	public List<Product> getProductsByWeight() {
		return productService.getProductsByWeight();
	}

	//4- traer un producto
	@GetMapping("/{id}")
	public Product findProduct(@PathVariable Long id) {
		return productService.findProduct(id);
	}

	//5- buscar por codigo de barras exacto: lo usa el lector (CU-03)
	@GetMapping("/barcode/{barcode}")
	public Product findProductByBarcode(@PathVariable String barcode) {
		return productService.findProductByBarcode(barcode);
	}

	//6- datos sugeridos para un codigo que no esta en el catalogo del comercio (CU-03 paso 6)
	@GetMapping("/suggestion/{barcode}")
	public ProductSuggestionDTO suggestProduct(@PathVariable String barcode) {
		return productService.suggestProduct(barcode);
	}

	//7- crear un producto
	// isUserInRole: el rol sale de la sesion, no de lo que manda el cliente.
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product saveProduct(@Valid @RequestBody ProductDTO productDTO, HttpServletRequest request) {
		return productService.saveProduct(productDTO, request.isUserInRole("ADMIN"));
	}

	//8- modificar un producto (CU-20). El stock no se modifica por aca
	@PutMapping("/{id}")
	public Product editProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO,
			HttpServletRequest request) {
		return productService.editProduct(id, productDTO, request.isUserInRole("ADMIN"));
	}

	//9- cargar la foto del producto (CU-22). Va aparte del alta: el nombre del archivo sale del id
	// del producto, asi que recien se puede guardar cuando el producto existe
	@PostMapping("/{id}/image")
	public Product saveImage(@PathVariable Long id, @RequestParam MultipartFile file) {
		return productService.saveImage(id, file);
	}

	//10- quitar la foto
	@DeleteMapping("/{id}/image")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteImage(@PathVariable Long id) {
		productService.deleteImage(id);
	}

	//11- dar de baja: deja de aparecer, pero no se borra
	@PatchMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateProduct(@PathVariable Long id) {
		productService.deactivateProduct(id);
	}

	//12- reactivar un producto dado de baja
	@PatchMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateProduct(@PathVariable Long id) {
		productService.activateProduct(id);
	}
}

package com.gestionalmacen.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.gestionalmacen.dto.ProductDTO;
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

	//2- traer un producto
	@GetMapping("/{id}")
	public Product findProduct(@PathVariable Long id) {
		return productService.findProduct(id);
	}

	//3- buscar por codigo de barras exacto: lo usa el lector (CU-03)
	@GetMapping("/barcode/{barcode}")
	public Product findProductByBarcode(@PathVariable String barcode) {
		return productService.findProductByBarcode(barcode);
	}

	//4- crear un producto
	// isUserInRole: el rol sale de la sesion, no de lo que manda el cliente.
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product saveProduct(@Valid @RequestBody ProductDTO productDTO, HttpServletRequest request) {
		return productService.saveProduct(productDTO, request.isUserInRole("ADMIN"));
	}

	//5- modificar un producto (CU-20). El stock no se modifica por aca
	@PutMapping("/{id}")
	public Product editProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO,
			HttpServletRequest request) {
		return productService.editProduct(id, productDTO, request.isUserInRole("ADMIN"));
	}

	//6- dar de baja: deja de aparecer, pero no se borra
	@PatchMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateProduct(@PathVariable Long id) {
		productService.deactivateProduct(id);
	}

	//7- reactivar un producto dado de baja
	@PatchMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateProduct(@PathVariable Long id) {
		productService.activateProduct(id);
	}
}

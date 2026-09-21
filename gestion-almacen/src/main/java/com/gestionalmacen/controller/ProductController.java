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

	//3- crear un producto
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product saveProduct(@Valid @RequestBody ProductDTO productDTO) {
		return productService.saveProduct(productDTO);
	}

	//4- modificar un producto (CU-20). El stock no se modifica por aca
	@PutMapping("/{id}")
	public Product editProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
		return productService.editProduct(id, productDTO);
	}

	//5- dar de baja: deja de aparecer, pero no se borra
	@PatchMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateProduct(@PathVariable Long id) {
		productService.deactivateProduct(id);
	}

	//6- reactivar un producto dado de baja
	@PatchMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateProduct(@PathVariable Long id) {
		productService.activateProduct(id);
	}
}

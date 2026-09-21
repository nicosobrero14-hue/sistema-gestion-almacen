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

import com.gestionalmacen.dto.SupplierDTO;
import com.gestionalmacen.entity.Supplier;
import com.gestionalmacen.service.ISupplierService;

import jakarta.validation.Valid;

// API de proveedores (RF-08 / CU-05).
// @Valid revisa las anotaciones del DTO; si algo no cumple, responde 400 sin entrar al metodo.
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

	@Autowired
	private ISupplierService supplierService;

	//1- listar proveedores, con busqueda por nombre o apellido
	@GetMapping
	public List<Supplier> getSuppliers(@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "true") boolean activeOnly) {
		return supplierService.getSuppliers(search, activeOnly);
	}

	//2- traer un proveedor
	@GetMapping("/{id}")
	public Supplier findSupplier(@PathVariable Long id) {
		return supplierService.findSupplier(id);
	}

	//3- crear un proveedor
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Supplier saveSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
		return supplierService.saveSupplier(supplierDTO);
	}

	//4- modificar un proveedor
	@PutMapping("/{id}")
	public Supplier editSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO supplierDTO) {
		return supplierService.editSupplier(id, supplierDTO);
	}

	//5- dar de baja: deja de aparecer, pero no se borra
	@PatchMapping("/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateSupplier(@PathVariable Long id) {
		supplierService.deactivateSupplier(id);
	}

	//6- reactivar un proveedor dado de baja
	@PatchMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateSupplier(@PathVariable Long id) {
		supplierService.activateSupplier(id);
	}
}

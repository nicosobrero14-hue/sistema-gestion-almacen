package com.gestionalmacen.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.PurchaseDTO;
import com.gestionalmacen.entity.Purchase;
import com.gestionalmacen.service.IPurchaseService;

import jakarta.validation.Valid;

// API de compras a proveedores (RF-15 / CU-23).
// Cargar una factura lo puede hacer el Empleado, porque conviene sacarle la foto cuando llega el
// reparto. Consultarlas es del Administrador: ahi se ven los saldos. Eso se define en SecurityConfig.
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

	@Autowired
	private IPurchaseService purchaseService;

	//1- las compras de un periodo, opcionalmente de un proveedor
	@GetMapping
	public List<Purchase> getPurchases(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long supplierId) {
		return purchaseService.getPurchases(from, to, supplierId);
	}

	//2- traer una compra
	@GetMapping("/{id}")
	public Purchase findPurchase(@PathVariable Long id) {
		return purchaseService.findPurchase(id);
	}

	//3- cargar la factura de un proveedor
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Purchase savePurchase(@Valid @RequestBody PurchaseDTO purchaseDTO) {
		return purchaseService.savePurchase(purchaseDTO);
	}

	//4- la foto del comprobante. Va aparte del alta: el nombre del archivo sale del id de la compra
	@PostMapping("/{id}/image")
	public Purchase saveImage(@PathVariable Long id, @RequestParam MultipartFile file) {
		return purchaseService.saveImage(id, file);
	}

	//5- quitar la foto
	@DeleteMapping("/{id}/image")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteImage(@PathVariable Long id) {
		purchaseService.deleteImage(id);
	}
}

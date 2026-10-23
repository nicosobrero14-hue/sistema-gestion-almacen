package com.gestionalmacen.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gestionalmacen.dto.PurchaseDTO;
import com.gestionalmacen.dto.SupplierPaymentDTO;
import com.gestionalmacen.entity.Purchase;

// Facturas de los proveedores (RF-15 / CU-23).
public interface IPurchaseService {

	// Las compras de un periodo. supplierId es opcional: si es null, trae las de todos.
	List<Purchase> getPurchases(LocalDate from, LocalDate to, Long supplierId);

	Purchase findPurchase(Long id);

	Purchase savePurchase(PurchaseDTO purchaseDTO);

	// La foto del comprobante. Va aparte del alta: el nombre del archivo sale del id.
	Purchase saveImage(Long id, MultipartFile file);

	void deleteImage(Long id);

	// CU-24: registrar lo que se le pago al proveedor. Devuelve la factura con el saldo al dia.
	Purchase addPayment(Long id, SupplierPaymentDTO paymentDTO);

	// Un pago no se borra, se anula: asi queda el rastro de que estuvo registrado.
	Purchase cancelPayment(Long id, Long paymentId);
}

package com.gestionalmacen.service;

import java.util.List;

import com.gestionalmacen.dto.SupplierDTO;
import com.gestionalmacen.entity.Supplier;

// Operaciones sobre proveedores (RF-08 / CU-05).
public interface ISupplierService {

	List<Supplier> getSuppliers(String search, boolean activeOnly);

	Supplier findSupplier(Long id);

	Supplier saveSupplier(SupplierDTO supplierDTO);

	Supplier editSupplier(Long id, SupplierDTO supplierDTO);

	void deactivateSupplier(Long id);

	void activateSupplier(Long id);
}

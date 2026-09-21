package com.gestionalmacen.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gestionalmacen.dto.SupplierDTO;
import com.gestionalmacen.entity.Supplier;
import com.gestionalmacen.exception.BusinessRuleException;
import com.gestionalmacen.exception.NotFoundException;
import com.gestionalmacen.repository.ISupplierRepository;

@Service
public class SupplierService implements ISupplierService {

	@Autowired
	private ISupplierRepository supplierRepository;

	@Override
	public List<Supplier> getSuppliers(String search, boolean activeOnly) {
		return supplierRepository.search(search.trim(), activeOnly);
	}

	@Override
	public Supplier findSupplier(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el proveedor con id " + id));
	}

	@Override
	public Supplier saveSupplier(SupplierDTO supplierDTO) {
		// Se controla antes de guardar para dar un mensaje claro, en vez de que falle la base.
		if (supplierDTO.getEmail() != null && supplierRepository.existsByEmail(supplierDTO.getEmail())) {
			throw new BusinessRuleException("Ya existe un proveedor con el email " + supplierDTO.getEmail());
		}

		Supplier supplier = new Supplier();
		this.copyData(supplierDTO, supplier);
		return supplierRepository.save(supplier);
	}

	@Override
	public Supplier editSupplier(Long id, SupplierDTO supplierDTO) {
		Supplier supplier = this.findSupplier(id);

		if (supplierDTO.getEmail() != null && supplierRepository.existsByEmailAndIdNot(supplierDTO.getEmail(), id)) {
			throw new BusinessRuleException("Ya existe otro proveedor con el email " + supplierDTO.getEmail());
		}

		this.copyData(supplierDTO, supplier);
		return supplierRepository.save(supplier);
	}

	@Override
	public void deactivateSupplier(Long id) {
		Supplier supplier = this.findSupplier(id);
		supplier.setActive(false);
		supplierRepository.save(supplier);
	}

	@Override
	public void activateSupplier(Long id) {
		Supplier supplier = this.findSupplier(id);
		supplier.setActive(true);
		supplierRepository.save(supplier);
	}

	// Pasa los datos del DTO a la entidad. Lo usan el alta y la modificacion.
	private void copyData(SupplierDTO supplierDTO, Supplier supplier) {
		supplier.setName(supplierDTO.getName());
		supplier.setLastName(supplierDTO.getLastName());
		supplier.setEmail(supplierDTO.getEmail());
		supplier.setPhone(supplierDTO.getPhone());
		supplier.setAddress(supplierDTO.getAddress());
	}
}

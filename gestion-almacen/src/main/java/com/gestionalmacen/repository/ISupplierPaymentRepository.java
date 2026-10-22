package com.gestionalmacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.SupplierPayment;

// Acceso a la tabla pagos_proveedor (RF-15 / CU-24).
@Repository
public interface ISupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
}

package com.gestionalmacen.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.SupplierPayment;

// Acceso a la tabla pagos_proveedor (RF-15 / CU-24).
@Repository
public interface ISupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

	// Todo lo que se le pago a los proveedores. Los pagos anulados no cuentan: un pago no se
	// borra, se anula, igual que una venta no se borra sino que se cancela.
	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM SupplierPayment p WHERE p.cancelled = FALSE")
	BigDecimal totalPaid();
}

package com.gestionalmacen.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Purchase;

// Acceso a la tabla compras (RF-15).
@Repository
public interface IPurchaseRepository extends JpaRepository<Purchase, Long> {

	// El mismo comprobante no se puede cargar dos veces. La base tambien lo impide, pero
	// conviene avisarlo con un mensaje claro antes de que salte la restriccion.
	boolean existsBySupplierIdAndVoucherTypeAndSalesPointAndNumber(
			Long supplierId, Integer voucherType, Integer salesPoint, Long number);

	// Las compras de un periodo, opcionalmente de un proveedor. La mas reciente primero.
	@Query("""
			SELECT c FROM Purchase c
			WHERE c.date BETWEEN :from AND :to
			  AND (:supplierId IS NULL OR c.supplier.id = :supplierId)
			ORDER BY c.date DESC, c.id DESC
			""")
	List<Purchase> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("supplierId") Long supplierId);

	// Lo que se gasto en mercaderia en el periodo. Cuenta el total acordado, que es lo que
	// realmente se debe, y no el facturado. Devuelve null si no hubo compras.
	@Query("SELECT SUM(c.agreedTotal) FROM Purchase c WHERE c.date BETWEEN :from AND :to")
	BigDecimal totalForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

	// Todo lo comprado hasta hoy. Con lo pagado, da lo que se le debe a los proveedores (RF-15).
	@Query("SELECT COALESCE(SUM(c.agreedTotal), 0) FROM Purchase c")
	BigDecimal totalPurchased();
}

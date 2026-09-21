package com.gestionalmacen.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Sale;

// Acceso a la tabla ventas. Los renglones y el pago se guardan junto con la venta.
@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long> {

	// Historial (CU-16): las ventas del periodo, de la mas nueva a la mas vieja.
	// Empleado y producto son opcionales: vacios, no filtran. El producto se busca en los renglones de la venta.
	// JOIN FETCH: trae los renglones y el pago en la misma consulta, en lugar de hacer una consulta por venta.
	// Dos ventas del mismo segundo se ordenan por numero: la base guarda la hora sin fracciones de segundo.
	@Query("""
			SELECT s FROM Sale s
			LEFT JOIN FETCH s.details
			LEFT JOIN FETCH s.payments
			WHERE s.dateTime >= :from AND s.dateTime < :to
			  AND (:username = '' OR s.username = :username)
			  AND (:product = '' OR EXISTS (
			        SELECT d FROM SaleDetail d
			        WHERE d.sale = s AND LOWER(d.productName) LIKE LOWER(CONCAT('%', :product, '%'))))
			ORDER BY s.dateTime DESC, s.id DESC
			""")
	List<Sale> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
			@Param("username") String username, @Param("product") String product);
}

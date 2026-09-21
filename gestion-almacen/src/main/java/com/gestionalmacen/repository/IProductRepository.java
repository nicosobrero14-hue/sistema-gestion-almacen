package com.gestionalmacen.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Product;

// Acceso a la tabla productos.
@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

	// Busca por nombre o codigo de barras (RF-01). Misma idea que la busqueda de proveedores.
	@Query("""
			SELECT p FROM Product p
			WHERE (:activeOnly = FALSE OR p.active = TRUE)
			  AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :text, '%'))
			       OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :text, '%')))
			ORDER BY p.name
			""")
	List<Product> search(@Param("text") String text, @Param("activeOnly") boolean activeOnly);

	// Alerta de stock bajo (RF-09): productos activos con el stock en el minimo o por debajo.
	@Query("""
			SELECT p FROM Product p
			WHERE p.active = TRUE AND p.stock <= p.minimumStock
			ORDER BY p.stock, p.name
			""")
	List<Product> findLowStock();

	// Alerta de vencimiento (RF-09): productos activos, con stock, que vencen hasta la fecha limite.
	// Incluye los que ya vencieron. Los que no tienen fecha no entran: la comparacion con NULL da falso.
	@Query("""
			SELECT p FROM Product p
			WHERE p.active = TRUE AND p.stock > 0 AND p.expirationDate <= :limit
			ORDER BY p.expirationDate, p.name
			""")
	List<Product> findExpiring(@Param("limit") LocalDate limit);

	// El producto con ese codigo exacto. Lo usa el lector de codigo de barras (CU-03).
	Optional<Product> findByBarcode(String barcode);

	boolean existsByBarcode(String barcode);

	// Igual que el anterior, pero sin contar al producto que se esta editando.
	boolean existsByBarcodeAndIdNot(String barcode, Long id);
}

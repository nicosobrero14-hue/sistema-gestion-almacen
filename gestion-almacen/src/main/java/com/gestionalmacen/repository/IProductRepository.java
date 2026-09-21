package com.gestionalmacen.repository;

import java.util.List;

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

	boolean existsByBarcode(String barcode);

	// Igual que el anterior, pero sin contar al producto que se esta editando.
	boolean existsByBarcodeAndIdNot(String barcode, Long id);
}

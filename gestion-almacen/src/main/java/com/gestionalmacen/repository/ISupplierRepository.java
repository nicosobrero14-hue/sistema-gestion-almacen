package com.gestionalmacen.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Supplier;

// Acceso a la tabla proveedores. findById, save y el resto ya vienen con JpaRepository.
@Repository
public interface ISupplierRepository extends JpaRepository<Supplier, Long> {

	// Busca por nombre o apellido. Con texto vacio trae todos; con activeOnly en false incluye los dados de baja.
	@Query("""
			SELECT s FROM Supplier s
			WHERE (:activeOnly = FALSE OR s.active = TRUE)
			  AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :text, '%'))
			       OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :text, '%')))
			ORDER BY s.name
			""")
	List<Supplier> search(@Param("text") String text, @Param("activeOnly") boolean activeOnly);

	boolean existsByEmail(String email);

	// Igual que el anterior, pero sin contar al proveedor que se esta editando.
	boolean existsByEmailAndIdNot(String email, Long id);
}

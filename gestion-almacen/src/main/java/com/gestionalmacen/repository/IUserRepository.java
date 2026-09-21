package com.gestionalmacen.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.entity.Role;
import com.gestionalmacen.entity.User;

// Acceso a la tabla usuarios.
@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

	// Busca por nombre, apellido o nombre de usuario.
	@Query("""
			SELECT u FROM User u
			WHERE (:activeOnly = FALSE OR u.active = TRUE)
			  AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :text, '%'))
			       OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :text, '%'))
			       OR LOWER(u.username) LIKE LOWER(CONCAT('%', :text, '%')))
			ORDER BY u.username
			""")
	List<User> search(@Param("text") String text, @Param("activeOnly") boolean activeOnly);

	// Lo usa el inicio de sesion (CU-01).
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	// Igual que el anterior, pero sin contar al usuario que se esta editando.
	boolean existsByUsernameAndIdNot(String username, Long id);

	// Para no dejar el sistema sin ningun administrador activo (CU-18).
	long countByRoleAndActiveTrue(Role role);
}

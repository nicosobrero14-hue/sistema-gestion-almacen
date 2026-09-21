package com.gestionalmacen.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Proveedor del comercio (RF-08).
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "proveedores")
public class Supplier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_proveedor")
	private Long id;

	@Column(name = "nombre", nullable = false, length = 80)
	private String name;

	// Vacio cuando el proveedor es una empresa.
	@Column(name = "apellido", length = 80)
	private String lastName;

	@Column(name = "email", length = 120, unique = true)
	private String email;

	@Column(name = "telefono", length = 30)
	private String phone;

	@Column(name = "direccion", length = 150)
	private String address;

	// Baja logica: en false deja de aparecer, pero no se borra.
	@Column(name = "activo", nullable = false)
	private boolean active = true;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDateTime createdAt;

	// Se ejecuta antes de guardarlo por primera vez.
	@PrePersist
	private void onCreate() {
		createdAt = LocalDateTime.now();
	}
}

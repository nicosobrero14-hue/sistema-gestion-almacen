package com.gestionalmacen.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Usuario del sistema (RF-11).
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "usuarios")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long id;

	@Column(name = "nombre", nullable = false, length = 80)
	private String name;

	@Column(name = "apellido", nullable = false, length = 80)
	private String lastName;

	@Column(name = "username", nullable = false, length = 50, unique = true)
	private String username;

	// Hash BCrypt, nunca la contraseña en texto plano. @JsonIgnore evita que salga por la API.
	@JsonIgnore
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	// Se guarda el nombre del rol ("ADMIN") y no su posicion en el enum.
	@Enumerated(EnumType.STRING)
	@Column(name = "rol", nullable = false, length = 20)
	private Role role;

	// Baja logica: un usuario dado de baja no puede entrar al sistema.
	@Column(name = "activo", nullable = false)
	private boolean active = true;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	private void onCreate() {
		createdAt = LocalDateTime.now();
	}
}

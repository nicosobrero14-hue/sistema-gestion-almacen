package com.gestionalmacen.dto;

import com.gestionalmacen.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al crear o modificar un usuario (CU-18).
@Getter @Setter
public class UserDTO {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
	private String name;

	@NotBlank(message = "El apellido es obligatorio")
	@Size(max = 80, message = "El apellido no puede superar los 80 caracteres")
	private String lastName;

	@NotBlank(message = "El nombre de usuario es obligatorio")
	@Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
	@Pattern(regexp = "^[a-zA-Z0-9._-]+$",
			message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
	private String username;

	// Sin @NotBlank: al modificar viene vacia para conservar la actual.
	// RNF-02: de 8 a 72 caracteres, con al menos una letra y un numero.
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
			message = "La contraseña debe tener al menos 8 caracteres e incluir letras y números")
	private String password;

	@NotNull(message = "El rol es obligatorio")
	private Role role;
}

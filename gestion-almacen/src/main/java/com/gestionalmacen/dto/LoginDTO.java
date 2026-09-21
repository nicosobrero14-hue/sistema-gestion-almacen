package com.gestionalmacen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Usuario y contraseña del inicio de sesion (CU-01).
// CU-01 exc. 3a: si algun campo esta vacio, se avisa que los dos son obligatorios.
@Getter @Setter
public class LoginDTO {

	@NotBlank(message = "El nombre de usuario es obligatorio")
	private String username;

	@NotBlank(message = "La contraseña es obligatoria")
	private String password;
}

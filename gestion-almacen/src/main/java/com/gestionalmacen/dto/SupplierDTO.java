package com.gestionalmacen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan al crear o modificar un proveedor (CU-05). Solo el nombre es obligatorio.
@Getter @Setter
public class SupplierDTO {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
	private String name;

	@Size(max = 80, message = "El apellido no puede superar los 80 caracteres")
	private String lastName;

	@Email(message = "El email no tiene un formato válido")
	@Size(max = 120, message = "El email no puede superar los 120 caracteres")
	private String email;

	@Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
	private String phone;

	@Size(max = 150, message = "La dirección no puede superar los 150 caracteres")
	private String address;
}

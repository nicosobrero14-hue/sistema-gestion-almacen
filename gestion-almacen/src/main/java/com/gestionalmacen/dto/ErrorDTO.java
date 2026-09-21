package com.gestionalmacen.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Formato de todos los errores de la API: un mensaje y, si corresponde, el error de cada campo.
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDTO {

	private String message;

	// campo -> motivo. Por ejemplo: "price" -> "El precio debe ser mayor a cero"
	private Map<String, String> errors;

	public ErrorDTO(String message) {
		this.message = message;
	}
}

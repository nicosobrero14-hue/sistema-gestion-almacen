package com.gestionalmacen.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Datos sugeridos para un producto que todavia no esta en el catalogo del comercio (CU-03).
// Es una sugerencia, no un dato del sistema: la carga la gente en un catalogo publico y
// suele venir desprolija, asi que el empleado la revisa antes de guardar.
@Getter
@AllArgsConstructor
public class ProductSuggestionDTO {

	private String barcode;

	// Nombre armado con la marca, el nombre y el tamanio que devuelve el catalogo.
	private String name;

	// Enlace a la foto del frente del producto en el catalogo. Puede venir vacio: hay fichas
	// cargadas sin foto. El sistema la descarga al dar de alta, no se queda con el enlace (RF-12).
	private String imageUrl;
}

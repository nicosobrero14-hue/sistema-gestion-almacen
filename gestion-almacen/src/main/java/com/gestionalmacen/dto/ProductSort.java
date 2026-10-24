package com.gestionalmacen.dto;

import lombok.Getter;

// Las columnas por las que se puede ordenar el catalogo (RF-13).
//
// Es un enum y no un texto libre a proposito: el valor llega de afuera y termina adentro de la
// consulta, asi que solo pueden pedirse las columnas que estan aca. Una que no exista la rechaza
// Spring antes de llegar a la base y la pantalla recibe un error claro.
@Getter
public enum ProductSort {

	NOMBRE("name"),

	PRECIO("price"),

	STOCK("stock"),

	CODIGO("barcode");

	// El nombre del campo en la entidad, que es por el que ordena la consulta.
	private final String field;

	ProductSort(String field) {
		this.field = field;
	}
}

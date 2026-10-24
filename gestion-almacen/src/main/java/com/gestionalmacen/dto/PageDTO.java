package com.gestionalmacen.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

// Una pagina de resultados, con lo que necesitan los controles de la pantalla (RF-13).
//
// Es un DTO propio y no la pagina de Spring Data porque esa trae mucho detalle que la pantalla
// no usa y su formato cambio entre versiones. Viaja lo justo: el contenido, en que pagina
// estamos, de que tamanio y cuantas hay en total.
@Getter
public class PageDTO<T> {

	private List<T> content;

	private int page;

	private int size;

	private long totalElements;

	private int totalPages;

	public PageDTO(Page<T> page) {
		this.content = page.getContent();
		this.page = page.getNumber();
		this.size = page.getSize();
		this.totalElements = page.getTotalElements();
		this.totalPages = page.getTotalPages();
	}
}

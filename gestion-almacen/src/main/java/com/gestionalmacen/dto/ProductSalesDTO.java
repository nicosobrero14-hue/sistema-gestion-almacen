package com.gestionalmacen.dto;

import com.gestionalmacen.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Un producto y cuantas unidades se vendieron en un periodo. Es la sugerencia por baja rotacion (RF-10).
// Es un DTO de salida porque las unidades vendidas no son un dato del producto: se calculan.
@Getter
@AllArgsConstructor
public class ProductSalesDTO {

	private Product product;

	private Long unitsSold;
}

package com.gestionalmacen.dto;

import java.math.BigDecimal;

import com.gestionalmacen.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Un producto y cuanto se vendio de el en un periodo. Es la sugerencia por baja rotacion (RF-10).
// Es un DTO de salida porque lo vendido no es un dato del producto: se calcula.
// Decimal y no entero: lo que se vende por peso se suma en kilos (RF-12).
@Getter
@AllArgsConstructor
public class ProductSalesDTO {

	private Product product;

	private BigDecimal unitsSold;
}

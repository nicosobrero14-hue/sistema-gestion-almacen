package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

// Los numeros del panel principal (RF-15 / CU-06).
//
// No todos los ve todo el mundo. El empleado ve lo cobrado en el dia y como se reparte por forma
// de pago, que es lo que necesita para cerrar su caja. La ganancia y la deuda a proveedores son
// del comercio, no del turno, asi que las ve el administrador.
//
// Los campos que el empleado no ve quedan en null y no salen en el JSON: no es que la pantalla
// los esconde, es que nunca se envian.
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticsDTO {

	// ---------- lo que ve cualquiera ----------

	private BigDecimal soldToday;

	private long ticketsToday;

	// Lo que gasta en promedio cada cliente. Null si todavia no se vendio nada hoy.
	private BigDecimal averageTicket;

	private Map<String, BigDecimal> byMethod;

	// ---------- solo el administrador ----------

	// Lo vendido menos lo que costo esa mercaderia. No incluye los renglones sin costo cargado.
	private BigDecimal profitToday;

	// Cuantos renglones de hoy quedaron afuera de la ganancia por no tener costo. Con esto la
	// pantalla puede avisar que falta cargar precios de costo en vez de mostrar un numero flojo.
	private Long linesWithoutCost;

	private BigDecimal monthPurchases;

	private BigDecimal supplierDebt;

	// Lo vendido cada dia de las ultimas dos semanas, para el grafico.
	private List<DaySalesDTO> lastDays;

	private List<TopProductDTO> topProducts;
}

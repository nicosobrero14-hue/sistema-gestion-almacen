package com.gestionalmacen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Lo vendido en un dia. Es cada barra del grafico del panel principal (RF-15).
@Getter
@AllArgsConstructor
public class DaySalesDTO {

	private LocalDate date;

	private BigDecimal total;
}

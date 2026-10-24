package com.gestionalmacen.service;

import com.gestionalmacen.dto.StatisticsDTO;

// Los numeros del panel principal (RF-15 / CU-06).
public interface IStatisticsService {

	// isAdmin: si quien mira es administrador. La ganancia y la deuda a proveedores son del
	// comercio y no del turno, asi que al empleado no se le envian.
	StatisticsDTO getStatistics(boolean isAdmin);
}

package com.gestionalmacen.service;

import java.time.LocalDate;
import java.util.List;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.entity.Sale;

// Ventas (RF-03, RF-06 / CU-07 a CU-15).
public interface ISaleService {

	// Registra la venta con sus renglones y su pago, y descuenta el stock.
	Sale saveSale(SaleDTO saleDTO);

	Sale findSale(Long id);

	// Historial (CU-16): las ventas entre dos fechas, incluidas las dos. Empleado y producto vacios no filtran.
	List<Sale> getSales(LocalDate from, LocalDate to, String username, String product);
}

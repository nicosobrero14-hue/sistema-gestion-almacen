package com.gestionalmacen.service;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.entity.Sale;

// Ventas (RF-03, RF-06 / CU-07 a CU-15).
public interface ISaleService {

	// Registra la venta con sus renglones y su pago, y descuenta el stock.
	Sale saveSale(SaleDTO saleDTO);

	Sale findSale(Long id);
}

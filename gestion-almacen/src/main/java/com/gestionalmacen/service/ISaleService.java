package com.gestionalmacen.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.gestionalmacen.dto.SaleDTO;
import com.gestionalmacen.dto.SaleSummaryDTO;
import com.gestionalmacen.entity.Sale;

// Ventas (RF-03, RF-06 / CU-07 a CU-15).
public interface ISaleService {

	// Registra la venta con sus renglones y su pago, y descuenta el stock.
	Sale saveSale(SaleDTO saleDTO);

	Sale findSale(Long id);

	// Le pregunta a MercadoPago como viene el cobro y resuelve la venta si ya se definio (CU-10).
	Sale checkPayment(Long id);

	// Da de baja el cobro y cancela la venta (CU-10 exc. 5a).
	Sale cancelPayment(Long id);

	// Historial (CU-16): las ventas entre dos fechas, incluidas las dos.
	// Empleado, producto y numero de operacion vacios no filtran.
	// RF-13: el historial se trae de a paginas, siempre de la venta mas nueva a la mas vieja.
	Page<Sale> getSales(LocalDate from, LocalDate to, String username, String product, String reference,
			int page, int size);

	// CU-16 paso 6: lo cobrado en el periodo y como se reparte por forma de pago. Usa los mismos
	// filtros que el listado, pero mira todas las ventas y no la pagina que se esta viendo.
	SaleSummaryDTO getSalesSummary(LocalDate from, LocalDate to, String username, String product, String reference);
}

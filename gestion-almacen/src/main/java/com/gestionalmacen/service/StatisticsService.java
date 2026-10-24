package com.gestionalmacen.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestionalmacen.dto.DaySalesDTO;
import com.gestionalmacen.dto.MethodTotalDTO;
import com.gestionalmacen.dto.SaleSummaryDTO;
import com.gestionalmacen.dto.StatisticsDTO;
import com.gestionalmacen.repository.IPurchaseRepository;
import com.gestionalmacen.repository.ISaleRepository;
import com.gestionalmacen.repository.ISupplierPaymentRepository;

// Los numeros del panel principal (RF-15 / CU-06).
//
// Se calculan cada vez que se abre la pantalla, igual que las alertas de stock: son pocas
// consultas sobre un dia de ventas, y un numero guardado se desactualiza sin que nadie lo note.
@Service
@Transactional(readOnly = true)
public class StatisticsService implements IStatisticsService {

	// Cuantos dias muestra el grafico. Dos semanas dejan ver los fines de semana y comparar
	// esta con la anterior, que es como se mira si una semana vino floja.
	private static final int DIAS_DEL_GRAFICO = 14;

	@Autowired
	private ISaleRepository saleRepository;

	@Autowired
	private IPurchaseRepository purchaseRepository;

	@Autowired
	private ISupplierPaymentRepository supplierPaymentRepository;

	@Override
	public StatisticsDTO getStatistics(boolean isAdmin) {
		LocalDate hoy = LocalDate.now();
		LocalDateTime desde = hoy.atStartOfDay();
		LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();

		StatisticsDTO estadisticas = new StatisticsDTO();

		//1- lo cobrado hoy y como se reparte. Lo ve cualquiera: el empleado cierra su caja con esto
		SaleSummaryDTO delDia = saleRepository.summary(desde, hasta, "", "", "");
		estadisticas.setSoldToday(delDia.getTotal());
		estadisticas.setTicketsToday(delDia.getCharged());
		if (delDia.getCharged() > 0) {
			estadisticas.setAverageTicket(
					delDia.getTotal().divide(BigDecimal.valueOf(delDia.getCharged()), 2, RoundingMode.HALF_UP));
		}

		Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
		for (MethodTotalDTO fila : saleRepository.totalsByMethod(desde, hasta, "", "", "")) {
			porMetodo.put(fila.getMethod().name(), fila.getAmount());
		}
		estadisticas.setByMethod(porMetodo);

		if (!isAdmin) {
			return estadisticas;
		}

		//2- de aca en adelante, solo para el administrador
		estadisticas.setProfitToday(saleRepository.profit(desde, hasta));
		estadisticas.setLinesWithoutCost(saleRepository.linesWithoutCost(desde, hasta));
		estadisticas.setSupplierDebt(
				purchaseRepository.totalPurchased().subtract(supplierPaymentRepository.totalPaid()));

		// Lo comprado en el mes en curso. Null significa que todavia no se cargo ninguna factura.
		BigDecimal comprado = purchaseRepository.totalForPeriod(hoy.withDayOfMonth(1), hoy);
		estadisticas.setMonthPurchases(comprado == null ? BigDecimal.ZERO : comprado);

		estadisticas.setLastDays(this.lastDays(hoy));
		estadisticas.setTopProducts(saleRepository.topProducts(hoy.withDayOfMonth(1).atStartOfDay(), hasta));
		return estadisticas;
	}

	// Lo vendido cada uno de los ultimos dias, para el grafico.
	//
	// La consulta devuelve solo los dias en que hubo ventas. Si se dibujaran tal cual, un domingo
	// cerrado desapareceria del grafico y los demas dias se corren: la semana se veria mas corta
	// de lo que es. Asi que se recorren los dias uno por uno y los que faltan quedan en cero.
	private List<DaySalesDTO> lastDays(LocalDate hoy) {
		LocalDate primero = hoy.minusDays(DIAS_DEL_GRAFICO - 1L);

		Map<LocalDate, BigDecimal> vendido = new HashMap<>();
		for (DaySalesDTO dia : saleRepository.salesByDay(primero.atStartOfDay(), hoy.plusDays(1).atStartOfDay())) {
			vendido.put(dia.getDate(), dia.getTotal());
		}

		List<DaySalesDTO> dias = new ArrayList<>();
		for (int i = 0; i < DIAS_DEL_GRAFICO; i++) {
			LocalDate fecha = primero.plusDays(i);
			dias.add(new DaySalesDTO(fecha, vendido.getOrDefault(fecha, BigDecimal.ZERO)));
		}
		return dias;
	}
}

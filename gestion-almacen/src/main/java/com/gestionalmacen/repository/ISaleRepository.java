package com.gestionalmacen.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestionalmacen.dto.DaySalesDTO;
import com.gestionalmacen.dto.MethodTotalDTO;
import com.gestionalmacen.dto.SaleSummaryDTO;
import com.gestionalmacen.dto.TopProductDTO;
import com.gestionalmacen.entity.Sale;

// Acceso a la tabla ventas. Los renglones y el pago se guardan junto con la venta.
@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long> {

	// Los filtros del historial, en un solo lugar. El listado y el resumen los comparten para que
	// los dos miren exactamente el mismo conjunto de ventas: si se separaran, el total de la
	// pantalla podria no corresponder con las ventas que se estan viendo.
	// Empleado, producto y numero de operacion son opcionales: vacios, no filtran.
	// El producto se busca en los renglones de la venta, y el numero de operacion en sus pagos (CU-10).
	String FILTERS = """
			s.dateTime >= :from AND s.dateTime < :to
			  AND (:username = '' OR s.username = :username)
			  AND (:product = '' OR EXISTS (
			        SELECT d FROM SaleDetail d
			        WHERE d.sale = s AND LOWER(d.productName) LIKE LOWER(CONCAT('%', :product, '%'))))
			  AND (:reference = '' OR EXISTS (
			        SELECT p FROM Payment p
			        WHERE p.sale = s AND LOWER(p.externalReference) LIKE LOWER(CONCAT('%', :reference, '%'))))
			""";

	// Historial (CU-16): las ventas del periodo, de la mas nueva a la mas vieja, de a paginas (RF-13).
	//
	// Sin JOIN FETCH a proposito: trayendo los renglones en la misma consulta, una venta de tres
	// productos ocupa tres filas y la pagina sale cortada; Hibernate lo resuelve trayendo todo y
	// paginando en memoria, que es justo lo que la paginacion viene a evitar. Los renglones y los
	// pagos se cargan despues en bloque, con default_batch_fetch_size, en dos consultas mas.
	//
	// Dos ventas del mismo segundo se ordenan por numero: la base guarda la hora sin fracciones.
	@Query("SELECT s FROM Sale s WHERE " + FILTERS + " ORDER BY s.dateTime DESC, s.id DESC")
	Page<Sale> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
			@Param("username") String username, @Param("product") String product,
			@Param("reference") String reference, Pageable pageable);

	// CU-16 paso 6: cuantas ventas se cobraron en el periodo y cuanto suman.
	// Es sobre todas las que cumplen el filtro, no sobre la pagina que se esta viendo.
	// Solo las confirmadas: una venta cancelada aparece en la lista pero no entro a la caja.
	@Query("SELECT new com.gestionalmacen.dto.SaleSummaryDTO(COUNT(s), COALESCE(SUM(s.total), 0)) "
			+ "FROM Sale s WHERE " + FILTERS
			+ " AND s.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA")
	SaleSummaryDTO summary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
			@Param("username") String username, @Param("product") String product,
			@Param("reference") String reference);

	// Lo mismo, abierto por forma de pago: es como se controla la caja contra el efectivo que hay.
	@Query("SELECT new com.gestionalmacen.dto.MethodTotalDTO(p.method, COALESCE(SUM(p.amount), 0)) "
			+ "FROM Payment p JOIN p.sale s WHERE " + FILTERS
			+ " AND s.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA"
			+ " GROUP BY p.method")
	List<MethodTotalDTO> totalsByMethod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
			@Param("username") String username, @Param("product") String product,
			@Param("reference") String reference);

	// ---------- Panel principal (RF-15 / CU-06) ----------

	// La ganancia del periodo: lo que se cobro menos lo que costo esa mercaderia.
	// El costo sale del renglon y no del producto, que es lo que hace que la ganancia de un mes
	// cerrado no cambie cuando el proveedor aumenta.
	// Los renglones sin costo quedan afuera: contarlos como costo cero inventaria una ganancia.
	@Query("""
			SELECT COALESCE(SUM((d.unitPrice - d.costPrice) * d.quantity), 0)
			FROM SaleDetail d
			WHERE d.sale.dateTime >= :from AND d.sale.dateTime < :to
			  AND d.sale.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA
			  AND d.costPrice IS NOT NULL
			""")
	BigDecimal profit(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// Cuantos renglones quedaron afuera de la cuenta anterior. La pantalla lo usa para avisar
	// que hay productos sin precio de costo, en vez de mostrar una ganancia incompleta sin decirlo.
	@Query("""
			SELECT COUNT(d)
			FROM SaleDetail d
			WHERE d.sale.dateTime >= :from AND d.sale.dateTime < :to
			  AND d.sale.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA
			  AND d.costPrice IS NULL
			""")
	long linesWithoutCost(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// Lo mas vendido del periodo, por importe. Cinco alcanza para ver de que vive el comercio.
	@Query("""
			SELECT new com.gestionalmacen.dto.TopProductDTO(d.productName, SUM(d.quantity),
			        SUM(d.unitPrice * d.quantity))
			FROM SaleDetail d
			WHERE d.sale.dateTime >= :from AND d.sale.dateTime < :to
			  AND d.sale.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA
			GROUP BY d.productName
			ORDER BY SUM(d.unitPrice * d.quantity) DESC
			LIMIT 5
			""")
	List<TopProductDTO> topProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// Lo vendido cada dia, para el grafico. Los dias sin ventas no vienen: los completa el servicio,
	// porque un dia cerrado tiene que dibujarse como una barra en cero y no desaparecer del grafico.
	@Query("""
			SELECT new com.gestionalmacen.dto.DaySalesDTO(CAST(s.dateTime AS LocalDate),
			        COALESCE(SUM(s.total), 0))
			FROM Sale s
			WHERE s.dateTime >= :from AND s.dateTime < :to AND s.status = com.gestionalmacen.entity.SaleStatus.CONFIRMADA
			GROUP BY CAST(s.dateTime AS LocalDate)
			ORDER BY CAST(s.dateTime AS LocalDate)
			""")
	List<DaySalesDTO> salesByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

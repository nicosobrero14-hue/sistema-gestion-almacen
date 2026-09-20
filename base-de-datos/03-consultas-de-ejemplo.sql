-- =============================================================================
--  Consultas de ejemplo
--
--  Muestran que el modelo resuelve lo que piden los requisitos. Son las mismas
--  consultas que usa la aplicación, escritas en SQL para poder probarlas a mano:
--      mysql -u root -p gestion_almacen < 03-consultas-de-ejemplo.sql
-- =============================================================================

USE gestion_almacen;


-- 1. Catálogo activo con su proveedor  (RF-01)
SELECT p.id_producto, p.nombre, p.precio, p.stock_actual, v.nombre AS proveedor
FROM productos p
LEFT JOIN proveedores v ON v.id_proveedor = p.id_proveedor
WHERE p.activo = 1
ORDER BY p.nombre;


-- 2. Buscar por nombre o por código de barras  (RF-01)
SELECT id_producto, nombre, codigo_barras, precio, stock_actual
FROM productos
WHERE activo = 1
  AND (nombre LIKE '%leche%' OR codigo_barras LIKE '%leche%');


-- 3. Productos en el mínimo o por debajo  (RF-09)
--    Compara dos columnas de la misma fila, por eso no alcanza con un filtro simple.
SELECT id_producto, nombre, stock_actual, stock_minimo
FROM productos
WHERE activo = 1
  AND stock_actual <= stock_minimo
ORDER BY stock_actual;


-- 4. Productos que vencen dentro de los próximos 30 días, incluidos los vencidos  (RF-09)
SELECT id_producto, nombre, fecha_vencimiento,
       DATEDIFF(fecha_vencimiento, CURDATE()) AS dias_para_vencer
FROM productos
WHERE activo = 1
  AND fecha_vencimiento IS NOT NULL
  AND fecha_vencimiento <= CURDATE() + INTERVAL 30 DAY
ORDER BY fecha_vencimiento;


-- 5. Historial de movimientos de un producto  (RF-02)
SELECT fecha_hora, tipo, stock_anterior, stock_nuevo,
       stock_nuevo - stock_anterior AS diferencia, motivo, username
FROM movimientos_stock
WHERE id_producto = 2
ORDER BY fecha_hora DESC;


-- 6. Ticket de una venta  (RF-06)
SELECT d.nombre_producto, d.cantidad, d.precio_unitario,
       d.cantidad * d.precio_unitario AS subtotal
FROM detalle_venta d
WHERE d.id_venta = 1;


-- 7. Ventas de un período con su empleado y su método de pago  (RF-07)
SELECT v.id_venta, v.fecha_hora, v.username AS empleado, v.total, p.metodo
FROM ventas v
LEFT JOIN pagos p ON p.id_venta = v.id_venta AND p.estado = 'APROBADO'
WHERE v.estado = 'CONFIRMADA'
  AND v.fecha_hora >= '2026-01-01'
  AND v.fecha_hora <  '2027-01-01'
ORDER BY v.fecha_hora DESC;


-- 8. Total recaudado en un período, separado por método de pago  (RF-07)
SELECT p.metodo, COUNT(*) AS cantidad_ventas, SUM(v.total) AS recaudado
FROM ventas v
JOIN pagos p ON p.id_venta = v.id_venta AND p.estado = 'APROBADO'
WHERE v.estado = 'CONFIRMADA'
  AND v.fecha_hora >= '2026-01-01'
GROUP BY p.metodo;


-- 9. Productos más vendidos y menos vendidos de un período  (RF-10)
--    Es la consulta que alimenta las sugerencias de oferta por baja rotación.
SELECT p.id_producto, p.nombre, COALESCE(SUM(d.cantidad), 0) AS unidades_vendidas
FROM productos p
LEFT JOIN detalle_venta d ON d.id_producto = p.id_producto
LEFT JOIN ventas v ON v.id_venta = d.id_venta
     AND v.estado = 'CONFIRMADA'
     AND v.fecha_hora >= CURDATE() - INTERVAL 30 DAY
WHERE p.activo = 1
GROUP BY p.id_producto, p.nombre
ORDER BY unidades_vendidas ASC;


-- 10. Control de consistencia del stock
--     El stock actual de cada producto tiene que coincidir con la suma de sus
--     movimientos. Si alguna fila aparece acá, hubo un cambio sin registrar.
SELECT p.id_producto, p.nombre, p.stock_actual,
       COALESCE(SUM(m.stock_nuevo - m.stock_anterior), 0) AS suma_movimientos
FROM productos p
LEFT JOIN movimientos_stock m ON m.id_producto = p.id_producto
GROUP BY p.id_producto, p.nombre, p.stock_actual
HAVING p.stock_actual <> suma_movimientos;

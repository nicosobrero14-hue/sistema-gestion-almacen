-- ---------------------------------------------------------------------------
--  Migración: cantidades y stock de enteros a decimales
--
--  Hace falta solo en bases creadas antes de la fase 9. Una base nueva ya sale
--  con estos tipos desde 01-esquema.sql.
--
--  No transforma datos ni puede perder información: un stock de 3 pasa a 3,000.
--  Se agrega además la unidad de venta, que define si el precio del producto es
--  por unidad o por kilo. Todos los productos existentes quedan por unidad, que
--  es como se venían vendiendo.
-- ---------------------------------------------------------------------------

USE gestion_almacen;

ALTER TABLE productos
    MODIFY stock_actual DECIMAL(10,3) NOT NULL DEFAULT 0,
    MODIFY stock_minimo DECIMAL(10,3) NOT NULL DEFAULT 0,
    ADD COLUMN unidad_venta VARCHAR(10) NOT NULL DEFAULT 'UNIDAD' AFTER stock_minimo,
    ADD CONSTRAINT ck_productos_unidad CHECK (unidad_venta IN ('UNIDAD', 'KG'));

ALTER TABLE detalle_venta
    MODIFY cantidad DECIMAL(10,3) NOT NULL,
    ADD COLUMN unidad_venta VARCHAR(10) NOT NULL DEFAULT 'UNIDAD' AFTER precio_unitario,
    ADD CONSTRAINT ck_detalle_unidad CHECK (unidad_venta IN ('UNIDAD', 'KG'));

-- El ticket y el historial muestran el nombre del empleado, no su usuario.
-- Las ventas ya registradas se completan con el nombre que figura en usuarios.
ALTER TABLE ventas
    ADD COLUMN nombre_empleado VARCHAR(120) NOT NULL DEFAULT '' AFTER id_usuario;

UPDATE ventas v
    JOIN usuarios u ON u.id_usuario = v.id_usuario
    SET v.nombre_empleado = CONCAT(u.nombre, ' ', u.apellido)
    WHERE v.nombre_empleado = '';

ALTER TABLE movimientos_stock
    MODIFY stock_anterior DECIMAL(10,3) NOT NULL,
    MODIFY stock_nuevo    DECIMAL(10,3) NOT NULL;

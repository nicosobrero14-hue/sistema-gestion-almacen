-- ---------------------------------------------------------------------------
--  Migración: el precio de costo queda guardado en el renglón de la venta
--
--  Hace falta solo en bases creadas antes de la fase 16. Una base nueva ya sale
--  con la columna desde 01-esquema.sql.
--
--  Por qué se copia el costo en vez de leerlo del producto: el renglón de la
--  venta ya guarda una copia del nombre, del precio y de la unidad, para que el
--  ticket no cambie si después cambia el producto. Con la ganancia pasa lo
--  mismo. Si se calculara con el precio de costo actual, cada vez que un
--  proveedor aumenta, la ganancia de los meses anteriores cambiaría sola.
--
--  No transforma ni borra nada: agrega una columna. Las ventas que ya estaban
--  quedan sin costo, que es lo correcto, porque no hay forma de saber cuánto
--  costaba la mercadería ese día. Esas ventas no entran en el cálculo de la
--  ganancia y la pantalla avisa cuántos renglones quedaron sin costo.
-- ---------------------------------------------------------------------------

USE gestion_almacen;

ALTER TABLE detalle_venta
    ADD COLUMN precio_costo DECIMAL(10,2) NULL AFTER precio_unitario,
    ADD CONSTRAINT ck_detalle_costo CHECK (precio_costo IS NULL OR precio_costo >= 0);

-- Para verificar:
-- SHOW COLUMNS FROM detalle_venta LIKE 'precio_costo';

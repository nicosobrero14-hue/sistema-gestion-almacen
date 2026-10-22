-- ---------------------------------------------------------------------------
--  Migración: imagen del producto
--
--  Hace falta solo en bases creadas antes de la fase 13. Una base nueva ya sale
--  con esta columna desde 01-esquema.sql.
--
--  Guarda el nombre del archivo, no la imagen. Un BLOB engorda los respaldos y
--  servir un archivo estático es más rápido que sacar el binario de la base.
--  Los productos que ya estaban quedan sin imagen, que es lo correcto: todavía
--  no se les cargó ninguna.
-- ---------------------------------------------------------------------------

USE gestion_almacen;

ALTER TABLE productos
    ADD COLUMN imagen VARCHAR(255) NULL AFTER codigo_barras;

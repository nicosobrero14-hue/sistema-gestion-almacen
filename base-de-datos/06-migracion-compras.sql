-- ---------------------------------------------------------------------------
--  Migración: compras, pagos a proveedores, CUIT y precio de costo
--
--  Hace falta solo en bases creadas antes de la fase 15. Una base nueva ya sale
--  con todo esto desde 01-esquema.sql.
--
--  No transforma ni borra nada: agrega dos tablas y dos columnas. Los productos
--  que ya estaban quedan sin precio de costo, que es lo correcto porque todavía
--  no se les cargó ninguno, y los proveedores sin CUIT.
-- ---------------------------------------------------------------------------

USE gestion_almacen;

ALTER TABLE proveedores
    ADD COLUMN cuit VARCHAR(13) NULL AFTER direccion,
    ADD CONSTRAINT uq_proveedores_cuit UNIQUE (cuit);

ALTER TABLE productos
    ADD COLUMN precio_costo DECIMAL(10,2) NULL AFTER precio,
    ADD CONSTRAINT ck_productos_costo CHECK (precio_costo IS NULL OR precio_costo >= 0);

CREATE TABLE IF NOT EXISTS compras (
    id_compra        BIGINT        NOT NULL AUTO_INCREMENT,
    id_proveedor     BIGINT        NOT NULL,
    cuit_emisor      VARCHAR(13)   NULL,
    tipo_comprobante SMALLINT      NOT NULL,
    punto_venta      INT           NOT NULL,
    numero           BIGINT        NOT NULL,
    fecha            DATE          NOT NULL,
    total_facturado  DECIMAL(12,2) NOT NULL,
    total_acordado   DECIMAL(12,2) NOT NULL,
    motivo_ajuste    VARCHAR(255)  NULL,
    cae              VARCHAR(20)   NULL,
    imagen           VARCHAR(255)  NULL,
    fecha_alta       DATETIME      NOT NULL,
    id_usuario       BIGINT        NULL,
    username         VARCHAR(50)   NOT NULL,

    PRIMARY KEY (id_compra),
    CONSTRAINT uq_compras_comprobante UNIQUE (id_proveedor, tipo_comprobante, punto_venta, numero),

    CONSTRAINT fk_compras_proveedor
        FOREIGN KEY (id_proveedor) REFERENCES proveedores (id_proveedor)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_compras_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_compras_totales CHECK (total_facturado > 0
                                     AND total_acordado  > 0
                                     AND total_acordado <= total_facturado),
    CONSTRAINT ck_compras_numero  CHECK (punto_venta > 0 AND numero > 0 AND tipo_comprobante > 0),

    INDEX idx_compras_fecha (fecha),
    INDEX idx_compras_proveedor (id_proveedor, fecha)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pagos_proveedor (
    id_pago     BIGINT        NOT NULL AUTO_INCREMENT,
    id_compra   BIGINT        NOT NULL,
    fecha       DATE          NOT NULL,
    monto       DECIMAL(12,2) NOT NULL,
    metodo      VARCHAR(20)   NOT NULL,
    anulado     TINYINT(1)    NOT NULL DEFAULT 0,
    fecha_alta  DATETIME      NOT NULL,
    id_usuario  BIGINT        NULL,
    username    VARCHAR(50)   NOT NULL,

    PRIMARY KEY (id_pago),

    CONSTRAINT fk_pagosprov_compra
        FOREIGN KEY (id_compra) REFERENCES compras (id_compra)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_pagosprov_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_pagosprov_monto  CHECK (monto > 0),
    CONSTRAINT ck_pagosprov_metodo CHECK (metodo IN ('EFECTIVO', 'TRANSFERENCIA', 'MERCADOPAGO')),

    INDEX idx_pagosprov_compra (id_compra)
) ENGINE = InnoDB;

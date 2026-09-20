-- =============================================================================
--  Sistema de Gestión de Almacén y Control de Stock
--  Esquema de la base de datos - MySQL 8
--
--  Ejecutar una sola vez:
--      mysql -u root -p < 01-esquema.sql
--
--  El porqué de cada decisión está explicado en documentacion/04-modelo-datos.md
-- =============================================================================

CREATE DATABASE IF NOT EXISTS gestion_almacen
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gestion_almacen;


-- ----------------------------------------------------------------------------
--  proveedores
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(80)  NOT NULL,
    apellido     VARCHAR(80)  NULL,                 -- vacío si el proveedor es una empresa
    email        VARCHAR(120) NULL,
    telefono     VARCHAR(30)  NULL,                 -- texto: admite +54, guiones y ceros a la izquierda
    direccion    VARCHAR(150) NULL,
    activo       TINYINT(1)   NOT NULL DEFAULT 1,   -- baja lógica
    fecha_alta   DATETIME     NOT NULL,

    PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_proveedores_email UNIQUE (email)
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  usuarios
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario    BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(80)  NOT NULL,
    apellido      VARCHAR(80)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,            -- hash BCrypt, nunca la contraseña en texto plano
    rol           VARCHAR(20)  NOT NULL,
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_alta    DATETIME     NOT NULL,

    PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'EMPLEADO'))
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  productos
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS productos (
    id_producto       BIGINT        NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(120)  NOT NULL,
    descripcion       VARCHAR(255)  NULL,
    precio            DECIMAL(10,2) NOT NULL,            -- DECIMAL y no coma flotante: es dinero
    stock_actual      INT           NOT NULL DEFAULT 0,
    stock_minimo      INT           NOT NULL DEFAULT 0,  -- umbral de la alerta de stock bajo
    codigo_barras     VARCHAR(64)   NULL,                -- opcional: no todo producto tiene código
    fecha_vencimiento DATE          NULL,                -- solo perecederos
    en_oferta         TINYINT(1)    NOT NULL DEFAULT 0,
    precio_oferta     DECIMAL(10,2) NULL,
    activo            TINYINT(1)    NOT NULL DEFAULT 1,
    fecha_alta        DATETIME      NOT NULL,
    id_proveedor      BIGINT        NULL,                -- un producto puede no tener proveedor

    PRIMARY KEY (id_producto),
    CONSTRAINT uq_productos_codigo_barras UNIQUE (codigo_barras),

    -- Al borrar un proveedor sus productos quedan sin proveedor, no se borran.
    CONSTRAINT fk_productos_proveedor
        FOREIGN KEY (id_proveedor) REFERENCES proveedores (id_proveedor)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_productos_precio CHECK (precio > 0),
    CONSTRAINT ck_productos_stock  CHECK (stock_actual >= 0 AND stock_minimo >= 0),
    CONSTRAINT ck_productos_oferta CHECK (en_oferta = 0 OR (precio_oferta IS NOT NULL AND precio_oferta < precio)),

    INDEX idx_productos_nombre (nombre),
    INDEX idx_productos_activo (activo)
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  ventas
--  El id de la venta es también el número del ticket.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ventas (
    id_venta   BIGINT        NOT NULL AUTO_INCREMENT,
    fecha_hora DATETIME      NOT NULL,
    id_usuario BIGINT        NULL,
    username   VARCHAR(50)   NOT NULL,            -- copia: quién hizo la venta
    subtotal   DECIMAL(10,2) NOT NULL,            -- suma de las líneas
    descuento  DECIMAL(10,2) NOT NULL DEFAULT 0,
    total      DECIMAL(10,2) NOT NULL,            -- subtotal - descuento
    estado     VARCHAR(20)   NOT NULL,

    PRIMARY KEY (id_venta),

    CONSTRAINT fk_ventas_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_ventas_estado CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'CANCELADA')),
    CONSTRAINT ck_ventas_importes CHECK (subtotal >= 0 AND descuento >= 0 AND descuento <= subtotal AND total >= 0),

    INDEX idx_ventas_fecha (fecha_hora),
    INDEX idx_ventas_usuario (id_usuario, fecha_hora)
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  detalle_venta
--  Guarda una copia del nombre y del precio: el ticket no cambia aunque el
--  producto cambie de precio o se borre del catálogo.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS detalle_venta (
    id_detalle      BIGINT        NOT NULL AUTO_INCREMENT,
    id_venta        BIGINT        NOT NULL,
    id_producto     BIGINT        NULL,
    nombre_producto VARCHAR(120)  NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    cantidad        INT           NOT NULL,

    PRIMARY KEY (id_detalle),

    -- Las líneas pertenecen a la venta: si la venta se borra, se van con ella.
    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (id_venta) REFERENCES ventas (id_venta)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (id_producto) REFERENCES productos (id_producto)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio   CHECK (precio_unitario >= 0),

    INDEX idx_detalle_producto (id_producto)
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  pagos
--  Una venta puede tener más de un intento de pago: el QR que venció y el que
--  se pagó. El aprobado es el que vale.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pagos (
    id_pago            BIGINT        NOT NULL AUTO_INCREMENT,
    id_venta           BIGINT        NOT NULL,
    metodo             VARCHAR(20)   NOT NULL,
    monto              DECIMAL(10,2) NOT NULL,
    estado             VARCHAR(20)   NOT NULL,
    referencia_externa VARCHAR(100)  NULL,        -- id del pago en MercadoPago
    fecha_hora         DATETIME      NOT NULL,
    fecha_confirmacion DATETIME      NULL,

    PRIMARY KEY (id_pago),

    -- UNIQUE: si MercadoPago avisa dos veces del mismo pago, no se registra dos veces.
    CONSTRAINT uq_pagos_referencia UNIQUE (referencia_externa),

    CONSTRAINT fk_pagos_venta
        FOREIGN KEY (id_venta) REFERENCES ventas (id_venta)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT ck_pagos_metodo CHECK (metodo IN ('EFECTIVO', 'TRANSFERENCIA', 'MERCADOPAGO')),
    CONSTRAINT ck_pagos_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO', 'CANCELADO')),
    CONSTRAINT ck_pagos_monto  CHECK (monto > 0)
) ENGINE = InnoDB;


-- ----------------------------------------------------------------------------
--  movimientos_stock
--  Cada cambio de stock deja su renglón: carga inicial, ajuste manual o venta.
--  La suma de los movimientos de un producto da su stock actual.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimientos_stock (
    id_movimiento   BIGINT       NOT NULL AUTO_INCREMENT,
    id_producto     BIGINT       NULL,
    nombre_producto VARCHAR(120) NOT NULL,        -- copia, para poder leer el registro siempre
    id_usuario      BIGINT       NULL,
    username        VARCHAR(50)  NOT NULL,        -- copia
    id_venta        BIGINT       NULL,            -- solo en los movimientos originados por una venta
    tipo            VARCHAR(20)  NOT NULL,
    stock_anterior  INT          NOT NULL,
    stock_nuevo     INT          NOT NULL,
    motivo          VARCHAR(255) NOT NULL,
    fecha_hora      DATETIME     NOT NULL,

    PRIMARY KEY (id_movimiento),

    CONSTRAINT fk_movimientos_producto
        FOREIGN KEY (id_producto) REFERENCES productos (id_producto)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_movimientos_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_movimientos_venta
        FOREIGN KEY (id_venta) REFERENCES ventas (id_venta)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_movimientos_tipo  CHECK (tipo IN ('CARGA_INICIAL', 'AJUSTE_MANUAL', 'VENTA')),
    CONSTRAINT ck_movimientos_stock CHECK (stock_anterior >= 0 AND stock_nuevo >= 0),

    INDEX idx_movimientos_producto (id_producto, fecha_hora)
) ENGINE = InnoDB;

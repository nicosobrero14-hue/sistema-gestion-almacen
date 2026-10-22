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
    -- Sirve para reconocer al proveedor con el CUIT que trae el QR de sus facturas
    cuit         VARCHAR(13)  NULL,
    activo       TINYINT(1)   NOT NULL DEFAULT 1,   -- baja lógica
    fecha_alta   DATETIME     NOT NULL,

    PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_proveedores_email UNIQUE (email),
    CONSTRAINT uq_proveedores_cuit  UNIQUE (cuit)
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
    precio_costo      DECIMAL(10,2) NULL,                -- lo que cuesta reponerlo; sirve para el margen
    -- DECIMAL y no entero: los productos que se venden por peso tienen stock fraccionado
    stock_actual      DECIMAL(10,3) NOT NULL DEFAULT 0,
    stock_minimo      DECIMAL(10,3) NOT NULL DEFAULT 0,  -- umbral de la alerta de stock bajo
    -- Como se vende el producto. Define si "precio" es por unidad o por kilo
    unidad_venta      VARCHAR(10)   NOT NULL DEFAULT 'UNIDAD',
    codigo_barras     VARCHAR(64)   NULL,                -- opcional: no todo producto tiene código
    -- Nombre del archivo de la imagen, no la imagen: un BLOB engorda los respaldos
    imagen            VARCHAR(255)  NULL,
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
    CONSTRAINT ck_productos_costo  CHECK (precio_costo IS NULL OR precio_costo >= 0),
    CONSTRAINT ck_productos_stock  CHECK (stock_actual >= 0 AND stock_minimo >= 0),
    CONSTRAINT ck_productos_unidad CHECK (unidad_venta IN ('UNIDAD', 'KG')),
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
    nombre_empleado VARCHAR(120) NOT NULL DEFAULT '',  -- copia: es lo que se muestra en el ticket
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
    unidad_venta    VARCHAR(10)   NOT NULL DEFAULT 'UNIDAD',  -- copia: el ticket no cambia si el producto se reclasifica
    cantidad        DECIMAL(10,3) NOT NULL,          -- fraccionada en los productos que se venden por peso

    PRIMARY KEY (id_detalle),

    -- Las líneas pertenecen a la venta: si la venta se borra, se van con ella.
    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (id_venta) REFERENCES ventas (id_venta)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (id_producto) REFERENCES productos (id_producto)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_unidad   CHECK (unidad_venta IN ('UNIDAD', 'KG')),
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
    stock_anterior  DECIMAL(10,3) NOT NULL,
    stock_nuevo     DECIMAL(10,3) NOT NULL,
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


-- ----------------------------------------------------------------------------
--  compras
--  La factura que entrega un proveedor. Es el egreso: hasta la fase 15 el
--  sistema registraba solo la plata que entra y no podía decir cuánto se ganó.
--
--  Se guardan dos totales. El facturado es el que figura en el comprobante y el
--  que trae su código QR. El acordado es lo que realmente se debe, y se usa
--  cuando no se paga el total: por ejemplo si faltó un producto del pedido. Casi
--  siempre son iguales. El saldo y la ganancia se calculan sobre el acordado.
--
--  No se guarda el detalle renglón por renglón. Las facturas del reparto vienen
--  sin código de producto y con descripciones del distribuidor que no coinciden
--  con el catálogo del comercio, así que no habría con qué relacionarlas.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS compras (
    id_compra        BIGINT        NOT NULL AUTO_INCREMENT,
    id_proveedor     BIGINT        NOT NULL,          -- una factura siempre es de alguien
    cuit_emisor      VARCHAR(13)   NULL,              -- el que viene en el QR del comprobante
    tipo_comprobante INT           NOT NULL,          -- código de ARCA: 1 factura A, 6 factura B, 11 factura C
    punto_venta      INT           NOT NULL,
    numero           BIGINT        NOT NULL,
    fecha            DATE          NOT NULL,          -- la del comprobante, no la de carga
    total_facturado  DECIMAL(12,2) NOT NULL,          -- lo que dice el papel
    total_acordado   DECIMAL(12,2) NOT NULL,          -- lo que realmente se debe
    motivo_ajuste    VARCHAR(255)  NULL,              -- por qué difieren, si difieren
    cae              VARCHAR(20)   NULL,              -- código de autorización del comprobante
    imagen           VARCHAR(255)  NULL,              -- nombre del archivo de la foto, no la foto
    fecha_alta       DATETIME      NOT NULL,
    id_usuario       BIGINT        NULL,
    username         VARCHAR(50)   NOT NULL,          -- copia, igual que en ventas

    PRIMARY KEY (id_compra),

    -- El mismo comprobante no se puede cargar dos veces. Con fotos y reparto diario,
    -- cargar la misma factura repetida es un error que va a pasar.
    CONSTRAINT uq_compras_comprobante UNIQUE (id_proveedor, tipo_comprobante, punto_venta, numero),

    -- RESTRICT y no SET NULL: una compra sin proveedor no significa nada. En el sistema
    -- los proveedores se dan de baja lógicamente, así que esto no se dispara en el uso normal.
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


-- ----------------------------------------------------------------------------
--  pagos_proveedor
--  La plata que sale. Va aparte de la factura porque no coinciden: se le puede
--  pagar a un proveedor en dos veces, o pagarle tres facturas el mismo día.
--
--  Un pago pertenece a una factura. Pagar tres juntas son tres pagos del mismo
--  día: es menos fiel que un pago que cubra varias, pero mucho más simple, y el
--  total pagado en el día se sigue pudiendo consultar.
--
--  El estado de la factura no se guarda, se calcula con la suma de sus pagos,
--  por lo mismo que no se guardan las alertas de stock: queda desactualizado
--  apenas se registra un pago.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pagos_proveedor (
    id_pago     BIGINT        NOT NULL AUTO_INCREMENT,
    id_compra   BIGINT        NOT NULL,
    fecha       DATE          NOT NULL,
    monto       DECIMAL(12,2) NOT NULL,
    metodo      VARCHAR(20)   NOT NULL,
    anulado     TINYINT(1)    NOT NULL DEFAULT 0,   -- un pago no se borra, se anula
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

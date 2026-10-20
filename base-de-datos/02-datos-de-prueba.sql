-- =============================================================================
--  Datos de prueba para desarrollo
--
--  Ejecutar después de 01-esquema.sql y una sola vez:
--      mysql -u root -p gestion_almacen < 02-datos-de-prueba.sql
--
--  Las contraseñas de este archivo son de prueba. Antes de usar el sistema en
--  el comercio hay que cambiar la del administrador.
-- =============================================================================

USE gestion_almacen;

SET NAMES utf8mb4;


-- ----------------------------------------------------------------------------
--  usuarios
--  Contraseñas hasheadas con BCrypt:  admin / Admin1234   y   vendedor / Empleado1234
-- ----------------------------------------------------------------------------
INSERT INTO usuarios (nombre, apellido, username, password_hash, rol, activo, fecha_alta) VALUES
('Nicolás', 'Sobrero', 'admin',    '$2a$10$HQFOswxGfiC.CZqHX.aZyOCh3ssJQzFLgP0mY2pkUSB.7fx9DIvCi', 'ADMIN',    1, NOW()),
('María',   'Gómez',   'vendedor', '$2a$10$IMid1q86NnZwfCUSqEuvC.8YFm7Ba19ca.y5Um4e4mw2dXIFF3.G2', 'EMPLEADO', 1, NOW());


-- ----------------------------------------------------------------------------
--  proveedores
-- ----------------------------------------------------------------------------
INSERT INTO proveedores (nombre, apellido, email, telefono, direccion, activo, fecha_alta) VALUES
('Distribuidora del Sur', NULL,    'ventas@distsur.com.ar',    '+54 351 4567890', 'Av. Colón 1250, Córdoba',    1, NOW()),
('Lácteos La Serenísima', NULL,    'pedidos@laserenisima.com', '+54 11 45678901', 'Ruta 5 Km 42, Buenos Aires', 1, NOW()),
('Juan',                  'Pérez', 'juanperez@gmail.com',      '+54 351 2223344', 'Bv. San Juan 430, Córdoba',  1, NOW());


-- ----------------------------------------------------------------------------
--  productos
--  Los casos son variados a propósito, para poder probar el sistema:
--    - con y sin código de barras
--    - con y sin fecha de vencimiento
--    - con y sin proveedor
--    - uno con el stock por debajo del mínimo, que dispara la alerta
--    - uno en oferta
--    - uno dado de baja
--
--  La columna imagen guarda el nombre del archivo, y ese nombre sale del id del producto.
--  Como los id los asigna la base en el orden de este INSERT, las filas no se pueden mover
--  de lugar sin que cada producto quede con la foto de otro. Los archivos están en
--  imagenes-de-prueba/ y hay que copiarlos a la carpeta de imágenes del servidor.
-- ----------------------------------------------------------------------------
INSERT INTO productos
    (nombre, descripcion, precio, stock_actual, stock_minimo, codigo_barras, fecha_vencimiento, en_oferta, precio_oferta, activo, fecha_alta, id_proveedor, imagen) VALUES
('Yerba Mate 1kg',       'Yerba mate elaborada con palo',   4850.00, 40, 10, '7790010001234', NULL,         0, NULL,    1, NOW(), 1,    'producto-1.png'),
('Leche entera 1L',      'Leche entera larga vida',         1650.00,  8, 20, '7790742001122', '2026-10-15', 0, NULL,    1, NOW(), 2,    'producto-2.png'),
('Fideos guiseros 500g', 'Fideos tipo mostachol',           1200.00, 60, 15, '7790040998877', '2027-03-01', 0, NULL,    1, NOW(), 1,    'producto-3.png'),
('Aceite girasol 900ml', 'Aceite de girasol común',         3400.00, 25, 10, '7790070445566', '2027-01-20', 1, 2990.00, 1, NOW(), 1,    'producto-4.png'),
('Azúcar 1kg',           'Azúcar común tipo A',             1890.00, 30, 10, NULL,            NULL,         0, NULL,    1, NOW(), 3,    'producto-5.png'),
('Galletitas surtidas',  'Paquete surtido 400g',            2250.00, 12,  8, '7790580112233', '2026-11-30', 0, NULL,    1, NOW(), NULL, 'producto-6.png'),
('Gaseosa cola 2.25L',   'Gaseosa sabor cola',              3100.00, 45, 12, '7790895667788', '2026-12-05', 0, NULL,    1, NOW(), 1,    'producto-7.png'),
('Jabón en polvo 800g',  'Jabón en polvo para ropa blanca', 5600.00,  5,  5, '7791290334455', NULL,         0, NULL,    0, NOW(), 3,    'producto-8.png');


-- ----------------------------------------------------------------------------
--  movimientos_stock
--  El stock inicial de cada producto también deja su movimiento, así la suma de
--  los movimientos coincide con el stock actual desde el primer día.
-- ----------------------------------------------------------------------------
INSERT INTO movimientos_stock
    (id_producto, nombre_producto, id_usuario, username, id_venta, tipo, stock_anterior, stock_nuevo, motivo, fecha_hora)
SELECT p.id_producto, p.nombre, u.id_usuario, u.username, NULL, 'CARGA_INICIAL', 0, p.stock_actual,
       'Carga inicial del catálogo', NOW()
FROM productos p
JOIN usuarios u ON u.username = 'admin';

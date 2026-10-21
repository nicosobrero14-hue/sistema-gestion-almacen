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
--  Son productos reales, con su código de barras de verdad. Los ocho primeros
--  se cargaron escaneándolos: el sistema buscó el código en los catálogos
--  públicos y trajo el nombre y la foto. Los dos últimos se venden sueltos, por
--  kilo, así que no tienen código que leer ni foto en ningún catálogo.
--
--  Los casos son variados a propósito, para poder probar el sistema:
--    - con y sin código de barras
--    - por unidad y por kilo
--    - con y sin foto
--    - con y sin fecha de vencimiento
--    - con y sin proveedor
--    - uno con el stock por debajo del mínimo, que dispara la alerta
--    - uno ya vencido y uno próximo a vencer
--    - uno en oferta
--    - uno dado de baja
--
--  La columna imagen guarda el nombre del archivo, y ese nombre sale del id del
--  producto. Como los id los asigna la base en el orden de este INSERT, las
--  filas no se pueden mover de lugar sin que cada producto quede con la foto de
--  otro. Los archivos están en imagenes-de-prueba/ y hay que copiarlos a la
--  carpeta de imágenes del servidor.
-- ----------------------------------------------------------------------------
INSERT INTO productos
    (nombre, descripcion, precio, stock_actual, stock_minimo, unidad_venta, codigo_barras, fecha_vencimiento, en_oferta, precio_oferta, activo, fecha_alta, id_proveedor, imagen) VALUES
('Leche La Serenísima clásica 1 L',   'Leche entera larga vida',          1650.00,  8.000, 20.000, 'UNIDAD', '7790742363008', '2026-10-15', 0, NULL,    1, NOW(), 2,    'producto-1.jpg'),
('Fideos Matarazzo tallarín N°5 500 g','Fideos secos de sémola',          1450.00, 60.000, 15.000, 'UNIDAD', '7790070336316', '2027-06-01', 0, NULL,    1, NOW(), 1,    'producto-2.jpg'),
('Aceite Natura girasol 900 ml',      'Aceite puro de girasol',           3400.00, 25.000, 10.000, 'UNIDAD', '7790272001005', '2027-03-15', 1, 2990.00, 1, NOW(), 1,    'producto-3.jpg'),
('Arroz Molinos Ala largo fino 1 kg', 'Arroz largo fino doble carolina',  2100.00, 32.000, 10.000, 'UNIDAD', '7791120031557', NULL,         0, NULL,    1, NOW(), 1,    'producto-4.jpg'),
('Galletitas 9 de Oro clásicas 200 g','Bizcochos salados',                2250.00, 12.000,  8.000, 'UNIDAD', '7792200000159', '2026-11-28', 0, NULL,    1, NOW(), NULL, 'producto-5.jpg'),
('Coca-Cola 2,25 L',                  'Gaseosa sabor cola',               3500.00, 45.000, 12.000, 'UNIDAD', '7790895000997', '2027-02-10', 0, NULL,    1, NOW(), 1,    'producto-6.jpg'),
('Celusal sal fina 500 g',            'Sal fina de mesa',                  980.00,  5.000,  5.000, 'UNIDAD', '7790072002080', NULL,         0, NULL,    0, NOW(), 3,    'producto-7.jpg'),
('Seven Up 1.5 L',                    'Gaseosa sabor lima limón',         2800.00, 30.000, 10.000, 'UNIDAD', '7791813444411', NULL,         0, NULL,    1, NOW(), 1,    'producto-8.jpg'),
('Queso cremoso',                     'Se vende por kilo, cortado',      12000.00,  3.250,  1.000, 'KG',     NULL,            '2026-11-10', 0, NULL,    1, NOW(), 2,    NULL),
('Jamón cocido',                      'Se vende por kilo, feteado',      14500.00,  1.800,  0.500, 'KG',     NULL,            '2026-10-28', 0, NULL,    1, NOW(), 3,    NULL);


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

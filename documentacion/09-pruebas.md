# 9. Pruebas y validación

## 9.1 Estrategia

Las pruebas se hacen en tres niveles. Cada uno detecta problemas que los otros no ven.

| Nivel | Qué se prueba | Cómo | Cuándo |
|---|---|---|---|
| Base de datos | Que las restricciones rechacen los datos imposibles y que las relaciones se comporten como fueron diseñadas | Scripts SQL sobre una base temporal | Al cerrar el modelo de datos |
| Backend | Cada endpoint y cada regla del negocio | Pruebas automáticas con JUnit y MockMvc | En cada cambio, con `mvnw test` |
| Sistema completo | Que la pantalla, la API y la base funcionen juntas | Recorrido manual en el navegador contra MySQL, con capturas | Al cerrar cada fase |

**Las pruebas automáticas no usan MySQL.** Corren sobre H2, una base en memoria que se crea al
empezar y se descarta al terminar. Así se pueden ejecutar en cualquier máquina y nunca tocan los
datos reales. Además, cada prueba corre dentro de una transacción que se deshace al final, así
ninguna depende de lo que dejó otra.

**Las pruebas del sistema completo sí usan MySQL.** Es la única forma de verificar que las
entidades coinciden con las tablas reales y que los datos se ven bien en pantalla.

Para ejecutar las automáticas:

```bash
cd gestion-almacen
```

```bash
mvnw test
```

## 9.2 Casos de prueba

En todos los casos de esta sección el resultado obtenido coincidió con el esperado. Los problemas
que aparecieron durante las pruebas están en el punto 9.3.

### Base de datos

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-01 | Restricción de stock | Base creada con el esquema | Producto con stock -1 | Rechazo por `ck_productos_stock` | Rechazado | Aprobada |
| CP-02 | Restricción de precio | Base creada | Producto con precio 0 | Rechazo por `ck_productos_precio` | Rechazado | Aprobada |
| CP-03 | Regla de la oferta | Base creada | Oferta de 150 con precio 100 | Rechazo por `ck_productos_oferta` | Rechazado | Aprobada |
| CP-04 | Roles válidos | Base creada | Usuario con rol `JEFE` | Rechazo por `ck_usuarios_rol` | Rechazado | Aprobada |
| CP-05 | Usuario único | Existe `admin` | Otro usuario `admin` | Rechazo por clave única | Rechazado | Aprobada |
| CP-06 | Email de proveedor único | Existe el email | Otro proveedor con ese email | Rechazo por clave única | Rechazado | Aprobada |
| CP-07 | Código de barras único | Existe el código | Otro producto con ese código | Rechazo por clave única | Rechazado | Aprobada |
| CP-08 | Importes de la venta | Base creada | Descuento de 150 sobre subtotal de 100 | Rechazo por `ck_ventas_importes` | Rechazado | Aprobada |
| CP-09 | Cantidad vendida | Venta creada | Renglón con cantidad 0 | Rechazo por `ck_detalle_cantidad` | Rechazado | Aprobada |
| CP-10 | Tipos de movimiento | Base creada | Movimiento de tipo `ROBO` | Rechazo por `ck_movimientos_tipo` | Rechazado | Aprobada |
| CP-11 | Ticket de una venta | Venta con dos renglones | Consulta del detalle | Dos renglones con su subtotal | Correcto | Aprobada |
| CP-12 | Consistencia del stock | Venta registrada con sus movimientos | Stock contra suma de movimientos | Coinciden | 38 y 7 en los dos productos | Aprobada |
| CP-13 | Borrar un proveedor | Proveedor con 4 productos | Borrado del proveedor | Los productos quedan sin proveedor | 4 productos, todos sin proveedor | Aprobada |
| CP-14 | Borrar un producto vendido | Producto en una venta | Borrado del producto | El renglón conserva nombre y precio | Conservados | Aprobada |
| CP-15 | Borrar una venta | Venta con renglones, pago y movimientos | Borrado de la venta | Se borran renglones y pago; el movimiento queda | Correcto | Aprobada |

### Arranque del sistema

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-16 | Arranque de la aplicación | Proyecto compilado | — | El contexto levanta sin errores | Correcto | Aprobada |
| CP-17 | Estado del sistema | Aplicación en ejecución | `GET /api/status` | `application` y `database` en `ok` | Correcto | Aprobada |

### Proveedores

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-18 | Alta de proveedor | — | Nombre, email y teléfono | `201`, proveedor activo con id | Correcto | Aprobada |
| CP-19 | Nombre obligatorio | — | Proveedor sin nombre | `400`, error en el campo `name` | Correcto | Aprobada |
| CP-20 | Email repetido | Existe un proveedor con ese email | Otro proveedor con el mismo email | `409` con mensaje | Correcto | Aprobada |
| CP-21 | Modificación | Proveedor existente | Nombre nuevo, mismo email | `200`; conservar su propio email no cuenta como repetido | Correcto | Aprobada |
| CP-22 | Búsqueda | Dos proveedores | Texto "lácteos" | Solo el que coincide | 1 resultado | Aprobada |
| CP-23 | Baja y reactivación | Proveedor activo | Baja, después reactivación | Oculto entre los activos, visible con "ver todos", y de vuelta al reactivar | Correcto | Aprobada |
| CP-24 | Proveedor inexistente | — | Id 999999 | `404` | Correcto | Aprobada |

### Productos

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-25 | Alta con proveedor | Proveedor activo | Producto con ese proveedor | `201`, con el proveedor incluido | Correcto | Aprobada |
| CP-26 | Stock bajo | — | Stock 5, mínimo 20 | `lowStock` en verdadero | Correcto | Aprobada |
| CP-27 | Precio inválido | — | Precio -5 | `400`, error en el campo `price` | Correcto | Aprobada |
| CP-28 | Código repetido | Existe el código | Otro producto con ese código | `409` | Correcto | Aprobada |
| CP-29 | Oferta más cara (CU-19 exc. 4a) | — | Oferta de 150 con precio 100 | `409` | Correcto | Aprobada |
| CP-30 | Proveedor dado de baja | Proveedor inactivo | Producto con ese proveedor | `409` | Correcto | Aprobada |
| CP-31 | La edición no cambia el stock | Producto con stock 60 | Modificación con stock 999 | Cambian nombre y precio; el stock queda en 60 | Correcto | Aprobada |
| CP-32 | Búsqueda por nombre y por código | Dos productos | "gaseosa" y "7790895" | El producto que coincide | Correcto | Aprobada |
| CP-33 | Baja de producto | Producto activo | Baja | `204` y queda inactivo | Correcto | Aprobada |

### Usuarios

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-34 | Alta sin exponer la contraseña | — | Usuario con contraseña | `201`; la respuesta no trae la contraseña ni el hash | Correcto | Aprobada |
| CP-35 | Contraseña hasheada (RNF-02) | — | Contraseña "Clave1234" | En la base se guarda un hash BCrypt que corresponde a esa contraseña | Correcto | Aprobada |
| CP-36 | Usuario repetido (CU-18 exc. 5a) | Existe el usuario | Otro con el mismo nombre de usuario | `409` | Correcto | Aprobada |
| CP-37 | Contraseña débil | — | Contraseña "corta" | `400`, error en el campo `password` | Correcto | Aprobada |
| CP-38 | Contraseña obligatoria en el alta | — | Alta sin contraseña | `409` | Correcto | Aprobada |
| CP-39 | Editar sin cambiar la contraseña | Usuario existente | Modificación sin contraseña | Se conserva la anterior | Correcto | Aprobada |
| CP-40 | Último administrador (CU-18 exc. 3a) | Un solo administrador | Darlo de baja | `409` | Correcto | Aprobada |
| CP-41 | Baja con otro administrador | Dos administradores | Dar de baja a uno | `204` | Correcto | Aprobada |
| CP-42 | Quitar el rol al último administrador | Un solo administrador | Cambiar su rol a Empleado | `409` | Correcto | Aprobada |

### Sistema completo, en el navegador

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-43 | Mapeo contra la base real | MySQL con el esquema | Arranque del backend | Arranca sin errores de validación del esquema | Correcto | Aprobada |
| CP-44 | Listados con datos reales | Datos de prueba cargados | Abrir las tres pantallas | 7 productos, 3 proveedores, 2 usuarios, con oferta tachada y stock bajo marcado | Correcto | Aprobada |
| CP-45 | Formulario incompleto | — | Guardar un producto vacío | Aviso general y error debajo de nombre y precio | Correcto | Aprobada |
| CP-46 | Edición de producto | Producto con proveedor y vencimiento | Abrir "Editar" | Datos cargados y stock bloqueado | Correcto | Aprobada |
| CP-47 | Aviso de baja con stock (CU-02 exc. 3b) | Producto con 40 unidades | Dar de baja | Pide confirmación mencionando las 40 unidades | Correcto | Aprobada |
| CP-48 | Último administrador en pantalla | Un solo administrador | Darlo de baja | Mensaje de error y el usuario sigue activo | Correcto | Aprobada |

### Inicio de sesión y permisos

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-49 | Acceso sin sesión | Sin iniciar sesión | Pedir productos, proveedores y la alerta de stock | `401` | Correcto | Aprobada |
| CP-50 | Estado público | Sin iniciar sesión | `GET /api/status` | `200` | Correcto | Aprobada |
| CP-51 | Inicio de sesión (CU-01) | Usuario activo | Usuario y contraseña correctos | `200` con el usuario y su rol, sin la contraseña | Correcto | Aprobada |
| CP-52 | La sesión se mantiene | Sesión iniciada | Pedir productos y `/api/auth/me` | Responden sin volver a mandar la contraseña | Correcto | Aprobada |
| CP-53 | Cierre de sesión | Sesión iniciada | `POST /api/auth/logout` | `204` y la sesión queda invalidada en el servidor | Correcto | Aprobada |
| CP-54 | Contraseña incorrecta (CU-01 exc. 4a) | Usuario activo | Contraseña equivocada | `401`, "Usuario o contraseña incorrectos" | Correcto | Aprobada |
| CP-55 | Usuario inexistente | — | Usuario que no existe | `401` con el mismo mensaje que el caso anterior | Correcto | Aprobada |
| CP-56 | Campos vacíos (CU-01 exc. 3a) | — | Usuario y contraseña vacíos | `400` con el error de los dos campos | Correcto | Aprobada |
| CP-57 | Usuario dado de baja | Usuario inactivo | Su usuario y contraseña | `401`, "El usuario está dado de baja" | Correcto | Aprobada |
| CP-58 | Permisos del Empleado (RF-11) | Sesión de Empleado | Pedir usuarios, productos y proveedores | `403` en usuarios; `200` en productos y proveedores | Correcto | Aprobada |
| CP-59 | Permisos del Administrador | Sesión de Administrador | Pedir usuarios | `200` | Correcto | Aprobada |
| CP-60 | Empleado edita sin cambiar el precio | Producto de $900.00, sesión de Empleado | Nombre nuevo y precio "900" | `200`: mismo precio con otra escritura | Correcto | Aprobada |
| CP-61 | Empleado cambia el precio (CU-20) | Producto de $900.00, sesión de Empleado | Precio 1000 | `403` | Correcto | Aprobada |
| CP-62 | Empleado pone una oferta (CU-19) | Sesión de Empleado | Alta de producto en oferta | `403` | Correcto | Aprobada |

### Inicio de sesión en el navegador

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-63 | Pantalla de inicio de sesión | Sin sesión | Abrir el sistema y apretar "Ingresar" vacío | Se ve el login y se marcan los dos campos | Correcto | Aprobada |
| CP-64 | Error de credenciales en pantalla | Sin sesión | `vendedor` con una contraseña equivocada | Aviso "Usuario o contraseña incorrectos" | Correcto | Aprobada |
| CP-65 | Menú del Empleado | Sesión de `vendedor` | Recorrer el menú y escribir `/users` en la dirección | El menú no muestra Usuarios y la dirección vuelve al inicio | Correcto | Aprobada |
| CP-66 | Formulario del Empleado | Sesión de `vendedor` | Editar el aceite, que está en oferta, y guardar sin cambios | Precio y oferta bloqueados; guarda sin error | Correcto | Aprobada |
| CP-67 | Sesión al recargar | Sesión de `vendedor` | Recargar la página | Sigue conectado | Correcto | Aprobada |
| CP-68 | Cierre de sesión | Sesión de `vendedor` | "Cerrar sesión" | Vuelve al login y `/api/auth/me` responde `401` | Correcto | Aprobada |
| CP-69 | Menú y formulario del Administrador | Sesión de `admin` | Recorrer el menú y editar el aceite | Aparece Usuarios; precio y oferta editables | Correcto | Aprobada |

### Stock y alertas

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-70 | Carga inicial (RF-02) | Sesión de Empleado | Alta de un producto con 40 unidades | Un movimiento `CARGA_INICIAL` de 0 a 40, con el usuario de la sesión | Correcto | Aprobada |
| CP-71 | Ajuste manual (CU-04) | Producto con 60 unidades | Stock nuevo 48, motivo "Rotura de 12 paquetes" | `200` con stock 48; movimiento `AJUSTE_MANUAL` de 60 a 48 con motivo, usuario y fecha, primero en la lista | Correcto | Aprobada |
| CP-72 | Ajuste que deja stock bajo (CU-04 paso 7) | Producto con 30 unidades y mínimo 20 | Stock nuevo 8 | `lowStock` en `true` y el producto aparece en la alerta | Correcto | Aprobada |
| CP-73 | Stock negativo (CU-04 exc. 5a) | Producto activo | Stock nuevo -3 | `400`, "El stock no puede ser negativo" | Correcto | Aprobada |
| CP-74 | Motivo vacío | Producto activo | Motivo con un espacio | `400`, "El motivo es obligatorio" | Correcto | Aprobada |
| CP-75 | Ajuste sin cambios | Producto con 10 unidades | Stock nuevo 10 | `409`, "El stock nuevo es igual al actual." | Correcto | Aprobada |
| CP-76 | Ajuste de un producto dado de baja | Producto inactivo | Stock nuevo 0 | `409` | Correcto | Aprobada |
| CP-77 | Producto inexistente | — | Ajuste y movimientos del producto 9999 | `404` en los dos | Correcto | Aprobada |
| CP-78 | Alerta de stock bajo (RF-09) | Uno normal, uno en el mínimo, uno sin stock y uno dado de baja | `GET /api/stock/low-stock` | Solo los dos activos, primero el de menos stock | Correcto | Aprobada |
| CP-79 | Alerta de vencimiento (RF-09) | Vencido, vence en 10 días, en 60 días, sin fecha, sin stock y dado de baja | `GET /api/stock/expiring`, y después con `days=90` | Con 30 días: el vencido y el de 10 días, en ese orden. Con 90: suma el de 60 | Correcto | Aprobada |

### Stock y alertas en el navegador

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-80 | Mapeo de los movimientos | MySQL con el esquema | Arranque del backend | Arranca sin errores: `StockMovement` coincide con `movimientos_stock` | Correcto | Aprobada |
| CP-81 | Alertas en el panel (CU-06) | Datos de prueba, sesión de `vendedor` | Abrir el panel | Leche entera 1L en stock bajo (8 de 20) y por vencer (en 24 días) | Correcto | Aprobada |
| CP-82 | Acceso desde la alerta de stock (CU-06 paso 6) | Panel con alertas | "Ajustar stock" en la leche | Abre Stock con la leche ya buscada | Correcto | Aprobada |
| CP-83 | Acceso desde la alerta de vencimiento | Panel con alertas | "Ver producto" en la leche | Abre Productos con la leche ya buscada | Correcto | Aprobada |
| CP-84 | Ajuste vacío en pantalla | Ventana de ajuste abierta | "Guardar ajuste" sin datos | Aviso general y error debajo de los dos campos | Correcto | Aprobada |
| CP-85 | Ajuste sin cambios en pantalla | Leche con 8 unidades | Stock nuevo 8 y un motivo | Aviso "El stock nuevo es igual al actual." | Correcto | Aprobada |
| CP-86 | Stock negativo en pantalla | Ventana de ajuste abierta | Stock nuevo -1 | Error "El stock no puede ser negativo" debajo del campo | Correcto | Aprobada |
| CP-87 | Ajuste que deja stock bajo en pantalla | Galletitas con 12 unidades y mínimo 8, sesión de `vendedor` | Stock nuevo 6, motivo "Rotura de 6 paquetes en el depósito" | Muestra "Diferencia: -6" antes de guardar; al guardar, aviso naranja y el panel pasa a 2 productos con stock bajo | Correcto | Aprobada |
| CP-88 | Movimientos en pantalla | Ajuste del caso anterior | "Movimientos" de las galletitas | El ajuste de `vendedor` arriba (-6) y la carga inicial de `admin` abajo (+12) | Correcto | Aprobada |
| CP-89 | Hora de los movimientos | Backend contra MySQL | Un ajuste por la API | `fecha_hora` igual a la hora de MySQL en ese momento | Correcto | Aprobada |
| CP-90 | Stock consistente con los movimientos | Después de los ajustes | Consulta 10 de `03-consultas-de-ejemplo.sql` | Ningún producto con diferencias | Correcto | Aprobada |

### Venta y ticket

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-91 | Venta en efectivo (CU-11) | Yerba a $4.850 y fideos a $1.200, sesión de Empleado | 2 yerbas, 3 fideos, descuento 100 | `201`: subtotal 13.300, total 13.200, estado confirmada, dos renglones con nombre y precio, pago en efectivo aprobado por 13.200 | Correcto | Aprobada |
| CP-92 | Descuento de stock (CU-12) | Yerba con 40 unidades | Venta de 2 | Stock en 38 y movimiento `VENTA` de 40 a 38 con motivo "Venta N°" y el número | Correcto | Aprobada |
| CP-93 | Venta por transferencia | Producto con stock | Forma de pago `TRANSFERENCIA`, sin descuento | `201`, descuento en cero y pago por transferencia | Correcto | Aprobada |
| CP-94 | Precio de oferta | Aceite a $3.400 en oferta a $2.990 | Venta de 1 | Renglón y total a $2.990 | Correcto | Aprobada |
| CP-95 | El ticket conserva el precio | Venta de arroz a $900 | Cambiar el precio a $1.100 y volver a pedir la venta | El renglón sigue a $900 | Correcto | Aprobada |
| CP-96 | Stock insuficiente (CU-08 exc. 4a) | Leche con 5 unidades | Venta de 6 | `409`, "No hay stock suficiente de Leche 1L. Disponible: 5."; el stock sigue en 5 | Correcto | Aprobada |
| CP-97 | Venta sin productos (CU-11 exc. 1a) | — | Lista de productos vacía | `400`, "Agregue al menos un producto a la venta" | Correcto | Aprobada |
| CP-98 | Cantidad en cero (CU-08 exc. 3a) | Producto con stock | Cantidad 0 | `400`, "La cantidad tiene que ser mayor a cero" | Correcto | Aprobada |
| CP-99 | Forma de pago obligatoria | Producto con stock | Sin forma de pago | `400`, "Elija la forma de pago" | Correcto | Aprobada |
| CP-100 | Descuento igual al subtotal | Producto de $1.650 | Descuento de $1.650 | `409`, "El descuento tiene que ser menor al subtotal." | Correcto | Aprobada |
| CP-101 | Producto dado de baja | Producto inactivo | Venta de 1 | `409` | Correcto | Aprobada |
| CP-102 | Producto repetido | Leche con 5 unidades | Dos renglones de 3 | `409`: si se aceptara, cada renglón pasaría el control por separado y se venderían 6 | Correcto | Aprobada |
| CP-103 | Venta para el ticket (CU-14) | Venta registrada | `GET /api/sales/{id}`, y después una venta que no existe | `200` con fecha, renglones y pago; `404` para la inexistente | Correcto | Aprobada |

### Venta y ticket en el navegador

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-104 | Mapeo de las ventas | MySQL con el esquema | Arranque del backend | Arranca sin errores: `Sale`, `SaleDetail` y `Payment` coinciden con sus tablas | Correcto | Aprobada |
| CP-105 | Producto no encontrado (CU-07 exc. 3a) | Sesión de `vendedor` | Buscar "chocolate" | Aviso "No hay ningún producto con ese nombre o código." | Correcto | Aprobada |
| CP-106 | Cantidad mayor al stock en pantalla | Leche con 8 unidades en el carrito | Cantidad 9 | El renglón dice "Hay 8 disponibles" y no se puede confirmar | Correcto | Aprobada |
| CP-107 | Agregar de más (CU-07 exc. 5a) | Leche con 8 unidades, 8 en el carrito | "Agregar" otra vez | Aviso "Solo hay 8 unidades" y el carrito no cambia | Correcto | Aprobada |
| CP-108 | Descuento igual al subtotal en pantalla | Carrito de $13.200 | Descuento 13.200 | Error en el descuento y no se puede confirmar | Correcto | Aprobada |
| CP-109 | Vuelto | Total de $13.000 en efectivo | Paga con 15.000, después con 10.000 | "Vuelto: $ 2.000,00"; después "Faltan $ 3.000,00" y no se puede confirmar | Correcto | Aprobada |
| CP-110 | Venta completa (CU-11) | Datos de prueba | 2 yerbas, 1 aceite en oferta y 3 fideos, descuento 290, paga con 20.000 | Vuelto $4.000; al confirmar, ticket N° 000001 por $16.000 con el aceite a $2.990 | Correcto | Aprobada |
| CP-111 | Ticket leído desde la base | Venta del caso anterior | Abrir el ticket | La pantalla pide la venta al servidor y muestra los tres renglones en orden y el pago | Correcto | Aprobada |
| CP-112 | Impresión (CU-15) | Ticket abierto | Vista de impresión | Sale solo el comprobante, de 72 mm, sin menú ni botones | Correcto | Aprobada |
| CP-113 | Descuento de stock en pantalla (CU-12) | Venta del caso CP-110 | Movimientos de la yerba | Movimiento "Venta" de 38 a 36 con motivo "Venta N° 1" | Correcto | Aprobada |
| CP-114 | Venta registrada en la base | Venta del caso CP-110 | Consultas 6, 7 y 8 de `03-consultas-de-ejemplo.sql` | Los tres renglones con su precio, la venta con su empleado y su forma de pago, y $16.000 recaudados en efectivo | Correcto | Aprobada |
| CP-115 | Stock consistente después de vender | Venta del caso CP-110 | Consulta 10 | Ningún producto con diferencias | Correcto | Aprobada |

### Historial y lector

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-116 | Producto por código (CU-03) | Gaseosa con código 7790895667788 | `GET /api/products/barcode/7790895667788` | `200` con la gaseosa | Correcto | Aprobada |
| CP-117 | Código inexistente (CU-03 paso 6) | — | Código 7790000099999 | `404`, "No hay ningún producto con el código 7790000099999" | Correcto | Aprobada |
| CP-118 | Historial del período (CU-16) | Venta del empleado y después del administrador, hoy | Desde y hasta: hoy | Las dos, primero la del administrador, con renglones y pago | Correcto | Aprobada |
| CP-119 | Filtro por empleado | Las dos ventas | `username=vendedor` | Solo la del empleado, por $9.700 | Correcto | Aprobada |
| CP-120 | Filtro por producto | Las dos ventas | `product=leche` | Solo la que tiene leche | Correcto | Aprobada |
| CP-121 | Período sin ventas (CU-16 exc. 3a) | Las dos ventas, hoy | Desde y hasta: ayer | Lista vacía | Correcto | Aprobada |
| CP-122 | Período invertido | — | Desde mañana, hasta hoy | `409`, "La fecha desde no puede ser posterior a la fecha hasta." | Correcto | Aprobada |
| CP-123 | Fecha faltante o mal escrita | — | Sin fecha hasta; después "21/09/2026" | `400` en los dos, "Falta un dato de la búsqueda o tiene un formato inválido." | Correcto | Aprobada |
| CP-124 | Historial del Empleado (RF-07) | Sesión de Empleado | Pedir el historial | `403` | Correcto | Aprobada |

### Historial y lector en el navegador

| ID | Funcionalidad | Condición inicial | Datos de entrada | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|---|
| CP-125 | Historial del mes (CU-16) | Ventas N° 1 y N° 2, sesión de `admin` | Abrir "Historial de ventas" | Del 1/9 a hoy: 2 ventas, la más nueva primero; recaudado $33.880, todo en efectivo | Correcto | Aprobada |
| CP-126 | Filtro por empleado en pantalla | Historial abierto | Empleado `vendedor` | 1 venta de $16.000, y la dirección guarda el filtro | Correcto | Aprobada |
| CP-127 | Filtro por producto en pantalla | Historial abierto | "azúcar"; después, además, empleado `vendedor` | La venta N° 2; con los dos filtros, "No se encontraron ventas con esos filtros." | Correcto | Aprobada |
| CP-128 | Ticket desde el historial (CU-16 paso 5) | Historial filtrado por "azúcar" | "Ver ticket" y "Volver al historial" | Ticket N° 000002; al volver, el mismo filtro y la misma venta | Correcto | Aprobada |
| CP-129 | Período contra MySQL | Sesión de `admin` | Período invertido; período sin ventas; sin fecha hasta | `409`, lista vacía y `400` | Correcto | Aprobada |
| CP-130 | Historial para el Empleado | Sesión de `vendedor` | Recorrer el menú, ir a `/sales` y pedir la API | Sin la opción en el menú, la dirección vuelve al inicio, la API responde `403` y el ticket sigue respondiendo `200` | Correcto | Aprobada |
| CP-131 | Lector en la venta (CU-03 paso 4) | Nueva venta, sesión de `vendedor` | Leer yerba, aceite y otra vez yerba | 2 renglones, la yerba con 2 unidades; el buscador queda vacío después de cada lectura | Correcto | Aprobada |
| CP-132 | Código inexistente en la venta (CU-03 paso 6) | Nueva venta | Leer un código que no existe | Un solo aviso, con el botón "Dar de alta con ese código" | Correcto | Aprobada |
| CP-133 | Alta desde el lector | Aviso del caso anterior | "Dar de alta", mermelada a $2.300 con 15 unidades | El formulario trae el código; al guardar, la mermelada entra al carrito y queda su carga inicial | Correcto | Aprobada |
| CP-134 | Lector en el catálogo (CU-03 paso 5) | Productos | Leer el código de la gaseosa | Se abre la ficha de la gaseosa | Correcto | Aprobada |
| CP-135 | El buscador conserva el foco | Nueva venta, con el cursor en una cantidad | "Agregar" | El cursor vuelve al buscador | Correcto | Aprobada |
| CP-136 | Consultas a la base | Backend contra MySQL | Historial, ticket y movimientos | Una sola consulta para el historial y una para el ticket, sin buscar productos ni usuarios aparte | Correcto | Aprobada |
| CP-137 | Stock consistente | Después del alta del caso CP-133 | Consulta 10 | Ningún producto con diferencias | Correcto | Aprobada |

## 9.3 Incidencias detectadas

| Fase | Incidencia | Solución |
|---|---|---|
| 2 | En una pantalla de 1280 píxeles la tabla de productos no entraba y el botón "Dar de baja" quedaba cortado | Los nombres largos pasan a una segunda línea y el precio de oferta se muestra en dos renglones |
| 2 | El campo de un formulario con error y con el cursor adentro mostraba el borde del foco en negro, tapando el rojo del error | El borde del foco toma el color del error |
| 2 | La línea divisoria de la columna de acciones quedaba desfasada respecto de las demás | Se corrigió el estilo de esa celda para que respete el formato de la tabla |
| 2 | Al editar un producto, el campo de stock decía "Stock inicial" | Dice "Stock actual" al editar y "Stock inicial" en el alta |
| 4 | Los movimientos guardados por la aplicación quedaban con 3 horas de más que los cargados con los scripts, y al leerlos se mostraban con 3 horas de menos | La dirección de conexión tenía `serverTimezone=UTC`: el conector pasaba las horas a UTC al guardar y al leer, mientras MySQL trabaja en hora de Argentina. Se quitó el parámetro y ahora el backend usa la zona horaria de la computadora, que es la misma de MySQL. Se corrigió a mano la hora del único movimiento afectado. CP-89 verifica el arreglo |
| 4 | La fecha de los movimientos se mostraba como "20/9/26, 1:18 p. m." | Se muestra como "20/09/2026, 13:18" |
| 5 | Con un pago en efectivo menor al total, la venta se podía confirmar igual | Si lo que paga el cliente no alcanza, el campo se marca en rojo con lo que falta y no deja confirmar |
| 5 | En una pantalla de 1280 píxeles los nombres del carrito ocupaban dos renglones y el botón "Confirmar venta" quedaba al borde de la pantalla | El carrito ocupa más ancho que la búsqueda |
| 5 | El campo de cantidad del carrito mostraba el foco en negro | Toma el mismo estilo que los formularios: azul, o rojo si hay error |
| 6 | El historial, el ticket y los movimientos buscaban aparte cada producto y cada usuario relacionados, aunque no salían en la respuesta. En los movimientos, cada venta se traía completa | Esas relaciones pasaron a carga diferida (`LAZY`): no se buscan salvo que el código las use. CP-136 lo verifica |
| 6 | Un dato faltante o mal escrito en la dirección, como una fecha, respondía error 500 | Responde 400 con un mensaje claro. Vale también para el plazo de la alerta de vencimiento |
| 6 | Después de tocar "Agregar" o de dar de alta un producto, el cursor quedaba fuera del buscador y la siguiente lectura del lector se perdía | El cursor vuelve al buscador cada vez que se agrega un producto |
| 6 | Un código inexistente mostraba dos avisos: el del lector y el de la búsqueda | Queda solo el del lector, que tiene el botón para darlo de alta |
| 6 | Los buscadores y filtros mostraban el foco en negro | Toman el mismo estilo que los formularios |

En la fase 3 no se registraron incidencias: los casos pasaron en el primer intento.

## 9.4 Resumen

| Nivel | Casos | Aprobados |
|---|---|---|
| Base de datos | 15 | 15 |
| Backend automático | 73 | 73 |
| Sistema completo | 49 | 49 |
| **Total** | **137** | **137** |

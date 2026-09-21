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

## 9.3 Incidencias detectadas

| Fase | Incidencia | Solución |
|---|---|---|
| 2 | En una pantalla de 1280 píxeles la tabla de productos no entraba y el botón "Dar de baja" quedaba cortado | Los nombres largos pasan a una segunda línea y el precio de oferta se muestra en dos renglones |
| 2 | El campo de un formulario con error y con el cursor adentro mostraba el borde del foco en negro, tapando el rojo del error | El borde del foco toma el color del error |
| 2 | La línea divisoria de la columna de acciones quedaba desfasada respecto de las demás | Se corrigió el estilo de esa celda para que respete el formato de la tabla |
| 2 | Al editar un producto, el campo de stock decía "Stock inicial" | Dice "Stock actual" al editar y "Stock inicial" en el alta |

## 9.4 Resumen

| Nivel | Casos | Aprobados |
|---|---|---|
| Base de datos | 15 | 15 |
| Backend automático | 27 | 27 |
| Sistema completo | 6 | 6 |
| **Total** | **48** | **48** |

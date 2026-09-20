# 1. Análisis y definición del sistema

Este documento resume el análisis del sistema. El desarrollo completo, con el diagrama de casos de
uso, el modelado de procesos en BPMN y los instrumentos de elicitación, está en el documento de
análisis de la materia (`PP2_Entrega3_v2.docx`).

## 1.1 Problema

La gestión manual de ventas, stock, productos y proveedores en comercios pequeños genera errores
frecuentes, pérdida de información y desorganización. El propietario no tiene visibilidad sobre qué
productos están por debajo del stock mínimo o próximos a vencer, ni un historial de ventas que le
permita tomar decisiones. Esto produce pérdidas económicas.

En el comercio relevado, el stock se controla a ojo y las ventas se anotan en papel. Cuando falta
mercadería, se descubre recién cuando un cliente la pide.

## 1.2 Objetivo general

Mejorar el control y la organización de los productos, proveedores y ventas de un almacén mediante
un sistema de gestión web que digitalice los procesos operativos del comercio y brinde información
para la toma de decisiones.

## 1.3 Objetivos específicos

1. Centralizar el catálogo de productos y los datos de los proveedores en una única base de datos.
2. Mantener el stock actualizado de forma automática con cada venta, sin intervención manual.
3. Dejar registrado cada cambio de stock con su motivo, su responsable y su fecha.
4. Avisar en el panel principal cuando un producto llega al stock mínimo o está por vencer.
5. Registrar cada venta con su detalle, su total y el empleado que la realizó.
6. Emitir el comprobante de la venta y conservarlo asociado a su registro.
7. Permitir consultar el historial de ventas con filtros por fecha, producto y empleado.
8. Restringir el acceso mediante usuario y contraseña, con permisos según el rol.

## 1.4 Alcance

El sistema está orientado a comercios pequeños y medianos que necesiten administrar su inventario
y registrar las ventas diarias. Lo usan el propietario y los empleados, desde una terminal dentro
del local.

## 1.5 Límites

| Aspecto | Límite |
|---|---|
| Tecnología | No se integra con terminales físicas de tarjeta. El pago digital es solo con MercadoPago |
| Usuarios | Lo usan el propietario y sus empleados. No tiene acceso público ni de clientes |
| Organización | Un solo comercio. No contempla sucursales |
| Acceso | Solo usuarios registrados por el Administrador |

## 1.6 Actores

| Actor | Tipo | Qué hace en el sistema |
|---|---|---|
| Empleado | Persona | Vende, consulta y carga productos, ajusta stock, ve alertas y proveedores |
| Administrador | Persona | Todo lo del Empleado, más usuarios, historial de ventas, precios y ofertas |
| Lector de código de barras | Hardware | Envía el código escaneado; el sistema lo recibe como si fuera tecleado |
| Impresora de tickets | Hardware | Imprime el comprobante de la venta |
| MercadoPago | Servicio externo | Genera el QR o link de pago y avisa cuando el pago se acredita |

El Administrador hereda todos los permisos del Empleado.

## 1.7 Requisitos funcionales

| Código | Requisito |
|---|---|
| RF-01 | Gestión de productos: alta, baja, modificación y búsqueda por nombre o código |
| RF-02 | Control de stock: descuento automático por venta, ajuste manual y umbral mínimo |
| RF-03 | Carrito de ventas: agregar productos, modificar cantidades, aplicar descuento y confirmar |
| RF-04 | Lector de código de barras para cargar productos en la venta o en el catálogo |
| RF-05 | Pagos con MercadoPago mediante QR o link, y registro del método de pago |
| RF-06 | Generación del ticket con productos, cantidades, precios, total y fecha |
| RF-07 | Historial de ventas con filtros por fecha, producto y empleado |
| RF-08 | Gestión de proveedores y su asociación con los productos |
| RF-09 | Alertas de stock bajo y de vencimiento próximo en el panel principal |
| RF-10 | Sugerencias de productos candidatos a oferta por baja rotación o vencimiento |
| RF-11 | Gestión de usuarios y roles, con acceso protegido por usuario y contraseña |

## 1.8 Requisitos no funcionales

| Código | Requisito | Cómo se verifica |
|---|---|---|
| RNF-01 | Rendimiento: las operaciones frecuentes responden en menos de dos segundos | Medición sobre el catálogo con cientos de productos |
| RNF-02 | Seguridad: acceso con usuario y contraseña, contraseñas hasheadas, credenciales fuera del código | Revisión del hash en la base y del archivo de configuración local |
| RNF-03 | Usabilidad: interfaz clara, mensajes en español, contraste y controles amplios | Recorrido de una venta completa sin manual |
| RNF-04 | Disponibilidad: vender no depende de internet; los datos sobreviven a un corte | Prueba con la conexión desconectada |
| RNF-05 | Mantenibilidad: responsabilidades separadas, código comentado, control de versiones con ramas | Estructura del código y historial del repositorio |
| RNF-06 | Portabilidad: funciona en Windows 10 y 11, con instalación simple | Instalación siguiendo el manual en una máquina limpia |

## 1.9 Procesos que se automatizan

Del modelado de procesos de negocio se desprenden cuatro procesos principales:

1. **Procesar las ventas.** El empleado inicia la venta, agrega productos, elige el método de pago y
   confirma. Es el proceso central del sistema.
2. **Cargar los productos.** Alta y actualización del catálogo. Sin esto, la venta no tiene contra
   qué validar.
3. **Actualizar precios y stock.** De forma manual, o como consecuencia de una venta confirmada.
4. **Generar imágenes de productos.** Previsto para una futura visualización web. Queda fuera del
   alcance de esta entrega.

## 1.10 Casos de uso

| Código | Caso de uso | Actor principal | Requisito |
|---|---|---|---|
| CU-01 | Iniciar sesión | Empleado / Administrador | RF-11 |
| CU-02 | Gestionar productos | Empleado / Administrador | RF-01 |
| CU-03 | Escanear productos | Lector de código de barras | RF-04 |
| CU-04 | Gestionar stock | Empleado / Administrador | RF-02 |
| CU-05 | Gestionar proveedores | Empleado / Administrador | RF-08 |
| CU-06 | Gestionar alertas | Empleado / Administrador | RF-09 |
| CU-07 | Iniciar venta | Empleado | RF-03 |
| CU-08 | Agregar a carrito | Empleado | RF-03 |
| CU-09 | Seleccionar método de pago | Empleado | RF-05 |
| CU-10 | Pago con MercadoPago | MercadoPago | RF-05 |
| CU-11 | Confirmar venta | Empleado | RF-03 |
| CU-12 | Actualizar stock | Sistema | RF-02 |
| CU-13 | Registrar en historial | Sistema | RF-07 |
| CU-14 | Generar ticket | Sistema | RF-06 |
| CU-15 | Imprimir ticket | Impresora de tickets | RF-06 |
| CU-16 | Consultar historial de ventas | Administrador | RF-07 |
| CU-17 | Gestionar roles | Administrador | RF-11 |
| CU-18 | Gestionar usuarios | Administrador | RF-11 |
| CU-19 | Gestionar ofertas | Administrador | RF-10 |
| CU-20 | Modificar precios y productos | Administrador | RF-01 |

CU-17 queda fuera del desarrollo. El sistema tiene dos roles fijos, Administrador y Empleado, y
hacerlos configurables agregaría una tabla de permisos y una consulta en cada pedido para resolver
algo que no cambia. La decisión está justificada en el documento de arquitectura.

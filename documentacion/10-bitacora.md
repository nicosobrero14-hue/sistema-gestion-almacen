# 10. Bitácora del desarrollo

Registro de lo que se hizo en cada fase, las decisiones que se tomaron y cómo se verificó el
resultado.

---

## Fase 0 — Repositorio, documentación y modelo de datos

**20 de septiembre de 2026**

### Qué se hizo

1. Se creó el repositorio con la estructura de carpetas: `base-de-datos/`, `documentacion/` y las
   dos carpetas del código, que se completan en la fase siguiente.
2. Se escribió el esquema completo de la base: siete tablas con sus claves, restricciones e
   índices.
3. Se cargaron los datos de prueba: dos usuarios, tres proveedores y ocho productos, con casos
   variados a propósito (con y sin código de barras, con y sin vencimiento, uno bajo el mínimo,
   uno en oferta y uno dado de baja).
4. Se escribieron diez consultas de ejemplo que cubren las necesidades de los requisitos.
5. Se documentó el análisis, la arquitectura, las tecnologías y el modelo de datos, con sus dos
   diagramas.

### Decisiones

**El modelo de datos se diseña completo desde el principio, no fase por fase.** Las tablas de
ventas y pagos se crean ahora aunque el módulo de ventas llegue en la fase 5. Diseñar el modelo
entero de una vez evita tener que modificar tablas más adelante, que es cuando aparecen los
problemas: datos que migrar, código que ajustar y consultas que revisar.

**El número de ticket es el identificador de la venta.** No se agrega una secuencia aparte. El
comercio no emite comprobantes fiscales, así que no hay requisito de numeración propia.

**Las alertas no se guardan en la base.** Se calculan con una consulta cada vez que se abre el
panel. Una alerta guardada queda desactualizada apenas cambia el stock.

El detalle de estas decisiones y del resto está en `04-modelo-datos.md`.

### Cómo se verificó

El esquema se ejecutó sobre una base temporal, separada de la de desarrollo:

- Las siete tablas se crearon sin errores y los datos de prueba cargaron completos: 3 proveedores,
  8 productos, 2 usuarios y 8 movimientos de carga inicial.
- Se intentó insertar diez datos inválidos (stock negativo, precio en cero, oferta más cara que el
  precio, rol inexistente, usuario repetido, correo repetido, código de barras repetido, descuento
  mayor al subtotal, cantidad cero y tipo de movimiento inventado). La base los rechazó a todos.
- Se cargó una venta completa con dos renglones, su pago y sus movimientos de stock. El stock quedó
  consistente con la suma de los movimientos.
- Se borró un proveedor: sus cuatro productos sobrevivieron, sin proveedor asignado.
- Se borró un producto vendido: el renglón de la venta conservó el nombre y el precio.
- Se borró una venta: se fueron sus renglones y su pago, y el movimiento de stock quedó registrado.

La base temporal se eliminó al terminar.

### Estado al cerrar la fase

La base de datos está diseñada, documentada y probada. El repositorio tiene la documentación de las
cuatro primeras actividades de la guía. Falta el código, que empieza en la fase 1.

# De dónde salieron estas fotos

Son las fotos de los ocho primeros productos de `02-datos-de-prueba.sql`. No las sacamos nosotros:
las trajo el sistema desde los catálogos públicos, igual que cuando se escanea un producto nuevo en
el mostrador. Después pasaron por `herramientas/normalizar-imagen.py`, que las recortó, las centró y
las dejó de 600×600.

| Archivo | Producto | Código de barras |
|---|---|---|
| producto-1.jpg | Leche La Serenísima clásica 1 L | 7790742363008 |
| producto-2.jpg | Fideos Matarazzo tallarín N°5 500 g | 7790070336316 |
| producto-3.jpg | Aceite Natura girasol 900 ml | 7790272001005 |
| producto-4.jpg | Arroz Molinos Ala largo fino 1 kg | 7791120031557 |
| producto-5.jpg | Galletitas 9 de Oro clásicas 200 g | 7792200000159 |
| producto-6.jpg | Coca-Cola 2,25 L | 7790895000997 |
| producto-7.jpg | Celusal sal fina 500 g | 7790072002080 |
| producto-8.jpg | Seven Up 1.5 L | 7791813444411 |

Con el código de barras se puede abrir la ficha original en
`https://world.openfoodfacts.org/producto/<código>` y ver quién subió cada foto.

## Licencia

Según los términos de uso de Open Food Facts, los datos están bajo la **Open Database License** y
las fotos de los productos bajo **Creative Commons Attribution ShareAlike**. Por eso están
acreditadas acá: la licencia pide atribución.

Los mismos términos aclaran algo que conviene dejar dicho: esa licencia cubre la fotografía y los
derechos de quien la sacó, no las marcas que aparecen en el envase. Las marcas son de sus dueños y
acá figuran solamente porque los productos de prueba son productos reales de un almacén.

## Los dos que no tienen foto

El queso cremoso y el jamón cocido se venden sueltos, por kilo. No tienen código de barras que leer
ni ficha en ningún catálogo, así que arrancan sin foto y el sistema les muestra el recuadro con la
inicial. Es a propósito: ningún comercio tiene la foto de todo su catálogo el primer día, y esa
pantalla tiene que verse bien igual.

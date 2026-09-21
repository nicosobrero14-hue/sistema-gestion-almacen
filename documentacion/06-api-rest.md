# 6. API REST

Base: `http://localhost:8080/api` · Formato: JSON

## 6.1 Códigos de respuesta

| Código | Cuándo |
|---|---|
| `200 OK` | Consulta o modificación correcta |
| `201 Created` | Alta correcta |
| `204 No Content` | Baja o reactivación correcta |
| `400 Bad Request` | Los datos no pasaron la validación, o falta un dato de la búsqueda o está mal escrito. En la validación trae el error de cada campo |
| `401 Unauthorized` | No hay sesión, o el usuario y la contraseña no coinciden |
| `403 Forbidden` | El rol no alcanza para lo que se quiere hacer |
| `404 Not Found` | El registro pedido no existe |
| `409 Conflict` | Los datos son válidos pero rompen una regla del negocio |
| `500 Internal Server Error` | Error inesperado. El detalle queda en el log del servidor |

## 6.2 Formato de los errores

Todos los errores tienen la misma forma:

```json
{
  "message": "Hay campos con errores",
  "errors": {
    "name": "El nombre es obligatorio",
    "price": "El precio debe ser mayor a cero"
  }
}
```

`errors` aparece solo en los `400`. La clave de cada error es el nombre del campo, así el
formulario lo muestra debajo del campo que corresponde.

## 6.3 Convenciones

- Todos los endpoints piden haber iniciado sesión, salvo `POST /api/auth/login` y
  `GET /api/status`.
- Los pedidos que modifican datos (`POST`, `PUT`, `PATCH`) tienen que mandar el token CSRF: el
  servidor lo deja en la cookie `XSRF-TOKEN` y el cliente lo devuelve en la cabecera
  `X-XSRF-TOKEN`. El frontend lo hace solo.
- Los listados aceptan `search` (texto a buscar) y `activeOnly` (`true` por defecto, oculta los
  dados de baja).
- No hay `DELETE`: el sistema no borra registros. Se dan de baja con `PATCH /{id}/deactivate` y se
  reactivan con `PATCH /{id}/activate`.
- El alta y la modificación reciben un DTO. La respuesta devuelve el registro completo.

---

## 6.4 Estado — `/api/status`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/status` | Informa si la aplicación y la base responden |

```json
{ "application": "ok", "database": "ok" }
```

Es público: sirve para verificar la instalación sin tener que iniciar sesión.

---

## 6.5 Sesión — `/api/auth`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Inicia sesión y devuelve el usuario conectado |
| `POST` | `/api/auth/logout` | Cierra la sesión en el servidor → `204` |
| `GET` | `/api/auth/me` | Devuelve quién está conectado. `401` si no hay sesión |

**Lo que entra en el login:**

```json
{ "username": "admin", "password": "Admin1234" }
```

**Lo que sale:** el usuario, igual que en `/api/users`, sin la contraseña.

**Devuelve `401` si:**

- El usuario no existe o la contraseña no coincide: *"Usuario o contraseña incorrectos"*.
- El usuario está dado de baja: *"El usuario está dado de baja. Consulte con el administrador."*

---

## 6.6 Proveedores — `/api/suppliers`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/suppliers?search=&activeOnly=true` | Lista y busca por nombre o apellido |
| `GET` | `/api/suppliers/{id}` | Trae un proveedor |
| `POST` | `/api/suppliers` | Alta → `201` |
| `PUT` | `/api/suppliers/{id}` | Modificación |
| `PATCH` | `/api/suppliers/{id}/deactivate` | Baja → `204` |
| `PATCH` | `/api/suppliers/{id}/activate` | Reactivación → `204` |

**Lo que entra:**

```json
{
  "name": "Distribuidora del Sur",
  "lastName": null,
  "email": "ventas@distsur.com.ar",
  "phone": "+54 351 4567890",
  "address": "Av. Colón 1250, Córdoba"
}
```

**Lo que sale:**

```json
{
  "id": 1,
  "name": "Distribuidora del Sur",
  "lastName": null,
  "email": "ventas@distsur.com.ar",
  "phone": "+54 351 4567890",
  "address": "Av. Colón 1250, Córdoba",
  "active": true,
  "createdAt": "2026-09-20T16:05:12"
}
```

**Devuelve `409` si:** el email ya lo usa otro proveedor.

---

## 6.7 Productos — `/api/products`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/products?search=&activeOnly=true` | Lista y busca por nombre o código de barras |
| `GET` | `/api/products/{id}` | Trae un producto |
| `GET` | `/api/products/barcode/{barcode}` | Trae el producto con ese código de barras exacto. Lo usa el lector (CU-03). `404` si no existe |
| `POST` | `/api/products` | Alta → `201` |
| `PUT` | `/api/products/{id}` | Modificación. No cambia el stock |
| `PATCH` | `/api/products/{id}/deactivate` | Baja → `204` |
| `PATCH` | `/api/products/{id}/activate` | Reactivación → `204` |

**Lo que entra:**

```json
{
  "name": "Yerba Mate 1kg",
  "description": "Yerba mate elaborada con palo",
  "price": 4850.00,
  "stock": 40,
  "minimumStock": 10,
  "barcode": "7790010001234",
  "expirationDate": null,
  "onOffer": false,
  "offerPrice": null,
  "supplierId": 1
}
```

`stock` solo se usa en el alta. En la modificación se ignora: el stock se cambia con un ajuste
(punto 6.9). El alta deja registrado ese stock inicial como un movimiento de tipo `CARGA_INICIAL`,
con el usuario de la sesión.

**Lo que sale:**

```json
{
  "id": 1,
  "name": "Yerba Mate 1kg",
  "description": "Yerba mate elaborada con palo",
  "price": 4850.00,
  "stock": 40,
  "minimumStock": 10,
  "barcode": "7790010001234",
  "expirationDate": null,
  "onOffer": false,
  "offerPrice": null,
  "active": true,
  "createdAt": "2026-09-20T16:05:12",
  "supplier": { "id": 1, "name": "Distribuidora del Sur", "...": "..." },
  "lowStock": false,
  "salePrice": 4850.00
}
```

`lowStock` y `salePrice` no son columnas, se calculan:

- `lowStock`: `stock <= minimumStock`.
- `salePrice`: el precio que se cobra. Es el de oferta si el producto está en oferta, y si no, el
  normal.

**Devuelve `409` si:**

- El código de barras ya lo usa otro producto.
- Está en oferta y falta el precio de oferta, o es mayor o igual al precio normal.
- El proveedor indicado está dado de baja.

**Devuelve `404` si:** el proveedor indicado no existe.

**Devuelve `403` si quien lo pide es Empleado y:**

- En el alta, marca el producto en oferta (CU-19).
- En la modificación, cambia el precio o la oferta (CU-20). Mandar el mismo precio con otra
  escritura, como `900` en lugar de `900.00`, no cuenta como cambio.

---

## 6.8 Usuarios — `/api/users` (solo Administrador)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/users?search=&activeOnly=true` | Lista y busca por nombre, apellido o usuario |
| `GET` | `/api/users/{id}` | Trae un usuario |
| `POST` | `/api/users` | Alta → `201` |
| `PUT` | `/api/users/{id}` | Modificación |
| `PATCH` | `/api/users/{id}/deactivate` | Baja → `204` |
| `PATCH` | `/api/users/{id}/activate` | Reactivación → `204` |

**Lo que entra:**

```json
{
  "name": "María",
  "lastName": "Gómez",
  "username": "vendedor",
  "password": "Empleado1234",
  "role": "EMPLEADO"
}
```

- `role` admite `ADMIN` o `EMPLEADO`.
- `password` es obligatoria en el alta. En la modificación, si llega `null`, se conserva la actual.
- La contraseña tiene que tener al menos 8 caracteres, con letras y números.

**Lo que sale:** los mismos datos, más `id`, `active` y `createdAt`. **Nunca la contraseña ni su
hash.**

**Devuelve `409` si:**

- El nombre de usuario ya existe.
- Es un alta sin contraseña.
- Se intenta dar de baja al último administrador activo, o quitarle el rol.

---

## 6.9 Stock y alertas — `/api/stock`

| Método | Ruta | Descripción |
|---|---|---|
| `PATCH` | `/api/stock/{productId}` | Ajuste manual del stock (CU-04). Devuelve el producto actualizado |
| `GET` | `/api/stock/{productId}/movements` | Movimientos del producto, del más nuevo al más viejo |
| `GET` | `/api/stock/low-stock` | Alerta de stock bajo (CU-06) |
| `GET` | `/api/stock/expiring?days=30` | Alerta de vencimiento: los que vencen en los próximos días (CU-06) |

Los usan el Empleado y el Administrador.

**Lo que entra en el ajuste:**

```json
{ "newStock": 6, "reason": "Rotura de 6 paquetes en el depósito" }
```

`newStock` es la cantidad que hay ahora, no la diferencia. Es lo que se cuenta en la estantería.

**Lo que sale de los movimientos:**

```json
[
  {
    "id": 16,
    "productName": "Galletitas surtidas",
    "username": "vendedor",
    "type": "AJUSTE_MANUAL",
    "previousStock": 12,
    "newStock": 6,
    "reason": "Rotura de 6 paquetes en el depósito",
    "dateTime": "2026-09-21T17:22:35"
  }
]
```

`type` puede ser `CARGA_INICIAL`, `AJUSTE_MANUAL` o `VENTA`. El nombre del producto y el usuario son
copias: el movimiento se sigue leyendo igual aunque el producto cambie de nombre.

**Las alertas** devuelven una lista de productos, igual que `/api/products`:

- `low-stock`: los activos con el stock en el mínimo o por debajo, del stock más bajo al más alto.
- `expiring`: los activos con stock que vencen dentro de `days` días, incluidos los ya vencidos,
  del más próximo al más lejano. Si no se indica `days`, son 30. Los productos sin fecha de
  vencimiento no entran.

**Devuelve `400` si:** falta el stock nuevo, es negativo, o falta el motivo (CU-04 exc. 5a).

**Devuelve `409` si:**

- El stock nuevo es igual al actual. Un ajuste que no cambia nada solo ensuciaría el historial.
- El producto está dado de baja.

**Devuelve `404` si:** el producto no existe.

---

## 6.10 Ventas — `/api/sales`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/sales` | Confirma una venta (CU-11) → `201` |
| `GET` | `/api/sales/{id}` | Trae una venta con sus renglones y su pago. Es lo que muestra el ticket (CU-14) |
| `GET` | `/api/sales?from=2026-09-01&to=2026-09-30&username=&product=` | Historial (CU-16). **Solo Administrador** |

Confirmar una venta y ver su ticket lo pueden hacer el Empleado y el Administrador. El historial es
solo del Administrador: al Empleado le responde `403`.

**Lo que entra:**

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 4, "quantity": 1 }
  ],
  "discount": 290,
  "paymentMethod": "EFECTIVO"
}
```

- `items` es el carrito: qué productos y cuántos. **El precio no viaja**: lo pone el servidor con el
  precio de venta de cada producto en ese momento.
- `discount` es un descuento en pesos sobre el total. Es opcional.
- `paymentMethod` admite `EFECTIVO` o `TRANSFERENCIA`.

**Lo que sale:**

```json
{
  "id": 1,
  "dateTime": "2026-09-21T17:51:52",
  "username": "vendedor",
  "subtotal": 16290.00,
  "discount": 290.00,
  "total": 16000.00,
  "status": "CONFIRMADA",
  "details": [
    { "id": 1, "productName": "Yerba Mate 1kg", "unitPrice": 4850.00, "quantity": 2, "subtotal": 9700.00 },
    { "id": 2, "productName": "Aceite girasol 900ml", "unitPrice": 2990.00, "quantity": 1, "subtotal": 2990.00 }
  ],
  "payments": [
    { "id": 1, "method": "EFECTIVO", "amount": 16000.00, "status": "APROBADO",
      "dateTime": "2026-09-21T17:51:52", "confirmedAt": "2026-09-21T17:51:52" }
  ]
}
```

El `id` es también el número de ticket. El nombre y el precio de cada renglón son copias: el ticket
no cambia aunque después cambie el producto.

Al confirmar, en la misma transacción, el servidor:

1. Registra la venta con sus renglones.
2. Registra el pago como aprobado.
3. Descuenta el stock de cada producto y deja un movimiento de tipo `VENTA` con el motivo
   "Venta N° 1" (CU-12).

Si algo falla, no se guarda nada y el stock queda como estaba.

**Devuelve `400` si:**

- No hay productos: *"Agregue al menos un producto a la venta"* (CU-11 exc. 1a).
- Una cantidad es cero o negativa (CU-08 exc. 3a).
- El descuento es negativo.
- Falta la forma de pago.

**Devuelve `409` si:**

- Se pide más de lo que hay: *"No hay stock suficiente de Leche entera 1L. Disponible: 8."*
  (CU-07 exc. 5a, CU-08 exc. 4a).
- Un producto está dado de baja.
- Un producto aparece en dos renglones.
- El descuento es igual o mayor al subtotal: el total tiene que quedar mayor a cero.

**Devuelve `404` si:** un producto o la venta pedida no existe.

**El historial:**

- `from` y `to` son obligatorias, con el formato `2026-09-21`. `to` incluye todo ese día.
- `username` filtra por el empleado que hizo la venta. Vacío, no filtra.
- `product` busca ese texto en el nombre de los productos vendidos. Vacío, no filtra.
- Devuelve las ventas completas, con sus renglones y su pago, de la más nueva a la más vieja.

Devuelve `400` si falta una fecha o está mal escrita, y `409` si `from` es posterior a `to`.

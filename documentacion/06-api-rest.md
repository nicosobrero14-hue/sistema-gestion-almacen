# 6. API REST

Base: `http://localhost:8080/api` · Formato: JSON

## 6.1 Códigos de respuesta

| Código | Cuándo |
|---|---|
| `200 OK` | Consulta o modificación correcta |
| `201 Created` | Alta correcta |
| `204 No Content` | Baja o reactivación correcta |
| `400 Bad Request` | Los datos no pasaron la validación. Trae el error de cada campo |
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

`stock` solo se usa en el alta. En la modificación se ignora.

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
  "lowStock": false
}
```

`lowStock` no es una columna: se calcula como `stock <= minimumStock`.

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

# Sistema de Gestión de Almacén y Control de Stock

Sistema web para administrar el catálogo, el stock y las ventas de un comercio de barrio.
Proyecto Final de Software.

El backend es una API REST en Spring Boot, el frontend una aplicación React y los datos se
guardan en MySQL. Todo corre en la terminal del comercio.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `base-de-datos/` | Scripts SQL: esquema, datos de prueba y consultas de ejemplo |
| `documentacion/` | Documentación del proyecto |
| `documentacion/imagenes/` | Diagramas y capturas de pantalla |
| `gestion-almacen/` | Backend en Spring Boot |
| `gestion-almacen-frontend/` | Frontend en React |

## Documentación

| Documento | Contenido |
|---|---|
| [01-analisis.md](documentacion/01-analisis.md) | Problema, objetivos, alcance, actores y requisitos |
| [02-arquitectura.md](documentacion/02-arquitectura.md) | Arquitectura de la solución y decisiones de diseño |
| [03-tecnologias.md](documentacion/03-tecnologias.md) | Herramientas y tecnologías, con su justificación |
| [04-modelo-datos.md](documentacion/04-modelo-datos.md) | Modelo de datos, diccionario y reglas de integridad |
| [05-modulos.md](documentacion/05-modulos.md) | Módulos implementados, con sus reglas y capturas |
| [06-api-rest.md](documentacion/06-api-rest.md) | Descripción de la API |
| [07-instalacion.md](documentacion/07-instalacion.md) | Manual de instalación y configuración |
| [09-pruebas.md](documentacion/09-pruebas.md) | Estrategia de pruebas, casos ejecutados e incidencias |
| [10-bitacora.md](documentacion/10-bitacora.md) | Registro del desarrollo, fase por fase |
| 08-manual-usuario.md | Manual de usuario (fase 5) |

## Fases

El desarrollo avanza por fases. Cada una deja una funcionalidad completa, probada y documentada.

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Repositorio, documentación y modelo de datos | Terminada |
| 1 | Esqueleto: backend y frontend en funcionamiento | Terminada |
| 2 | Alta, baja y modificación de proveedores, productos y usuarios | Terminada |
| 3 | Inicio de sesión y permisos por rol | Terminada |
| 4 | Control de stock y alertas | Terminada |
| 5 | Venta y ticket | Pendiente |
| 6 | Historial de ventas y lector de código de barras | Pendiente |
| 7 | Sugerencias de ofertas y cierre del proyecto | Pendiente |

## Puesta en marcha

Con MySQL 8, Java 25 y Node instalados:

```bash
mysql -u root -p < base-de-datos/01-esquema.sql
```

```bash
mysql -u root -p gestion_almacen < base-de-datos/02-datos-de-prueba.sql
```

Después hay que crear `gestion-almacen/src/main/resources/application-local.properties` con el
usuario y la clave de MySQL, copiando el archivo de ejemplo que está al lado. Ese archivo no se
sube al repositorio.

Backend, desde la carpeta `gestion-almacen`:

```bash
mvnw spring-boot:run
```

Frontend, desde la carpeta `gestion-almacen-frontend`:

```bash
npm install && npm run dev
```

El sistema queda en `http://localhost:5173`. Los datos de prueba traen dos usuarios: `admin` / `Admin1234`
(Administrador) y `vendedor` / `Empleado1234` (Empleado). Son contraseñas de prueba: hay que
cambiarlas antes de usar el sistema en el comercio.

Los pasos detallados y los problemas frecuentes están en el
[manual de instalación](documentacion/07-instalacion.md).

## Pruebas

```bash
cd gestion-almacen && mvnw test
```

Las pruebas corren sobre una base en memoria: no necesitan MySQL ni tocan los datos reales.

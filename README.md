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
| `gestion-almacen/` | Backend en Spring Boot (desde la fase 1) |
| `gestion-almacen-frontend/` | Frontend en React (desde la fase 1) |

## Documentación

| Documento | Contenido |
|---|---|
| [01-analisis.md](documentacion/01-analisis.md) | Problema, objetivos, alcance, actores y requisitos |
| [02-arquitectura.md](documentacion/02-arquitectura.md) | Arquitectura de la solución y decisiones de diseño |
| [03-tecnologias.md](documentacion/03-tecnologias.md) | Herramientas y tecnologías, con su justificación |
| [04-modelo-datos.md](documentacion/04-modelo-datos.md) | Modelo de datos, diccionario y reglas de integridad |
| 05-modulos.md | Módulos implementados (desde la fase 2) |
| 06-api-rest.md | Descripción de la API (desde la fase 2) |
| 07-instalacion.md | Manual de instalación y configuración (fase 1) |
| 08-manual-usuario.md | Manual de usuario (fase 5) |
| 09-pruebas.md | Estrategia de pruebas y casos ejecutados (fase 2) |
| 10-bitacora.md | Registro del desarrollo, fase por fase |

## Fases

El desarrollo avanza por fases. Cada una deja una funcionalidad completa, probada y documentada.

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Repositorio, documentación y modelo de datos | En curso |
| 1 | Esqueleto: backend y frontend en funcionamiento | Pendiente |
| 2 | Alta, baja y modificación de proveedores, productos y usuarios | Pendiente |
| 3 | Inicio de sesión y permisos por rol | Pendiente |
| 4 | Control de stock y alertas | Pendiente |
| 5 | Venta y ticket | Pendiente |
| 6 | Historial de ventas y lector de código de barras | Pendiente |
| 7 | Sugerencias de ofertas y cierre del proyecto | Pendiente |

## Base de datos

Con MySQL 8 instalado y en ejecución:

```bash
mysql -u root -p < base-de-datos/01-esquema.sql
```

```bash
mysql -u root -p gestion_almacen < base-de-datos/02-datos-de-prueba.sql
```

El primer script crea la base y las siete tablas. El segundo carga datos de prueba: dos usuarios,
tres proveedores y ocho productos.

Las credenciales de conexión no están en el repositorio. Se configuran en un archivo local que
Git ignora, explicado en el manual de instalación.

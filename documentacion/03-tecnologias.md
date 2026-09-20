# 3. Herramientas y tecnologías

## 3.1 Cuadro de tecnologías

| Tecnología | Para qué se usa | Por qué se eligió | Dónde |
|---|---|---|---|
| **Java 25** | Lenguaje del backend | Es el lenguaje trabajado en la carrera, con tipado estático que detecta errores al compilar | Backend |
| **Spring Boot 4.1** | Framework del backend | Trae configurado el servidor, la conexión a la base y la seguridad; evita armar todo a mano | Backend |
| **Spring MVC** | Controladores REST | Es la parte de Spring que traduce HTTP a métodos de Java | Controladores |
| **Spring Data JPA** | Acceso a datos | Genera las consultas comunes a partir del nombre del método y deja escribir JPQL cuando hace falta | Repositorios |
| **Hibernate** | Implementación de JPA | Es la que usa Spring Boot por defecto; traduce objetos a filas y valida el mapeo al arrancar | Repositorios |
| **Spring Security** | Autenticación y permisos | Resuelve sesión, hash de contraseñas, CSRF y control por rol sin escribirlo a mano | Seguridad |
| **Bean Validation** | Validación de datos que entran | Las reglas quedan declaradas junto al campo, no repartidas en el código | DTO |
| **Lombok** | Getters, setters y constructores | Saca de las clases el código repetido que no aporta nada al entendimiento | Entidades y DTO |
| **MySQL 8** | Base de datos | Relacional, gratuita y conocida; el modelo tiene relaciones y restricciones que necesitan integridad referencial | Datos |
| **MySQL Workbench** | Diseño y consulta de la base | Permite ver el diagrama entidad-relación y probar consultas antes de llevarlas al código | Datos |
| **React 19** | Interfaz de usuario | Componentes reutilizables y actualización de pantalla sin recargar, que es lo que pide una pantalla de venta | Frontend |
| **Vite** | Servidor de desarrollo y compilado | Arranca rápido, refresca al guardar y evita configurar el empaquetado | Frontend |
| **React Router** | Navegación entre pantallas | Permite que la aplicación tenga direcciones propias sin recargar la página | Frontend |
| **axios** | Cliente HTTP | Centraliza el manejo de errores y el envío del token de seguridad en un solo archivo | Frontend |
| **JUnit 5** | Pruebas automáticas | Es el estándar para probar en Java | Pruebas |
| **MockMvc** | Pruebas de la API | Prueba los endpoints completos sin levantar el servidor ni el navegador | Pruebas |
| **H2** | Base de datos de las pruebas | Corre en memoria: las pruebas no necesitan MySQL y no tocan los datos reales | Pruebas |
| **Maven** | Compilado y dependencias | Descarga las bibliotecas y corre las pruebas con un solo comando | Backend |
| **Git** | Control de versiones | Permite trabajar por ramas y volver atrás cualquier cambio | Todo el proyecto |
| **GitHub** | Repositorio remoto | Guarda el proyecto fuera de la máquina y ordena los cambios con pull requests | Todo el proyecto |
| **GitHub Projects** | Seguimiento de tareas | Tablero Kanban integrado con los issues del propio repositorio | Gestión |
| **GitHub Actions** | Integración continua | Corre las pruebas en cada cambio subido, así un error no queda escondido | Gestión |
| **Markdown** | Documentación | Se versiona junto al código y se lee tanto en GitHub como en texto plano | Documentación |

## 3.2 Cómo se integran en el trabajo diario

El flujo de una funcionalidad, de principio a fin:

1. Se crea un **issue** en GitHub con lo que hay que hacer y se lo pone en el tablero de
   **GitHub Projects**.
2. Se abre una **rama** propia con Git. La rama principal nunca se toca directamente.
3. Se escribe el código en el **backend** y en el **frontend**.
4. Se escriben las **pruebas** con JUnit y MockMvc, que corren sobre **H2**.
5. **Maven** compila y ejecuta todas las pruebas.
6. Se prueba en el navegador y se saca la **captura de pantalla** para la documentación.
7. Se sube la rama y se abre un **pull request**. **GitHub Actions** vuelve a correr las pruebas.
8. Se integra el cambio y el issue se cierra solo.

## 3.3 Qué quedó afuera y por qué

| Herramienta | Motivo |
|---|---|
| Docker | El sistema se instala en la computadora del comercio. Un contenedor agrega una capa que el comerciante no va a poder mantener |
| JWT | Se usa sesión con cookie. El motivo está explicado en el documento de arquitectura |
| MapStruct | Para siete entidades, escribir la conversión a mano es más corto que configurar la herramienta |
| TypeScript | Suma tipado al frontend, pero también configuración y tiempo de aprendizaje. Se priorizó terminar la funcionalidad |

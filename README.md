# BugTracker — Entrega 1

Primera entrega del TP Integrador de la materia **Laboratorio 1** d ela Universidad de Palermo.
Incluye **CRUD de Usuarios** con interfaz **Java Swing** y persistencia en **MySQL** usando **Hibernate + Jakarta Persistence**.
Este módulo es la base para avanzar luego con **Proyectos**, **Incidencias** y **Reportes**.

---

## Qué se entrega en esta versión

* Registro de usuarios.
* Inicio de sesión.
* Pantalla “Mi Perfil” con edición habilitada.
* Panel de administración de usuarios (solo para rol ADMIN): listado, cambio de rol y eliminación de usuarios.
* Navegación por pantallas con `PanelManager` y componentes reutilizables (header, formularios).

---

## Requisitos previos

* Java 11 (JDK) y Maven instalados.
* MySQL 8 en ejecución.
* Base de datos creada con el esquema del proyecto adjuntado (archivo bugstracker_db_creation.sql).
* Archivo de configuración de persistencia ajustado con tus credenciales (usuario, contraseña y URL de la base).

---

## Cómo levantar el proyecto

1. Verificar que MySQL esté levantado y que la base de datos **bugtracker** exista con sus tablas.
2. Ajustar las credenciales de conexión a la base en el archivo de persistencia.
3. Ejecutar la aplicación desde el `Main` del proyecto (por ejemplo, desde tu IDE).
4. Se abrirá la interfaz en la pantalla de **Login**.

---

## Flujo funcional (por rol)

### Usuario no autenticado

* **Login:** ingreso con email y contraseña.
* **Registro:** creación de cuenta (nombre, apellido, email, contraseña y rol).

### Usuario autenticado (USUARIO)

* **Mi Perfil:** ver y editar mail, nombre y apellido. Rol y contraseña se muestran no editables.
* **Cerrar sesión** disponible desde el header.

Para ambos usuarios la **Home** se encuentra vavia, ahí se visualizará más adelante el listado de proyectos.

### Usuario autenticado con rol ADMIN

* Todo lo anterior, más:
* **Usuarios:** listado de todos los usuarios **excepto** el usuario actualmente logueado.

    * **Editar:** permite cambiar el **rol** del usuario.
    * **Eliminar:** permite eliminar un usuario.


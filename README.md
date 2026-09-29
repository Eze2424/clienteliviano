# DonaTrack — Cliente liviano

Interfaz web de DonaTrack, el sistema de registro y trazabilidad de donaciones de
bienes materiales. Es un **proyecto independiente** del backend: no tiene base de
datos ni lógica de negocio, consume las APIs REST de los microservicios y renderiza
las vistas en el servidor con Thymeleaf.

**Entrega 3 — Diseño de Sistemas de Información, UTN FRBA.**

---

## 1. Levantarlo

Necesitás **Java 21**. No hace falta Docker ni base de datos.

```bash
sh ./mvnw spring-boot:run
```

Abrí <http://localhost:8086>.

> Si tocaste una plantilla y no ves el cambio, corré `sh ./mvnw compile`.
> `spring-boot:run` sirve las vistas desde `target/classes`, no desde `src/`.

---

## 2. Probarlo sin backend: el modo demo

Hoy el cliente **todavía no llama a las APIs**. Las vistas están completas y
renderizan datos dinámicos, pero esos datos salen de un andamio en memoria para que
cualquiera pueda ver y revisar la interfaz sin levantar los cuatro microservicios,
MySQL, MongoDB, RabbitMQ y Keycloak.

### Cómo entrar

Andá a <http://localhost:8086/demo> y elegí un rol. O directo:

| Rol | URL | Usuario de prueba |
|---|---|---|
| Donante | <http://localhost:8086/demo/donante> | Elena Martínez |
| Entidad beneficiaria | <http://localhost:8086/demo/entidad> | Comedor Los Hornos |
| Administración | <http://localhost:8086/demo/admin> | Carlos Gómez |

Cada enlace abre una sesión de prueba y te lleva al panel de ese rol. Para cambiar de
rol, volvé a `/demo`. **Esto no es autenticación**: solo deja un usuario en la sesión
del servidor para poder recorrer las vistas.

### Qué mirar en cada rol

**Donante** — `/donante/dashboard`
- Métricas de incentivos, misión en curso con barra de progreso, insignias.
- Tres donaciones en estados distintos. Entrá a una: vas al **nivel 3** de navegación,
  con línea de tiempo y mapa si hay un camión en viaje.
- `/donante/entidades`: catálogo con búsqueda. Los filtros de tipo y urgencia están
  deshabilitados a propósito (ver *Faltantes*).
- `/donante/ranking`: tabla del mes e histórico.

**Entidad beneficiaria** — `/entidad/dashboard`
- Cuatro donaciones asignadas, una por estado, y el panel de avisos.
- `/entidad/necesidades`: publicá una necesidad. Probá mandarla **vacía**: aparece el
  error real del servidor, no una validación simulada.
- `/entidad/entregas`: mapa con los camiones ubicados por sus coordenadas reales, y
  la misma información en texto al lado.
- Entrá a una donación → botón **Confirmar recepción** → modal con carga de fotos.
  Probá confirmar **sin adjuntar ninguna**: el servidor la rechaza y te lo dice.

**Administración** — `/staff/dashboard`
- El depósito completo. Una donación está vencida: mirá el banner y el botón
  **Marcar vencida**.
- La donación #1055 no tiene destino: tocá **Asignar** y vas a ver los tres grupos
  que devuelven los algoritmos de selección del backend.
- `/staff/donaciones/nueva`: alta con lista de bienes que crece con un botón.
- `/staff/donantes`: alta manual e **importación de CSV**.
- `/staff/camiones`: mapa de la flota y alta por modal.
- `/staff/ranking`: ranking mensual e histórico.

### Qué es de verdad y qué no

| Funciona de verdad | Todavía es andamio |
|---|---|
| Todas las rutas, controllers y plantillas | Los **datos**: salen de `DatosDemo`, no de la API |
| Protección por rol y redirección a `/login` | La **autenticación**: `/demo/*` reemplaza al login |
| Validación de formato y errores del servidor | Los **POST** no escriben nada: muestran el toast y redirigen |
| Toasts, estados vacíos, estados de carga, 404 | |

### Cómo se borra el andamio

Está todo aislado en tres lugares. Cuando el cliente llame a la API de verdad:

1. Borrar `models/dto/../demo/DatosDemo.java`, `controllers/DemoController.java` y
   `templates/demo/`.
2. En los controllers, reemplazar `datos.loQueSea()` por el servicio cliente.
3. Quitar la inyección de `DatosDemo` de `SesionControllerAdvice`.

No hay ninguna otra dependencia hacia el andamio.

---

## 3. Cómo fluyen los datos

```
Navegador
   │  HTTP (HTML renderizado en el servidor)
   ▼
┌─────────────────────── Cliente liviano · Spring Boot :8086 ───────────────────────┐
│                                                                                   │
│  @Controller ──────────► arma el Model y elige la plantilla. No habla HTTP.       │
│      │                                                                            │
│      ├──► servicio cliente ──► llama a la API, mapea DTOs y traduce errores       │
│      │    (PENDIENTE: hoy lo suple demo/DatosDemo)                                │
│      │                                                                            │
│      ├──► HttpSession ──────► usuario autenticado + token JWT                     │
│      │                        Nunca sale al navegador.                            │
│      │                                                                            │
│      └──► Thymeleaf ────────► templates/ + fragments/ renderizan el Model         │
│                                                                                   │
│  RestClientConfig: un interceptor lee el token de la sesión y lo agrega como      │
│  `Authorization: Bearer …` en cada llamada saliente.                              │
└───────────────────────────────────────────────────────────────────────────────────┘
   │  HTTP/JSON con el token
   ▼
donaciones :8080   notificaciones :8081   incentivos :8082   logística :8083
        (los cuatro validan el JWT que emite Keycloak :8085)
```

### El recorrido de una petición

1. El navegador pide `GET /entidad/dashboard`.
2. `EntidadController` comprueba contra la sesión que haya un usuario con rol
   `ENTIDAD`. Si no, redirige a `/login`.
3. Pide los datos a la capa de servicios (hoy, a `DatosDemo`) y los deja en el `Model`.
4. Thymeleaf renderiza `entidad/dashboard.html`, que arma la página con los fragmentos
   compartidos y recorre las listas con `th:each`. Si una lista viene vacía, muestra
   su estado vacío.
5. Vuelve HTML terminado. El navegador solo ejecuta `main.js` para modales, toasts y
   estados de carga.

### Una acción que escribe

Patrón **Post/Redirect/Get**, para que recargar no repita la acción:

```
POST /entidad/necesidades
   → el controller valida el formato
   → llama a la API
   → redirect.addFlashAttribute("toast", Toast.exito("…"))
   → redirect:/entidad/necesidades
        → la vista siguiente renderiza el toast y main.js lo muestra
```

### Dónde viven las cosas

```
src/main/java/…/clienteliviano/
├── controllers/   una clase por área: Publico · Donante · Entidad · Staff
│                  (+ Login y Register, que son la parte de Keycloak)
├── models/
│   ├── dto/       espejos EXACTOS de los DTOs del backend + objetos de formulario
│   └── vista/     lo que la vista necesita y la API todavía no da.
│                  Cada uno documenta en su Javadoc qué falta y dónde.
├── web/           Sesion · SesionControllerAdvice · ProyeccionMapa
├── demo/          ANDAMIO: datos en memoria
└── services/      (vacío: acá van los servicios cliente HTTP)

src/main/resources/
├── static/        css/ js/ img/     ← archivos servidos al navegador
└── templates/
    ├── fragments/ layout · navbar · componentes
    ├── publico/   index · login · registro · legal
    ├── donante/   dashboard · donacion · entidades · ranking
    ├── entidad/   dashboard · necesidades · entregas · donacion
    ├── staff/     dashboard · asignar · nueva-donacion · donantes · camiones · ranking
    └── error/     403 · 404 · 5xx
```

### Reglas que sostienen el desacople

- **Ninguna lógica de negocio acá.** Estados, rankings, asignaciones y validaciones de
  dominio los decide la API. El cliente presenta, valida formato para dar feedback
  rápido, y traduce errores a texto comprensible.
- **El token vive en la sesión del servidor.** Nunca en `localStorage`, ni en una
  cookie legible por JavaScript, ni escrito en el HTML.
- **`th:text` para todo dato que venga de la API.** `th:utext` solo con contenido
  propio y controlado.
- **Los formularios usan `th:action`**, que inyecta el token CSRF. Cerrar sesión es un
  POST, no un enlace.
- **Todo valor visual sale de un token** (`var(--color-…)`, `var(--space-…)`). Ningún
  color literal.

---

## 4. Configuración

`src/main/resources/application.properties`:

| Propiedad | Para qué |
|---|---|
| `server.port` | Puerto del cliente (8086) |
| `backend.api.url.donaciones` | Base de donaciones-service |
| `spring.security.oauth2.client.provider.keycloak.issuer-uri` | Realm de Keycloak |
| `spring.docker.compose.enabled=false` | `compose.yaml` no declara servicios y sin esto la app no arranca en máquinas sin Docker |
| `keycloak.base-url` · `keycloak.realm` · `keycloak.client-id` | Dónde está Keycloak y con qué cliente se pide el token |
| `keycloak.admin.*` | Cuenta que usa el alta de usuarios contra la Admin API |

### Credenciales

Toda la configuración de Keycloak sale de `application.properties` y **se puede
sobreescribir por variable de entorno**. Los valores por defecto son los del
contenedor de desarrollo, para que el proyecto arranque sin configurar nada:

```bash
export KEYCLOAK_BASE_URL=...
export KEYCLOAK_ADMIN_USER=...
export KEYCLOAK_ADMIN_PASSWORD=...
```

**En cualquier entorno que no sea tu máquina, sobreescribilas.** Nunca commitees el
valor real: si alguna vez hay que usar una credencial que no sea la de desarrollo,
va por variable de entorno y no al archivo.

---

## 5. Qué falta

### Del cliente

1. **Los servicios cliente HTTP.** Es lo único que separa las vistas de los datos
   reales. `services/` está vacío y los controllers ya están escritos para recibirlos.
2. **Login real.** `LoginController` obtiene el token de Keycloak y lo guarda en la
   sesión, pero no lee los roles del JWT ni redirige según el rol.
3. **Protección por rol en Spring Security.** Hoy la hace cada controller a mano
   contra la sesión. `SecurityConfig` tiene `anyRequest().permitAll()` y CSRF
   deshabilitado; al reactivarlo, los `th:action` ya emiten el token solos.
4. **Timeouts en `RestTemplate`.** Sin ellos, una API colgada cuelga la vista.
5. **Manejo centralizado de errores** en un `@ControllerAdvice`: 401 → cerrar sesión y
   volver a `/login`; 5xx → banner de servicio no disponible.
6. **Subida de fotos**: la vista de confirmación de recepción ya las envía como
   multipart; falta decidir a dónde van.

### De la API (hay que pedirlo al equipo de backend)

Cada faltante está documentado en el Javadoc del record de `models/vista/` que lo
necesita.

| # | Qué falta | Quién lo necesita |
|---|---|---|
| G1 | Listar las donaciones de **un** donante. `GET /donaciones` es solo ADMIN y devuelve todas | dashboard del donante |
| G2 | Un `GET /donantes/me` que resuelva el donante del JWT. Sin eso el cliente no conoce su `donanteId`, que piden todos los endpoints de incentivos | donante |
| G3 | El **estado** de la donación. El enum `TipoEstado` existe, pero `EstadosController` está entero comentado y `DonacionResponse` solo trae id, descripción y donanteId | todas las vistas con seguimiento |
| G4 | Endpoint de **ranking**. `RankingService` es una interfaz sin implementación | ranking |
| G5 | Estadísticas **públicas**. Los cuatro servicios son `anyRequest().authenticated()`, así que la portada no puede mostrar ninguna cifra real | portada |
| G6 | Tipo de entidad, nivel de urgencia y foto en `EntidadResponse`: son los filtros del catálogo, por eso están deshabilitados, y la tarjeta muestra la inicial en vez de una imagen | catálogo de entidades |
| G7 | Listado de **subcategorías**. `BienCreateRequest` pide `subcategoriaId` y nada expone las opciones | alta de donación, alta de necesidad |
| G8 | Un `GET` de la bandeja de notificaciones. Hoy solo hay POST que las *envían* | avisos del donante y de la entidad |
| G9 | Relación entrega ↔ entidad y hora de la última posición del camión | mapa de entregas |

### Fuera de alcance, elegido

- Los enlaces sin destino quedaron **deshabilitados** con `aria-disabled` en vez de
  llevar a páginas vacías.
- La portada no muestra cifras: preferimos no inventar datos que la API no da (G5).
- El mapa es esquemático. `web/ProyeccionMapa` ubica los pines a partir de las
  coordenadas reales; integrar Leaflet o Google Maps es reemplazar esa clase.

---

## 6. Verificación

```bash
sh ./mvnw test
```

Además hay un juego de verificadores de desarrollo (lint de plantillas, contraste
WCAG y pruebas end-to-end) que **no viven en este repositorio**: no forman parte del
entregable. Pedíselos a Gerónimo si querés correrlos.

Estado al cerrar la migración:

```
mvnw test           Tests run: 7, Failures: 0, Errors: 0
lint de plantillas  25 plantillas · ERROR: 0 · WARN: 0 · INFO: 0
contraste           TODO CUMPLE WCAG AA
accesibilidad del HTML renderizado (18 páginas)   ERROR: 0 · WARN: 0
responsive          0 px de desborde horizontal en 375, 768 y 1440
```

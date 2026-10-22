# Roadmap de desarrollo Android — Somphie Zürich

> Guía de trabajo para que el dueño implemente la app Android del personal. La persona desarrolla; el agente puede explicar, revisar decisiones, ayudar a depurar y validar. Este plan describe una ruta incremental, no afirma que las pantallas pendientes ya existan.
>
> **Fuente de verdad:** decisiones y reglas del dominio → `docs/02-dominio/`; contrato → `docs/04-api/openapi.yaml`; guías → `docs/08-agentes/android.md` y `docs/08-agentes/roadmap-fases.md`. Si este roadmap discrepa, prevalecen esos documentos.

---

## 1. Objetivo y alcance

La aplicación es una herramienta operativa del personal. Muestra información autorizada, permite solicitar acciones al backend y representa la respuesta confirmada. **No decide** permisos, transiciones, precios, saldos ni disponibilidad final.

El objetivo principal es completar este recorrido:

```text
Iniciar sesión
      ↓
Ver pedido entrante → aceptar/rechazar
      ↓
Enviar los productos aceptados a sus estaciones
      ↓
Preparar → marcar listo → entregar
      ↓
Consultar el estado confirmado desde el backend
```

Luego se agregan, según las decisiones y APIs aprobadas, mesas, disponibilidad y caja. Android **no se comunica directamente con Web**: ambas apps consultan el mismo backend, que persiste los datos en PostgreSQL.

## Alcance de este documento: solo Android

Este roadmap desarrolla únicamente la franja **“App Android del personal”** del plano visual, no la Web ni el panel administrativo.

La lámina actual muestra **8 pantallas Android dibujadas como teléfonos**:

1. Login del personal (`SC-S-01`).
2. Inicio/panel del turno (`SC-S-02`).
3. Pedidos entrantes (`SC-S-03`).
4. Detalle del pedido (`SC-S-04`).
5. Estado de mesas (`SC-S-07`).
6. Cola de Cocina (`SC-S-05`, filtrada por la estación autorizada).
7. Cola de Barra (`SC-S-05`, filtrada por la estación autorizada).
8. Perfil (`SC-S-13`).

El encabezado de marca, la paleta y los textos de pie son referencias visuales, no pantallas adicionales. Cocina y Barra son dos vistas de la misma función de cola por estación, no dos contratos o permisos nuevos. Las acciones de cada producto (`SC-S-06`) forman parte del flujo de preparación. Detalle de sesión, caja/pagos y cierre siguen en el mapa funcional, pero **no aparecen en esta lámina**; disponibilidad tampoco aparece entre los ocho teléfonos. Los estados de carga, vacío, error, sin conexión y sesión vencida se diseñan dentro de cada pantalla.

El Login es para cuentas de personal existentes. **No hay registro público ni autoservicio para crear usuarios**: las cuentas las crea el dueño mediante un proceso autorizado. La lámina muestra “Olvidaste tu contraseña”, pero el contrato actual no ofrece recuperación de contraseña; no se debe implementar ni presentar ese enlace como funcional hasta aprobar y documentar un flujo. El alta de usuarios desde una pantalla administrativa también queda fuera de este roadmap mientras no haya contrato aprobado.

---

## 2. Estado real de la app al comenzar

### Ya implementado

- Aplicación Kotlin/Jetpack Compose en un único módulo Gradle `:app`.
- Hilt, Retrofit, OkHttp, Kotlinx Serialization, Coroutines/Flow y Material 3.
- Autenticación, cambio obligatorio de contraseña, `/me`, cierre de sesión y restauración de sesión.
- Access y refresh tokens cifrados con AES-GCM/Android Keystore y guardados en Preferences DataStore.
- Selección de estación autorizada, consulta de cola por REST, paginación y refresco en primer plano.
- Acciones de preparación existentes: iniciar, marcar listo, entregar, cancelar y deshacer según permiso/estado.
- ViewModels y pruebas locales; tests Compose instrumentados de inicio.
- Colores base aplicados en `core/design/StaffTheme.kt`.

### Todavía no implementado como pantalla

- Bandeja visual de pedidos entrantes y detalle para aceptar/rechazar: `WP-AND-51`.
- Navegación entre varias secciones; hoy `StaffApp` conduce al login/cambio de clave o a la pantalla de preparación. No existe todavía un `NavHost`.
- Tablero de mesas, disponibilidad, perfil operativo, caja/pagos, ajustes y cierre.
- Consumo SSE.

### Métrica que no hay que confundir

El `94,76 %` citado en avances es cobertura de líneas de una corrida histórica del backend. **No es el porcentaje de funcionalidades terminadas**, ni mide el frontend Android. El avance Android se informa por flujos completos, pruebas y endpoints integrados.

---

## 3. Arquitectura limpia, sencilla y compatible con el proyecto

### 3.1 No crear capas por obligación

El proyecto actual es un solo módulo Gradle y está organizado por responsabilidad y feature. Mantené ese enfoque. **No lo conviertas en varios módulos Gradle ni agregues una capa `domain` con una clase por cada DTO sin una razón concreta.**

Usá estas separaciones:

```text
Composable
   ↓ eventos y UiState
ViewModel (presentación)
   ↓ llama una operación de aplicación cuando aporta claridad
Repository (contrato y coordinación de datos)
   ↓
StaffApi / Retrofit (DTO HTTP)
   ↓ HTTP
Backend
```

- **UI (`ui/`):** Compose, navegación visual, validación de formato y representación de estados. No hace llamadas HTTP ni crea reglas comerciales.
- **Presentación (`ui/`):** ViewModel y `UiState` inmutable; coordina carga, acciones, conflictos y actualización de la pantalla.
- **Dominio (`domain/`, solo cuando haga falta):** modelos de aplicación y casos de uso que coordinan una operación real o evitan duplicar lógica de presentación. No replica las reglas de negocio del servidor.
- **Datos (`data/`):** repositorio, fuente remota, DTOs/mappers si son útiles, manejo de errores y claves de idempotencia.
- **Core (`core/`):** red/autenticación, almacenamiento seguro, tema, textos y componentes realmente compartidos.
- **Backend:** conserva la decisión final y vuelve a validar permiso, recurso, transición, idempotencia y persistencia.

Una feature nueva puede seguir, por ejemplo:

```text
feature/inbox/
├── data/
│   ├── InboxRepository.kt
│   └── dto o mappers, solo si se necesitan
├── domain/
│   └── operaciones/modelos de aplicación, solo si aportan separación real
└── ui/
    ├── InboxScreen.kt
    ├── InboxViewModel.kt
    └── InboxUiState.kt
```

Las carpetas representan responsabilidades, no obligan a crear cada archivo por adelantado. Conservá `StaffApi` y los modelos de red en `core/network` mientras resulte manejable; separalos por feature solo si mejora de verdad la comprensión o las pruebas.

### 3.2 Flujo de datos unidireccional

1. La pantalla observa un `StateFlow<UiState>`.
2. El usuario toca una acción; la UI llama un método del ViewModel.
3. El ViewModel pide la operación al repositorio.
4. El repositorio llama a Retrofit.
5. El backend responde; el repositorio traduce errores conocidos sin ocultar fallos.
6. El ViewModel actualiza estado visible con la respuesta y, cuando corresponda, vuelve a consultar el recurso.
7. Compose representa ese estado.

No mutar objetos de UI desde callbacks ni almacenar la misma lista como verdad en pantalla, ViewModel y repositorio sin necesidad. El servidor sigue siendo la fuente de verdad.

### 3.3 Reglas para todas las features

- Los textos visibles van en `res/values/strings.xml`, no como literales en Composables.
- Mantener loading, content, empty, error, offline y acción en curso donde correspondan. Un error de actualización no debe borrar silenciosamente el último contenido confirmado.
- No mostrar acciones solo por el nombre del rol: usar permisos/estaciones de `/me`, y dejar que el servidor vuelva a autorizar cada solicitud.
- En cambios de estado enviar `Idempotency-Key`; una respuesta ambigua se reintenta con la **misma** clave y el mismo cuerpo. Una acción nueva recibe clave nueva.
- Nunca fingir éxito optimista en aceptación, preparación, pago o cierre. Tras éxito/conflicto leer el estado confirmado.
- No registrar tokens, contraseñas, cuerpos sensibles ni datos de sesión en logs.
- Respetar paginación/cursor, estados desconocidos y campos nullable del contrato.
- Importes siempre en centavos enteros (`Long`); no calcular saldo final local.
- Accesibilidad: botones ≥ 48 dp, contenido descriptivo para íconos, contraste, texto ampliable y estados comunicados con texto además de color.
- Las pruebas unitarias/mock verifican comportamiento local; no se presentan como integración con PostgreSQL.

---

## 4. Navegación y catálogo de pantallas

La pantalla **Inicio** puede funcionar como contenedor de secciones. Mostrá solo destinos para los que el perfil tiene permisos. El nombre y cantidad final de pestañas son una decisión de UI; no inventes permisos. Los identificadores que aparecen abajo son referencias del mapa funcional.

| Ref. | Pantalla/flujo | Estado | Prioridad |
|---|---|---|---|
| SC-S-01 | Splash / Login (Pantalla 1 y 2) | ✅ Implementada y personalizada con animaciones de flujo | Base |
| Cambio de contraseña | Implementada cuando el backend lo exige | Base |
| SC-S-02 | Inicio/contenedor según permisos (Pantalla 3) | 🔄 En curso | Alta |
| SC-S-03 | Pedidos entrantes | No implementada | Muy alta |
| SC-S-04 | Detalle entrante: aceptar/rechazar/exclusiones permitidas | No implementada | Muy alta |
| SC-S-05 | Cola por estación | Implementada | Alta: pulido e integración |
| SC-S-06 | Acciones por producto | Implementadas según estado y permiso | Alta: pruebas integrales |
| SC-S-07 | Tablero de mesas | No implementado | Después de confirmar alcance API |
| SC-S-08 | Detalle de mesa/sesión | No implementado como pantalla | Después de confirmar permisos/estados |
| SC-S-09 | Pago/reembolso en caja | No implementado | Fase posterior |
| SC-S-10 | Cierre de sesión | No implementado como UI | Fase posterior |
| SC-S-11 | Disponibilidad de productos/opciones | No implementada como UI | Bloqueada parcialmente por Q-27 |
| SC-S-12 | Crear pedido en nombre del cliente | No implementada como UI | Fase posterior |
| SC-S-13 | Perfil/ajustes | No implementada como pantalla propia | Después del flujo operativo base |
| SC-S-14 | Estados globales de red/sesión/error | Parciales por pantalla; unificar al crecer la navegación | Continua |

### Correspondencia con la lámina actual

| Teléfono | Contenido que se ve | Referencia funcional |
|---|---|---|
| 1 | Login del personal | SC-S-01; sin registro público |
| 2 | Inicio con turno, contadores y resumen | SC-S-02; solo datos que autorice el backend |
| 3 | Lista de pedidos nuevos por mesa | SC-S-03 |
| 4 | Productos, nota y decisión del pedido | SC-S-04 |
| 5 | Cuadrícula con estado de mesas | SC-S-07 |
| 6 | Cola de Cocina | SC-S-05/06, estación permitida |
| 7 | Cola de Barra | SC-S-05/06, estación permitida |
| 8 | Datos de perfil y opciones básicas | SC-S-13; cerrar sesión sigue la sesión autenticada |

La maqueta orienta la composición visual, no agrega rutas ni autoriza datos. En particular, los contadores, ventas, minutos estimados y saldos solo se muestran cuando exista un endpoint aprobado que los entregue para ese perfil. No inventes un endpoint para llenar el panel.

### Esqueleto de navegación sugerido

```text
AuthFlow
├── LoadingSession
├── Login
├── ChangePassword
└── SignedInFlow (perfil de /me)
    ├── Inbox (si el permiso del contrato lo permite)
    ├── Preparation (estaciones asignadas)
    ├── Tables (solo con permiso/API disponibles)
    ├── Availability (solo con permiso/API disponibles)
    └── More/Profile
```

Los nombres descriptivos de arriba son destinos de navegación, **no nuevos permisos ni endpoints**. Definí la visibilidad con los permisos que realmente devuelve `/me`. Si un usuario tiene una sola función, puede entrar directamente a ella sin una pantalla intermedia vacía.

---

## 5. Bloque 0 — Preparar la base visual y la navegación

### Objetivo

Que el proyecto pueda agregar pantallas sin convertir `StaffApp` en un `when` enorme ni duplicar login/tema/red.

### Pasos

1. Dibujar en papel los destinos del apartado 4 y marcar qué permiso y API requiere cada uno.
2. Mantener `MainActivity`, Hilt y el estado de autenticación existentes.
3. Cuando haya más de un destino operativo real, introducir Navigation Compose y un contenedor autenticado. No migrar ahora la cola a una arquitectura distinta solo para poner un `NavHost`.
4. Al crear navegación, impedir volver al login con Back después de login y proteger la sesión expirada volviendo al flujo de autenticación.
5. Mantener selección/estado de estación al rotar pantalla si el flujo actual lo espera; no persistir tokens o decisiones operativas en SavedState.
6. Definir componentes comunes cuando se repitan: encabezado, estado vacío, error con reintento, chip estado, diálogo destructivo y botón con progreso.
7. Aplicar el sistema visual aprobado:
   - Fondo crema `#F6F1E8`; superficies/tarjetas `#FBF8F2`.
   - Texto café `#3A2B22`; acción principal caramelo `#945C34`.
   - Acentos salvia `#61715B` y gris azulado `#59696E`; borde `#DED5C8`.
   - Títulos serif estilo Fraunces/Georgia solo donde ayude; UI en sans-serif Inter/sistema.
   - Diseño de operación: contraste, jerarquía, botones claros y superficies sin decoración excesiva.
8. Agregar strings centralizados y pruebas de navegación/estado de sesión antes de mover features.

### Aceptación

- Una sola actividad; login, cambio de contraseña y sesión restaurada siguen funcionando.
- Destinos no permitidos no se muestran; probar navegación directa tampoco sustituye la autorización del servidor.
- No se creó otro store de tokens ni una copia del perfil como verdad.

### Dependencias

No requiere backend nuevo. Navigation Compose ya aparece en la arquitectura prevista; comprobar dependencia Gradle antes de usarla. No agregar librerías nuevas sin justificar.

---

## 6. Bloque 1 — WP-AND-51: pedidos entrantes

**Este es el siguiente bloque recomendado para programar.** Completa el paso manual que hoy impide que el pedido Web aparezca en una estación.

### 6.1 Contrato que se debe usar

- Lista: `GET /staff/orders?status=PENDING_REVIEW&limit=...`; respuesta paginada con `items` y `nextCursor`.
- Detalle: `GET /staff/orders/{orderId}`.
- Aceptar: `POST /staff/orders/{orderId}/accept`, requiere permiso `order.accept` y `Idempotency-Key`.
- Rechazar: `POST /staff/orders/{orderId}/reject`, requiere `order.reject`, `Idempotency-Key` y `reasonCode`; `reasonText` es opcional, máximo 140 caracteres.
- Exclusiones al aceptar: `excludedItems` con `itemId` y `reason` según enum contractual; excluir todo responde `ORDER_ALL_ITEMS_EXCLUDED` y debe usarse rechazo.
- Conflictos relevantes: `ORDER_ALREADY_DECIDED`, `ORDER_EXPIRED`; el primer decisor gana. El servidor pasa los productos aceptados a `QUEUED`.
- La ruta `/staff/orders/pending` **no existe en el contrato**. No implementarla.
- El request de aceptación **no tiene notas libres**. No agregar un campo `notes`.
- No mostrar montos o datos generales a un rol de Cocina cuando el contrato los omite. Presentar solo información que llegue autorizada en esa respuesta.

Revisá siempre el OpenAPI vigente y el `StaffOrdersController` antes de fijar DTOs; este resumen no sustituye su esquema.

### 6.2 Tareas de implementación

1. **Mapear permisos y perfil**
   - Confirmar qué perfil recibe la app mediante `/me`.
   - Identificar permisos y alcance efectivo de lectura/decisión en OpenAPI.
   - Escribir pruebas de acceso autorizado y denegado; no asumir que cada rol ve lo mismo.

2. **Red/API**
   - Añadir en `StaffApi` las rutas contractuales de lista, detalle, aceptar y rechazar.
   - Declarar DTOs serializables con campos reales del contrato, incluyendo nulos y paginación.
   - Añadir `Idempotency-Key` como header en las mutaciones.
   - Probar serialización de body/query/header con MockWebServer.

3. **Datos**
   - Crear `feature/inbox/data/InboxRepository.kt` como punto de acceso para la feature.
   - El repositorio obtiene la página, envía la decisión y traduce errores HTTP a fallos conocidos.
   - Evitar que Compose conozca Retrofit, JSON o headers.
   - No introducir base local/offline de pedidos: no está requerida para este alcance.

4. **Presentación**
   - Crear `InboxUiState` inmutable con carga inicial, contenido, vacío, error, refresco y decisión en curso identificada por pedido.
   - Crear `InboxViewModel`: cargar, refrescar, paginar, abrir detalle, aceptar y rechazar.
   - Evitar dos decisiones simultáneas sobre el mismo pedido; no bloquear sin motivo otros pedidos independientes.
   - Después de aceptar/rechazar, volver a pedir lista/detalle. No retirar la tarjeta como éxito hasta confirmar respuesta del backend.

5. **Pantalla de pedidos entrantes**
   - Lista ordenada como la entrega el servidor; tarjeta con código, mesa, hora/datos permitidos y resumen de ítems autorizado.
   - Mostrar los estados Loading, Empty, Error/reintentar, Content y actualizando.
   - Acceso al detalle, botón de actualizar y paginación si `nextCursor` no es nulo.
   - No inventar contador de cuenta regresiva/alarma para TTL: el contrato no define esos criterios visuales operativos como requisito de Android.

6. **Detalle y decisiones**
   - Mostrar productos y disponibilidad únicamente con datos autorizados.
   - Aceptación normal con confirmación clara.
   - Rechazo en diálogo con selector de `reasonCode`; texto adicional solo si se define UI para `reasonText` y se valida el máximo contractual.
   - Exclusión opcional por producto únicamente con códigos `reason` permitidos; no permitir excluir todo.
   - Indicar en el diálogo que los productos excluidos se cancelan y no se cobran, conforme a contrato.
   - Mostrar “decidido por otra persona” al recibir `ORDER_ALREADY_DECIDED`; refrescar sin borrar toda la lista ni fingir aceptación propia.
   - `401`: flujo de sesión/token existente; `403`: mensaje de permiso; `409`: refrescar y explicar; red/5xx: conservar contexto y ofrecer reintento seguro.

7. **Idempotencia**
   - Crear una clave por intento lógico al iniciar aceptar/rechazar.
   - Si no se conoce si el servidor aplicó la acción, conservar body y clave para reintentar esa misma acción.
   - Si cambia la selección de exclusiones o el motivo, es una acción distinta: generar clave nueva.
   - Limpiar la acción pendiente cuando se confirma el resultado o se obtiene estado terminal que requiere una nueva decisión.

8. **Pruebas**
   - Unitarias ViewModel: loading, lista con datos, vacío, error recuperable, refrescar, paginar y respuesta tardía.
   - Decisiones: éxito, error, conflicto `ORDER_ALREADY_DECIDED`, vencimiento, exclusión de ítem y evitar exclusión total.
   - Reintento ambiguo: misma clave y mismo cuerpo.
   - Repositorio/API: paths/query/header/JSON correctos con MockWebServer.
   - UI Compose: ver lista/detalle, progreso, error y acción deshabilitada durante envío; semántica accesible.
   - Integración real: crear pedido desde Web, verlo en Android, aceptarlo y comprobar ítems en las estaciones asignadas.

### Aceptación de WP-AND-51

- Un usuario autorizado encuentra el pedido pendiente; un usuario sin alcance no obtiene datos indebidos.
- Aceptar lo saca de entrantes y deja sus productos en `QUEUED` en estaciones asignadas.
- Rechazar conserva el pedido según respuesta backend y muestra el resultado real.
- Errores de red no crean duplicados ni dan falsa confirmación.
- Tests unitarios, UI y flujo real están reportados por separado.

**Dependencias:** WP-AND-50 existente, endpoints backend `WP-BE-44` y contrato actual. No depende de SSE, del tablero de mesas ni de que Web tenga una pantalla nueva.

---

## 7. Bloque 2 — WP-AND-52: preparación por estación

La feature ya existe. El trabajo es verificar y completar sus flujos, no reescribirla.

### Lista de revisión

1. **Selección de estación:** solo las estaciones que entrega `/staff/stations`; respetar asignaciones devueltas por el servidor.
2. **Cola:** consultar `GET /staff/stations/{stationId}/queue` con filtros contractuales, límite y cursor.
3. **Tarjeta:** código/mesa, producto, cantidad, opciones, nota del ítem y demora solo cuando el contrato incluya esos datos. Cocina no debe ver nota general ni importes omitidos por alcance.
4. **Filtros:** “En curso”, “Listos” y “Todos” deben enviar los estados permitidos por OpenAPI; tolerar estados/valores nulos.
5. **Acciones:** iniciar/listo/entregar/cancelar/deshacer según las acciones permitidas por estado y perfil. Backend valida de nuevo.
6. **Confirmación:** tras éxito refrescar REST; tras conflicto actualizar la cola y explicar; tras desconexión preservar acción/clave para el reintento previsto por la implementación.
7. **Ciclo de vida:** refresco automático únicamente cuando la pantalla está en primer plano; no ejecutar polling indefinido en background.
8. **Pruebas:** cubrir las transiciones contractuales, permisos, estación propia/ajena, error y reintento. No debilitar tests para igualar lo que hace la UI.

### Mejoras de experiencia visual

- Orden visual legible en cocina: producto primero, después opciones/notas y acciones disponibles.
- Badge de estado con color, texto e ícono; no usar solo color.
- Botón destructivo separado para cancelar, con motivo y confirmación cuando corresponda.
- Vacío con estación y filtro elegidos claramente; indicar que los entrantes deben aceptarse primero.
- Botón Actualizar y gesto pull-to-refresh son opciones de UI; antes de agregar dependencia, revisar si el Compose BOM/Material ya ofrece componente adecuado.

**Dependencias:** backend de preparación WP-BE-50, login/perfil existente. **No espera SSE**: el polling REST actual es suficiente para completar el bloque funcional.

---

## 8. Bloque 3 — SC-S-02: inicio y visibilidad según permisos

Construir o completar el inicio cuando Entrantes u otras features estén listas.

1. Recibir el perfil cargado por la autenticación; no volver a interpretar JWT en la UI.
2. Preparar destinos según permisos reales retornados en `/me`, no por etiquetas como OWNER/WAITER/KITCHEN.
3. Permitir acceso a estaciones autorizadas que entrega `/staff/stations`.
4. Evitar un destino “Mesas” o “Catálogo” hasta comprobar permiso, endpoints disponibles y alcance del contrato.
5. Mantener deep links/rutas inaccesibles protegidas por el flujo autenticado; el backend sigue siendo la barrera efectiva.
6. Perfil/ajustes mínimos: cerrar sesión mediante flujo existente, versión/build informativa si ya se requiere; no agregar cambio de ajustes funcionales sin contrato.
7. Navegación: Back no debe devolver a pantallas autenticadas luego de cerrar sesión.
8. Probar perfiles con permisos distintos y perfil con más de una estación.

**Dependencias:** destinos del Bloque 1 y 2, `/me`, decisiones de UX. No se necesita un endpoint nuevo para un contenedor de navegación, pero sí para el contenido de cada destino.

---

## 9. Bloque 4 — SC-S-07/08/11: mesas y disponibilidad

Roadmap funcional previsto como WP-AND-53. Dividilo para no bloquear lectura por acciones aún incompletas.

### 9.1 Tablero y detalle de mesa

1. Confirmar en OpenAPI campos de `GET /staff/tables` y `GET /staff/sessions/{sessionId}`, permisos, estados y alcance por tenant.
2. Implementar lectura: Loading, lista vacía, error, actualización y tarjetas con el estado que responde el servidor.
3. Detalle con pedidos, productos, participantes y cuenta solo si la respuesta/permiso los habilita.
4. Actualizar después de una acción exitosa; no calcular `displayStatus` ni saldo localmente.
5. Verificar Q-31 antes de presentar saldo negativo/estado de mesa como categoría nueva: no inventar etiquetas.

### 9.2 Disponibilidad de productos

1. La lectura del menú puede servir para mostrar catálogo según endpoints existentes; verificar qué API staff retorna la lista necesaria.
2. La mutación de disponibilidad está declarada en OpenAPI, pero la ficha de backend marca pendiente la coordinación entre `catalogVersion` y `catalog.updated` (Q-27/WP-BE-52).
3. No dar por terminado el switch si una mutación puede dejar ETag/catálogo desactualizado; confirmar primero el estado real de backend/decisión.
4. Si se habilita el endpoint, enviar `catalogVersion` y disponibilidad según contrato, mostrar progreso y aplicar el valor confirmado por respuesta; ante conflicto/error, releer.
5. No incluir gestión CRUD de productos, mesas, usuarios o estaciones en esta pantalla: esas escrituras administrativas están en Q-26/WP-BE-80 y no se deben inventar.

**Dependencias:** API de piso WP-BE-51; disponibilidad WP-BE-52 y Q-27; estado de clasificación de mesa/Q-31 para etiquetas completas. Se puede adelantar layout de lectura, no afirmar gestión de disponibilidad completa antes de validar API real.

---

## 10. Bloque 5 — SC-S-09/10/12: caja, cierre y pedido a nombre del cliente

Estas son superficies distintas; impleméntalas por separado y contra sus contratos reales.

### 10.1 Caja y pagos (`WP-AND-70`)

1. Leer rutas/esquemas de cuenta staff, pago de efectivo/simulado, anulación y reembolso; confirmar permisos y headers.
2. Crear modelos en minor units; UI formatea los valores que recibe, no deriva la cuenta.
3. Formulario presenta solo métodos aprobados. Nunca recolecta número, CVV ni datos reales de tarjeta.
4. Bloquear el submit duplicado y usar `Idempotency-Key` según contrato.
5. Confirmar pago mostrando respuesta real; reconsultar cuenta/sesión.
6. Gestionar saldo insuficiente, exceso, rechazo simulado, operación repetida, permiso denegado, red y conflicto usando errores contractuales.
7. Probar límites/montos enteros, idempotencia y persistencia PostgreSQL en backend; MockWebServer/ViewModel en Android.

**Dependencias:** backend Billing WP-BE-70/71, contratos API y pruebas de integración. Los hitos de pedido y cocina no dependen de caja.

### 10.2 Cierre de sesión (`WP-AND-70`)

1. Mostrar impedimentos que devuelve el backend (pedidos pendientes, productos activos o saldo) sin inferir reglas.
2. Ofrecer cierre normal solo con permiso correspondiente.
3. El cierre forzado es una acción destructiva: permiso `session.force_close`, motivo contractual, aviso explícito y confirmación adicional.
4. No mostrarlo como “liberar mesa” inocuo: cancela trabajo operativo y puede dejar deuda no cobrada.
5. Tras resultado confirmado, volver a consultar la sesión y tablero; no borrar historial.

**Dependencias:** `/staff/sessions/{id}/close` y `force-close`, permisos, respuesta API y auditoría persistente. No se usa SQL desde Android.

### 10.3 Crear pedido en nombre del cliente (`SC-S-12`)

1. Leer el contrato staff `POST /staff/sessions/{sessionId}/orders`, permisos y validaciones.
2. Reutilizar modelos/validaciones de selección solo donde la API lo permita; no enviar identidad de actor suministrada por UI.
3. Mostrar mesa/sesión confirmada, catálogo actual, opciones permitidas y errores de disponibilidad/precio.
4. Reutilizar patrón de carrito si mejora claridad, sin duplicar componentes acoplados a Web.
5. Probar autorización, tenant, idempotencia y pedido persistido.

**Dependencias:** ruta staff aprobada, sesión identificada y APIs de catálogo; Q-34 contiene condiciones de atribución que deben seguirse al pie de la letra.

---

## 11. Bloque 6 — SSE (solo cuando REST ya esté sólido)

Trabajo `WP-AND-60`, dependiente de `WP-BE-60`.

1. Mantener la cola funcional con polling mientras el backend SSE no esté completo y validado.
2. Antes de implementarlo, verificar el endpoint `/events/stream`, autenticación, eventos, cursor, filtrado y política de retención en OpenAPI y backend.
3. Esperar criterios operativos aprobados para capacidad/conexiones y evidencia reproducible de desconexión TCP; no inventar umbrales.
4. Introducir cliente SSE como fuente de invalidación/refresco, no como segunda verdad local.
5. Definir manejo de `Last-Event-ID`, duplicados, cursor vencido, eventos fuera de orden, token expirado, proceso en segundo plano y reconexión según contrato.
6. Mantener REST como lectura de reconciliación/fallback si así lo establece el contrato.
7. Notificación sonora/vibración es opcional y requiere ajuste/permiso Android; no es condición para cerrar la app operativa.
8. Probar reconexión y deduplicación con servidor de prueba y backend real. Las pruebas de “50 conexiones” del servidor no prueban por sí mismas el cliente Android.

**Dependencias:** backend SSE, decisiones de capacidad/lifecycle y pruebas de red. SSE no es requisito del MVP de preparación; no reemplazar el refresco actual antes de tener la ruta completa.

---

## 12. Bloqueos explícitos y qué no programar por conjetura

- **Q-26 / WP-BE-80/81/82:** administración de catálogo/imágenes, mesas/QR, usuarios/roles/estaciones/settings, opciones y reportes requiere alcance, permisos y contratos aprobados. No hacer panel de administración Android ni CRUD improvisado.
- **Q-27 / WP-BE-52:** versionado del catálogo y evento `catalog.updated` deben resolverse antes de declarar lista la mutación de disponibilidad.
- **Q-31:** representación visual de ciertos saldos/estados de mesa no está resuelta; no inventar estado.
- **Q-35/36:** registro/verificación de clientes, recuperación, correo y encuesta no pertenecen a Android personal base y siguen sin decisiones/contrato.
- **WP-BE-60:** capacidades y desconexión TCP no deben anunciarse como terminadas hasta que haya criterio y evidencia.
- Si aparece un campo, endpoint, transición o permiso que falta, detener esa subfunción, registrar consulta en `decisiones.md` con el dueño del contrato y continuar con el bloque independiente siguiente.

---

## 13. Pruebas por capa

### Unitarias

- ViewModel: estados, paginación, refresco, acciones, conflicto, error y reintento.
- Repositorio: mapeo de respuestas/errores, claves idempotentes y serialización.
- Evitar probar lógica de dominio duplicada que debe residir en backend.

### HTTP simulado

- MockWebServer verifica URL, query, header, cuerpo, serialización y respuestas Problem+JSON.
- Es una prueba del cliente HTTP; **no** demuestra permisos ni persistencia real.

### UI instrumentada

- Compose verifica navegación, accesibilidad, loading/empty/error y controles habilitados/deshabilitados.
- Debe probarse al menos un perfil con varias funciones y uno con alcance menor, con fixtures seguros.

### Integración real

- Backend y PostgreSQL ejecutándose; Android conectado al backend local mediante emulador o `adb reverse`.
- Probar login, permisos, pedido real de Web, aceptación, aparición en estación, acciones y estado final en Web/API/DB.
- Anotar qué se probó desde la app, qué se consultó por API/SQL y qué no se pudo probar.
- No preparar pruebas borrando datos ni alterando estados directamente en SQL.

### Comandos de referencia

Desde `android/`:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon
```

Si hay dispositivo/emulador conectado y autorizado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

Desde la raíz, los tests backend/PostgreSQL se ejecutan con el wrapper de `backend`; su resultado valida el servidor, no reemplaza las pruebas Android instrumentadas. Si Docker no está disponible, dejar evidencia del bloqueo y ejecutar pruebas independientes posibles.

---

## 14. Definition of Done por pantalla

Una pantalla no se considera terminada solo porque “se ve”:

- [ ] El propósito y la ruta de navegación están claros.
- [ ] Cada dato viene de una respuesta contractual; no hay datos de demostración presentados como reales.
- [ ] Permisos y recursos se respetan; el backend vuelve a validarlos.
- [ ] Tiene loading, contenido, vacío, error recuperable y offline según aplique.
- [ ] Los estados de acción previenen dobles toques y no fingen éxito.
- [ ] Reintento ambiguo reutiliza la misma operación/idempotency key.
- [ ] Textos viven en recursos `strings.xml`; UI accesible y adaptable.
- [ ] Pruebas de ViewModel/repositorio/Compose adecuadas y aserciones sin debilitar.
- [ ] Build Kotlin, tests unitarios y lint pasan; instrumentadas cuando el entorno lo permite.
- [ ] Integración real reportada por separado cuando sea parte del hito.
- [ ] Se actualizó este roadmap con el estado real y se anotó el avance del proyecto.

---

## 15. Orden recomendado de trabajo

1. **Mañana: WP-AND-51 Entrantes** — es la brecha que explica por qué un pedido web enviado no aparece automáticamente en Android.
2. **Cerrar WP-AND-52 Preparación** — pulir visualmente y asegurar estados, errores y prueba real, sin reescribir la cola.
3. **Contenedor de navegación + inicio por permisos** — cuando Entrantes y Estaciones existan como destinos reales.
4. **Prueba integral Web → backend/PostgreSQL → Android → Web**.
5. **Tablero/Detalle de sesión** — lectura primero; verificar estado contractual y Q-31.
6. **Disponibilidad** — cuando WP-BE-52/Q-27 permita una mutación coherente de versión/evento.
7. **Caja y cierre** — una operación por vez sobre los endpoints de Billing/Sesión.
8. **Pedido a nombre del cliente** — respetar actor, sesión y snapshot contractual.
9. **SSE y notificaciones opcionales** — después de backend y criterios operativos aprobados.
10. **Iteración final:** accesibilidad, errores, modo sin conexión, pruebas en dispositivo, documentación y distribución.

### ¿Hasta dónde podés llegar sin esperar decisiones nuevas?

Podés construir por tu cuenta, usando los contratos actuales, el flujo de **Entrantes → Aceptar/Rechazar → Estación → Preparar/Entregar**, además del login ya existente. Eso habilita la demostración principal junto con la Web. El trabajo de mesas, disponibilidad, caja y cierre se incorpora solo después de comprobar endpoints/permisos y bloqueos concretos; SSE, paneles administrativos y extras no son prerequisito para el recorrido principal.

La arquitectura limpia no significa crear muchas capas ni retrasar la pantalla: significa que cada pieza tiene una responsabilidad, se puede probar en aislamiento y no duplica las decisiones del backend.

---

## 16. Progreso

Actualizar esta tabla cuando un bloque tenga evidencia; no marcarlo completo por haber creado archivos.

| Bloque | Estado inicial comprobado | Evidencia requerida para marcar completo |
|---|---|---|
| Base/Auth | ✅ Implementada | Login/cambio/restore y pruebas siguen pasando |
| WP-AND-51 Entrantes | ⬜ Pendiente | Aceptar/rechazar desde Android contra backend real + pruebas |
| WP-AND-52 Estaciones | ✅ Implementada; integración/pulido continuo | Flujos por estado/permiso y pruebas real/simulada distinguidas |
| Navegación/Inicio | ⬜ Parcial | Destinos por perfil, Back/logout y tests Compose |
| WP-AND-53 Mesas | ⬜ Pendiente; confirmar contrato y Q-31 | Lectura de tablero/detalle autorizada y probada |
| Disponibilidad | 🟡 Contrato existe; confirmar Q-27/backend | Cambio y catálogo versionado coherentes, probado en integración |
| WP-AND-70 Caja/cierre | ⬜ UI pendiente; backend por confirmar por operación | Cuenta/pago/cierre reales, permisos e idempotencia |
| WP-AND-71 Ajustes/opciones | ⬜ Pendiente; dependencias contractuales/backend | Flujos contractuales y límites validados |
| WP-AND-60 SSE | 🔴 Bloqueado parcialmente por criterios operativos | Endpoint, capacidad/lifecycle y cliente recuperable probados |
| Prueba integral | 🟡 Parcial: preparación física validada; falta aceptación Android | Pedido creado en Web, decidido en Android, preparado y reflejado/persistido |

**Conteo visual vigente:** 8 teléfonos en la lámina actual. Perfil ya es uno de esos ocho; detalle de sesión, caja, cierre y disponibilidad son pantallas adicionales del mapa funcional, no teléfonos representados en esta imagen. Los estados de carga/error/offline forman parte de cada pantalla.

**Última revisión del plan:** 2026-10-21. Cambiar las fechas/estados solo con evidencia nueva y actualizar también `docs/00-gobierno/advances.md`.

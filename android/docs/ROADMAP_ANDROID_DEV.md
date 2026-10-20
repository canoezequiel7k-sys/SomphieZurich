# 🗺️ Plan de Desarrollo y Hoja de Ruta Android (Somphie Zúrich)

> **Documento Vivo de Coordinación:** Guía paso a paso para el desarrollo del Frontend Android (`:app`).  
> **Roles:** **Desarrollador** (Tú programas y aplicas cambios) | **Guía Técnico** (IA asesora, estructura y valida).

---

## 📌 1. Reglas de Oro y Principios del Proyecto

1. **El Backend es la Única Fuente de Verdad:**
   * La app **muestra y solicita acciones**, jamás decide estados, precios finales, permisos o saldos localmente de forma definitiva.
   * Sin mutaciones optimistas inseguras.
2. **Idempotencia Obligatoria:**
   * Toda acción que cambie estado (`START`, `READY`, `DELIVER`, `CANCEL`, `ACCEPT`, `REJECT`) envía un UUID único en la cabecera `Idempotency-Key`.
   * En caso de reintento por fallo de red o error 5xx, **se debe reutilizar exactamente la misma clave**.
3. **Manejo Completo de Estados UI:**
   * Toda pantalla debe soportar explícitamente: `Cargando (Loading)`, `Contenido (Content)`, `Vacío (Empty)`, `Error (Error)` y `Sin Conexión (Offline)`.
4. **Textos e Idioma:**
   * **Código y DTOs:** Inglés.
   * **Interfaz de Usuario:** Español Rioplatense (`es-AR`) centralizado en `res/values/strings.xml`. Cero literales dentro de Composables.
5. **Dinero en Centavos (`Minor`):**
   * Siempre representar montos en números enteros (`Long` / `Int` en centavos). Nunca `Float` o `Double`.

---

## 🔍 2. Estado Actual de la App (`:app`)

### 🟢 Ya Construido e Integrado (Fase 5 - Parcial)
* **Arquitectura Base:** Kotlin, Jetpack Compose, Material 3, Hilt, Corrutinas/Flow, Retrofit + OkHttp, Kotlinx Serialization.
* **Seguridad y Tokens:** `EncryptedTokenStore` (cifrado AES-GCM con Android Keystore + Preferences DataStore) y `RefreshTokenAuthenticator` para renovación síncrona en OkHttp.
* **Módulo `feature/auth`:** Login (`/auth/login`), Cambio obligatorio de contraseña (`/auth/change-password`), Consulta de Perfil/Permisos (`/me`), Logout.
* **Módulo `feature/stations`:**
  * Selección de estaciones asignadas según `/me` (`item.prepare`).
  * Cola de preparación de productos (`QUEUED`, `IN_PREPARATION`, `READY`).
  * Paginación por cursor (*keyset*).
  * Acciones de producto (`Empezar`, `Marcar Listo`, `Entregar`, `Cancelar`) con diálogo de motivo y conservación de clave idempotente.
  * Polling automático REST cada 5 segundos al estar en primer plano (`RESUMED`).
* **Pruebas:** Unitarias con `MockWebServer`, pruebas de ViewModel y pruebas instrumentadas Compose (`AppStartupTest`).

---

## 🎯 3. Plan de Trabajo Detallado Paso a Paso

---

### 🎨 BLOQUE 0 — Pulido Visual y UX de la Pantalla de Estaciones (Inmediato)
> **Objetivo:** Elevar la calidad visual y usabilidad de la pantalla actual antes de agregar nuevos módulos.

- [ ] **Tarea 0.1: Badges y Colores Semánticos para Estados de Ítems**
  * Diseñar un componente `StatusBadge` con colores distintivos de Material 3:
    * `QUEUED` (En espera): Fondo `surfaceVariant`, texto neutro.
    * `IN_PREPARATION` (En preparación): Fondo secundario / ámbar sutil.
    * `READY` (Listo): Fondo primario / verde sutil.
  * *Archivo a modificar:* `StationPreparationScreen.kt`

- [ ] **Tarea 0.2: Estilo Destructivo para la Cancelación de Producto**
  * Modificar el botón "Cancelar producto" para usar `OutlinedButton` o `Button` con color de contenedor de error (`MaterialTheme.colorScheme.errorContainer` o `error`).
  * Agregar ícono vectorial de advertencia/cancelación.
  * *Archivo a modificar:* `StationPreparationScreen.kt`

- [ ] **Tarea 0.3: Iconografía Material 3 en Tarjetas y Acciones**
  * Agregar íconos vectoriales a las acciones:
    * `Empezar`: Ícono de inicio/play o fuego/cocina.
    * `Marcar listo`: Ícono de check/tilde.
    * `Entregar`: Ícono de entrega/bandeja.
    * `Actualizar`: Ícono de refresco.
  * *Archivo a modificar:* `StationPreparationScreen.kt`

- [ ] **Tarea 0.4: Componente de Estado Vacío Ilustrado/Enriquecido**
  * Reemplazar el texto simple de cola vacía por una vista con ícono centrado y descripción amable.
  * *Archivo a modificar:* `StationPreparationScreen.kt`

- [ ] **Tarea 0.5: Implementar Gesto Pull-To-Refresh Nativo**
  * Integrar `PullRefreshBox` (o `Modifier.pullRefresh`) para permitir deslizar hacia abajo para actualizar la cola manualmente.
  * *Archivo a modificar:* `StationPreparationScreen.kt`

---

### 📥 BLOQUE 1 — WP-AND-51: Bandeja de Pedidos Entrantes (`PENDING_REVIEW`)
> **Objetivo:** Permitir al personal/camarero revisar pedidos en espera de confirmación y aceptarlos, rechazarlos o excluirlos.

- [ ] **Tarea 1.1: DTOs y Contrato de Red para Pedidos Entrantes**
  * Definir en `ApiModels.kt`:
    * `PendingOrderResponse`: Lista de pedidos pendientes con código, mesa, cliente, lista de ítems con opciones y hora.
    * `AcceptOrderRequest`: Lista de IDs de ítems excluidos (si aplica) y notas.
    * `RejectOrderRequest`: Motivo de rechazo.
  * Agregar endpoints en `StaffApi.kt`:
    * `GET /staff/orders/pending`
    * `POST /staff/orders/{orderId}/accept`
    * `POST /staff/orders/{orderId}/reject`

- [ ] **Tarea 1.2: Repositorio e Interfaz `InboxRepository`**
  * Crear en `feature/inbox/data/`:
    * `InboxRepository.kt` para orquestar las llamadas a `StaffApi`, capturar errores `ApiFailure` y gestionar claves idempotentes de decisión.

- [ ] **Tarea 1.3: Máquina de Estados e `InboxViewModel`**
  * Crear en `feature/inbox/ui/`:
    * `InboxUiState`: `Loading`, `Content` (lista de pedidos entrantes), `Empty`, `Error`.
    * `InboxViewModel.kt`: Manejar polling/actualización, aceptación con exclusiones opcionales y rechazo con motivo.

- [ ] **Tarea 1.4: Interfaz Compose para Bandeja de Entrantes**
  * Crear `InboxScreen.kt`:
    * Tarjeta de pedido entrante (`PendingOrderCard`): desglose de ítems, cantidad, opciones y resumen de montos.
    * Diálogo para confirmar rechazo con motivo obligatorio.
    * Diálogo/Hoja para desmarcar ítems individuales antes de aceptar (exclusiones).

- [ ] **Tarea 1.5: Manejo del Conflicto `409 ORDER_ALREADY_DECIDED`**
  * Capturar el conflicto si otro operador ya aceptó/rechazó el pedido, mostrar aviso claro al usuario y refrescar la lista.

- [ ] **Tarea 1.6: Pruebas Unitarias de Bandeja**
  * Crear `InboxViewModelTest.kt` para verificar estados de carga, aceptación exitosa, rechazo y reintentos ante error de red.

---

### 🪑 BLOQUE 2 — WP-AND-53: Tablero de Mesas y Disponibilidad de Productos
> **Objetivo:** Permitir la visualización del estado del salón/mesas y el control rápido del menú (pausar/activar productos u opciones).

- [ ] **Tarea 2.1: Contrato de Red para Mesas y Disponibilidad**
  * Definir DTOs en `ApiModels.kt`:
    * `StaffTableResponse`: Código de mesa, nombre, estado de sesión (`FREE`, `OCCUPIED`, `WAITING_PAYMENT`, etc.), mozo asignado.
    * `ProductAvailabilityRequest`: `available: Boolean`, `catalogVersion: Long`.
  * Agregar endpoints en `StaffApi.kt`:
    * `GET /staff/tables`
    * `GET /staff/sessions/{sessionId}`
    * `PATCH /staff/products/{productId}/availability`

- [ ] **Tarea 2.2: Repositorio de Mesas y Disponibilidad**
  * Crear en `feature/tables/data/` y `feature/availability/data/`:
    * `TablesRepository.kt` y `AvailabilityRepository.kt`.

- [ ] **Tarea 2.3: ViewModels y Estados de Mesas y Disponibilidad**
  * `TablesViewModel.kt` & `TablesUiState`.
  * `AvailabilityViewModel.kt` & `AvailabilityUiState`.

- [ ] **Tarea 2.4: Interfaces Compose (`TablesScreen` y `AvailabilityScreen`)**
  * `TablesScreen.kt`: Cuadrícula/Grid de tarjetas de mesas con colores según estado de la sesión.
  * `SessionDetailSheet.kt`: Hoja inferior para ver pedidos y total de la mesa.
  * `AvailabilityScreen.kt`: Lista de productos con interruptores (*Switch*) para activar/desactivar disponibilidad al instante.

---

### ⚡ BLOQUE 3 — WP-AND-60: Tiempo Real mediante SSE (Fase 6)
> **Objetivo:** Reemplazar el polling de 5s por una conexión persistente Server-Sent Events (SSE).

- [ ] **Tarea 3.1: Cliente OkHttp-SSE**
  * Configurar `EventSource.Factory` en `core/network/`.
  * Manejar reconexión automática con *backoff* exponencial.

- [ ] **Tarea 3.2: Consumo y Deduplicación de Eventos**
  * Suscribirse a eventos de dominio (`order_created`, `order_status_changed`, `item_status_changed`).
  * Implementar deduplicación por versión monotónica / cursor.

- [ ] **Tarea 3.3: Indicadores Sonoros y Vibración Opcional**
  * Reproducir un sonido breve o vibración al ingresar un pedido nuevo (configurable desde perfil).

---

### 💳 BLOQUE 4 — WP-AND-70 y WP-AND-71: Cobro en Caja, Pagos y Ajustes (Fase 7)
> **Objetivo:** Permitir el cobro en efectivo/simulado, aplicación de descuentos o cortesías y cierre de sesión de mesa.

- [ ] **Tarea 4.1: Módulo `feature/billing` (Cobro y Pagos)**
  * Formulario de cobro: efectivo, tarjeta/simulado, división de cuenta y propina libre.
  * Endpoint `POST /staff/sessions/{sessionId}/payments`.

- [ ] **Tarea 4.2: Aplicación y Anulación de Ajustes**
  * Pantalla para aplicar descuentos/cortesías con motivo obligatorio y validación de límites.

- [ ] **Tarea 4.3: Cierre de Sesión Normal y Forzado**
  * Cierre de mesa `POST /staff/sessions/{sessionId}/close`.

---

### 🧭 BLOQUE 5 — Navegación Estructurada (NavHost)
> **Objetivo:** Organizar la navegación entre múltiples módulos a medida que la app crece.

- [ ] **Tarea 5.1: Integrar Navigation Compose**
  * Configurar `NavHost` en `app/StaffApp.kt`.
  * Definir rutas con tipos seguros (Kotlin Serialisation Navigation / String routes).

- [ ] **Tarea 5.2: Barra de Navegación Inferior (`NavigationBar`)**
  * Mostrar pestañas según los permisos recibidos en `/me`:
    * 📥 **Entrantes** (`orders.accept`)
    * 🍳 **Estaciones** (`item.prepare`)
    * 🪑 **Mesas** (`tables.read`)
    * 📋 **Disponibilidad** (`catalog.manage`)

---

## 📝 4. Registro de Progresos (Checklist Activa)

| Bloque / Tarea | Estado | Observaciones |
|---|:---:|---|
| **Bloque 0: Pulido UI Estaciones** | ⏳ Pendiente | Tareas 0.1 a 0.5 |
| **Bloque 1: WP-AND-51 Entrantes** | ⏳ Pendiente | Tareas 1.1 a 1.6 |
| **Bloque 2: WP-AND-53 Mesas y Menú** | ⏳ Pendiente | Tareas 2.1 a 2.4 |
| **Bloque 3: WP-AND-60 Tiempo Real SSE** | ⏳ Pendiente | Tareas 3.1 a 3.3 |
| **Bloque 4: WP-AND-70/71 Cobro y Ajustes** | ⏳ Pendiente | Tareas 4.1 a 4.3 |
| **Bloque 5: Navegación NavHost** | ⏳ Pendiente | Tareas 5.1 y 5.2 |

---

> **Instrucciones para avanzar:** Pídeme la tarea que quieras comenzar (por ejemplo: *"Empecemos con la Tarea 0.1"*) y te daré la guía exacta, los componentes Compose y las instrucciones de implementación paso a paso.

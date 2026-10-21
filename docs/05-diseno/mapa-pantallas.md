# Mapa de pantallas

Cada pantalla lista: casos de uso, **datos** que consume (endpoints), **estados** a diseñar y notas. Todas deben cubrir: cargando · vacío · error · sin conexión (salvo que se indique).
Ver wireframes de baja fidelidad y matriz de cobertura en [wireframes.md](wireframes.md) (WP-UX-11).

## 1. Web cliente (`SC-C-*`)

```mermaid
flowchart TD
  C01[SC-C-01 Entrada QR] --> |sin sesión| C02[SC-C-02 Abrir mesa]
  C01 --> |con sesión| C03[SC-C-03 Unirse con código]
  C01 --> |token válido| C04
  C02 --> C04[SC-C-04 Menú]
  C03 --> C04
  C04 --> C05[SC-C-05 Detalle de producto]
  C04 --> C07[SC-C-07 Carrito y revisión]
  C05 --> C07
  C07 --> C08[SC-C-08 Estado del pedido]
  C08 --> C09[SC-C-09 Pedidos de la mesa]
  C04 --> C10[SC-C-10 Cuenta]
  C10 --> C11[SC-C-11 Pagar]
  C04 --> C12[SC-C-12 Participantes]
  C10 --> C13[SC-C-13 Visita finalizada y comprobante]
  C01 --> C14[SC-C-14 Errores globales]
```

| ID | Pantalla | CU | Datos (endpoint) | Estados / notas |
|---|---|---|---|---|
| SC-C-01 | Entrada QR (resolver mesa) | CU-01/04 | `GET /public/tables/{code}`, `GET /sessions/current` | QR inválido, mesa no disponible, local cerrado, retomar |
| SC-C-02 | Abrir mesa (nombre) | CU-02 | `POST …/sessions` | Validación de nombre; carrera `SESSION_ALREADY_OPEN` |
| SC-C-03 | Unirse (nombre + código) | CU-03 | `POST …/join` | Código erróneo, bloqueo con cuenta regresiva, sesión llena |
| SC-C-04 | Menú (categorías + lista) | CU-05 | `GET /menu` (ETag) | Agotado, vacío, caché sin conexión, local cerrado (solo lectura) |
| SC-C-05 | Detalle de producto | CU-05/06 | `GET /products/{id}` | Alérgenos, nota, cantidad, agotado |
| SC-C-06 | (reservado: opciones/modificadores, Q-02) | — | — | — |
| SC-C-07 | Carrito y revisión | CU-06/07 | local + `POST …/orders` | Total estimado, `PRICE_CHANGED`, `PRODUCT_UNAVAILABLE`, enviando, límite de pendientes |
| SC-C-08 | Estado del pedido | CU-08/09/10 | `GET /orders/{id}` (+SSE) | PENDING_REVIEW (con tiempo), ACCEPTED/IN_PREPARATION/READY/DELIVERED, REJECTED (motivo + corregir), EXPIRED, CANCELLED, exclusiones |
| SC-C-09 | Pedidos de la mesa | CU-08 | `GET /sessions/current/orders` | Quién pidió qué; vacío |
| SC-C-10 | Cuenta compartida | CU-11 | `GET …/bill` | Cargos, pagos, saldo, "pedidos pendientes no incluidos", saldo a favor |
| SC-C-11 | Pagar (simulado) | CU-12 | `POST …/payments` | Total / parcial / mi parte (divisor UI), **sin campos de tarjeta**, rechazo demo, excede saldo |
| SC-C-12 | Participantes y código | CU-13 | `GET …/participants` | Host: revocar/rotar; compartir código |
| SC-C-13 | Visita finalizada + comprobante | CU-04/14 | `GET …/receipt.pdf` | Ventana 24 h, vencido |
| SC-C-14 | Errores globales | — | — | Acceso retirado, sesión no encontrada, sin conexión, servidor dormido (arranque en frío) |
| SC-C-15 | Login/registro (F9) | CU-60 | `/auth/*` | — |
| SC-C-16 | Encuesta (F9) | CU-61 | — | — |

## 2. App Android del personal (`SC-S-*`)

```mermaid
flowchart TD
  S01[SC-S-01 Login] --> S02[SC-S-02 Home según permisos]
  S02 --> S03[SC-S-03 Entrantes]
  S03 --> S04[SC-S-04 Detalle de pedido: aceptar / rechazar / excluir]
  S02 --> S05[SC-S-05 Cola de estación]
  S05 --> S06[SC-S-06 Acciones de ítem]
  S02 --> S07[SC-S-07 Tablero de mesas]
  S07 --> S08[SC-S-08 Detalle de mesa y sesión]
  S08 --> S09[SC-S-09 Registrar pago / reembolso]
  S08 --> S10[SC-S-10 Cerrar sesión]
  S08 --> S12[SC-S-12 Nuevo pedido en nombre del cliente]
  S02 --> S11[SC-S-11 Disponibilidad de productos]
  S02 --> S13[SC-S-13 Perfil y ajustes]
```

| ID | Pantalla | CU | Datos | Estados / notas |
|---|---|---|---|---|
| SC-S-01 | Login del personal | CU-20 | `/auth/login` | Solo cuentas existentes; credenciales inválidas, bloqueo y cambio de contraseña obligatorio. Sin registro público ni autoservicio |
| SC-S-02 | Home (pestañas por permisos) | CU-20 | `/me` | Pestañas visibles según `permissions` |
| SC-S-03 | Entrantes | CU-21 | `GET /staff/orders` | Vacío ("Sin pedidos nuevos"), tiempo de espera, resaltado cerca del TTL, sonido/vibración opcional |
| SC-S-04 | Detalle de pedido | CU-22/23 | `GET /staff/orders/{id}` | Ítems con disponibilidad actual; excluir con motivo; `ORDER_ALREADY_DECIDED` (mostrar quién decidió) |
| SC-S-05 | Cola de estación (Cocina o Barra) | CU-24 | `GET …/queue` | Una vista filtrada por estación autorizada; FIFO, `delayed`, notas destacadas, filtro por estado |
| SC-S-06 | Acciones de ítem | CU-25–28 | `…/start·ready·deliver·cancel·undo` | Confirmación en cancelar (motivo), deshacer, errores de transición |
| SC-S-07 | Tablero de mesas | CU-29 | `GET /staff/tables` | 6 `displayStatus`, contador de pendientes, saldo (F7) |
| SC-S-08 | Detalle de mesa/sesión | CU-26/29/13 | `GET /staff/sessions/{id}` | Pedidos, cuenta, participantes, código; entregar listos |
| SC-S-09 | Pago en caja / reembolso | CU-32/33 | `/staff/…/payments`, `/refunds` | Saldo, saldo a favor |
| SC-S-10 | Cerrar sesión | CU-34/35 | `…/close`, `…/force-close` | Checklist de impedimentos, forzar (solo con permiso + motivo) |
| SC-S-11 | Disponibilidad | CU-36 | `GET /menu` + `PATCH …/availability` | Búsqueda, toggle inmediato con rollback si falla |
| SC-S-12 | Nuevo pedido a nombre del cliente | CU-31 | `POST …/orders` | Selector de productos |
| SC-S-13 | Perfil/ajustes | CU-20 | `/me` | Datos del usuario y estación; cerrar sesión y ajustes básicos solo si están soportados |
| SC-S-14 | Estados globales | — | — | Offline, sesión expirada, error de servidor |

### Correspondencia con la lámina Android recibida

La lámina representa ocho teléfonos: **Login (SC-S-01), Inicio (SC-S-02), Pedidos entrantes (SC-S-03), Detalle del pedido (SC-S-04), Mesas (SC-S-07), Cocina y Barra (dos vistas de SC-S-05), y Perfil (SC-S-13)**. Las acciones sobre cada producto corresponden a SC-S-06 dentro del flujo de estación; no requieren contar otro teléfono.

La maqueta es una referencia visual: no agrega endpoints ni autoriza mostrar métricas, saldos, temporizadores o datos personales que el backend no entregue al usuario. El texto “Olvidaste tu contraseña” de la imagen no tiene flujo de recuperación definido en el contrato actual, por lo que no se considera una acción implementable todavía.

Las cuentas de personal las crea el dueño; **no se ofrece registro público**. No se debe agregar un formulario de alta en la app. El alta desde un panel administrativo tampoco se implementa hasta que se aprueben sus operaciones y permisos (Q-26).

## 3. Panel de administración (`SC-A-*`)

| ID | Pantalla | CU | Notas |
|---|---|---|---|
| SC-A-01 | Login | CU-20 | Rutas protegidas; carga diferida del chunk admin |
| SC-A-02 | Dashboard | CU-43 | Ventas del día, pedidos activos, tiempos, tasa de rechazo |
| SC-A-03 | Pedidos y sesiones (lista + detalle) | CU-29 | Filtros, cursor |
| SC-A-04 | Catálogo (categorías y productos) | CU-40 | Reordenar, archivar, disponibilidad |
| SC-A-05 | Editor de producto | CU-40 | Imagen + alt, alérgenos, estación, `OPTIMISTIC_LOCK_FAILED` |
| SC-A-06 | Mesas y QR | CU-41 | Regenerar QR (con advertencia), hoja imprimible |
| SC-A-07 | Usuarios | CU-42 | Alta, roles, estaciones, deshabilitar; flujo administrativo pendiente de contrato (Q-26), no es registro público |
| SC-A-08 | Roles (solo lectura en MVP) | CU-42 | Matriz de permisos |
| SC-A-09 | Reportes | CU-43 | Rango de fechas, exportar PDF |
| SC-A-10 | Auditoría | CU-44 | Filtros y detalle |
| SC-A-11 | Ajustes del local | CU-45 | Horarios, límites, textos de comprobante |

## 4. Rutas (Web)
| Ruta | Pantalla |
|---|---|
| `/t/:tableCode` | SC-C-01 |
| `/mesa/abrir`, `/mesa/unirse` | SC-C-02, 03 |
| `/menu`, `/menu/p/:productId` | SC-C-04, 05 |
| `/carrito` | SC-C-07 |
| `/pedidos`, `/pedidos/:id` | SC-C-09, 08 |
| `/cuenta`, `/cuenta/pagar` | SC-C-10, 11 |
| `/participantes` | SC-C-12 |
| `/fin` | SC-C-13 |
| `/admin/*` | SC-A-* (lazy) |

## 5. Navegación Android (resumen)
`Login → Home(bottom nav conceptual: Inicio · Pedidos · Mesas · Más)`; Cocina/Barra se muestran como colas de la estación autorizada. La composición final depende de `permissions` y `stations` de `/me`; disponibilidad u otros destinos solo aparecen si están aprobados y habilitados para el perfil.

## 6. Etiquetas visibles para estados (es-AR)

| Estado | Etiqueta cliente | Etiqueta personal | Ícono sugerido |
|---|---|---|---|
| `PENDING_REVIEW` | Esperando confirmación | Nuevo | reloj |
| `ACCEPTED` | Confirmado | En cola | check |
| `IN_PREPARATION` | En preparación | En preparación | llama/taza |
| `READY` | Listo | Listo para entregar | campana |
| `DELIVERED` | Entregado | Entregado | doble check |
| `REJECTED` | No pudimos aceptarlo | Rechazado | x |
| `CANCELLED` | Cancelado | Cancelado | tachado |
| `EXPIRED` | Sin respuesta, podés reenviarlo | Vencido | reloj tachado |
| Mesa `FREE` / `OCCUPIED` / `PENDING_ORDERS` / `IN_SERVICE` / `AWAITING_PAYMENT` / `READY_TO_CLOSE` | — | Libre / Ocupada / Pedidos pendientes / En servicio / Esperando pago / Lista para cerrar | — |

> Estas etiquetas viven en el archivo de textos de cada app (no dentro de componentes).

## 7. Cambios de la v0.2
| Pantalla | Cambio |
|---|---|
| SC-C-04/05 | Selector de idioma; hoja de **opciones** (SC-C-06 deja de estar reservada); placeholder de imagen |
| SC-C-07 | Líneas por combinación de opciones; mensajes `OPTION_*` |
| SC-C-10 | Ajustes con etiqueta genérica y propinas aparte |
| SC-C-11 | **Propina libre** (campo + ayudas), pago solo propina |
| SC-C-09 / nuevo SC-C-17 | Botón **Llamar al mozo** con motivos |
| SC-S-05/06 | Opciones visibles en cada ítem |
| SC-S-07 | Insignia de llamados al mozo (F9) |
| SC-S-08 | Acción **Aplicar ajuste** (tipo, monto o ítem, motivo) |
| SC-S-11 | Disponibilidad de **opciones** |
| SC-A-04/05 | Grupos de opciones, asignación a productos, traducciones |
| SC-A-09 | Reportes de propinas y descuentos |

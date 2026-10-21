# Wireframes de baja fidelidad y matriz de cobertura de estados (WP-UX-11)

Este documento define los wireframes de baja fidelidad (esquemas estructurales) y la matriz de cobertura de estados obligatorios para todas las pantallas y superficies de Somphie Zürich, conforme a `docs/05-diseno/mapa-pantallas.md` y las reglas de UI (`AGENTS.md` §2.10).

---

## 1. Matriz de Cobertura de Estados Obligatorios

Toda pantalla interactiva del sistema (Web cliente y App Android) debe tener diseño y manejo explícito para los siguientes 5 estados:

| Estado | Definición | Comportamiento esperado en UI |
|---|---|---|
| **1. Cargando (Loading)** | Datos en tránsito o inicialización | Skeleton screens o indicador de progreso sutil (sin bloquear toda la pantalla a menos que sea crítico). |
| **2. Éxito (Success)** | Datos cargados o acción completada | Visualización de contenido, toasts de confirmación breves, redirección limpia. |
| **3. Vacío (Empty)** | Sin registros (ej. sin pedidos, carrito vacío) | Ilustración/ícono sutil, texto explicativo ("Aún no hay pedidos en esta mesa") y llamada a la acción (CTA) clara. |
| **4. Error (Error)** | Fallo de red, validación o servidor | Mensaje descriptivo amigable, código de error si aplica, botón de reintento ("Reintentar") y opción de soporte si persiste. |
| **5. Sin Conexión (Offline)** | Pérdida de conectividad | Banner superior no intrusivo ("Sin conexión a internet. Mostrando datos cacheados"), desactivación temporal de acciones de escritura. |

---

## 2. Wireframes de Baja Fidelidad — Vista Cliente Web (`SC-C-*`)

### SC-C-01: Entrada QR / Resolución de Mesa
```text
+------------------------------------+
|  [Logo Somphie Zürich]             |
|                                    |
|       [Escaneando QR / Mesa #3]    |
|                                    |
|   Estado: Verificando mesa...      |
|   [==================>     ]       |
|                                    |
|   [Cargando...]                    |
+------------------------------------+
```

### SC-C-04: Menú (Categorías + Productos)
```text
+------------------------------------+
|  Mesa #3 | [🔍 Buscar] | [🛒 (2)]   |
+------------------------------------+
| [Cafés] [Bebidas] [Panadería] [Sal]|
+------------------------------------+
| [Img] Café Cortado                 |
|       Equilibrado con leche...     |
|       $ 2.500 ARS        [ + ]     |
+------------------------------------+
| [Img] Medialuna de Manteca         |
|       Recién horneada              |
|       $ 1.200 ARS        [ + ]     |
+------------------------------------+
| [ Estado: Conectado | Barra Nav ]  |
+------------------------------------+
```

### SC-C-07: Carrito y Revisión
```text
+------------------------------------+
| < Volver al Menú      Mi Carrito   |
+------------------------------------+
| Mesa #3 - Resumen de Pedido        |
|                                    |
| 1x Café Cortado         $ 2.500    |
| 2x Medialunas           $ 2.400    |
| ---------------------------------- |
| Subtotal                $ 4.900    |
| Propina sugerida (10%)    $ 490    |
| ---------------------------------- |
| TOTAL ESTIMADO          $ 5.390    |
|                                    |
| [ Nota para cocina (opcional) ]    |
|                                    |
| [ ENVIAR PEDIDO A COCINA ]         |
+------------------------------------+
```

### SC-C-08: Estado del Pedido
```text
+------------------------------------+
| Estado del Pedido                  |
+------------------------------------+
| Pedido #104                        |
| [⏳ Pendiente de revisión]         |
| Tiempo estimado: ~10 min           |
|                                    |
| Ítems:                             |
| • 1x Café Cortado (En preparación) |
| • 2x Medialunas   (Listo)          |
|                                    |
| [ Actualizar estado ]              |
+------------------------------------+
```

---

## 3. Wireframes de Baja Fidelidad — App Android del Personal (`SC-S-*`)

La lámina de referencia contiene ocho teléfonos. Los esquemas siguientes cubren cada uno; son estructura visual, no agregan endpoints ni autorizan datos que el backend no entregue. Cocina y Barra son dos selecciones de la misma cola por estación (`SC-S-05`).

### SC-S-01: Login del personal
```text
+------------------------------------+
|            Somphie Zürich          |
|                                    |
|       Acceso del personal          |
|                                    |
| Usuario / correo                   |
| [____________________________]     |
| Contraseña                  [◉]    |
| [____________________________]     |
|                                    |
| [          Ingresar            ]   |
|                                    |
| No hay registro público.           |
+------------------------------------+
```

Las cuentas las crea el dueño; no se muestra registro. La recuperación de contraseña no se incluye hasta que exista un flujo contractual. El cambio obligatorio de contraseña se muestra después del login cuando lo exige el backend.

### SC-S-02: Inicio / panel del turno
```text
+------------------------------------+
| ☰ Inicio                    [MC]   |
| Hola, Matías                       |
| Turno actual: Barra                |
+------------------------------------+
| Pedidos       Mesas                |
| Nuevos: 5     Activas: 8           |
+------------------------------------+
| Cocina        Barra                |
| En preparación                    |
+------------------------------------+
| Resumen del día                    |
| [Solo métricas que entregue la API]|
+------------------------------------+
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

Las cifras y el resumen se muestran solo si el endpoint autorizado los provee; no se calculan en la app. Los destinos dependen del perfil y sus permisos.

### SC-S-03: Pedidos entrantes
```text
+------------------------------------+
| ‹ Pedidos                     [↻]  |
| [Nuevos] [En cocina] [En barra]    |
+------------------------------------+
| Mesa 4                     Nuevo  |
| 2 productos · 10:24                |
| [Ver detalle]                      |
| [Rechazar]             [Aceptar]   |
+------------------------------------+
| Mesa 7                     Nuevo  |
| 2 productos · 10:18                |
| [Ver detalle]                      |
| [Rechazar]             [Aceptar]   |
+------------------------------------+
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

### SC-S-04: Detalle del pedido
```text
+------------------------------------+
| ‹ Pedidos     Detalle del pedido   |
+------------------------------------+
| Mesa 4 · 3 productos        Nuevo  |
| 10:24                              |
+------------------------------------+
| 2 × Latte                          |
| 1 × Tostado mixto                  |
| Nota: sin azúcar en el latte       |
+------------------------------------+
| Total: solo si la respuesta lo     |
| incluye para este perfil           |
+------------------------------------+
| [          Rechazar             ]  |
| [          Aceptar              ]  |
+------------------------------------+
```

El rechazo y las exclusiones deben respetar motivos y permisos del contrato; al recibir un conflicto se vuelve a consultar el pedido.

### SC-S-07: Estado de mesas
```text
+------------------------------------+
| ‹ Mesas                       [↻]  |
| [Todas] [Ocupadas] [Libres]        |
+------------------------------------+
| Mesa 1       Mesa 2       Mesa 3   |
| Ocupada      Ocupada      Ocupada  |
|                                  |
| Mesa 4       Mesa 5       Mesa 6   |
| Ocupada      Libre        Ocupada  |
+------------------------------------+
| El estado visible viene del backend|
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

No sumar estados ni acciones que no estén en la respuesta/contrato. Saldo y pendientes solo se muestran para perfiles autorizados.

### SC-S-05: Cola de estación — Cocina (teléfono 6)
```text
+------------------------------------+
| ☰ Cocina                     [↻]  |
| [En preparación]          [Listos] |
+------------------------------------+
| Mesa 7                     8 min   |
| 2 × Tostado mixto                  |
|                                    |
| Mesa 4                    12 min   |
| 1 × Medialuna                      |
+------------------------------------+
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

### SC-S-05: Cola de estación — Barra (teléfono 7)
```text
+------------------------------------+
| ☰ Barra                      [↻]  |
| [En preparación]          [Listos] |
+------------------------------------+
| Mesa 2                     6 min   |
| 2 × Cappuccino                     |
|                                    |
| Mesa 6                    10 min   |
| 1 × Café espresso                  |
+------------------------------------+
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

Los tiempos, opciones, notas y acciones por ítem se presentan solo si vienen en la respuesta y el perfil puede verlos. Las acciones de preparación pertenecen a `SC-S-06`.

### SC-S-13: Perfil
```text
+------------------------------------+
| Perfil                       [⚙]  |
+------------------------------------+
|              [MC]                  |
|            Matías C.               |
|              Camarero              |
+------------------------------------+
| Mi turno · Barra                ›  |
| Cambiar estación                 ›  |
| Ayuda                            ›  |
| Cerrar sesión                    ›  |
+------------------------------------+
| Inicio | Pedidos | Mesas | Más     |
+------------------------------------+
```

Mostrar cambio de estación solo cuando el backend permita seleccionar otra estación autorizada; cerrar sesión usa el flujo de autenticación existente.

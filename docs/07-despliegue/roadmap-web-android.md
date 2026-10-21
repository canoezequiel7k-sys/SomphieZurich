# Roadmap práctico de Web y Android

Este documento ordena qué falta para terminar los frontends y qué necesita cada uno del backend. Está basado en el código, el OpenAPI y el seguimiento del repositorio al 19-10-2026. No reemplaza las reglas ni el contrato.

## Lo importante primero

**Web y Android no se envían datos directamente entre sí.** Los dos usan el mismo backend; el backend guarda y decide los estados en PostgreSQL.

El recorrido ya existe en sus partes principales:

1. Web abre/retoma una sesión, muestra el menú, arma el carrito, envía el pedido y consulta su estado.
2. El backend guarda el pedido como `PENDING_REVIEW`.
3. El personal lo acepta. Hoy esto puede hacerse mediante la API; todavía no hay bandeja Android para aceptar pedidos.
4. Al aceptarlo, sus productos entran en las colas de las estaciones asignadas.
5. Android prepara los productos y confirma las acciones al backend.
6. Web vuelve a consultar el estado del pedido.

Por eso, que la Web diga **“El pedido se recibió”** no quiere decir que Android ya lo tenga en la cola. En la captura revisada el pedido esperaba confirmación y Android estaba en **Cocina**: el Flat white corresponde a **Barra**; la medialuna, a **Cocina**. Esa pantalla vacía no demuestra que la persistencia haya fallado.

## Estado y porcentaje del backend

- **Web cliente:** el recorrido QR, menú, carrito, envío y seguimiento está implementado. La bitácora registra una validación previa con backend y PostgreSQL reales.
- **Android personal:** ya tiene login, sesión segura, estaciones, cola, acciones de preparación, refresco y manejo de errores. La guía Android registra pruebas en Moto G31 contra el backend real.
- **Backend:** hay APIs de sesión, pedido, decisión de aceptar/rechazar, identidad, permisos y preparación persistida. El contrato define la aceptación y los estados de los productos.
- **Falta para que todo sea visual y continuo:** bandeja Android para pedidos entrantes; además faltan pantallas de otras fases, integraciones y decisiones que se detallan abajo.

La bitácora registra **408 pruebas backend y 94,76 % de cobertura de líneas** como resultado de una ejecución anterior. El 94,76 % mide qué líneas ejecutaron las pruebas; **no** significa que el 94,76 % del producto esté terminado. No hay un porcentaje único y honesto de “backend completo”: los paquetes tienen tamaños distintos y algunos están implementados localmente, pero aún no integrados o bloqueados por decisiones. No se volvió a correr esa suite para escribir este roadmap.

## Dependencia entre las partes

```text
OpenAPI y reglas aprobadas
          ↓
Backend + PostgreSQL (valida permisos, precios y estados)
          ↓
   ┌──────┴─────────┐
   ↓                ↓
Web cliente     Android personal
crea/consulta   acepta/prepara
   └──────┬─────────┘
          ↓
 Ambos vuelven a leer el estado del backend
```

- Web y Android pueden programarse en paralelo cuando ambos usan un contrato existente.
- Ninguna app debe modificar el estado por su cuenta ni hablar directamente con la base de datos.
- Para una pantalla que necesita una ruta o regla aún no aprobada, se puede preparar diseño y pruebas de componentes; no se debe inventar la integración ni simularla como terminada.

## Camino recomendado, de principio a fin

Cada paso indica quién depende de quién. “Vos” se refiere a la persona que implementa el frontend; el agente puede explicar, revisar el contrato y ayudar a probar, pero no hace falta que programe por vos.

### 0. Asegurar una base repetible

**Estado: casi listo; usá la guía de prueba actual.**

1. Levantá PostgreSQL, backend y Web según [probar Web y Android](./probar-web-y-android.md).
2. Confirmá salud del backend, login y que la API devuelve mesas/menú.
3. Usá una mesa habilitada. Si no hay ninguna, seguí el procedimiento de cierre demo de esa guía solo si aceptás cancelar los pedidos pendientes que muestra.
4. Probá Web → pedido persistido → aceptación por API → Android → acción de preparación → estado actualizado en Web.

**Depende de:** entorno local, contrato ya implementado y una mesa de prueba disponible.  
**Terminado cuando:** una ejecución completa se repite sin limpiar la base y los cambios se verifican en la API o PostgreSQL.  
**No exige:** terminar primero la bandeja Android; la aceptación API es el puente temporal.

### 1. Completar la bandeja de pedidos entrantes en Android — prioridad para empezar

**Trabajo:** `WP-AND-51`. La API de aceptar/rechazar ya existe; la pantalla Android no.

1. Leer en OpenAPI `GET /staff/orders`, `GET /staff/orders/{orderId}`, `POST .../accept` y `POST .../reject`; confirmar campos, permiso `order.read`/`order.accept`, respuestas y errores.
2. Agregar los modelos Retrofit/serialización necesarios sin cambiar el contrato.
3. Implementar repositorio y ViewModel con estados de carga, lista vacía, error, actualización y detalle.
4. Mostrar mesa, código de pedido y productos permitidos por el alcance del usuario. No mostrar pedidos/ítems que el backend no autorice.
5. Añadir aceptar/rechazar con `Idempotency-Key`; bloquear doble toque mientras se envía.
6. Manejar `ORDER_ALREADY_DECIDED` y otros conflictos volviendo a leer el pedido y explicando el resultado, no forzando un estado local.
7. Agregar pruebas de ViewModel/repository para éxito, vacío, error, conflicto, permisos e idempotencia; agregar prueba UI de la ruta principal.
8. Integrar contra backend/PostgreSQL reales y comprobar que al aceptar el pedido sus ítems aparecen en las estaciones correctas.

**Depende de:** backend actual (`WP-BE-44`), login/permisos (`WP-AND-50`) y contrato existente. No depende de Web: la Web solo genera los pedidos para una prueba integral.

**Límite del primer corte:** el rechazo puede implementarse desde su contrato. La exclusión de productos requiere revisar con cuidado el esquema de aceptación y el detalle de ítems; no agregues un formulario o motivo no definido.

### 2. Completar preparación en Android

**Trabajo:** terminar y probar `WP-AND-52`; luego ampliar con las fases disponibles.

1. Revisar las colas por estación y que cada fila muestre datos útiles, opciones y estados.
2. Comprobar acciones permitidas por estado y permiso: iniciar, marcar listo, entregar, cancelar/deshacer solo cuando el backend lo autoriza.
3. Tras cada respuesta, volver a consultar la cola; no adelantar el cambio en pantalla como definitivo.
4. Probar conflicto, pérdida de red y reintento con la misma clave idempotente.
5. Verificar una orden con productos en Barra y Cocina, cada uno en su estación.

La cola actual usa REST y refresco; **no esperes SSE** para terminar este bloque.

**Depende de:** autenticación y preparación backend (`WP-BE-50`) ya disponibles en el alcance registrado.

### 3. Cerrar el flujo de Web cliente

**Trabajo:** completar pruebas y los defectos concretos que aparezcan en `WP-WEB-40/41/42`.

1. Verificar mesa/QR, sesión nueva y retomada en ventana privada.
2. Verificar menú, opciones obligatorias, carrito y total estimado.
3. Enviar pedido y manejar cambio de precio, producto no disponible, error y reintento.
4. Confirmar que “recibido” significa `PENDING_REVIEW`.
5. Consultar el pedido después de aceptar/preparar desde Android; probar refresco manual y polling actual.
6. Probar móvil, lista vacía, carga, error de backend y pérdida/recuperación de conexión.

**Depende de:** endpoints de cliente ya implementados (`WP-BE-40/42`) y backend/PostgreSQL real para E2E. **No depende de Android** para programar; sí para probar de punta a punta aceptación y preparación.

### 4. Hacer la prueba integral Web → Android sin pasos manuales

Esta meta queda alcanzada cuando completás el paso 1 y podés:

1. Crear pedido desde Web.
2. Verlo como entrante en Android.
3. Aceptarlo desde Android.
4. Ver los productos en las estaciones asignadas.
5. Cambiar su estado desde Android.
6. Ver la actualización en la Web y persistida en PostgreSQL.

**Depende de:** pasos 1, 2 y 3 y de un backend/base de prueba activos. Es un hito alcanzable con las APIs existentes; no requiere que Web y Android se conecten entre sí.

### 5. Completar Android de mesas y disponibilidad

**Trabajo previsto:** `WP-AND-53`.

1. Revisar qué operaciones de tablero, detalle y disponibilidad existen realmente en OpenAPI y backend.
2. Construir tablero/detalle usando únicamente los estados y permisos del servidor.
3. Añadir estados de carga, vacío, error y actualización; confirmar cambios tras una respuesta del backend.
4. Cubrir permisos y reglas con pruebas locales y contra PostgreSQL.

**Depende de:** operaciones de `WP-BE-51/52` y sus decisiones abiertas aplicables. No empezar por una pantalla si no está clara la mutación que la alimenta.

### 6. Completar pagos, cuenta y cierre

**Trabajo previsto:** Android `WP-AND-70/71`; Web `WP-WEB-70/71`.

1. Antes de UI, revisar las rutas de cuenta, pagos, ajustes, anulaciones/reembolsos y cierre en OpenAPI.
2. Implementar primero el flujo staff de caja/cierre que habilita backend; después la vista de cuenta/pago del cliente Web.
3. Mostrar los importes que devuelve el servidor en centavos formateados; no calcular un saldo final localmente ni pedir datos de tarjeta.
4. Añadir confirmaciones, permisos, estados de error/conflicto y claves idempotentes donde el contrato las requiere.
5. Probar casos de pago, saldo, reembolso y cierre con pruebas de backend y flujo integrado.

**Depende de:** backend de Billing/cierre (`WP-BE-70/71`) y UX aprobada. El código backend registrado tiene partes implementadas localmente; confirmá la rama/contrato actuales antes de conectar cada pantalla. Este paso no es requisito para probar el flujo de pedido y preparación.

### 7. Tiempo real — después, no como bloqueo

**Trabajo previsto:** backend `WP-BE-60`, Web `WP-WEB-60`, Android `WP-AND-60`.

1. Mantener REST/polling mientras cubra la necesidad actual.
2. Completar backend SSE, lifecycle y productores pendientes.
3. Acordar objetivos operativos de conexiones/recursos y reproducir la desconexión TCP antes de declarar capacidad o reconexión resuelta.
4. Recién entonces implementar consumo SSE en cada app, deduplicación, cursores, reconexión y fallback.

**Bloqueos:** faltan criterios aprobados de capacidad sostenida y evidencia reproducible de desconexión TCP. No inventes umbrales ni backoff como regla del servidor. La funcionalidad principal de preparar pedidos puede terminar antes con REST.

### 8. Panel de administración y reportes

**Trabajo previsto:** Web `WP-WEB-80/81`, backend `WP-BE-80/81/82`.

No empieces mutaciones ni inventes pantallas funcionales hasta resolver **Q-26**: operaciones y permisos de catálogo/imágenes, mesas/QR, personal/estaciones, opciones/traducciones; además, filtros, fórmulas y permisos de reportes/auditoría. Primero se aprueban decisiones, luego OpenAPI y `CHANGELOG.md`, luego backend y finalmente Web.

**Se puede adelantar sin bloquearse:** estudiar React, preparar componentes genéricos y revisar wireframes existentes. No conectar mocks a una API inexistente ni presentar el panel como operativo.

### 9. Registro de clientes, correo y encuesta

**Trabajo previsto:** backend `WP-BE-90/91`, Web `WP-WEB-90`.

Permanece bloqueado por **Q-35** (verificación, recuperación, sesiones/vinculación) y **Q-36** (correos, destinatarios, transporte, duplicados y reintentos). Requiere decisiones aprobadas y contrato antes de UI conectada.

### 10. Calidad y entrega

Al terminar cada pantalla:

- Web: `corepack pnpm run check` desde `web`; pruebas de componentes y E2E real según el cambio.
- Android: `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug` desde `android`; si hay dispositivo, `.\gradlew.bat connectedDebugAndroidTest`.
- Backend: ejecutar pruebas focalizadas para verificar contratos, permisos, estados y persistencia.
- Para la entrega: prueba en móvil real, errores y desconexión, instalación limpia, configuración de URL, guía corta de arranque/apagado y lista explícita de límites.

No uses MSW/fakes para afirmar integración real: sirven para desarrollar pantallas mientras backend o servicios no están disponibles, pero el hito integrado exige backend y PostgreSQL reales.

## ¿Se puede terminar?

**Sí, el recorrido principal de pedidos y preparación se puede terminar con el backend y el contrato actuales.** La ruta más directa es: Android entrantes → preparación integrada → recorrido Web validado → prueba integral. La bandeja Android elimina el paso manual de aceptación.

**No se puede prometer hoy el producto entero de todas las fases:** panel admin/reportes y registro/correos dependen de Q-26, Q-35 y Q-36; SSE productivo depende de objetivos y pruebas operativas todavía pendientes. Esos bloqueos no impiden que vos termines los frontends y pruebes el MVP de pedido/preparación.

## Referencias vigentes

- [Roadmap por fases](../08-agentes/roadmap-fases.md)
- [Guía de desarrollo Android](../08-agentes/android.md)
- [Guía de desarrollo Web](../08-agentes/web.md)
- [Contrato API](../04-api/openapi.yaml)
- [Guía para probar Web y Android](./probar-web-y-android.md)
- [Decisiones abiertas](../00-gobierno/decisiones.md)

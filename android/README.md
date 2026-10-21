# App Android del personal

Aplicación nativa Kotlin/Compose para que el personal opere los pedidos desde las estaciones que el backend le asigna. La primera entrega cubre el login de personal y el flujo de preparación de Fase 5; no reemplaza la validación de permisos del servidor.

Para estudiar responsabilidades, flujo de datos, permisos, pruebas y lectura progresiva del módulo, consultá la [guía de arquitectura y estudio](./docs/ARCHITECTURE_AND_STUDY_GUIDE.md).

Para el plan detallado de pantallas, dependencias y pasos de implementación personal, consultá el [roadmap Android](./docs/ROADMAP_ANDROID_DEV.md).

## Ejecutar localmente

Se requiere Android Studio/JDK 17 o superior y Android SDK 37. El wrapper incluido usa Gradle 9.8.1; el proyecto compila con AGP 9.3.3, Kotlin integrado y Hilt 2.60.1, cuya versión actual requiere AGP 9 o superior. El plugin `com.android.legacy-kapt` conserva el procesamiento de anotaciones que Hilt necesita mientras el proyecto sigue usando la DSL nueva de AGP. En este worktree, el nombre de la carpeta contiene un carácter no ASCII; por eso `android.overridePathCheck=true` evita que AGP detenga el build en Windows. El emulador usa por defecto `http://10.0.2.2:8080/api/v1/`, que apunta al backend local del host. El backend debe estar levantado con PostgreSQL y las semillas locales de usuarios/estaciones.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

Para un dispositivo físico conectado por USB, `adb reverse` hace que `127.0.0.1:8080` del celular llegue al puerto 8080 de esta computadora. Así no dependés de una IP Wi-Fi que puede cambiar ni abrís el puerto del backend a la red local:

```powershell
adb devices
adb reverse tcp:8080 tcp:8080
.\gradlew.bat -PsomphieApiBaseUrl=http://127.0.0.1:8080/api/v1/ assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.somphie.staff/.MainActivity
```

`-PsomphieApiBaseUrl` solo configura el APK de esta compilación; no modifica archivos del repositorio. Sin `adb reverse`, un celular físico no puede usar `10.0.2.2`: se puede indicar la IP LAN de la computadora, por ejemplo `-PsomphieApiBaseUrl=http://192.168.0.242:8080/api/v1/`, siempre que celular y PC estén en la misma red y el firewall permita ese puerto. No se debe usar esa URL HTTP para `release`: producción exige HTTPS. La variante `debug` permite HTTP para facilitar pruebas locales. La app no guarda contraseñas. El access token y el refresh token se cifran con AES-GCM usando una clave no exportable de Android Keystore y se escriben en Preferences DataStore.

## Levantar backend local para probar con el celular

Desde la raíz del repositorio, iniciá PostgreSQL sin borrar su volumen:

```powershell
docker compose -f infra\docker-compose.yml up -d postgres
```

En una segunda terminal PowerShell, iniciá el backend en perfil `dev` con una clave JWT efímera de 256 bits y la contraseña demo documentada. La clave solo vive en esa terminal y se puede regenerar al reiniciar:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DEMO_STAFF_PASSWORD = "demo-only-password"
.\backend\gradlew.bat -p backend bootRun --no-daemon --max-workers=1
```

Cuando Spring informe que escucha en el puerto 8080, en otra terminal configurá el túnel USB y compilá/instalá el APK apuntando a él:

```powershell
adb reverse tcp:8080 tcp:8080
.\android\gradlew.bat -p android assembleDebug -PsomphieApiBaseUrl=http://127.0.0.1:8080/api/v1/ --no-daemon --max-workers=1
adb install -r .\android\app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.somphie.staff
adb shell am start -n com.somphie.staff/.MainActivity
```

Para probar autenticación, usá una cuenta demo aprobada en `docs/07-despliegue/entorno-local.md` (por ejemplo `bar@somphie.demo`) y la `DEMO_STAFF_PASSWORD` configurada para ese backend. No uses esas cuentas en producción. Los errores de crash se consultan con `adb logcat -v time -s AndroidRuntime:E '*:S'`; `/actuator/health` en `http://127.0.0.1:8080/api/v1/actuator/health` permite comprobar la disponibilidad del backend desde la PC.

## Flujo de preparación

1. El personal inicia sesión con `/auth/login`; si la cuenta exige un cambio de contraseña, la app usa `/auth/change-password` y vuelve a consultar `/me`.
2. `/me` determina si el perfil tiene `item.prepare`. `/staff/stations` entrega únicamente las estaciones que esa cuenta puede consultar (o todas para OWNER); el selector no construye estaciones a partir del nombre del rol.
3. La cola se consulta con estados `QUEUED`, `IN_PREPARATION` y `READY`. Cocina/barra ve orden, mesa, cantidad, opciones, nota del producto y demora, sin precios.
4. Los botones se derivan de los permisos de `/me` y del estado leído. El backend vuelve a comprobar permiso, estación, sesión y transición en cada solicitud.
5. Después de una acción confirmada, la app vuelve a consultar la cola: no inventa una transición local. Un conflicto `409` también dispara una lectura nueva.

La pantalla actualiza la cola por REST cada 5 segundos mientras está en primer plano, de acuerdo con `docs/03-arquitectura/tiempo-real.md` para Fase 5. SSE queda para Fase 6; WP-BE-60 todavía no tiene criterios aprobados de capacidad sostenida. La cola usa paginación keyset: “Cargar más” solicita la página siguiente, y “Actualizar” vuelve al inicio porque el contrato advierte que las páginas son de consistencia eventual.

Cada acción envía un UUID nuevo en `Idempotency-Key`. Si la conexión falla o el servidor responde 5xx, se conserva la misma operación, motivo y clave en memoria para que el personal pueda reintentar sin duplicar el efecto. Los estados visibles siempre vuelven a leerse del backend. Si el proceso Android se cierra, la siguiente apertura vuelve a consultar el servidor; la operación no se simula como aplicada localmente.

## Pruebas

- `testDebugUnitTest`: 8 pruebas locales; cubren estados de carga/vacío/error, permisos y transiciones visibles, conflictos, reintentos con la misma clave, y rutas HTTP con MockWebServer. No sustituyen integración con PostgreSQL.
- `connectedDebugAndroidTest`: `AppStartupTest` verifica en el dispositivo la composición Hilt/Compose sin sesión, los íconos oscuros sobre las barras claras y la recuperación ante el puerto local cerrado `127.0.0.1:1`; no simula una respuesta HTTP del backend.
- Validación de esta continuación: `:app:compileDebugKotlin testDebugUnitTest assembleDebug lintDebug` terminó con BUILD SUCCESSFUL; las 8 pruebas unitarias pasaron. `connectedDebugAndroidTest` terminó con BUILD SUCCESSFUL en el Moto G31: 3 pruebas, 0 fallos, 0 errores, 0 omitidas. `lintDebug` informó 19 advertencias (SDK, versiones disponibles, ortografía, backup e icono), sin errores.
- Las pruebas `IdentityIntegrationTest` del backend cubren persistencia HTTP/PostgreSQL de colas, permisos y transiciones. Ejecutarlas no equivale a probar la app Android instrumentada.

## Validación física del 18-10-2026

El Moto G31 con Android 12 inició la app y se conectó al backend y PostgreSQL locales. Desde el teléfono se probó un login inválido (mensaje de error visible) y uno válido con la cuenta demo Owner; `/me` cargó la estación BAR autorizada. La sesión de `TBL-001` (`5d4a6b28-395c-438b-8fba-2f1ed131a60d`) contiene el pedido `007e447e-5f16-4bfb-a665-88b6523ef104` y el ítem BAR `be7831c1-2d4b-4cd1-ae6d-2324558e9af1`. Desde Android se confirmaron las transiciones `QUEUED -> IN_PREPARATION -> READY` por REST y PostgreSQL. Una lectura posterior encontró el ítem `DELIVERED`; no se atribuye ese último cambio a esta prueba. La sesión continúa abierta y su saldo no fue liquidado.

Para dejar un ítem accionable en la cola, se creó mediante el flujo cliente QR una segunda sesión en `TBL-003` y el Owner aceptó el pedido con el endpoint staff existente (la bandeja de entrantes WP-AND-51 aún no está en Android). Su sesión es `342a26f3-a2f7-4913-a313-5954d0cd9c3e`, el pedido `d3062512-39a1-47ed-b140-237d55c609f9` y el ítem BAR `722c4566-e940-4549-8c48-b7809ce870cb`. En la versión final instalada, el ítem apareció en la cola y la acción **Empezar** lo dejó en `IN_PREPARATION`; PostgreSQL confirmó `IN_PREPARATION` para el pedido y el producto. La pantalla actual queda en la estación Barra con las acciones que corresponden a ese estado. La sesión preexistente de `TBL-002` no se modificó. No se insertaron, actualizaron ni borraron filas directamente en la base de datos.

La inspección visual confirmó íconos oscuros legibles sobre las barras claras. En la jerarquía accesible del dispositivo, **Empezar**, **Marcar listo** y **Cancelar producto** conservan sus etiquetas completas; las acciones rápidas comparten una fila y la cancelación ocupa una fila propia para evitar que su texto se comprima. La prueba instrumentada también comprueba el contraste de las barras del sistema.

El cambio obligatorio de contraseña no se pudo probar: las cuentas de la semilla `dev`/`demo` tienen `mustChangePassword=false` y no existe un flujo autorizado documentado para convertir una cuenta demo a contraseña temporal. No se modificó esa bandera. La pantalla de login ante un backend inaccesible se probó en la prueba instrumentada; el login inválido y el válido se probaron físicamente contra el backend local.

No se limpió ninguno de los fixtures. `TBL-001` ya tiene un ítem `DELIVERED`: no se debe intentar cancelarlo; para liberar esa mesa habrá que liquidar el saldo por el flujo de pago autorizado y luego cerrar la sesión con `POST /staff/sessions/{sessionId}/close`. El ítem de `TBL-003` sigue sin entregar y puede permanecer para continuar la prueba; cualquier cancelación requiere permiso, motivo, clave idempotente nueva y que el backend permita la transición desde su estado actual. No borres filas ni uses `docker compose down -v`. La disponibilidad de mesas para pruebas queda limitada también por la sesión preexistente e intacta de `TBL-002`.

## Límites de esta entrega

La app aún no incluye bandeja de pedidos entrantes, tablero de mesas, pagos ni cierre. Tampoco consume SSE: ese cambio pertenece a la siguiente fase del contrato de tiempo real, con su recuperación y criterios operativos correspondientes. El fixture de `TBL-003` está ahora **En preparación**; el siguiente paso manual es marcarlo listo y, luego, ejecutar **Entregar** si el permiso y el estado devuelto por el servidor lo permiten.

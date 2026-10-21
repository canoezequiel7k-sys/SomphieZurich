# Probar un pedido Web → backend → PostgreSQL → Android

La Web y Android usan el mismo backend y PostgreSQL. La Web crea el pedido; el personal lo acepta por API porque Android todavía no tiene bandeja de pedidos entrantes; después Android prepara el producto.

## Requisitos

- Windows, repositorio abierto en PowerShell desde su raíz y Docker Desktop iniciado.
- Backend: JDK 25 (el wrapper usa Gradle 9.8.1).
- Web: Node.js 24, Corepack y pnpm 12.
- Android: Android Studio con SDK 37, ADB y teléfono con depuración USB autorizada (o emulador).

## Antes de empezar: entrar a la carpeta del proyecto

En el error que viste, PowerShell estaba en `C:\Users\SystemAncestral`. Por eso no encontraba `.\backend\gradlew.bat`: esa ruta existe dentro del proyecto, no directamente en tu carpeta de usuario.

Pegá esto en PowerShell. Si abrís otra terminal más adelante, repetilo allí también:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) {
    throw "No encuentro el proyecto en $repo. Cambiá `$repo por la carpeta donde está SomphieZúrich."
}
Set-Location $repo
Get-Location
```

La última línea debe mostrar la carpeta `...\Documents\SomphieZúrich`. **No ejecutes los siguientes comandos hasta que la muestre.** Si guardaste el proyecto en otro lugar, cambiá la ruta de `$repo`.

## 1. Iniciar PostgreSQL

En esa misma terminal, ya ubicada en la raíz:

```powershell
docker info
docker compose -f infra\docker-compose.yml config --quiet
docker compose -f infra\docker-compose.yml up -d postgres
docker compose -f infra\docker-compose.yml ps
docker compose -f infra\docker-compose.yml exec -T postgres pg_isready
```

El último comando debe indicar que acepta conexiones. Solo PostgreSQL es necesario para esta prueba; Mailpit y Prism no se usan.

## 2. Iniciar el backend

Abrí otra terminal PowerShell y pegá el bloque completo. Primero entra al proyecto, genera una clave JWT temporal y después levanta Spring:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DEMO_STAFF_PASSWORD = "demo-only-password"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
.\backend\gradlew.bat -p backend bootRun --no-daemon --max-workers=1
```

Dejá esa terminal abierta. Spring/Flyway prepara el esquema al arrancar. En otra terminal, comprobá salud:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/actuator/health
```

Debe responder `UP`. Las cuentas demo usan la contraseña recién configurada; este paso fija `demo-only-password` para esta ejecución local.

Compose permite personalizar `DB_NAME`, `DB_USER` y `DB_PASSWORD` con `infra\.env`; Spring no carga ese archivo automáticamente. Si esos valores no son los predeterminados, exportá también `DB_URL`, `DB_USER` y `DB_PASSWORD` correspondientes en la terminal del backend antes de iniciar Gradle.

## 3. Comprobar si hay una mesa libre

Abrí una terminal PowerShell nueva, repetí el bloque **Antes de empezar** y consultá las mesas mientras el backend sigue activo:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
$tableCodes = @("TBL-001", "TBL-002", "TBL-003")
$tables = foreach ($code in $tableCodes) {
    $table = Invoke-RestMethod "http://localhost:8080/api/v1/public/tables/$code"
    [pscustomobject]@{
        Codigo = $code
        Mesa = $table.tableLabel
        PuedeAbrir = [bool]$table.canOpen
        YaTieneSesion = [bool]$table.hasActiveSession
    }
}
$tables | Format-Table -AutoSize
```

**`PuedeAbrir = False` no es un error.** Significa que la mesa está deshabilitada o ya tiene una sesión abierta. Si una fila dice `True`, usá esa mesa y seguí al paso 4.

Si todas dicen `False` y son estas sesiones locales de prueba, podés cerrarlas por la API con el bloque siguiente. **No lo uses con mesas de clientes reales:** el cierre forzado cancela pedidos pendientes y productos todavía no entregados. Conserva el historial y los pagos, pero un saldo positivo queda como deuda no cobrada; no registra un pago.

Pegá el bloque completo en PowerShell. Primero muestra todas las sesiones demo abiertas. Para cerrarlas, escribí **`CERRAR MESAS DEMO`** cuando aparezca la pregunta y presioná Enter. Si solo presionás Enter, no cierra nada y el bloque se detiene; no muestra una verificación engañosa.

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta." }
Set-Location $repo
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080/api/v1"
$body = @{ email = "owner@somphie.demo"; password = "demo-only-password" } | ConvertTo-Json -Compress
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body $body
if ($login.user.mustChangePassword) { throw "La cuenta exige cambiar la contraseña; completá ese flujo antes de continuar." }
if ($login.user.permissions -notcontains "session.force_close") { throw "La cuenta no tiene permiso para cierre forzado." }
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$board = Invoke-RestMethod -Uri "$base/staff/tables" -Headers $headers
$demoTables = @(
    @{ label = "Mesa 1"; code = "TBL-001" },
    @{ label = "Mesa 2"; code = "TBL-002" },
    @{ label = "Mesa 3"; code = "TBL-003" }
)
$demoLabels = @($demoTables | ForEach-Object { $_.label })
$openTables = @($board.items | Where-Object { $_.label -in $demoLabels -and $_.sessionId })
$sessionsToClose = @()

if ($openTables.Count -eq 0) {
    Write-Host "No hay sesiones abiertas en las tres mesas demo."
} else {
    foreach ($table in $openTables) {
        $sessionId = $table.sessionId
        $detail = Invoke-RestMethod -Uri "$base/staff/sessions/$sessionId" -Headers $headers
        if ($detail.session.status -ne "OPEN") { continue }
        $sessionsToClose += [pscustomobject]@{ label = $table.label; sessionId = $sessionId; detail = $detail }

        Write-Host "`n$($table.label) | Sesión: $sessionId | Saldo (centavos): $($detail.bill.balanceMinor)"
        if (@($detail.orders).Count -eq 0) {
            Write-Host "  Sin pedidos."
        } else {
            foreach ($order in $detail.orders) {
                Write-Host "  Pedido $($order.displayCode): $($order.status)"
                foreach ($item in $order.items) {
                    Write-Host "    $($item.quantity)x $($item.productName): $($item.status)"
                }
            }
        }
    }

    if ($sessionsToClose.Count -gt 0) {
        Write-Host "`nEl cierre forzado cancela pedidos pendientes e ítems activos. Los saldos positivos no se pagan."
        $confirmation = Read-Host "Para cerrar las sesiones demo mostradas, escribí exactamente CERRAR MESAS DEMO"
        if ($confirmation -cne "CERRAR MESAS DEMO") {
            throw "Cancelado: no se cerró ninguna sesión. Volvé a ejecutar el bloque y escribí CERRAR MESAS DEMO para confirmar."
        }

        foreach ($session in $sessionsToClose) {
            $reason = "Liberar mesa demo para prueba manual: $($session.label)"
            $key = [guid]::NewGuid().ToString()
            $closeHeaders = @{ Authorization = $headers.Authorization; "Idempotency-Key" = $key }
            $closeBody = @{ reason = $reason } | ConvertTo-Json -Compress
            $closed = Invoke-RestMethod -Method Post -Uri "$base/staff/sessions/$($session.sessionId)/force-close" -Headers $closeHeaders -ContentType "application/json" -Body $closeBody
            Write-Host "$($session.label) | Cierre: $($closed.status)"
        }
    }
}

foreach ($table in $demoTables) {
    $result = Invoke-RestMethod -Uri "$base/public/tables/$($table.code)"
    Write-Host "$($table.label) | Puede abrir: $($result.canOpen)"
}
```

Si una mesa sigue en `False` después de confirmar el cierre, no intentes cerrarla de nuevo a ciegas: puede estar deshabilitada. El historial queda guardado; no uses SQL ni borres datos para liberar mesas.

## 4. Iniciar la Web y enviar el pedido

Solo si encontraste una mesa libre, abrí una terminal PowerShell nueva y pegá el bloque completo:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
Set-Location .\web
corepack pnpm install --frozen-lockfile
$env:VITE_API_BASE_URL = "http://localhost:8080/api/v1"
corepack pnpm dev --host localhost --port 5173 --strictPort
```

La URL se configura solo en esa terminal; no hace falta modificar `.env.local`. El backend permite CORS para `http://localhost:5173`; `--strictPort` evita que Vite cambie silenciosamente a otro puerto. Abrí una ventana privada nueva y entrá a `http://localhost:5173/t/CODIGO-LIBRE`, reemplazando el código por la mesa que dio `PuedeAbrir = True`. Así la Web no reutiliza un token viejo guardado en el navegador.

Ingresá tu nombre, elegí un producto disponible y completá las opciones obligatorias que muestre la Web. Enviá el pedido. En seguimiento, anotá la mesa y el código visible (por ejemplo, `#001`). El mensaje **“El pedido se recibió”** confirma el envío, no la aceptación: el pedido queda `PENDING_REVIEW` y todavía no aparece en la cola Android.

## 5. Compilar y conectar Android

Con un teléfono por USB, abrí una terminal PowerShell nueva y pegá el bloque completo:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
adb devices -l
adb reverse tcp:8080 tcp:8080
.\android\gradlew.bat -p android assembleDebug -PsomphieApiBaseUrl=http://127.0.0.1:8080/api/v1/ --no-daemon --max-workers=1
adb install -r .\android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.somphie.staff/.MainActivity
```

Iniciá sesión con `owner@somphie.demo` y `demo-only-password`; seleccioná la estación del producto.

## 6. Encontrar y aceptar el pedido

En una terminal PowerShell nueva, pegá este bloque completo:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" -ContentType "application/json" -Body (@{ email = "owner@somphie.demo"; password = "demo-only-password" } | ConvertTo-Json -Compress)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$orders = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/staff/orders?status=PENDING_REVIEW&limit=50" -Headers $headers
$orders.items | Select-Object id, displayCode, tableLabel, createdAt
```

Buscá la fila que coincida con la mesa y el código anotados en la Web. Copiá su `id` (no `displayCode`) y aceptá el pedido con este bloque. La respuesta debe mostrar `status` como `ACCEPTED`; si da error, el pedido no se aceptó.

```powershell
$orderId = "PEGAR-ID-UUID-DE-LA-FILA-CORRECTA"
$key = [guid]::NewGuid().ToString()
$acceptHeaders = @{ Authorization = "Bearer $($login.accessToken)"; "Idempotency-Key" = $key }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/staff/orders/$orderId/accept" -Headers $acceptHeaders -ContentType "application/json" -Body "{}"
```

Este endpoint requiere el permiso `order.accept` y una clave idempotente. En Android, tocá **Actualizar** y elegí la estación del producto: **Flat white corresponde a Barra** y **Medialuna de manteca corresponde a Cocina**. Si el pedido tiene ambos productos, cada uno aparece en su propia estación. Si el pedido sigue `PENDING_REVIEW`, todavía no fue aceptado.

Si la respuesta de aceptación fue incierta, consultá el pedido por su ID antes de reintentar:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/staff/orders/$orderId" -Headers $headers
```

Si sigue `PENDING_REVIEW`, repetí exactamente la solicitud anterior con la misma `$key`; nunca generes una nueva para reintentar la misma operación.

## 7. Comprobar persistencia y estados

En una terminal PowerShell nueva, pegá el UUID copiado de la lista API en `$orderId` y ejecutá este bloque completo de solo lectura:

```powershell
$repo = Join-Path $HOME "Documents\SomphieZúrich"
if (-not (Test-Path (Join-Path $repo "backend\gradlew.bat"))) { throw "No encuentro el proyecto en $repo; corregí la ruta del proyecto." }
Set-Location $repo
$orderId = "PEGAR-EL-UUID-DEL-PEDIDO"
$dbUser = (docker compose -f infra\docker-compose.yml exec -T postgres printenv POSTGRES_USER).Trim()
$dbName = (docker compose -f infra\docker-compose.yml exec -T postgres printenv POSTGRES_DB).Trim()
if ($orderId -notmatch '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$') { throw "Pegá un UUID válido en `$orderId" }
$query = "SELECT o.id, o.display_code, o.status AS order_status, dt.table_code, oi.product_name, oi.status AS item_status FROM customer_order o JOIN table_session ts ON ts.id = o.session_id JOIN dining_table dt ON dt.id = ts.table_id LEFT JOIN order_item oi ON oi.order_id = o.id WHERE o.id = '$orderId'::uuid;"
docker compose -f infra\docker-compose.yml exec -T postgres psql -U $dbUser -d $dbName -c $query
```

En Android usá **Empezar** y después **Marcar listo** si el servidor muestra esas acciones permitidas. En la pestaña **Pedidos**, la Web vuelve a consultar cada 8 segundos; también podés tocar su botón de actualización. Repetí la consulta SQL y verificá que `item_status` cambie junto con el estado que muestra cada aplicación.

## Si algo falla

- **Docker/PostgreSQL:** `docker info`, `docker compose -f infra\docker-compose.yml ps` y `docker compose -f infra\docker-compose.yml logs --tail=100 postgres`. No sigas hasta que `pg_isready` funcione.
- **Backend:** ejecutá `Invoke-RestMethod http://localhost:8080/api/v1/actuator/health` y `Test-NetConnection localhost -Port 8080`; revisá los errores en la terminal de Gradle.
- **No aparece ninguna mesa disponible:** si son sesiones demo locales y aceptás las cancelaciones/saldos que muestra el paso 3, usá allí el cierre forzado confirmado por mesa. No lo uses con mesas de clientes reales.
- **Web/CORS:** confirmá que Vite usa puerto 5173, el backend sigue activo y que la terminal de Vite tiene `VITE_API_BASE_URL=http://localhost:8080/api/v1`. Revisá la consola del navegador.
- **Android:** `adb devices -l`, `adb reverse --list`, comprobá salud del backend en la PC y consultá `adb logcat -v time -s AndroidRuntime:E '*:S'`.
- **Error al aceptar:** verificá el UUID y la sesión Owner; consultá `GET /staff/orders/{orderId}` para saber el estado actual. El backend valida permiso y transición.

## Apagar y volver a levantar sin perder datos

1. Detené Android y el túnel:

   ```powershell
   adb shell am force-stop com.somphie.staff
   $mappings = adb reverse --list
   if ($mappings -match 'tcp:8080\s+tcp:8080$') { adb reverse --remove tcp:8080 }
   ```

2. En las terminales de Vite y Gradle, presioná **Ctrl+C**.
3. Desde la raíz, detené **todos los servicios de este Compose** (PostgreSQL, Mailpit y Prism):

   ```powershell
   Set-Location (Join-Path $HOME "Documents\SomphieZúrich")
   docker compose -f infra\docker-compose.yml stop
   ```

   `stop` conserva contenedores y volumen. No uses `down -v` ni borres el volumen. No detiene contenedores de otros proyectos.

Para volver a levantar y comprobar que el pedido sigue persistido:

1. Volvé a ejecutar el paso 1. PostgreSQL conserva la base en el volumen.
2. Repetí la consulta del paso 7 con el mismo `$orderId`. El pedido y los estados deben seguir allí.
3. Si querés seguir usando la app, repetí los pasos 2, 4 y 5. Al regenerar `JWT_SECRET`, Android requiere iniciar sesión otra vez. El pedido anterior no necesita volver a crearse.

## Comprobaciones de esta revisión

- **Ejecutadas:** `docker compose ... config --quiet` validó la configuración; se inició el contenedor PostgreSQL existente, `pg_isready` respondió y una consulta de solo lectura confirmó `TBL-001`, `TBL-002` y `TBL-003` abiertas. PostgreSQL se detuvo al terminar la comprobación.
- **Confirmadas en código/contrato, no ejecutadas ahora:** inicio backend, creación Web, login Owner, aceptación API y transiciones Android; sus rutas, variables y formatos corresponden a `application.yml`, OpenAPI y controladores.
- **Pendiente:** el recorrido completo con un pedido nuevo. No se creó ni modificó ningún dato; las tres mesas están ocupadas.

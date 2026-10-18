# Probar Web y Android juntos

Sí, ambas aplicaciones usan el mismo backend y PostgreSQL. Un pedido enviado desde la Web persiste y aparece en Android después de que el personal lo acepta. Android todavía no tiene la bandeja de pedidos entrantes, por eso la aceptación se hace una vez desde la API.

## 1. Iniciar la base

Desde la raíz del repositorio, abrir Docker Desktop y ejecutar:

```powershell
docker compose -f infra\docker-compose.yml up -d postgres
```

## 2. Iniciar el backend

En otra terminal, desde la raíz:

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

Esperar a que inicie en el puerto 8080. No cerrar esta terminal mientras se prueba.

## 3. Iniciar la Web

En una tercera terminal, desde la raíz:

```powershell
if (-not (Test-Path .\web\.env.local)) { Copy-Item .\web\.env.example .\web\.env.local }
Set-Location .\web
corepack pnpm install
corepack pnpm dev
```

Abrir la URL que muestra Vite (normalmente `http://localhost:5173`) y entrar a `/t/TBL-003`. Si esa mesa está ocupada y no tenés su código de unión, probá otra mesa libre cambiando el código de la URL. Iniciar sesión con un nombre, agregar un producto y enviarlo. La página de seguimiento muestra su estado también después de actualizar el navegador; anotá el código visible del pedido (por ejemplo, `#001`).

**Estado de esta base local (18-10-2026):** las tres mesas cargadas (`TBL-001`, `TBL-002`, `TBL-003`) tienen una sesión abierta. Para probar un pedido nuevo hace falta liberar una mesa por el flujo normal; no cierres sesiones ajenas ni borres datos para seguir esta guía.

## 4. Preparar Android

Con el teléfono conectado y autorizado por USB, en otra terminal desde la raíz:

```powershell
adb reverse tcp:8080 tcp:8080
.\android\gradlew.bat -p android assembleDebug -PsomphieApiBaseUrl=http://127.0.0.1:8080/api/v1/ --no-daemon --max-workers=1
adb install -r .\android\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.somphie.staff/.MainActivity
```

Iniciar sesión con `owner@somphie.demo` y `demo-only-password`; elegir **Barra** o **Cocina** según el producto.

## 5. Pasar el pedido a la cola Android

Los pedidos nuevos quedan `PENDING_REVIEW`; la cola Android muestra pedidos aceptados. La Web muestra el código visible, no el ID interno. En una terminal PowerShell, iniciar sesión como Owner y listar los pedidos pendientes:

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" -ContentType "application/json" -Body (@{ email = "owner@somphie.demo"; password = "demo-only-password" } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$orders = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/staff/orders?status=PENDING_REVIEW&limit=50" -Headers $headers
$orders.items | Select-Object id, displayCode, tableLabel, createdAt
```

Buscá en esa lista el código de pedido y la mesa que anotaste en la Web. Copiá su `id` y aceptalo con una clave idempotente nueva:

```powershell
$orderId = "PEGAR-EL-ID-DE-LA-FILA-CORRECTA"
$key = [guid]::NewGuid().ToString()
$acceptHeaders = @{ Authorization = "Bearer $($login.accessToken)"; "Idempotency-Key" = $key }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/staff/orders/$orderId/accept" -Headers $acceptHeaders -ContentType "application/json" -Body "{}"
```

En Android, tocar **Actualizar**: el pedido debe aparecer en la estación correspondiente al producto. Usar **Empezar** y **Marcar listo**; el seguimiento Web reflejará los estados. No repitas la aceptación si no sabés si la primera solicitud tuvo éxito.

## 6. Apagar todo sin borrar los datos

1. En Android, cerrar la app:

   ```powershell
   adb shell am force-stop com.somphie.staff
   adb reverse --remove tcp:8080
   ```

2. En las terminales de Vite y del backend, presionar **Ctrl+C**.
3. Desde la raíz, detener PostgreSQL:

   ```powershell
   docker compose -f infra\docker-compose.yml stop postgres
   ```

El volumen de PostgreSQL se conserva. La próxima vez, volver a ejecutar el paso 1; no uses `docker compose down -v`, porque elimina los datos.

## Límites

- El backend y PostgreSQL son compartidos; Web y Android no se envían datos directamente entre sí.
- La contraseña demo es solo para desarrollo. La clave JWT se genera temporalmente en la terminal.
- Las pruebas existentes de la Web con MSW son simuladas; el recorrido de estos pasos usa el backend y la base locales.
- Si todas las mesas están ocupadas, no uses una sesión ajena: hace falta una mesa libre para abrir una sesión nueva.

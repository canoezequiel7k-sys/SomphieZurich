# Guía de pruebas y entorno de desarrollo — Android, Web y Backend (VS Code)

Esta guía explica paso a paso cómo probar y depurar la aplicación Android (`:app`) trabajando **100% desde Visual Studio Code** y terminal de PowerShell, tanto de forma **aislada (solo la interfaz)** como **integrada (con backend y web)**.

---

## 1. Cómo ver y probar la interfaz en VS Code (Sin backend)

En **Visual Studio Code** no existe la ventana gráfica de `@Preview` interactivo nativa de Android Studio. 

Por lo tanto, la forma oficial, rápida y real de ver lo que estás diseñando es **desplegar directo a tu dispositivo** (teléfono físico por USB o emulador) mediante la terminal integrada de VS Code.

### Paso a paso:
1. Conectá tu celular por USB (con **Depuración USB** activada en Opciones de desarrollador) o abrí un emulador.
2. Verificá que la PC reconozca el dispositivo:
   ```powershell
   adb devices
   ```
   *(Debería aparecer el ID de tu dispositivo listado como `device`).*

3. Compilá e instalá la app en modo Debug:
   ```powershell
   cd android
   .\gradlew.bat installDebug
   ```

4. Lanzá la app en la pantalla del teléfono:
   ```powershell
   adb shell am start -n com.somphie.staff/.MainActivity
   ```

> **Atajo rápido para probar cambios de UI:**
> Cada vez que modifiques código en `HomeScreen.kt` o cualquier pantalla y quieras ver el resultado, corrés en la terminal de VS Code:
> ```powershell
> .\gradlew.bat installDebug; adb shell am start -n com.somphie.staff/.MainActivity
> ```
> Se compila, se actualiza en el teléfono y se abre automáticamente.

---

## 2. Probar la app con el Backend conectado (Integración local)

Cuando quieras probar el login real, llamadas a la API, pedidos y estaciones:

### Paso 1: Levantar la base de datos (PostgreSQL)
El backend requiere PostgreSQL. Desde la terminal en la raíz:
```powershell
cd infra
docker compose up -d
```

### Paso 2: Levantar el Backend (Spring Boot)
En una pestaña de terminal en VS Code:
```powershell
cd backend
.\gradlew.bat bootRun
```
El backend quedará escuchando en `http://localhost:8080/api/v1/`.

### Paso 3: Conectar el teléfono al backend de tu PC

#### Si usás Teléfono Físico por cable USB:
Tu teléfono no sabe qué es el `localhost` de tu computadora. Para que la app hable con el backend sin configurar redes Wi-Fi ni IPs manuales, ejecutás este comando en la terminal:
```powershell
adb reverse tcp:8080 tcp:8080
```
*Esto crea un túnel: cualquier petición que la app haga a `http://localhost:8080/` viaja directo por el cable USB a tu PC.*

Para que la app apunte a `localhost` en el teléfono físico, compilás así:
```powershell
cd android
.\gradlew.bat installDebug -PsomphieApiBaseUrl="http://localhost:8080/api/v1/"
adb shell am start -n com.somphie.staff/.MainActivity
```

#### Si usás Emulador de Android (AVD):
En los emuladores, la IP `10.0.2.2` apunta directamente al host (tu PC). Ya está configurado así por defecto en `build.gradle.kts`, por lo que solo compilás con:
```powershell
.\gradlew.bat installDebug
```

---

## 3. Probar el flujo completo: Cliente (Web) ➔ Backend ➔ Android

Para probar la experiencia punta a punta de la cafetería:

```text
[ Web Cliente (Vite/React) ] 
       │ 1. Cliente envía pedido desde la mesa
       ▼
[ Backend (Spring Boot :8080) ]
       │ 2. Procesa y persiste en PostgreSQL
       ▼
[ App Android Personal ]
       │ 3. Mozo/Cocina ve el pedido y lo atiende
```

### 1. Iniciar la Web de clientes:
En otra terminal de VS Code:
```powershell
cd web
pnpm run dev
```
Abre en `http://localhost:5173`.

### 2. Simular el circuito:
1. En tu navegador abrí una mesa de demostración:
   `http://localhost:5173/t/T-01`
2. Abrí la sesión, armá un pedido y presiona **Enviar pedido**.
3. En la app Android:
   - Ingresá con un usuario del personal (rol `WAITER` o `KITCHEN`).
   - El pedido aparecerá reflejado. Al aceptarlo o marcarlo como "Listo", verás cómo la Web del cliente actualiza su estado.

---

## 4. Tabla de comandos rápidos (Terminal VS Code)

| Acción | Comando PowerShell |
|---|---|
| Comprobar dispositivo USB | `adb devices` |
| Ver logs y errores en tiempo real | `adb logcat -s StaffApp:V AndroidRuntime:E` |
| Conectar teléfono USB al backend | `adb reverse tcp:8080 tcp:8080` |
| Compilar, instalar y abrir app | `cd android; .\gradlew.bat installDebug; adb shell am start -n com.somphie.staff/.MainActivity` |
| Correr pruebas unitarias | `cd android; .\gradlew.bat test` |
| Desinstalar app limpia | `adb uninstall com.somphie.staff` |

---

## 5. Reglas de interfaz a tener en cuenta
- **Textos:** Van en `android/app/src/main/res/values/strings.xml` (no hardcodear strings en composables).
- **Colores:** Usar el tema de Somphie (`StaffTheme`, crema `#F6F1E8`, café `#3A2B22`, caramelo `#945C34`, salvia `#61715B`).
- **Feedback:** La UI siempre contempla estados de *Cargando*, *Error*, *Vacío* y *Éxito*.

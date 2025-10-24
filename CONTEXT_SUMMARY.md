# Resumen de Contexto - Proyecto KYC Android

## 📋 Descripción del Proyecto
Aplicación Android para procesos de KYC (Know Your Customer) que incluye:
- Captura y verificación de documentos de identidad
- Prueba de vida (liveness detection)
- Comparación facial 1:1 (OTO - One-to-One)
- Consulta de listas oficiales (blacklist)
- Dashboard para visualizar sesiones KYC y sus detalles

## 🏗️ Arquitectura
- **Patrón**: MVVM (Model-View-ViewModel)
- **Inyección de Dependencias**: Dagger Hilt
- **Networking**: Retrofit + OkHttp + Gson
- **UI**: Material Design 3 + ViewBinding
- **Coroutines**: Para operaciones asíncronas
- **Image Loading**: Picasso (para cargar logos desde URL)

## 📁 Estructura de Archivos Clave

### 1. Modelos de Datos (`app/src/main/java/com/jaak/kyc/data/model/`)

#### API Models (`api/`)
- **`AuthModels.kt`**: Modelos de autenticación
  - `LoginRequest(email, password, captcha)`
  - `LoginResponse(accessToken, user, company)`
  - `UserInfo(id, email, fullName, rol, passwordChangeRequired)`
  - `CompanyInfo(id, name, logo)` - logo es URL de imagen

- **`SessionModels.kt`**: Modelos de sesiones KYC
  - `SessionListResponse` - Lista de sesiones
  - `SessionDetailResponse` - Detalle completo de una sesión
  - `SessionItem` - Item individual de sesión

#### Other Models
- **`blacklist/`**: Modelos para listas oficiales (OFAC, Interpol, INE, CURP, etc.)
- **`otoverify/`**: Modelos para comparación facial 1:1
- **`livenessverify/`**: Modelos para prueba de vida
- **`ocr/v4/`**: Modelos para extracción de documentos v4

### 2. Networking (`app/src/main/java/com/jaak/kyc/data/network/`)

#### `JaakDBApiClient.kt`
Interface Retrofit con todos los endpoints:
```kotlin
// Auth
@POST("api/auth/sign-in")
suspend fun loginApi(@Body request: LoginRequest): Response<LoginResponse>

// Sessions
@POST("api/v1/kyc/session")
suspend fun sessionApi(@Header("Short-Key") shortKey: String, ...): Response<SessionResponse>

@GET("api/v1/kyc/session")
suspend fun getSessionListApi(@Header("Authorization") auth: String, ...): Response<SessionListResponse>

@GET("api/v1/kyc/session/{id}")
suspend fun getSessionDetailApi(@Path("id") sessionId: String, ...): Response<SessionDetailResponse>

// KYC Operations
@POST("api/v4/document/extract")
suspend fun documentExtractV4Api(...)

@POST("api/v1/liveness/verify-and-bestframe")
suspend fun livenessVerifyApi(...)

@POST("api/v2/oto/verify")
suspend fun otoVerifyApi(...)

@POST("api/v2/blacklist/investigate")
suspend fun blacklistInvestigateApi(...)

@POST("api/v1/kyc/session/finish")
suspend fun finishApi(...)
```

#### `JaakDBService.kt`
Wrapper de API con manejo de errores centralizado usando `callService()`

### 3. UI - Activities (`app/src/main/java/com/jaak/kyc/ui/view/`)

#### `LoginActivity.kt` ⭐ NUEVO
- Formulario de login con email y password
- Integración con reCAPTCHA v2 (parcialmente configurado)
- Guarda token y datos de usuario usando ProfileManager
- Navega a MainActivity después del login exitoso
- Layout: `activity_login.xml`

#### `MainActivity.kt`
- Dashboard principal con bottom navigation
- Tabs: Dashboard, Sesiones, Ajustes

#### `SessionDetailActivity.kt`
- Muestra detalle de una sesión KYC con tabs
- **Tabs fijos**: "Resumen", "Flujo", "Detalles"
- Fragments: SessionInfoFragment, SessionFlowFragment

#### `BlacklistDetailActivity.kt`
- Muestra resultados de listas oficiales
- **Layout móvil**: UNA COLUMNA vertical
- Secciones: Listas de Riesgo (OFAC, Interpol, SAT69B) y Listas de Validación (INE, CURP)

#### `OtoDetailActivity.kt`
- Muestra resultados de comparación facial 1:1
- **Layout móvil**: UNA COLUMNA vertical
- Secciones: Imágenes, Accesorios faciales, Comparación 1:1, Calidad de imagen

#### `LivenessDetailActivity.kt`
- Muestra resultados de prueba de vida
- **Layout móvil**: UNA COLUMNA vertical
- Secciones: Video de sesión, Mejor fotograma, Tabla de liveness, Tiempo de procesamiento

### 4. Fragments

#### `SessionInfoFragment.kt`
- Tab "Resumen" del detalle de sesión
- Muestra: Usuario, Estado, Flow, Scores, Eventos recientes

#### `SessionFlowFragment.kt`
- Tab "Flujo" del detalle de sesión
- Lista todos los eventos del flujo KYC
- Click en evento navega a detalle (Blacklist/OTO/Liveness)

#### `DashboardFragment.kt`
- Vista principal con estadísticas
- Gráficos y resúmenes

#### `SessionsFragment.kt`
- Lista de sesiones KYC con filtros
- RecyclerView con KycSessionsAdapter

#### `SettingsFragment.kt`
- Configuración de la app
- Selección de perfil (QA, Sandbox, Dev)
- Logout

### 5. Utils (`app/src/main/java/com/jaak/kyc/utils/`)

#### `ProfileManager.kt` ⭐ ACTUALIZADO
Maneja tanto perfiles de ambiente como autenticación:

**Perfiles de Ambiente:**
```kotlin
fun getCurrentProfile(): String // "QA", "Sandbox", "Dev"
fun setCurrentProfile(profile: String)
fun getCurrentBaseUrl(): String // Retorna URL según perfil
```

**Autenticación:**
```kotlin
fun saveAccessToken(token: String)
fun getAccessToken(): String?
fun getBearerToken(): String? // Retorna "Bearer {token}"

fun setLoggedIn(isLoggedIn: Boolean)
fun isLoggedIn(): Boolean

fun saveUserInfo(userInfo: UserInfo)
fun getUserInfo(): UserInfo?

fun saveCompanyInfo(companyInfo: CompanyInfo)
fun getCompanyInfo(): CompanyInfo?

fun logout() // Limpia datos de auth
fun clearAll() // Limpia todo
```

### 6. Layouts (`app/src/main/res/layout/`)

#### `activity_login.xml` ⭐ NUEVO
- Logo JAAK
- Indicador SSL seguro
- Campo email (con icono de persona)
- Campo password (con icono de candado y toggle)
- Link "Olvidé mi contraseña"
- Container para reCAPTCHA (WebView)
- Botón "Iniciar Sesión"
- Link "Contactar soporte"

**IMPORTANTE SOBRE DISEÑO MÓVIL:**
Todas las pantallas de detalle usan **UNA SOLA COLUMNA** vertical (no dos columnas):
- `activity_blacklist_detail.xml`
- `activity_oto_detail.xml`
- `activity_liveness_detail.xml`
- `activity_session_detail.xml`

### 7. Dependencies (`app/build.gradle`)

```gradle
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Hilt
implementation "com.google.dagger:hilt-android:$hilt_version"
kapt "com.google.dagger:hilt-android-compiler:$hilt_version"

// Image Loading ⭐ NUEVO
implementation 'com.squareup.picasso:picasso:2.8'

// reCAPTCHA v2 ⭐ NUEVO
implementation 'com.google.android.gms:play-services-safetynet:18.0.1'

// JAAK SDKs
implementation 'com.jaak.stampssdk:jaakstamps-sdk:1.1.0-beta.4'
implementation 'com.jaak.visagesdk:jaakvisage-sdk:1.1.0-beta.12'

// Material Design
implementation 'com.google.android.material:material:1.12.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0'
```

## 🎨 Sistema de Colores

```xml
<!-- colors.xml -->
<color name="jaak_success">#00C853</color>      <!-- Verde - éxito/válido -->
<color name="jaak_error">#FF1744</color>        <!-- Rojo - error/riesgo -->
<color name="jaak_warning">#FFC107</color>      <!-- Amarillo - advertencia -->
<color name="jaak_primary">#1976D2</color>      <!-- Azul principal -->
<color name="jaak_background">#F5F5F5</color>
<color name="jaak_surface">#FFFFFF</color>
<color name="jaak_text_primary">#212121</color>
<color name="jaak_text_secondary">#757575</color>
```

## 🔑 reCAPTCHA v2 - Configuración Pendiente

El sistema está **parcialmente configurado**. Para completarlo:

1. Obtener Site Key de Google reCAPTCHA v2
2. En `LoginActivity.kt` línea ~84, reemplazar:
   ```kotlin
   <div class="g-recaptcha" data-sitekey="YOUR_SITE_KEY_HERE" ...>
   ```
3. Descomentar línea ~109:
   ```kotlin
   binding.webViewRecaptcha.loadDataWithBaseURL(null, recaptchaHtml, "text/html", "UTF-8", null)
   ```
4. Cambiar línea ~111:
   ```kotlin
   binding.flRecaptcha.visibility = View.VISIBLE
   ```

Por ahora usa token temporal: `"temp_token_for_testing"`

## 🚀 Flujo de Login

1. Usuario ingresa email y password
2. Se valida formato de email
3. Se obtiene token de reCAPTCHA (temporal por ahora)
4. Se llama a `POST /api/auth/sign-in`
5. Si exitoso:
   - Se guarda `accessToken` en ProfileManager
   - Se guarda `UserInfo` y `CompanyInfo`
   - Se marca como logueado
   - Se navega a MainActivity (dashboard)
6. Si falla: se muestra error

## 🔐 Uso del Token en Otros Servicios

Para usar el token guardado en llamadas API:

```kotlin
@Inject
lateinit var profileManager: ProfileManager

// En alguna función:
val token = profileManager.getBearerToken() // "Bearer abc123..."
val response = jaakDBService.getSessionListApi(auth = token, ...)
```

## 📱 Navegación de la App

```
MenuMainActivity (Splash/Entry)
    ↓
LoginActivity (si no está logueado)
    ↓ (login exitoso)
MainActivity (Dashboard principal)
    ├── DashboardFragment (Tab 1)
    ├── SessionsFragment (Tab 2)
    │   ↓ (click en sesión)
    │   SessionDetailActivity
    │       ├── SessionInfoFragment (Tab: Resumen)
    │       ├── SessionFlowFragment (Tab: Flujo)
    │       │   ↓ (click en evento)
    │       │   ├── BlacklistDetailActivity
    │       │   ├── OtoDetailActivity
    │       │   └── LivenessDetailActivity
    │       └── (Tab: Detalles)
    └── SettingsFragment (Tab 3)
        └── Logout → LoginActivity
```

## 🛠️ Tareas Pendientes

### Configuración reCAPTCHA
- [ ] Obtener Site Key de Google reCAPTCHA v2
- [ ] Reemplazar "YOUR_SITE_KEY_HERE" en LoginActivity
- [ ] Activar WebView y hacer visible flRecaptcha
- [ ] Probar flujo completo de verificación

### Dashboard
- [ ] Cargar logo de compañía usando Picasso:
  ```kotlin
  val companyInfo = profileManager.getCompanyInfo()
  Picasso.get()
      .load(companyInfo?.logo)
      .placeholder(R.drawable.ic_business)
      .into(imageView)
  ```

### Validaciones
- [ ] Agregar validación de token expirado
- [ ] Implementar refresh token si es necesario
- [ ] Manejar 401 Unauthorized globalmente

### Features Adicionales
- [ ] Implementar "Olvidé mi contraseña"
- [ ] Implementar "Contactar soporte"
- [ ] Guardar "Recordarme" en login

## 📝 Notas Importantes

### ⚠️ DISEÑO MÓVIL
**TODAS las pantallas de detalle (Blacklist, OTO, Liveness) deben usar UNA SOLA COLUMNA vertical**, no dos columnas lado a lado. Esto es crítico para dispositivos móviles.

### 🔒 Seguridad
- Los tokens se guardan en SharedPreferences
- Para producción, considerar usar EncryptedSharedPreferences
- El token se envía como `Bearer {token}` en el header Authorization

### 🌐 Base URLs por Perfil
- **QA**: https://api.qa.jaak.ai/
- **Sandbox**: https://api.sandbox.jaak.ai/
- **Dev**: https://api.dev.jaak.ai/

### 📦 SDKs JAAK
- **Visage SDK**: Prueba de vida (liveness)
- **Stamps SDK**: Captura de documentos

## 🐛 Debugging

### Ver logs:
```bash
adb logcat | grep "LoginActivity\|ProfileManager\|JaakDBService"
```

### Ver SharedPreferences:
```bash
adb shell
run-as com.jaak.kyc
cat /data/data/com.jaak.kyc/shared_prefs/jaak_kyc_prefs.xml
```

## 📞 Contacto / Soporte

Si el contexto se agota nuevamente, este archivo contiene toda la información necesaria para continuar el desarrollo. Prioridades:
1. Configurar reCAPTCHA con la Site Key real
2. Implementar carga de logo con Picasso en el dashboard
3. Probar flujo completo de login → dashboard → sesiones → detalles

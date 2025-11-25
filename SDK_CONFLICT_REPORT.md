# 🚨 Reporte de Conflicto: Stamps SDK 1.2.0 y Visage SDK 1.2.0-beta.3

**Fecha:** 20 de Noviembre de 2025  
**Proyecto:** jaak-android-kyc-example  
**Reportado por:** Michael Antonio Avila Escobar

---

## 📋 Resumen del Problema

Al actualizar los SDKs a las siguientes versiones:
- **Stamps SDK**: 1.1.0 → **1.2.0**
- **Visage SDK**: 1.1.0 → **1.2.0-beta.3**

Se detectó un **conflicto de inyección de dependencias con Dagger/Hilt** que impide la compilación del proyecto.

---

## ❌ Error de Compilación

```
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:hiltJavaCompileRelease'.
> Compilation failed; see the compiler output below.

error: [Dagger/DuplicateBindings] okhttp3.OkHttpClient is bound multiple times:
  @Provides @Singleton @org.jetbrains.annotations.NotNull okhttp3.OkHttpClient 
  com.jaak.stampssdk.di.NetworkModule.provideOkHttpClient(@dagger.hilt.android.qualifiers.ApplicationContext android.content.Context, com.jaak.stampssdk.utils.ProfileManager)
  
  @Provides @Singleton @org.jetbrains.annotations.NotNull okhttp3.OkHttpClient 
  com.jaak.visagesdk.di.NetworkModule.provideOkHttpClient()
```

---

## 🔍 Análisis Técnico

### **Causa Raíz:**
Ambos SDKs tienen sus propios módulos de inyección de dependencias con Dagger/Hilt:

1. **Stamps SDK 1.2.0** - `NetworkModule`:
   ```kotlin
   @Provides 
   @Singleton 
   @NotNull 
   fun provideOkHttpClient(
       @ApplicationContext context: Context, 
       profileManager: ProfileManager
   ): OkHttpClient
   ```

2. **Visage SDK 1.2.0-beta.3** - `NetworkModule`:
   ```kotlin
   @Provides 
   @Singleton 
   @NotNull 
   fun provideOkHttpClient(): OkHttpClient
   ```

Ambos módulos están marcados con `@Singleton` y proveen el mismo tipo (`okhttp3.OkHttpClient`), lo que causa un conflicto cuando Dagger/Hilt intenta construir el grafo de dependencias.

### **¿Por qué no pasaba en versiones 1.1.0?**
Las versiones anteriores (1.1.0) probablemente:
- No usaban Dagger/Hilt para la inyección de OkHttpClient
- Tenían cualificadores (@Named, @Qualifier) para diferenciar las instancias
- Usaban scope diferente (no @Singleton)

---

## 🔧 Intentos de Solución (Sin Éxito)

### **Intento 1: Excluir módulo OkHttp de Visage SDK**
```gradle
implementation("com.jaak.visagesdk:jaakvisage-sdk:1.2.0-beta.3") {
    exclude group: 'com.squareup.okhttp3', module: 'okhttp'
}
```
**Resultado:** ❌ El conflicto persiste porque está a nivel de código compilado (módulo Hilt), no en dependencias transitivas.

### **Intento 2: Excluir todo el grupo Hilt**
**Resultado:** ❌ No funcional porque rompería la arquitectura interna del SDK.

---

## ✅ Soluciones Propuestas para el Equipo de Jaak

### **Opción 1: Usar Cualificadores (Recomendado)**
Agregar anotaciones personalizadas para diferenciar las instancias:

**Stamps SDK:**
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StampsOkHttpClient

@Provides 
@Singleton 
@StampsOkHttpClient
fun provideOkHttpClient(...): OkHttpClient
```

**Visage SDK:**
```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VisageOkHttpClient

@Provides 
@Singleton 
@VisageOkHttpClient
fun provideOkHttpClient(): OkHttpClient
```

### **Opción 2: Compartir Instancia entre SDKs**
Crear un módulo común `JaakNetworkModule` que provea una única instancia de OkHttpClient compartida:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object JaakNetworkModule {
    @Provides
    @Singleton
    fun provideSharedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
```

Y que ambos SDKs inyecten esta instancia en lugar de crear la propia.

### **Opción 3: Usar Scopes Diferentes**
Si cada SDK necesita su propia configuración de OkHttpClient, usar scopes diferentes:

```kotlin
// Stamps SDK
@Provides 
@StampsScope 
fun provideOkHttpClient(...): OkHttpClient

// Visage SDK
@Provides 
@VisageScope 
fun provideOkHttpClient(...): OkHttpClient
```

### **Opción 4: Hacer OkHttpClient Configurable**
Permitir que el host app inyecte el OkHttpClient:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface OkHttpClientModule {
    @Binds
    fun bindOkHttpClient(impl: OkHttpClient): OkHttpClient
}
```

---

## 📊 Impacto

### **Bloqueado:**
- ✅ Actualización a Stamps SDK 1.2.0
- ✅ Actualización a Visage SDK 1.2.0-beta.3
- ❌ **No se pueden usar ambos SDKs simultáneamente en sus versiones más recientes**

### **Workaround Temporal:**
Mantener versiones anteriores que funcionan:
- Stamps SDK: **1.1.0**
- Visage SDK: **1.1.0**

---

## 🏷️ Versiones Afectadas

| SDK | Versión Funcional | Versión con Conflicto |
|-----|-------------------|----------------------|
| Stamps SDK | 1.1.0 | 1.2.0 |
| Visage SDK | 1.1.0 | 1.2.0-beta.3 |

---

## 📝 Archivos de Configuración

**build.gradle actual (funcional):**
```gradle
implementation("com.jaak.stampssdk:jaakstamps-sdk:1.1.0")
implementation("com.jaak.visagesdk:jaakvisage-sdk:1.1.0")
```

**build.gradle intentado (con conflicto):**
```gradle
implementation("com.jaak.stampssdk:jaakstamps-sdk:1.2.0")
implementation("com.jaak.visagesdk:jaakvisage-sdk:1.2.0-beta.3")
```

---

## 🔗 Información del Entorno

- **Gradle:** 8.12
- **Android Gradle Plugin:** (verificar en proyecto)
- **Dagger/Hilt:** Versión usada por SDKs
- **OkHttp:** 5.0.0-alpha.2 (en host app)
- **Kotlin:** Verificar versión
- **Java Target:** 17

---

## 📞 Acción Requerida

**Para el equipo de desarrollo de Jaak SDKs:**

1. ⚠️ **Prioridad Alta**: Resolver conflicto de OkHttpClient entre Stamps SDK y Visage SDK
2. 📋 Implementar una de las soluciones propuestas (Opción 1 recomendada)
3. 🧪 Probar compatibilidad entre SDKs antes del release
4. 📖 Documentar cualquier cambio en la arquitectura de inyección de dependencias

---

## 📎 Anexos

### Log Completo del Error:
```
/Users/michael/Documents/project_android/jaak-android-kyc-example/app/build/generated/hilt/component_sources/release/com/jaak/kyc/KYCApp_HiltComponents.java:194: error: [Dagger/DuplicateBindings] okhttp3.OkHttpClient is bound multiple times:
    public abstract static class SingletonC implements KYCApp_GeneratedInjector,
                           ^
        @Provides @Singleton @org.jetbrains.annotations.NotNull okhttp3.OkHttpClient com.jaak.stampssdk.di.NetworkModule.provideOkHttpClient(@dagger.hilt.android.qualifiers.ApplicationContext android.content.Context, com.jaak.stampssdk.utils.ProfileManager)
        @Provides @Singleton @org.jetbrains.annotations.NotNull okhttp3.OkHttpClient com.jaak.visagesdk.di.NetworkModule.provideOkHttpClient()
        okhttp3.OkHttpClient is injected at
            com.jaak.stampssdk.di.NetworkModule.provideStampsApiService(okHttpClient, …)
        com.jaak.stampssdk.data.network.StampsApiService is injected at
            com.jaak.stampssdk.sdk.ScanStampsActivity.apiService
        com.jaak.stampssdk.sdk.ScanStampsActivity is injected at
            com.jaak.stampssdk.sdk.ScanStampsActivity_GeneratedInjector.injectScanStampsActivity(com.jaak.stampssdk.sdk.ScanStampsActivity)
    It is also requested at:
        com.jaak.visagesdk.di.NetworkModule.provideVisageApiService(okHttpClient, …)
    The following other entry points also depend on it:
        dagger.hilt.android.internal.lifecycle.HiltViewModelFactory.ViewModelFactoriesEntryPoint.getHiltViewModelMap()
        com.jaak.stampssdk.ui.view.ValidationKycActivity_GeneratedInjector.injectValidationKycActivity(com.jaak.stampssdk.ui.view.ValidationKycActivity)
  1 error
  5 warnings
```

### Comando para Reproducir:
```bash
./gradlew clean build --no-daemon
```

---

**Documento generado automáticamente**  
**Contacto:** michael.avila@jaak.ai

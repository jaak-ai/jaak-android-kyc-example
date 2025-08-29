# Jaak Android KYC Example

## Descripción

El proyecto jaak-android-kyc-example es un ejemplo de implementación de un proceso KYC (Know Your Customer), cuyo objetivo es validar la autenticidad de documentos de identificación y verificar la identidad de una persona dentro de un flujo KYC. Esto garantiza que los documentos sean válidos, verídicos y cumplan con las normativas establecidas.

Este proyecto está desarrollado en Android, siguiendo la arquitectura MVVM (Model-View-ViewModel), e implementa dos SDKs creados por Jaak:

Document Detector SDK: Se encarga de capturar una o dos imágenes con la cámara del dispositivo, dependiendo del tipo de documento, y convertirlas en formato base64.

Face Detector SDK: Permite la grabación de un video con la cámara del dispositivo para verificar que el rostro capturado sea humano. Si el dispositivo lo permite, este proceso se realiza de manera automática.

Además, el ejemplo incluye la integración con servicios REST para:

- Validar los documentos capturados.

- Verificar la autenticidad del rostro.

- Comparar los documentos con la identidad de la persona.

Para la comunicación con los servicios REST, se utiliza Retrofit como cliente HTTP principal.

Este proyecto sirve como referencia para entender la implementación y validación de un proceso KYC utilizando Jaak como proveedor de tecnología.

## 🔄 Sistema Offline KYC

### Funcionalidad Offline-First

El sistema KYC ha sido extendido con capacidades offline-first que permite a los usuarios completar procesos KYC sin conexión a internet. Los datos se almacenan localmente y se sincronizan automáticamente cuando la conectividad regresa.

**Características principales:**

- **Almacenamiento local** con Room Database para persistencia offline
- **Sincronización automática** cuando regresa la conectividad  
- **Gestión de estados individuales** por cada servicio KYC
- **Notificaciones inteligentes** para recordar sincronización
- **Manejo robusto de errores** con reintentos automáticos
- **Soporte para múltiples procesos** simultáneos

### Arquitectura Offline

El sistema offline implementa los siguientes componentes:

#### 📊 Base de Datos Local (Room)
- `KycProcessEntity` - Almacena procesos KYC con estados granulares
- `KycProcessDao` - Operaciones de acceso a datos  
- `KycDatabase` - Configuración de base de datos Room

#### 🔄 Estados de Proceso
```kotlin
// Estados generales del proceso
KycProcessStatus {
    PENDING,           // No iniciado
    IN_PROGRESS,       // Algunos servicios completados
    COMPLETED_OFFLINE, // Completado offline, necesita sync
    SYNCING,           // Sincronizando actualmente
    COMPLETED,         // Completado y sincronizado
    FAILED,            // Fallo irrecuperable
    CANCELLED          // Cancelado por usuario
}

// Estados individuales por servicio
ServiceStatus {
    PENDING,     // No ejecutado
    COMPLETED,   // Completado (offline o online)
    SYNCED,      // Completado y sincronizado al servidor
    FAILED,      // Fallo en ejecución
    RETRYING     // Reintentando
}
```

#### 🛠️ Servicios KYC Offline
1. **Session Service** - Creación de sesión KYC
2. **Verify Service** - Verificación de documentos  
3. **OCR Service** - Extracción de texto de documentos
4. **Liveness Service** - Detección de vida facial
5. **OtoVerify Service** - Matching facial
6. **Finish Service** - Finalización del proceso

#### 📱 Interfaces de Usuario
- **MenuMainActivity** - Pantalla principal con indicadores de conectividad
- **KycProcessesActivity** - Listado de procesos pendientes offline
- **KycSyncActivity** - Pantalla de sincronización con vista híbrida
- **Indicadores visuales** de conectividad en tiempo real

#### 🔔 Sistema de Notificaciones
- **Notificaciones de sync** cuando hay procesos pendientes
- **Recordatorios de conectividad** para usuarios offline
- **Resúmenes diarios** de procesos pendientes  
- **Notificaciones de errores** para fallos que requieren atención

#### ⚙️ WorkManager Integration
- **Sync automático** en background cuando regresa conectividad
- **Retry automático** para procesos fallidos
- **Scheduling inteligente** de notificaciones
- **Manejo de constraints** de red y batería

### Flujo de Trabajo Offline

#### 1. Proceso Offline
```
Usuario inicia KYC → Servicios ejecutan offline → 
Datos almacenados en Room → Estado COMPLETED_OFFLINE
```

#### 2. Sincronización Automática  
```
Conectividad detectada → WorkManager inicia sync →
Procesos enviados al servidor → Estados actualizados a SYNCED
```

#### 3. Manejo de Errores
```
Error detectado → Retry automático → 
Si falla múltiples veces → Notificación al usuario
```

### Configuración del Sistema Offline

#### Dependencias Principales
```gradle
// Persistencia offline
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Trabajo en background
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Inyección de dependencias
implementation("com.google.dagger:hilt-android:$hilt_version")
```

#### Configuración de Base de Datos
```kotlin
@Database(
    entities = [KycProcessEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class KycDatabase : RoomDatabase() {
    abstract fun kycProcessDao(): KycProcessDao
}
```

### Testing del Sistema Offline

El sistema incluye testing exhaustivo:

#### Tests Unitarios
- **SimpleKycProcessEntityTest** - Validación de entidades
- Tests de DAOs y repositorios
- Tests de ViewModels y casos de uso

#### Tests de Integración  
- **SimpleWorkflowIntegrationTest** - Flujos completos offline→online
- **EdgeCasesAndPerformanceTest** - Casos extremos y rendimiento
- Tests de sincronización y manejo de errores

#### Cobertura de Testing
- ✅ Creación y gestión de procesos offline
- ✅ Transiciones de estado correctas
- ✅ Manejo de interrupciones y recuperación
- ✅ Sincronización automática
- ✅ Casos extremos y rendimiento (1000+ procesos)
- ✅ Manejo de errores y reintentos
- ✅ Persistencia de datos durante transiciones

### Uso del Sistema Offline

#### Para Desarrolladores

1. **Inicializar proceso offline:**
```kotlin
val process = KycProcessEntity(
    shortKey = "PROC-001",
    overallStatus = KycProcessStatus.PENDING
)
repository.insertProcess(process)
```

2. **Actualizar estado de servicio:**
```kotlin
val updatedProcess = process.copy(
    sessionStatus = ServiceStatus.COMPLETED
)
repository.updateProcess(updatedProcess)
```

3. **Sincronizar cuando hay conectividad:**
```kotlin
syncManager.syncPendingProcesses()
```

#### Para Usuarios Finales

1. **Modo Offline:** Los usuarios pueden completar procesos KYC sin internet
2. **Indicadores visuales:** La app muestra el estado de conectividad
3. **Sincronización automática:** Los procesos se envían automáticamente cuando regresa internet
4. **Notificaciones:** El sistema notifica sobre procesos pendientes y estados de sync

### Consideraciones de Rendimiento

- **Almacenamiento:** ~15-25MB por proceso KYC (imágenes/videos en Base64)
- **Batching:** Sincronización en lotes para optimizar red
- **Compresión:** Datos comprimidos antes de almacenar
- **Cleanup:** Limpieza automática de procesos sincronizados antiguos
- **Testing:** Validado con 1000+ procesos simultáneos


## Requerimientos

- Android Studio: Versión recomendada Iguana.

- Gradle: Versión recomendada 8.4.

- Android Gradle Plugin: Versión recomendada 8.3.2.

- Compilación del SDK: SDK 35.

- Compatibilidad de Source y Target: Java 18.

- minSdkVersion: 26.

- targetSdkVersion: 33.

- Groovy DSL: La configuración de build.gradle debe estar en Groovy DSL.


## Licencia
Este proyecto se distribuye bajo la licencia JAAK.
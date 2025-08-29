# Sistema Android KYC - Online/Offline

## Tabla de contenido

1. [Objetivo, alcance y usuarios](#1-objetivo-alcance-y-usuarios)
2. [Desarrollo](#2-desarrollo)
   - 2.1 [Prerrequisitos técnicos](#21-prerrequisitos-técnicos)
   - 2.2 [Configuración del entorno](#22-configuración-del-entorno)
     - [Paso 1. Instalación](#paso-1-instalación)
     - [Paso 2. Configuración Avanzada](#paso-2-configuración-avanzada)
   - 2.3 [Guía de implementación](#23-guía-de-implementación)
     - 2.3.1 [Implementación básica](#231-implementación-básica)
     - 2.3.2 [Implementación avanzada](#232-implementación-avanzada)
   - 2.4 [Referencias/Métodos](#24-referenciasmétodos)
     - 2.4.1 [Especificación principal](#241-especificación-principal)
     - 2.4.2 [Parámetros de configuración](#242-parámetros-de-configuración)
     - 2.4.3 [Métodos de ejecución](#243-métodos-de-ejecución)
     - 2.4.4 [Estructura de respuesta](#244-estructura-de-respuesta)
   - 2.5 [Pruebas y validación](#25-pruebas-y-validación)
   - 2.6 [Solución de problemas](#26-solución-de-problemas)
   - 2.7 [Consideraciones importantes](#27-consideraciones-importantes)
3. [Anexos](#3-anexos)
4. [Validez y gestión de documentos](#4-validez-y-gestión-de-documentos)
5. [Versionado](#5-versionado)
6. [Historial de versiones](#6-historial-de-versiones)

---

## 1. **Objetivo, alcance y usuarios**

El objetivo de este documento es proporcionar una guía completa para la implementación del sistema KYC híbrido (Online/Offline) en aplicaciones Android, permitiendo realizar procesos completos de verificación de identidad que funcionan tanto con conectividad a internet como sin ella, con sincronización automática cuando la conectividad regresa.

Este documento abarca la implementación de un sistema offline-first que incluye almacenamiento local con Room Database, gestión granular de estados por servicio, sincronización automática en background, sistema de notificaciones inteligentes, y manejo robusto de errores con reintentos automáticos para procesos de KYC empresariales.

**Dirigido a:** Desarrolladores senior con experiencia en desarrollo de aplicaciones móviles Android, arquitecturas offline-first, y sistemas de sincronización de datos.

**Nivel requerido:** Conocimientos avanzados en desarrollo Android, Kotlin, Room Database, WorkManager, Hilt, arquitecturas MVVM, manejo de estados complejos, y sistemas distribuidos offline/online.

---

## 2. **Desarrollo**

### 2.1 Prerrequisitos técnicos

#### a) **Requisitos técnicos**

| Requisito | Versión/Especificación | Obligatorio | Notas |
|-----------|------------------------|-------------|-------|
| Android Studio | Iguana (versión recomendada) | Sí | IDE recomendado para desarrollo |
| Gradle | 8.4 | Sí | Sistema de construcción |
| Android Gradle Plugin | 8.3.2 | Sí | Plugin para construcción Android |
| SDK de compilación | 35 | Sí | Nivel de API para compilación |
| Java/Kotlin Compatibility | 17 | Sí | Compatibilidad de source y target |
| minSdkVersion | 24 | Sí | Android 7.0+ - Versión mínima soportada |
| targetSdkVersion | 33 | Sí | Versión objetivo de Android |
| Build Configuration | Groovy DSL | Sí | Formato de configuración build.gradle |
| Kotlin Version | 1.9.22 | Sí | Versión específica requerida |
| Hilt Version | 2.46 | Sí | Para inyección de dependencias |

#### b) **Credenciales y configuración de accesos**

**Requisitos de acceso:**

- **Repositorio Maven:** Acceso a `https://us-maven.pkg.dev/jaak-platform/jaak-android`
- **Dependencias:** Visage SDK 1.0.0-beta y Stamps SDK 1.0.0-beta
- **Base de datos:** Room Database para persistencia local
- **Background Processing:** WorkManager para sincronización automática  
- **Notificaciones:** Sistema de notificaciones Android con canales
- **Permisos:** Acceso a cámara, almacenamiento, red, y notificaciones

### 2.2 Configuración del entorno

#### Paso 1. Instalación

##### a) **Configuración del proyecto:**

1. Crear un nuevo proyecto Android seleccionando "No Activity"
2. En "Build Configuration Language" seleccionar: **Groovy DSL (build.gradle)**

##### b) **Configuración build.gradle del proyecto:**

```groovy
buildscript {
    ext.kotlin_version = "1.9.22"
    ext.hilt_version = '2.46'
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://jitpack.io'
        }
        maven {
            url 'https://us-maven.pkg.dev/jaak-platform/jaak-android'
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.3.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"
    }
}

tasks.register('clean', Delete) {
    delete rootProject.buildDir
}
```

##### c) **Configuración settings.gradle:**

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven {
            url 'https://us-maven.pkg.dev/jaak-platform/jaak-android'
        }
    }
}
```

#### Paso 2. Configuración Avanzada

##### a) **Configuración build.gradle del módulo app:**

```groovy
plugins {
    id 'com.android.application'
    id 'kotlin-android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    compileSdk 35

    defaultConfig {
        applicationId "com.jaak.kyc"
        minSdkVersion 24
        targetSdk 33
        versionCode 100
        versionName "1.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures{
        viewBinding = true
    }
    namespace 'com.jaak.kyc'
}

dependencies {
    // Bibliotecas de Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    
    // Android Jetpack
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation 'com.google.android.material:material:1.12.0'

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    // Inyección de Dependencias
    implementation("com.google.dagger:hilt-android:$hilt_version")
    kapt("com.google.dagger:hilt-android-compiler:$hilt_version")

    // Persistencia de Datos - SISTEMA OFFLINE
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-core:1.1.2")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // WorkManager - SINCRONIZACIÓN AUTOMÁTICA
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    // Pruebas
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.0")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("com.google.truth:truth:1.4.0")
    testImplementation("org.robolectric:robolectric:4.12.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.0")
    androidTestImplementation("androidx.work:work-testing:2.9.0")

    // Otras utilidades
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Bibliotecas de Google ML Kit y CameraX
    implementation 'com.google.android.gms:play-services-mlkit-face-detection:17.1.0'
    implementation 'androidx.camera:camera-camera2:1.4.1'
    implementation 'androidx.camera:camera-lifecycle:1.4.1'
    implementation 'androidx.camera:camera-core:1.4.1'
    implementation 'androidx.camera:camera-view:1.4.1'
    implementation 'androidx.camera:camera-video:1.4.1'

    // SDKs de Jaak
    implementation("com.jaak.stampssdk:jaakstamps-sdk:1.0.0-beta")
    implementation("com.jaak.visagesdk:jaakvisage-sdk:1.0.0-beta")
}
```

### 2.3 Guía de implementación

#### 2.3.1 Implementación básica

##### a) **Crear clase Application:**

La Application class se crea para:

- Inicialización global: Configurar base de datos Room y WorkManager
- Inyección de dependencias: @HiltAndroidApp para el contenedor de dependencias
- Configuración única: Establecer configuraciones offline que solo deben ejecutarse una vez

```kotlin
package com.jaak.kyc

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KYCApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
    
    override fun onCreate() {
        super.onCreate()
        // Configuración inicial del sistema offline
        initializeOfflineSystem()
    }
    
    private fun initializeOfflineSystem() {
        // La base de datos Room se inicializa automáticamente con Hilt
        // WorkManager se configura automáticamente con HiltWorkerFactory
    }
}
```

##### b) **Entidad Principal - KycProcessEntity:**

```kotlin
package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "kyc_processes")
data class KycProcessEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val shortKey: String,
    val sessionId: String? = null,
    val accessToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val overallStatus: KycProcessStatus = KycProcessStatus.PENDING,
    
    // Estados individuales por servicio
    val sessionStatus: ServiceStatus = ServiceStatus.PENDING,
    val verifyStatus: ServiceStatus = ServiceStatus.PENDING,
    val ocrStatus: ServiceStatus = ServiceStatus.PENDING,
    val livenessStatus: ServiceStatus = ServiceStatus.PENDING,
    val otoVerifyStatus: ServiceStatus = ServiceStatus.PENDING,
    val finishStatus: ServiceStatus = ServiceStatus.PENDING,
    
    // Gestión de errores por servicio
    val sessionError: String? = null,
    val verifyError: String? = null,
    val ocrError: String? = null,
    val livenessError: String? = null,
    val otoVerifyError: String? = null,
    val finishError: String? = null,
    
    // Contadores de reintentos por servicio
    val sessionRetryCount: Int = 0,
    val verifyRetryCount: Int = 0,
    val ocrRetryCount: Int = 0,
    val livenessRetryCount: Int = 0,
    val otoVerifyRetryCount: Int = 0,
    val finishRetryCount: Int = 0,
    
    // Conectividad de red
    val requiresSync: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null
)

enum class KycProcessStatus {
    PENDING,           // No iniciado
    IN_PROGRESS,       // Algunos servicios completados
    COMPLETED_OFFLINE, // Completado offline, necesita sync
    SYNCING,           // Sincronizando actualmente
    COMPLETED,         // Completado y sincronizado
    FAILED,            // Fallo irrecuperable
    CANCELLED          // Cancelado por usuario
}

enum class ServiceStatus {
    PENDING,     // No ejecutado
    COMPLETED,   // Completado (offline o online)
    SYNCED,      // Completado y sincronizado al servidor
    FAILED,      // Fallo en ejecución
    RETRYING     // Reintentando actualmente
}
```

##### c) **DAO para operaciones de base de datos:**

```kotlin
package com.jaak.kyc.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessStatus

@Dao
interface KycProcessDao {
    
    @Query("SELECT * FROM kyc_processes ORDER BY createdAt DESC")
    fun getAllProcesses(): Flow<List<KycProcessEntity>>
    
    @Query("SELECT * FROM kyc_processes WHERE overallStatus = :status ORDER BY createdAt DESC")
    fun getProcessesByStatus(status: KycProcessStatus): Flow<List<KycProcessEntity>>
    
    @Query("SELECT * FROM kyc_processes WHERE requiresSync = 1 ORDER BY createdAt ASC")
    fun getPendingSyncProcesses(): Flow<List<KycProcessEntity>>
    
    @Query("SELECT * FROM kyc_processes WHERE id = :id")
    suspend fun getProcessById(id: String): KycProcessEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcess(process: KycProcessEntity)
    
    @Update
    suspend fun updateProcess(process: KycProcessEntity)
    
    @Delete
    suspend fun deleteProcess(process: KycProcessEntity)
    
    @Query("DELETE FROM kyc_processes WHERE overallStatus = :status AND updatedAt < :cutoffTime")
    suspend fun deleteOldProcesses(status: KycProcessStatus, cutoffTime: Long)
}
```

##### d) **Base de datos Room:**

```kotlin
package com.jaak.kyc.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.jaak.kyc.data.local.dao.KycProcessDao
import com.jaak.kyc.data.local.entity.KycProcessEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [KycProcessEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class KycDatabase : RoomDatabase() {
    abstract fun kycProcessDao(): KycProcessDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideKycDatabase(@ApplicationContext context: Context): KycDatabase {
        return Room.databaseBuilder(
            context,
            KycDatabase::class.java,
            "kyc_database"
        ).build()
    }
    
    @Provides
    fun provideKycProcessDao(database: KycDatabase): KycProcessDao {
        return database.kycProcessDao()
    }
}
```

##### e) **Repositorio con lógica offline:**

```kotlin
package com.jaak.kyc.data.repository

import com.jaak.kyc.data.local.dao.KycProcessDao
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessStatus
import com.jaak.kyc.data.local.entity.ServiceStatus
import com.jaak.kyc.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycProcessRepository @Inject constructor(
    private val dao: KycProcessDao,
    private val networkUtils: NetworkUtils
) {
    
    fun getAllProcesses(): Flow<List<KycProcessEntity>> = dao.getAllProcesses()
    
    fun getPendingSyncProcesses(): Flow<List<KycProcessEntity>> = dao.getPendingSyncProcesses()
    
    suspend fun createProcess(shortKey: String): KycProcessEntity {
        val process = KycProcessEntity(shortKey = shortKey)
        dao.insertProcess(process)
        return process
    }
    
    suspend fun updateServiceStatus(
        processId: String,
        serviceName: String,
        status: ServiceStatus,
        error: String? = null
    ) {
        val process = dao.getProcessById(processId) ?: return
        
        val updatedProcess = when (serviceName) {
            "session" -> process.copy(
                sessionStatus = status,
                sessionError = error,
                sessionRetryCount = if (status == ServiceStatus.FAILED) process.sessionRetryCount + 1 else process.sessionRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            "verify" -> process.copy(
                verifyStatus = status,
                verifyError = error,
                verifyRetryCount = if (status == ServiceStatus.FAILED) process.verifyRetryCount + 1 else process.verifyRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            "ocr" -> process.copy(
                ocrStatus = status,
                ocrError = error,
                ocrRetryCount = if (status == ServiceStatus.FAILED) process.ocrRetryCount + 1 else process.ocrRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            "liveness" -> process.copy(
                livenessStatus = status,
                livenessError = error,
                livenessRetryCount = if (status == ServiceStatus.FAILED) process.livenessRetryCount + 1 else process.livenessRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            "otoVerify" -> process.copy(
                otoVerifyStatus = status,
                otoVerifyError = error,
                otoVerifyRetryCount = if (status == ServiceStatus.FAILED) process.otoVerifyRetryCount + 1 else process.otoVerifyRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            "finish" -> process.copy(
                finishStatus = status,
                finishError = error,
                finishRetryCount = if (status == ServiceStatus.FAILED) process.finishRetryCount + 1 else process.finishRetryCount,
                updatedAt = System.currentTimeMillis()
            )
            else -> process
        }
        
        // Actualizar estado general del proceso
        val overallStatus = calculateOverallStatus(updatedProcess)
        val finalProcess = updatedProcess.copy(
            overallStatus = overallStatus,
            requiresSync = shouldRequireSync(overallStatus),
            updatedAt = System.currentTimeMillis()
        )
        
        dao.updateProcess(finalProcess)
    }
    
    private fun calculateOverallStatus(process: KycProcessEntity): KycProcessStatus {
        val allServices = listOf(
            process.sessionStatus,
            process.verifyStatus,
            process.ocrStatus,
            process.livenessStatus,
            process.otoVerifyStatus,
            process.finishStatus
        )
        
        return when {
            allServices.all { it == ServiceStatus.SYNCED } -> KycProcessStatus.COMPLETED
            allServices.all { it == ServiceStatus.COMPLETED || it == ServiceStatus.SYNCED } -> KycProcessStatus.COMPLETED_OFFLINE
            allServices.any { it == ServiceStatus.FAILED } -> KycProcessStatus.FAILED
            allServices.any { it == ServiceStatus.COMPLETED || it == ServiceStatus.SYNCED } -> KycProcessStatus.IN_PROGRESS
            else -> KycProcessStatus.PENDING
        }
    }
    
    private fun shouldRequireSync(status: KycProcessStatus): Boolean {
        return status == KycProcessStatus.COMPLETED_OFFLINE
    }
}
```

#### 2.3.2 Implementación avanzada

##### a) **Worker para sincronización automática:**

```kotlin
package com.jaak.kyc.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.jaak.kyc.data.repository.KycProcessRepository
import com.jaak.kyc.data.network.JaakApiService
import com.jaak.kyc.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: KycProcessRepository,
    private val apiService: JaakApiService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val pendingProcesses = repository.getPendingSyncProcesses().first()
            
            if (pendingProcesses.isEmpty()) {
                return Result.success()
            }
            
            var syncedCount = 0
            var failedCount = 0
            
            pendingProcesses.forEach { process ->
                try {
                    // Sincronizar proceso individual
                    val success = syncProcessToServer(process)
                    if (success) {
                        syncedCount++
                        // Actualizar estados a SYNCED
                        markProcessAsSynced(process)
                    } else {
                        failedCount++
                        incrementSyncAttempts(process)
                    }
                } catch (e: Exception) {
                    failedCount++
                    incrementSyncAttempts(process)
                }
            }
            
            // Mostrar notificación de resultado
            if (syncedCount > 0) {
                notificationHelper.showSyncSuccessNotification(syncedCount)
            }
            
            if (failedCount > 0) {
                notificationHelper.showSyncErrorNotification(failedCount)
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun syncProcessToServer(process: KycProcessEntity): Boolean {
        // Implementar lógica de sincronización con el servidor
        return try {
            // Enviar datos al servidor usando apiService
            // Retornar true si exitoso, false si falla
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun markProcessAsSynced(process: KycProcessEntity) {
        val syncedProcess = process.copy(
            sessionStatus = ServiceStatus.SYNCED,
            verifyStatus = ServiceStatus.SYNCED,
            ocrStatus = ServiceStatus.SYNCED,
            livenessStatus = ServiceStatus.SYNCED,
            otoVerifyStatus = ServiceStatus.SYNCED,
            finishStatus = ServiceStatus.SYNCED,
            overallStatus = KycProcessStatus.COMPLETED,
            requiresSync = false,
            lastSyncAttempt = System.currentTimeMillis()
        )
        repository.updateProcess(syncedProcess)
    }
    
    private suspend fun incrementSyncAttempts(process: KycProcessEntity) {
        val updatedProcess = process.copy(
            syncAttempts = process.syncAttempts + 1,
            lastSyncAttempt = System.currentTimeMillis()
        )
        repository.updateProcess(updatedProcess)
    }
}
```

##### b) **Sistema de notificaciones:**

```kotlin
package com.jaak.kyc.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jaak.kyc.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_SYNC = "kyc_sync_channel"
        const val CHANNEL_CONNECTIVITY = "kyc_connectivity_channel"
        const val CHANNEL_ERRORS = "kyc_errors_channel"
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Sincronización KYC",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de sincronización de procesos KYC"
            }
            
            val connectivityChannel = NotificationChannel(
                CHANNEL_CONNECTIVITY,
                "Conectividad KYC", 
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recordatorios de conectividad para procesos pendientes"
            }
            
            val errorsChannel = NotificationChannel(
                CHANNEL_ERRORS,
                "Errores KYC",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Errores que requieren atención del usuario"
            }
            
            notificationManager.createNotificationChannels(listOf(
                syncChannel, connectivityChannel, errorsChannel
            ))
        }
    }
    
    fun showSyncSuccessNotification(processCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_sync_success)
            .setContentTitle("Sincronización Completa")
            .setContentText("$processCount proceso(s) KYC sincronizado(s) exitosamente")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            
        notificationManager.notify(1001, notification)
    }
    
    fun showSyncErrorNotification(failedCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERRORS)
            .setSmallIcon(R.drawable.ic_sync_error)
            .setContentTitle("Error de Sincronización")
            .setContentText("$failedCount proceso(s) KYC fallaron al sincronizar")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            
        notificationManager.notify(1002, notification)
    }
    
    fun showPendingProcessesNotification(pendingCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTIVITY)
            .setSmallIcon(R.drawable.ic_pending)
            .setContentTitle("Procesos Pendientes")
            .setContentText("Tienes $pendingCount proceso(s) KYC esperando conexión")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        notificationManager.notify(1003, notification)
    }
}
```

### 2.4 Referencias/Métodos

#### 2.4.1 Especificación principal

**Sistema KYC Online/Offline - Arquitectura Híbrida**

**Descripción:** Sistema completo de KYC que funciona tanto online como offline, con almacenamiento local usando Room Database, sincronización automática en background con WorkManager, gestión granular de estados por servicio, sistema de notificaciones inteligentes, y manejo robusto de errores con reintentos automáticos.

**Características principales:**
- Funcionamiento offline completo con almacenamiento local
- Sincronización automática cuando regresa la conectividad
- Estados granulares por cada uno de los 6 servicios KYC
- Sistema de notificaciones contextual y no intrusivo
- Manejo de errores por servicio con reintentos automáticos
- Indicadores visuales de conectividad en tiempo real
- Soporte para múltiples procesos simultáneos
- Testing exhaustivo con casos extremos validados

#### 2.4.2 Parámetros de configuración

**Entidad KycProcessEntity - Configuración de estados:**

| Campo | Tipo | Descripción | Valores Posibles |
|-------|------|-------------|------------------|
| overallStatus | KycProcessStatus | Estado general del proceso | PENDING, IN_PROGRESS, COMPLETED_OFFLINE, SYNCING, COMPLETED, FAILED, CANCELLED |
| sessionStatus | ServiceStatus | Estado del servicio de sesión | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |
| verifyStatus | ServiceStatus | Estado del servicio de verificación | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |
| ocrStatus | ServiceStatus | Estado del servicio OCR | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |
| livenessStatus | ServiceStatus | Estado del servicio de detección de vida | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |
| otoVerifyStatus | ServiceStatus | Estado del servicio de matching facial | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |
| finishStatus | ServiceStatus | Estado del servicio de finalización | PENDING, COMPLETED, SYNCED, FAILED, RETRYING |

**Configuración WorkManager:**

| Parámetro | Tipo | Descripción | Valor Recomendado |
|-----------|------|-------------|-------------------|
| syncFrequency | PeriodicWorkRequest | Frecuencia de sincronización | 15 minutos |
| networkConstraint | Constraints | Restricción de conectividad | CONNECTED |
| batteryConstraint | Constraints | Restricción de batería | NOT_LOW_BATTERY |
| retryPolicy | BackoffPolicy | Política de reintentos | EXPONENTIAL |

#### 2.4.3 Métodos de ejecución

**Repositorio - Métodos principales:**

```kotlin
// Crear proceso offline
suspend fun createProcess(shortKey: String): KycProcessEntity

// Actualizar estado de servicio específico
suspend fun updateServiceStatus(
    processId: String,
    serviceName: String, // "session", "verify", "ocr", "liveness", "otoVerify", "finish"
    status: ServiceStatus,
    error: String? = null
)

// Obtener procesos pendientes de sincronización
fun getPendingSyncProcesses(): Flow<List<KycProcessEntity>>

// Marcar proceso como sincronizado
suspend fun markProcessAsSynced(processId: String)
```

**WorkManager - Métodos de sincronización:**

```kotlin
// Programar sincronización automática
fun scheduleSyncWork()

// Sincronización inmediata
fun syncNow()

// Cancelar trabajos de sincronización
fun cancelSyncWork()
```

#### 2.4.4 Estructura de respuesta

##### **Estados del Proceso (KycProcessStatus):**

```kotlin
enum class KycProcessStatus {
    PENDING,           // Proceso no iniciado
    IN_PROGRESS,       // Algunos servicios completados offline
    COMPLETED_OFFLINE, // Todos los servicios completados offline, pendiente sync
    SYNCING,           // Actualmente sincronizando con servidor  
    COMPLETED,         // Completamente terminado y sincronizado
    FAILED,            // Fallo irrecuperable en algún servicio
    CANCELLED          // Proceso cancelado por el usuario
}
```

##### **Estados por Servicio (ServiceStatus):**

```kotlin
enum class ServiceStatus {
    PENDING,     // Servicio no ejecutado aún
    COMPLETED,   // Servicio completado offline exitosamente
    SYNCED,      // Servicio completado y sincronizado al servidor
    FAILED,      // Servicio falló, requiere atención
    RETRYING     // Servicio reintentando automáticamente
}
```

### 2.5 Pruebas y validación

#### a) **Casos de prueba**

| Caso de Prueba | Configuración | Resultado Esperado | Criterio de Éxito |
|----------------|--------------|-------------------|-------------------|
| Flujo offline completo | Sin conectividad, completar todos los servicios | overallStatus = COMPLETED_OFFLINE | Todos los servicios en estado COMPLETED, datos persistidos en Room |
| Sincronización automática | Conectividad regresa, WorkManager detecta cambio | Procesos sincronizados automáticamente | overallStatus = COMPLETED, todos los servicios SYNCED |
| Manejo de errores individuales | Fallo en servicio específico (ej. OCR) | Error manejado sin afectar otros servicios | Solo ocrStatus = FAILED, otros servicios continúan |
| Reintentos automáticos | Servicio falla, configurado para reintentos | Incremento automático de retry counter | ocrRetryCount incrementado, estado RETRYING transitorio |
| Múltiples procesos simultáneos | Crear 3 procesos offline paralelos | Gestión independiente de cada proceso | Cada proceso mantiene su estado independiente |
| Notificaciones contextuales | Procesos pendientes + conectividad disponible | Notificación de procesos pendientes mostrada | Notificación no intrusiva con contador correcto |
| Persistencia de datos | Reiniciar app con procesos pendientes | Datos preservados después de reinicio | Procesos recuperados desde Room con estados correctos |
| Casos extremos | 1000+ procesos, memoria baja | Sistema mantiene rendimiento estable | Operaciones completadas en <5s, memoria estable |

### 2.6 Solución de problemas

#### a) **Problemas comunes**

| Problema | Error de sincronización automática |
|----------|-----------------------------------|
| **Descripción:** | WorkManager no ejecuta sincronización cuando regresa conectividad |
| **Causas posibles:** | Constraints no configuradas, batería baja, modo doze activo |
| **Solución:** | Verificar configuración de constraints y modo de batería |

```kotlin
// Configuración correcta de WorkManager
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(false) // Permitir en batería baja si es crítico
    .build()

val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(constraints)
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

| Problema | Base de datos Room corrupta |
|----------|----------------------------|
| **Descripción:** | Errores de acceso a la base de datos, procesos no persisten |
| **Causas posibles:** | Migración fallida, almacenamiento lleno, proceso interrumpido |
| **Solución:** | Implementar estrategia de recuperación y migración |

```kotlin
// Estrategia de recuperación para Room Database
@Database(
    entities = [KycProcessEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KycDatabase : RoomDatabase() {
    
    companion object {
        fun buildDatabase(context: Context): KycDatabase {
            return Room.databaseBuilder(context, KycDatabase::class.java, "kyc_database")
                .fallbackToDestructiveMigration() // En caso de corrupción
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Configuración inicial
                    }
                })
                .build()
        }
    }
}
```

#### b) **Códigos de error específicos**

| Código | Descripción | Causa | Solución |
|--------|-------------|-------|----------|
| SYNC_NETWORK_ERROR | Error de red durante sincronización | Conectividad inestable, servidor no disponible | WorkManager reintentará automáticamente |
| SYNC_AUTH_ERROR | Error de autenticación al sincronizar | Token expirado, credenciales inválidas | Renovar token, reintentar |
| DATABASE_WRITE_ERROR | Error escribiendo en Room Database | Almacenamiento lleno, permisos insuficientes | Liberar espacio, verificar permisos |
| SERVICE_RETRY_EXCEEDED | Máximo de reintentos alcanzado | Servicio falla consistentemente | Marcar como FAILED, notificar usuario |
| NOTIFICATION_PERMISSION_DENIED | Permisos de notificación denegados | Usuario denegó permisos | Funcionalidad continúa, sin notificaciones |

---

> Contacta al equipo de soporte (soporte@jaak.ai) cuando:
>
> - Los pasos de troubleshooting no resuelven problemas de sincronización
> - Recibes errores de base de datos Room persistentes
> - Necesitas configuraciones especiales de WorkManager para tu caso de uso
> - Experimentas problemas de rendimiento con múltiples procesos simultáneos
> 
> **Información a incluir:** Logs de Android Studio, configuración Room y WorkManager, pasos para reproducir, versión del sistema, cantidad de procesos simultáneos.

---

### 2.7 Consideraciones importantes

#### a) **Seguridad**
- **NUNCA** almacenar tokens de acceso en texto plano - usar EncryptedSharedPreferences
- **SIEMPRE** cifrar datos biométricos sensibles antes de almacenar en Room
- **VERIFICAR** que la sincronización use HTTPS y certificados válidos
- **IMPLEMENTAR** limpieza automática de datos locales después de sincronización exitosa
- **USAR** ProGuard/R8 para ofuscar código en producción

#### b) **Calidad**
- **PROBAR** funcionamiento offline completo sin conectividad durante días
- **VALIDAR** sincronización automática en diferentes escenarios de conectividad
- **REVISAR** manejo de memoria con múltiples procesos simultáneos
- **ASEGURAR** que Room Database maneja correctamente casos de corrupción
- **VERIFICAR** que WorkManager respeta constraints de batería y red

#### c) **Rendimiento**
- **OPTIMIZAR** consultas Room Database con índices apropiados
- **IMPLEMENTAR** paginación para listas grandes de procesos
- **CONSIDERAR** compresión de imágenes/videos Base64 antes de almacenar
- **MONITOREAR** uso de memoria durante sincronización masiva
- **CONFIGURAR** límites de procesos simultáneos según capacidad del dispositivo

#### d) **Experiencia de Usuario**
- **PROPORCIONAR** indicadores visuales claros del estado de conectividad
- **IMPLEMENTAR** notificaciones no intrusivas y configurables
- **ASEGURAR** que la app funciona fluidamente sin conectividad
- **PERMITIR** al usuario ver el progreso de sincronización
- **CONFIGURAR** mensajes de error contextuales y accionables

---

## 3. Anexos

### **Anexo A.** Glosario de términos

| Término | Definición |
|---------|------------|
| KYC | Know Your Customer - Proceso de verificación de identidad para servicios financieros |
| Room Database | Biblioteca de persistencia de Android que proporciona una capa de abstracción sobre SQLite |
| WorkManager | Biblioteca de Android para programar trabajo en background deferrable y confiable |
| Offline-First | Arquitectura donde la aplicación funciona primariamente offline con sincronización posterior |
| Sync Worker | Clase Worker que maneja sincronización automática en background |
| Service Status | Estados granulares para cada servicio individual del proceso KYC |
| Process Status | Estado general que engloba todos los servicios de un proceso KYC |
| Constraints | Condiciones que deben cumplirse para que WorkManager ejecute trabajos |
| BackoffPolicy | Estrategia de reintentos con intervalos crecientes en caso de fallos |

### **Anexo B.** Enlaces de referencia

- [Repositorio de ejemplo KYC Offline](https://github.com/jaak-ai/jaak-kyc-offline)
- [Documentación Room Database](https://developer.android.com/training/data-storage/room)
- [Guía WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Documentación Hilt](https://dagger.dev/hilt/)
- [Arquitecturas Offline-First](https://developer.android.com/topic/architecture/data-layer/offline-first)

---

## 4. **Validez y gestión de documentos**

*El propietario de este documento es el Senior Mobile Developer, quien debe verificar y actualizar el contenido cuando sea necesario para mantener la precisión técnica, la compatibilidad con las versiones más recientes de Room Database y WorkManager, y la efectividad de las estrategias de sincronización offline/online.*

---

## 5. **Versionado**

| **Responsable del documento:** | Senior Mobile Developer |
|-------------------------------|-------------------------------|
| **Aprobado por:** |       |
| **Fecha de aprobación:** | 29/08/2025 |
| **Clasificación de esta información:** | Documentación Técnica - Interna |
| **Código:** |  |

---

## 6. **Historial de versiones**

| Fecha | Versión | Tipo | Responsable | Descripción de la modificación |
|-------|---------|------|-------------|-------------------------------|
| 29/08/2025 | 1.0 | Creación | Senior Mobile Developer | Creación inicial de documentación del sistema KYC híbrido Online/Offline con arquitectura offline-first, Room Database, WorkManager, y sincronización automática |
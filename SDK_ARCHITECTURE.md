# 📦 Arquitectura de Modularización en SDKs

## Objetivo
Encapsular la funcionalidad KYC actual en 2 SDKs independientes y reutilizables.

---

## 📚 SDK 1: Jaak Document Verification SDK

### Responsabilidad
Captura, extracción y validación de documentos de identidad, incluyendo verificación contra listas negras.

### Componentes a Incluir

#### 1. **Dependencies**
```gradle
// SDK externo
implementation("com.jaak.stampssdk:jaakstamps-sdk:1.1.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
```

#### 2. **Código a Migrar**

##### Activities:
- ✅ `DocumentVerificationInstructionsActivity.kt`
- ✅ `VerifyOcrDocumentActivity.kt` (opcional, puede ser parte de la demo app)

##### Models:
```
data/model/ocr/
├── DocumentExtraBothRequest.kt
├── DocumentExtraBothResponse.kt
└── v4/
    ├── DocumentExtractV4Request.kt
    ├── DocumentExtractV4Response.kt
    └── (todos los submodelos)

data/model/verify/
├── VerifyRequest.kt
├── VerifyResponse.kt
└── State.kt

data/model/blacklist/
├── BlacklistRequest.kt
├── BlacklistResponse.kt
└── (todos los submodelos)
```

##### Network Layer:
```kotlin
// API Client
interface JaakDocumentApiClient {
    suspend fun documentExtractV4Api(
        auth: String,
        request: DocumentExtractV4Request
    ): Response<DocumentExtractV4Response>

    suspend fun verifyApi(
        auth: String,
        request: VerifyRequest
    ): Response<VerifyResponse>

    suspend fun blacklistInvestigateApi(
        auth: String,
        request: BlacklistRequest
    ): Response<BlacklistResponse>
}
```

##### Utils:
- ✅ `BlacklistRequestBuilder.kt`
- ✅ `FileStorageUtils.kt` (métodos relacionados con documentos)

##### Repository:
```kotlin
class DocumentVerificationRepository {
    // Extraer de KycOfflineRepository:
    // - performDocumentExtractService()
    // - performVerifyService()
    // - performBlacklistServices()
}
```

### 3. **API Pública del SDK**

```kotlin
// Punto de entrada principal
class JaakDocumentSDK private constructor(
    private val context: Context,
    private val config: DocumentSDKConfig
) {
    companion object {
        fun initialize(context: Context, config: DocumentSDKConfig): JaakDocumentSDK
    }

    // Configuración
    data class DocumentSDKConfig(
        val apiBaseUrl: String,
        val accessToken: String,
        val allowedCountries: List<String> = listOf("MEX"),
        val enableBlacklists: Boolean = true
    )

    // Callbacks
    interface DocumentCaptureListener {
        fun onDocumentCaptureSuccess(
            frontImage: String,
            backImage: String?
        )
        fun onDocumentCaptureError(error: DocumentError)
    }

    interface DocumentVerificationListener {
        fun onVerificationStarted()
        fun onDocumentExtracted(data: DocumentData)
        fun onDocumentVerified(result: VerifyResult)
        fun onBlacklistChecked(result: BlacklistResult)
        fun onVerificationCompleted(summary: VerificationSummary)
        fun onVerificationError(error: DocumentError)
    }

    // Métodos públicos
    fun startDocumentCapture(
        activity: Activity,
        listener: DocumentCaptureListener
    )

    fun verifyDocument(
        frontImagePath: String,
        backImagePath: String?,
        listener: DocumentVerificationListener
    )

    fun performBlacklistChecks(
        documentData: DocumentData,
        listener: DocumentVerificationListener
    )
}
```

### 4. **Modelos de Salida**

```kotlin
data class DocumentData(
    val firstName: String,
    val lastName: String,
    val secondName: String?,
    val motherSurname: String?,
    val dateOfBirth: String,
    val nationality: String,
    val curp: String?,
    val rfc: String?,
    val documentNumber: String,
    val documentType: String, // "I" (INE), "P" (Passport), etc.
    val expirationDate: String?,
    val faceImageBase64: String, // ⚠️ CRÍTICO para comparación con liveness
    val address: AddressData?,
    val rawOcrResponse: String // JSON completo para debugging
)

data class VerifyResult(
    val isValid: Boolean,
    val confidence: Float,
    val documentAuthenticity: Boolean,
    val securityFeatures: SecurityFeaturesResult
)

data class BlacklistResult(
    val ineResult: BlacklistCheckResult?,
    val interpolResult: BlacklistCheckResult?,
    val ofacResult: BlacklistCheckResult?,
    val renapoResult: BlacklistCheckResult?,
    val satResult: BlacklistCheckResult?,
    val overallStatus: BlacklistStatus // CLEAR, MATCH_FOUND, ERROR
)

data class VerificationSummary(
    val documentData: DocumentData,
    val verifyResult: VerifyResult,
    val blacklistResult: BlacklistResult,
    val processingTime: Long,
    val timestamp: Long
)
```

---

## 🎭 SDK 2: Jaak Biometric Verification SDK

### Responsabilidad
Detección de vida (liveness), captura de video biométrico y comparación facial one-to-one.

### Componentes a Incluir

#### 1. **Dependencies**
```gradle
// SDK externo
implementation("com.jaak.visagesdk:jaakvisage-sdk:1.1.0")

// ML Kit (opcional para face comparison on-device)
implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Video compression
implementation("com.github.AbedElazizShe:LightCompressor:1.3.2")
```

#### 2. **Código a Migrar**

##### Activities:
- ✅ `FacialVerificationInstructionsActivity.kt`
- ✅ `InitProcessLivenessActivity.kt`

##### Models:
```
data/model/livenessverify/
├── LivenessVerifyRequest.kt
├── LivenessVerifyResponse.kt
└── LivenessState.kt

data/model/otoverify/
├── OtoVerifyRequest.kt
├── OtoVerifyResponse.kt
└── OtoState.kt
```

##### Network Layer:
```kotlin
interface JaakBiometricApiClient {
    suspend fun livenessVerifyApi(
        auth: String,
        request: LivenessVerifyRequest
    ): Response<LivenessVerifyResponse>

    suspend fun otoVerifyApi(
        auth: String,
        request: OtoVerifyRequest
    ): Response<OtoVerifyResponse>
}
```

##### Utils:
- ✅ `FileStorageUtils.kt` (métodos relacionados con video/imágenes faciales)

##### Repository:
```kotlin
class BiometricVerificationRepository {
    // Extraer de KycOfflineRepository:
    // - performLivenessService()
    // - performOtoVerifyService()
}
```

### 3. **API Pública del SDK**

```kotlin
class JaakBiometricSDK private constructor(
    private val context: Context,
    private val config: BiometricSDKConfig
) {
    companion object {
        fun initialize(context: Context, config: BiometricSDKConfig): JaakBiometricSDK
    }

    // Configuración
    data class BiometricSDKConfig(
        val apiBaseUrl: String,
        val accessToken: String,
        val recordingDuration: Int = 8000, // ms
        val enableTutorial: Boolean = true
    )

    // Callbacks
    interface LivenessCaptureListener {
        fun onLivenessCaptureStarted()
        fun onLivenessRecordingProgress(secondsRemaining: Int)
        fun onLivenessCaptureSuccess(
            videoPath: String,
            bestFramePath: String
        )
        fun onLivenessCaptureError(error: BiometricError)
    }

    interface BiometricVerificationListener {
        fun onVerificationStarted()
        fun onLivenessVerified(result: LivenessResult)
        fun onFaceComparisonCompleted(result: OtoResult)
        fun onVerificationCompleted(summary: BiometricSummary)
        fun onVerificationError(error: BiometricError)
    }

    // Métodos públicos
    fun startLivenessCapture(
        activity: Activity,
        listener: LivenessCaptureListener
    )

    fun verifyLiveness(
        videoPath: String,
        bestFramePath: String,
        listener: BiometricVerificationListener
    )

    fun compareFaces(
        documentFaceBase64: String, // Del SDK 1
        livenessFaceBase64: String,
        listener: BiometricVerificationListener
    )
}
```

### 4. **Modelos de Salida**

```kotlin
data class LivenessResult(
    val isLive: Boolean,
    val confidence: Float,
    val bestFrameBase64: String,
    val videoCompressedPath: String?,
    val qualityScore: Float,
    val rawLivenessResponse: String
)

data class OtoResult(
    val isMatch: Boolean,
    val similarity: Float,
    val threshold: Float,
    val decision: String, // "MATCH", "NO_MATCH", "INCONCLUSIVE"
    val rawOtoResponse: String
)

data class BiometricSummary(
    val livenessResult: LivenessResult,
    val otoResult: OtoResult,
    val processingTime: Long,
    val timestamp: Long
)
```

---

## 🏗️ Estructura de Módulos Gradle

```
jaak-android-kyc-example/
├── app/                                    # App de demostración
│   └── build.gradle (consume ambos SDKs)
│
├── jaak-document-sdk/                      # SDK 1
│   ├── src/main/java/com/jaak/documentsdk/
│   │   ├── JaakDocumentSDK.kt
│   │   ├── api/
│   │   ├── models/
│   │   ├── repository/
│   │   └── utils/
│   └── build.gradle
│
├── jaak-biometric-sdk/                     # SDK 2
│   ├── src/main/java/com/jaak/biometricsdk/
│   │   ├── JaakBiometricSDK.kt
│   │   ├── api/
│   │   ├── models/
│   │   ├── repository/
│   │   └── utils/
│   └── build.gradle
│
└── settings.gradle
```

### settings.gradle
```gradle
rootProject.name = "jaak-android-kyc"
include ':app'
include ':jaak-document-sdk'
include ':jaak-biometric-sdk'
```

### app/build.gradle
```gradle
dependencies {
    // SDKs locales
    implementation project(':jaak-document-sdk')
    implementation project(':jaak-biometric-sdk')

    // O publicados en Maven
    // implementation("com.jaak.documentsdk:jaak-document-sdk:1.0.0")
    // implementation("com.jaak.biometricsdk:jaak-biometric-sdk:1.0.0")
}
```

---

## 🔄 Flujo de Integración en la App Host

```kotlin
class KycFlowActivity : AppCompatActivity() {

    private lateinit var documentSDK: JaakDocumentSDK
    private lateinit var biometricSDK: JaakBiometricSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar SDKs
        documentSDK = JaakDocumentSDK.initialize(
            context = this,
            config = JaakDocumentSDK.DocumentSDKConfig(
                apiBaseUrl = "https://api.qa.jaak.ai/",
                accessToken = profileManager.getAccessToken(),
                allowedCountries = listOf("MEX"),
                enableBlacklists = true
            )
        )

        biometricSDK = JaakBiometricSDK.initialize(
            context = this,
            config = JaakBiometricSDK.BiometricSDKConfig(
                apiBaseUrl = "https://api.qa.jaak.ai/",
                accessToken = profileManager.getAccessToken(),
                recordingDuration = 8000,
                enableTutorial = true
            )
        )
    }

    fun startKycFlow() {
        // Paso 1: Capturar documento
        documentSDK.startDocumentCapture(
            activity = this,
            listener = object : JaakDocumentSDK.DocumentCaptureListener {
                override fun onDocumentCaptureSuccess(
                    frontImage: String,
                    backImage: String?
                ) {
                    // Paso 2: Verificar documento
                    verifyDocument(frontImage, backImage)
                }

                override fun onDocumentCaptureError(error: DocumentError) {
                    showError(error)
                }
            }
        )
    }

    private fun verifyDocument(frontImage: String, backImage: String?) {
        documentSDK.verifyDocument(
            frontImagePath = frontImage,
            backImagePath = backImage,
            listener = object : JaakDocumentSDK.DocumentVerificationListener {
                override fun onVerificationCompleted(summary: VerificationSummary) {
                    // Guardar face del documento para comparación
                    val documentFace = summary.documentData.faceImageBase64

                    // Paso 3: Capturar liveness
                    startLivenessCapture(documentFace)
                }

                override fun onVerificationError(error: DocumentError) {
                    showError(error)
                }

                // Otros callbacks...
            }
        )
    }

    private fun startLivenessCapture(documentFace: String) {
        biometricSDK.startLivenessCapture(
            activity = this,
            listener = object : JaakBiometricSDK.LivenessCaptureListener {
                override fun onLivenessCaptureSuccess(
                    videoPath: String,
                    bestFramePath: String
                ) {
                    // Paso 4: Verificar liveness y comparar rostros
                    verifyBiometric(videoPath, bestFramePath, documentFace)
                }

                override fun onLivenessCaptureError(error: BiometricError) {
                    showError(error)
                }

                // Otros callbacks...
            }
        )
    }

    private fun verifyBiometric(
        videoPath: String,
        bestFramePath: String,
        documentFace: String
    ) {
        // Verificar liveness
        biometricSDK.verifyLiveness(
            videoPath = videoPath,
            bestFramePath = bestFramePath,
            listener = object : JaakBiometricSDK.BiometricVerificationListener {
                override fun onLivenessVerified(result: LivenessResult) {
                    // Comparar rostros
                    biometricSDK.compareFaces(
                        documentFaceBase64 = documentFace,
                        livenessFaceBase64 = result.bestFrameBase64,
                        listener = this
                    )
                }

                override fun onVerificationCompleted(summary: BiometricSummary) {
                    // ✅ KYC completado
                    showSuccess(summary)
                }

                override fun onVerificationError(error: BiometricError) {
                    showError(error)
                }

                // Otros callbacks...
            }
        )
    }
}
```

---

## 📊 Comparación: Antes vs Después

| Aspecto | Antes (Monolito) | Después (SDKs) |
|---------|------------------|----------------|
| **Líneas de código** | ~12,000 | SDK1: ~3,000<br>SDK2: ~2,500<br>App: ~6,500 |
| **Acoplamiento** | Alto | Bajo |
| **Reutilización** | Imposible | ✅ SDKs en múltiples apps |
| **Testing** | Difícil | ✅ Testing aislado por SDK |
| **Mantenimiento** | Complejo | ✅ Cambios aislados |
| **Publicación** | App completa | ✅ SDKs independientes en Maven |
| **Versionado** | Un solo version | ✅ Versionado independiente |

---

## 🚀 Plan de Migración

### Fase 1: Crear estructura de módulos (1-2 días)
1. Crear módulos `jaak-document-sdk` y `jaak-biometric-sdk`
2. Configurar `build.gradle` de cada módulo
3. Configurar dependencies mínimas

### Fase 2: Migrar SDK 1 - Document (3-4 días)
1. Mover modelos de datos
2. Mover network layer
3. Mover repository
4. Crear API pública
5. Testing unitario

### Fase 3: Migrar SDK 2 - Biometric (2-3 días)
1. Mover modelos de datos
2. Mover network layer
3. Mover repository
4. Crear API pública
5. Testing unitario

### Fase 4: Refactorizar App (2-3 días)
1. Actualizar dependencias
2. Refactorizar Activities para usar SDKs
3. Actualizar ViewModels
4. Testing de integración

### Fase 5: Documentación y ejemplos (1-2 días)
1. README para cada SDK
2. Javadoc/KDoc completo
3. App de ejemplo actualizada
4. Guías de integración

**Total estimado: 10-14 días de desarrollo**

---

## ✅ Beneficios de esta Arquitectura

1. **Separación de responsabilidades**: Cada SDK tiene un propósito claro
2. **Reutilización**: Otros equipos pueden usar los SDKs sin el código de la app
3. **Testing**: SDKs se pueden testear de forma aislada
4. **Versionado independiente**: SDK1 v1.2.0 + SDK2 v1.5.0
5. **Distribución**: Publicar en Maven/JitPack
6. **Mantenimiento**: Bugs en Document SDK no afectan Biometric SDK
7. **CI/CD**: Pipelines independientes por SDK

---

## 🎯 Próximos Pasos

¿Quieres que empiece con:
1. **Crear la estructura base** de módulos?
2. **Migrar SDK 1 primero** (Document Verification)?
3. **Migrar SDK 2 primero** (Biometric Verification)?
4. **Documentación detallada** antes de código?

# 🔐 Flujo de Datos y Autenticación entre SDKs

## Problema Crítico
Los SDKs necesitan **tokens de autenticación** para llamar a las APIs, y deben **intercambiar datos** entre sí para completar el flujo KYC.

---

## 🔑 Gestión de Tokens

### **Opción 1: Token por Configuración (Recomendado)**

Cada SDK recibe el token en su inicialización y lo usa internamente:

```kotlin
// Inicialización de SDKs con token
val documentSDK = JaakDocumentSDK.initialize(
    context = this,
    config = JaakDocumentSDK.DocumentSDKConfig(
        apiBaseUrl = "https://api.qa.jaak.ai/",
        accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", // ✅ Token aquí
        allowedCountries = listOf("MEX")
    )
)

val biometricSDK = JaakBiometricSDK.initialize(
    context = this,
    config = JaakBiometricSDK.BiometricSDKConfig(
        apiBaseUrl = "https://api.qa.jaak.ai/",
        accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", // ✅ Mismo token
        recordingDuration = 8000
    )
)
```

**Ventajas:**
- ✅ Simple de usar
- ✅ Token encapsulado dentro del SDK
- ✅ No se expone en cada llamada

**Desventajas:**
- ⚠️ Si el token expira, hay que reinicializar el SDK

---

### **Opción 2: Token Dinámico por Método (Flexible)**

Pasar el token en cada llamada a método:

```kotlin
// Método con token como parámetro
documentSDK.verifyDocument(
    frontImagePath = frontImage,
    backImagePath = backImage,
    accessToken = getCurrentToken(), // ✅ Token dinámico
    listener = verificationListener
)

biometricSDK.verifyLiveness(
    videoPath = videoPath,
    bestFramePath = bestFramePath,
    accessToken = getCurrentToken(), // ✅ Token se puede actualizar
    listener = biometricListener
)
```

**Ventajas:**
- ✅ Token se puede actualizar sin reinicializar
- ✅ Mejor para tokens de corta duración
- ✅ Permite usar diferentes tokens por operación

**Desventajas:**
- ⚠️ Más verboso
- ⚠️ El usuario del SDK debe manejar tokens

---

### **Opción 3: Token Provider (Recomendado para Producción)**

Usar un callback que provea el token bajo demanda:

```kotlin
interface TokenProvider {
    fun getAccessToken(): String
    fun refreshToken(): String?
}

// Inicialización con Token Provider
val documentSDK = JaakDocumentSDK.initialize(
    context = this,
    config = JaakDocumentSDK.DocumentSDKConfig(
        apiBaseUrl = "https://api.qa.jaak.ai/",
        tokenProvider = object : TokenProvider {
            override fun getAccessToken(): String {
                return profileManager.getAccessToken() // ✅ Siempre actualizado
            }

            override fun refreshToken(): String? {
                // Lógica de refresh si el token expira
                return profileManager.refreshAccessToken()
            }
        },
        allowedCountries = listOf("MEX")
    )
)
```

**Ventajas:**
- ✅ Token siempre actualizado
- ✅ Manejo automático de expiración
- ✅ Flexibilidad máxima
- ✅ Mejor práctica de seguridad

**Desventajas:**
- ⚠️ Un poco más complejo de implementar

---

## 📦 Datos Críticos de Entrada/Salida

### **SDK 1: Document Verification SDK**

#### **Entrada:**
```kotlin
// Configuración inicial
data class DocumentSDKConfig(
    val apiBaseUrl: String,                    // URL del API
    val accessToken: String,                   // Token de autenticación
    val allowedCountries: List<String>,        // ["MEX", "COL", etc.]
    val enableBlacklists: Boolean = true,      // Activar validación de listas
    val blacklistServices: List<BlacklistService> = listOf(
        BlacklistService.INE,
        BlacklistService.INTERPOL,
        BlacklistService.OFAC,
        BlacklistService.RENAPO,
        BlacklistService.SAT
    )
)

// ⭐ MÉTODO PRINCIPAL: Captura + Verificación completa
fun startDocumentVerification(
    activity: Activity,
    listener: DocumentVerificationListener
)
// Internamente:
// 1. Lanza Stamps SDK para capturar documento (frente + reverso)
// 2. Obtiene las imágenes de Stamps
// 3. Llama a Document Extract V4 API
// 4. Llama a Verify API
// 5. Llama a Blacklist APIs
// 6. Retorna DocumentVerificationSummary completo
```

#### **Salida Crítica:**
```kotlin
data class DocumentData(
    // ⭐ DATOS ESENCIALES PARA SDK 2
    val faceImageBase64: String,              // ⚠️ CRÍTICO: Face del documento para OTO
    val firstName: String,                     // Nombre
    val lastName: String,                      // Apellido paterno
    val secondName: String?,                   // Segundo nombre (opcional)
    val motherSurname: String?,                // Apellido materno

    // DATOS IDENTIFICACIÓN
    val curp: String?,                         // CURP (México)
    val rfc: String?,                          // RFC (México)
    val documentNumber: String,                // Número de documento
    val documentType: String,                  // "I" (INE), "P" (Passport)

    // DATOS BIOGRÁFICOS
    val dateOfBirth: String,                   // Fecha de nacimiento
    val nationality: String,                   // Nacionalidad
    val gender: String?,                       // Género

    // DATOS DIRECCIÓN
    val address: AddressData?,                 // Dirección completa

    // METADATA
    val expirationDate: String?,               // Fecha de vencimiento
    val issueDate: String?,                    // Fecha de expedición
    val confidence: Float,                     // Confianza del OCR (0.0 - 1.0)

    // RAW DATA para debugging
    val rawOcrResponse: String                 // JSON completo del OCR
)

data class VerifyResult(
    val isValid: Boolean,                      // Documento válido o no
    val confidence: Float,                     // Confianza (0.0 - 1.0)
    val documentAuthenticity: Boolean,         // Autenticidad del documento
    val securityFeatures: SecurityFeaturesResult, // Features de seguridad
    val errorMessages: List<String>            // Errores si hay
)

data class BlacklistResult(
    val overallStatus: BlacklistStatus,        // CLEAR, MATCH_FOUND, ERROR, PENDING
    val services: Map<BlacklistService, BlacklistCheckResult>,
    val hasMatches: Boolean,                   // Si hay coincidencias en alguna lista
    val matchedServices: List<BlacklistService> // Servicios que encontraron match
)

enum class BlacklistStatus {
    CLEAR,           // Sin coincidencias
    MATCH_FOUND,     // Encontrado en alguna lista
    ERROR,           // Error al consultar
    PENDING          // No se pudo consultar (offline/timeout)
}

// ⭐ SALIDA FINAL DEL SDK 1
data class DocumentVerificationSummary(
    val documentData: DocumentData,            // ⚠️ Contiene faceImageBase64
    val verifyResult: VerifyResult,
    val blacklistResult: BlacklistResult,
    val processingTimeMs: Long,
    val timestamp: Long
)
```

---

### **SDK 2: Biometric Verification SDK**

#### **Entrada:**
```kotlin
// Configuración inicial
data class BiometricSDKConfig(
    val apiBaseUrl: String,                    // URL del API
    val accessToken: String,                   // Token de autenticación
    val recordingDuration: Int = 8000,         // Duración del video (ms)
    val enableTutorial: Boolean = true,        // Mostrar tutorial
    val compressionQuality: Int = 80           // Calidad de compresión video
)

// ⭐ MÉTODO PRINCIPAL: Captura + Verificación completa
fun startBiometricVerification(
    activity: Activity,
    documentFaceBase64: String,                // ⚠️ VIENE DEL SDK 1
    listener: BiometricVerificationListener
)
// Internamente:
// 1. Lanza Visage SDK para capturar video de liveness
// 2. Obtiene video + best frame de Visage
// 3. Llama a Liveness Verify API
// 4. Llama a OTO API (comparando document face vs liveness face)
// 5. Retorna BiometricVerificationSummary completo
```

#### **Salida Crítica:**
```kotlin
data class LivenessResult(
    val isLive: Boolean,                       // Es una persona real
    val confidence: Float,                     // Confianza (0.0 - 1.0)
    val bestFrameBase64: String,               // ⚠️ CRÍTICO: Face para comparar con documento
    val videoCompressedPath: String?,          // Video comprimido (opcional)
    val qualityScore: Float,                   // Calidad del frame (0.0 - 1.0)
    val livenessScore: Float,                  // Score de vida (0.0 - 1.0)
    val errorMessages: List<String>,           // Errores si hay
    val rawLivenessResponse: String            // JSON completo
)

data class OtoResult(
    val isMatch: Boolean,                      // Rostros coinciden
    val similarity: Float,                     // Similitud (0.0 - 1.0)
    val threshold: Float,                      // Umbral usado
    val decision: OtoDecision,                 // MATCH, NO_MATCH, INCONCLUSIVE
    val errorMessages: List<String>,           // Errores si hay
    val rawOtoResponse: String                 // JSON completo
)

enum class OtoDecision {
    MATCH,           // Rostros coinciden (similarity >= threshold)
    NO_MATCH,        // Rostros NO coinciden
    INCONCLUSIVE     // No se pudo determinar (calidad baja, etc.)
}

// ⭐ SALIDA FINAL DEL SDK 2
data class BiometricVerificationSummary(
    val livenessResult: LivenessResult,
    val otoResult: OtoResult,
    val overallDecision: BiometricDecision,    // APPROVED, REJECTED, REVIEW_REQUIRED
    val processingTimeMs: Long,
    val timestamp: Long
)

enum class BiometricDecision {
    APPROVED,         // Liveness OK + OTO Match
    REJECTED,         // Liveness Fail o OTO No Match
    REVIEW_REQUIRED   // Scores bajos, requiere revisión manual
}
```

---

## 🔄 Flujo de Datos Completo entre SDKs

### **Diagrama de Flujo Completo**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           APP HOST (Orquestador)                        │
│                                                                          │
│  1. Inicializa SDK 1 con accessToken                                   │
│  2. Inicializa SDK 2 con accessToken                                   │
│  3. Llama: documentSDK.startDocumentVerification()                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     SDK 1: DOCUMENT VERIFICATION                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  PASO 1.1: 📸 Lanzar Stamps SDK                                        │
│  ├─ Usuario captura documento (frente + reverso)                       │
│  ├─ Stamps retorna: frontImagePath, backImagePath                      │
│  └─ Callback: onStampsCaptureCompleted()                               │
│                                                                          │
│  PASO 1.2: 🔍 Document Extract V4 API                                  │
│  ├─ POST /api/v4/document/extract                                      │
│  ├─ Headers: Authorization: Bearer {token}                             │
│  ├─ Body: { imageFront: base64, imageBack: base64 }                   │
│  └─ Response: DocumentData (firstName, CURP, faceImageBase64, ...)    │
│  └─ Callback: onDocumentExtracted(DocumentData)                        │
│       ⭐ Incluye: faceImageBase64 (CRÍTICO)                            │
│                                                                          │
│  PASO 1.3: 🔐 Verify API                                               │
│  ├─ POST /api/v1/kyc/verify                                            │
│  ├─ Headers: Authorization: Bearer {token}                             │
│  └─ Response: VerifyResult (isValid, confidence, ...)                  │
│  └─ Callback: onDocumentVerified(VerifyResult)                         │
│                                                                          │
│  PASO 1.4: 🚨 Blacklist APIs (5 servicios en paralelo)                │
│  ├─ POST /api/v1/kyc/blacklist/investigate (INE)                      │
│  ├─ POST /api/v1/kyc/blacklist/investigate (INTERPOL)                 │
│  ├─ POST /api/v1/kyc/blacklist/investigate (OFAC)                     │
│  ├─ POST /api/v1/kyc/blacklist/investigate (RENAPO)                   │
│  └─ POST /api/v1/kyc/blacklist/investigate (SAT69B)                   │
│  └─ Response: BlacklistResult (overallStatus, matches, ...)            │
│  └─ Callback: onBlacklistChecked(BlacklistResult)                      │
│                                                                          │
│  ✅ OUTPUT FINAL:                                                       │
│  └─ onVerificationCompleted(DocumentVerificationSummary)               │
│      ├─ documentData (con faceImageBase64)                             │
│      ├─ verifyResult                                                    │
│      └─ blacklistResult                                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ⭐ TRANSFERENCIA DE DATOS ENTRE SDKs
                    documentData.faceImageBase64
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    SDK 2: BIOMETRIC VERIFICATION                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  INPUT: documentFaceBase64 (del SDK 1)                                 │
│                                                                          │
│  PASO 2.1: 📹 Lanzar Visage SDK                                        │
│  ├─ Usuario graba video de liveness (8 segundos)                       │
│  ├─ Visage retorna: videoPath, bestFramePath                           │
│  └─ Callback: onVisageCaptureCompleted()                               │
│                                                                          │
│  PASO 2.2: 🎭 Liveness Verify API                                      │
│  ├─ POST /api/v1/kyc/liveness/verify                                   │
│  ├─ Headers: Authorization: Bearer {token}                             │
│  ├─ Body: { video: base64, bestFrame: base64 }                        │
│  └─ Response: LivenessResult (isLive, confidence, ...)                 │
│  └─ Callback: onLivenessVerified(LivenessResult)                       │
│                                                                          │
│  PASO 2.3: 🔍 OTO (One-to-One) API                                     │
│  ├─ POST /api/v1/kyc/oto/verify                                        │
│  ├─ Headers: Authorization: Bearer {token}                             │
│  ├─ Body: {                                                             │
│  │    documentFace: base64,  ⬅️ Del SDK 1                             │
│  │    livenessFace: base64   ⬅️ Del Visage                            │
│  │  }                                                                   │
│  └─ Response: OtoResult (isMatch, similarity, ...)                     │
│  └─ Callback: onFaceComparisonCompleted(OtoResult)                     │
│                                                                          │
│  ✅ OUTPUT FINAL:                                                       │
│  └─ onVerificationCompleted(BiometricVerificationSummary)              │
│      ├─ livenessResult                                                  │
│      ├─ otoResult                                                       │
│      └─ overallDecision (APPROVED/REJECTED/REVIEW_REQUIRED)            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           APP HOST (Final)                               │
│                                                                          │
│  ✅ Recibe ambos summaries:                                            │
│  ├─ DocumentVerificationSummary (SDK 1)                                │
│  └─ BiometricVerificationSummary (SDK 2)                               │
│                                                                          │
│  Determina decisión final: APPROVED / REJECTED / REVIEW_REQUIRED       │
│  Muestra resultado al usuario                                           │
│  Guarda en base de datos local                                          │
│  (Opcional) Sincroniza con backend                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

### **Datos Críticos en cada Paso**

```kotlin
class KycFlowOrchestrator {

    private lateinit var documentSDK: JaakDocumentSDK
    private lateinit var biometricSDK: JaakBiometricSDK

    // Datos compartidos entre SDKs
    private var documentFaceBase64: String? = null
    private var documentVerificationSummary: DocumentVerificationSummary? = null
    private var biometricVerificationSummary: BiometricVerificationSummary? = null

    fun initializeSDKs(accessToken: String) {
        documentSDK = JaakDocumentSDK.initialize(
            context = this,
            config = JaakDocumentSDK.DocumentSDKConfig(
                apiBaseUrl = "https://api.qa.jaak.ai/",
                accessToken = accessToken,  // ⭐ TOKEN
                allowedCountries = listOf("MEX")
            )
        )

        biometricSDK = JaakBiometricSDK.initialize(
            context = this,
            config = JaakBiometricSDK.BiometricSDKConfig(
                apiBaseUrl = "https://api.qa.jaak.ai/",
                accessToken = accessToken,  // ⭐ MISMO TOKEN
                recordingDuration = 8000
            )
        )
    }

    fun startKycFlow() {
        // ⭐ PASO 1: SDK 1 - Captura + Verificación de Documento (TODO EN UNO)
        documentSDK.startDocumentVerification(
            activity = this,
            listener = object : JaakDocumentSDK.DocumentVerificationListener {

                override fun onStampsCaptureStarted() {
                    // Stamps SDK está mostrando la cámara
                    Log.d("KYC", "📸 Stamps SDK iniciado - Capturando documento...")
                }

                override fun onStampsCaptureCompleted(
                    frontImagePath: String,
                    backImagePath: String?
                ) {
                    // Stamps SDK completó la captura
                    Log.d("KYC", "✅ Documento capturado")
                    Log.d("KYC", "  - Front: $frontImagePath")
                    Log.d("KYC", "  - Back: $backImagePath")
                }

                override fun onDocumentExtractStarted() {
                    // Iniciando llamada a Document Extract V4 API
                    Log.d("KYC", "🔍 Extrayendo datos del documento...")
                }

                override fun onDocumentExtracted(data: DocumentData) {
                    // OCR completado, datos extraídos
                    Log.d("KYC", "✅ Datos extraídos:")
                    Log.d("KYC", "  - Nombre: ${data.firstName} ${data.lastName}")
                    Log.d("KYC", "  - CURP: ${data.curp}")
                    Log.d("KYC", "  - Face extraído: ${data.faceImageBase64.take(50)}...")

                    // ⭐ GUARDAR FACE DEL DOCUMENTO
                    documentFaceBase64 = data.faceImageBase64
                }

                override fun onDocumentVerifyStarted() {
                    // Iniciando llamada a Verify API
                    Log.d("KYC", "🔐 Verificando autenticidad del documento...")
                }

                override fun onDocumentVerified(result: VerifyResult) {
                    // Verify API completado
                    Log.d("KYC", "✅ Documento verificado:")
                    Log.d("KYC", "  - Válido: ${result.isValid}")
                    Log.d("KYC", "  - Confianza: ${result.confidence}")
                    Log.d("KYC", "  - Autenticidad: ${result.documentAuthenticity}")
                }

                override fun onBlacklistCheckStarted() {
                    // Iniciando llamadas a Blacklist APIs
                    Log.d("KYC", "🚨 Verificando listas negras...")
                }

                override fun onBlacklistChecked(result: BlacklistResult) {
                    // Blacklist APIs completados
                    Log.d("KYC", "✅ Listas negras verificadas:")
                    Log.d("KYC", "  - Estado: ${result.overallStatus}")
                    Log.d("KYC", "  - Coincidencias: ${result.hasMatches}")
                }

                override fun onVerificationCompleted(summary: DocumentVerificationSummary) {
                    // ✅ SDK 1 COMPLETADO
                    Log.d("KYC", "✅ SDK 1 Completado en ${summary.processingTimeMs}ms")

                    // ⭐ GUARDAR RESUMEN COMPLETO
                    documentVerificationSummary = summary

                    // Validar que tenemos el face
                    if (summary.documentData.faceImageBase64.isEmpty()) {
                        handleError("No se pudo extraer el rostro del documento")
                        return
                    }

                    // ⭐ PASO 2: SDK 2 - Captura + Verificación Biométrica
                    startBiometricVerification(summary.documentData.faceImageBase64)
                }

                override fun onVerificationError(error: DocumentError) {
                    Log.e("KYC", "❌ Error en SDK 1: ${error.message}")
                    handleError(error)
                }
            }
        )
    }

    private fun startBiometricVerification(documentFaceBase64: String) {
        // ⭐ PASO 2: SDK 2 - Captura + Verificación Biométrica (TODO EN UNO)
        biometricSDK.startBiometricVerification(
            activity = this,
            documentFaceBase64 = documentFaceBase64,  // ⚠️ Del SDK 1
            listener = object : JaakBiometricSDK.BiometricVerificationListener {

                override fun onVisageCaptureStarted() {
                    // Visage SDK está mostrando la cámara
                    Log.d("KYC", "📹 Visage SDK iniciado - Grabando liveness...")
                }

                override fun onVisageRecordingProgress(secondsRemaining: Int) {
                    // Progreso de grabación
                    Log.d("KYC", "⏱️ Grabando... ${secondsRemaining}s restantes")
                }

                override fun onVisageCaptureCompleted(
                    videoPath: String,
                    bestFramePath: String
                ) {
                    // Visage SDK completó la captura
                    Log.d("KYC", "✅ Video capturado:")
                    Log.d("KYC", "  - Video: $videoPath")
                    Log.d("KYC", "  - Best Frame: $bestFramePath")
                }

                override fun onLivenessVerifyStarted() {
                    // Iniciando llamada a Liveness Verify API
                    Log.d("KYC", "🎭 Verificando liveness...")
                }

                override fun onLivenessVerified(result: LivenessResult) {
                    // Liveness API completado
                    Log.d("KYC", "✅ Liveness verificado:")
                    Log.d("KYC", "  - Es vivo: ${result.isLive}")
                    Log.d("KYC", "  - Confianza: ${result.confidence}")
                    Log.d("KYC", "  - Calidad: ${result.qualityScore}")
                }

                override fun onOtoComparisonStarted() {
                    // Iniciando llamada a OTO API
                    Log.d("KYC", "🔍 Comparando rostros (documento vs liveness)...")
                }

                override fun onFaceComparisonCompleted(result: OtoResult) {
                    // OTO API completado
                    Log.d("KYC", "✅ Comparación facial completada:")
                    Log.d("KYC", "  - Match: ${result.isMatch}")
                    Log.d("KYC", "  - Similitud: ${result.similarity}")
                    Log.d("KYC", "  - Decisión: ${result.decision}")
                }

                override fun onVerificationCompleted(summary: BiometricVerificationSummary) {
                    // ✅ SDK 2 COMPLETADO
                    Log.d("KYC", "✅ SDK 2 Completado en ${summary.processingTimeMs}ms")

                    // ⭐ GUARDAR RESUMEN BIOMÉTRICO
                    biometricVerificationSummary = summary

                    // ✅ KYC COMPLETADO
                    completeKycFlow()
                }

                override fun onVerificationError(error: BiometricError) {
                    Log.e("KYC", "❌ Error en SDK 2: ${error.message}")
                    handleError(error)
                }
            }
        )
    }

    private fun completeKycFlow() {
        // ⭐ COMBINAR RESULTADOS DE AMBOS SDKs
        val finalResult = KycFinalResult(
            documentSummary = documentVerificationSummary!!,
            biometricSummary = biometricVerificationSummary!!,
            overallDecision = determineOverallDecision()
        )

        // Mostrar resultado al usuario
        showKycResult(finalResult)
    }

    private fun determineOverallDecision(): KycDecision {
        val docValid = documentVerificationSummary?.verifyResult?.isValid == true
        val blacklistClear = documentVerificationSummary?.blacklistResult?.overallStatus == BlacklistStatus.CLEAR
        val biometricApproved = biometricVerificationSummary?.overallDecision == BiometricDecision.APPROVED

        return when {
            docValid && blacklistClear && biometricApproved -> KycDecision.APPROVED
            !docValid || !blacklistClear -> KycDecision.REJECTED_DOCUMENT
            !biometricApproved -> KycDecision.REJECTED_BIOMETRIC
            else -> KycDecision.REVIEW_REQUIRED
        }
    }
}

// ⭐ RESULTADO FINAL CONSOLIDADO
data class KycFinalResult(
    val documentSummary: DocumentVerificationSummary,
    val biometricSummary: BiometricVerificationSummary,
    val overallDecision: KycDecision,
    val completedAt: Long = System.currentTimeMillis()
)

enum class KycDecision {
    APPROVED,               // Todo OK
    REJECTED_DOCUMENT,      // Documento inválido o en lista negra
    REJECTED_BIOMETRIC,     // Liveness o face comparison fallaron
    REVIEW_REQUIRED         // Requiere revisión manual
}
```

---

## 📊 Matriz de Datos Críticos

| Dato | Origen | Destino | Cuándo | Propósito | ¿Obligatorio? |
|------|--------|---------|--------|-----------|---------------|
| **accessToken** | App Host | SDK 1 & SDK 2 | Inicialización | Autenticación API | ✅ SÍ |
| **frontImagePath** | Stamps SDK | SDK 1 (interno) | Después de captura | Imagen frontal documento | ✅ SÍ |
| **backImagePath** | Stamps SDK | SDK 1 (interno) | Después de captura | Imagen trasera documento | ⚠️ Opcional (pasaportes) |
| **faceImageBase64** | SDK 1 | SDK 2 | Después de OCR | Comparación OTO | ✅ SÍ (CRÍTICO) |
| **documentData** | SDK 1 | App Host | Al completar SDK 1 | Info del usuario | ✅ SÍ |
| **verifyResult** | SDK 1 | App Host | Al completar SDK 1 | Validez documento | ✅ SÍ |
| **blacklistResult** | SDK 1 | App Host | Al completar SDK 1 | Riesgo usuario | ⚠️ Configurable |
| **videoPath** | Visage SDK | SDK 2 (interno) | Después de captura | Video liveness | ✅ SÍ |
| **bestFramePath** | Visage SDK | SDK 2 (interno) | Después de captura | Frame liveness | ✅ SÍ |
| **livenessResult** | SDK 2 | App Host | Al completar SDK 2 | Prueba de vida | ✅ SÍ |
| **otoResult** | SDK 2 | App Host | Al completar SDK 2 | Match facial | ✅ SÍ |

### **Flujo de Datos Simplificado:**

```
App Host → accessToken → SDK 1
SDK 1 → Stamps SDK → frontImage, backImage
SDK 1 → Document Extract API → documentData (con faceImageBase64)
SDK 1 → Verify API → verifyResult
SDK 1 → Blacklist APIs → blacklistResult
SDK 1 → DocumentVerificationSummary → App Host

App Host → faceImageBase64 → SDK 2
SDK 2 → Visage SDK → videoPath, bestFramePath
SDK 2 → Liveness API → livenessResult
SDK 2 → OTO API (documentFace + livenessFace) → otoResult
SDK 2 → BiometricVerificationSummary → App Host

App Host → Decisión Final (APPROVED/REJECTED/REVIEW)
```

---

## 🎯 Recomendaciones Finales

### **Para el Token:**
✅ **Recomiendo Opción 3**: Token Provider con callback
- Más flexible
- Maneja expiración automáticamente
- Mejor práctica de seguridad

### **Para Datos entre SDKs:**
✅ **faceImageBase64** debe ser parte del output del SDK 1
- Es el dato crítico que conecta ambos SDKs
- Sin este dato, no se puede hacer OTO
- Debe ser base64 limpio (sin prefijo data:image)

### **Para Gestión de Estado:**
✅ Guardar los `Summary` de cada SDK en el App Host
- Permite auditoría completa
- Facilita debugging
- Puede almacenarse localmente o enviarse al backend

### **Para Errores:**
✅ Cada SDK debe retornar errores estructurados
- Códigos de error específicos
- Mensajes descriptivos
- Stack trace en debug mode

---

## ❓ Preguntas para ti

1. **Token**: ¿Prefieres Opción 1 (simple), 2 (flexible) o 3 (provider)?
2. **Face Image**: ¿Quieres que el SDK 1 limpie/valide el base64 del face?
3. **Persistencia**: ¿Los SDKs deben guardar datos localmente o solo en memoria?
4. **Offline Mode**: ¿Los SDKs deben soportar modo offline con sincronización posterior?
5. **Logs**: ¿Nivel de logging que quieres en los SDKs? (debug, info, warning, error)

¿Con esto te queda más claro el flujo de datos? 🚀

---

## 🎯 **RESUMEN EJECUTIVO**

### **Entradas y Salidas de cada SDK:**

#### **SDK 1: Document Verification**
```kotlin
// INPUT
- accessToken: String (del App Host)
- activity: Activity (para lanzar Stamps)

// PROCESO INTERNO
1. Lanza Stamps SDK → captura documento
2. Obtiene frontImage, backImage
3. Llama Document Extract V4 → extrae datos + face
4. Llama Verify → valida autenticidad
5. Llama Blacklists → verifica listas negras

// OUTPUT
- DocumentVerificationSummary
  ├─ documentData (firstName, CURP, faceImageBase64, ...)
  ├─ verifyResult (isValid, confidence, ...)
  └─ blacklistResult (overallStatus, matches, ...)
```

#### **SDK 2: Biometric Verification**
```kotlin
// INPUT
- accessToken: String (del App Host)
- documentFaceBase64: String (⭐ DEL SDK 1)
- activity: Activity (para lanzar Visage)

// PROCESO INTERNO
1. Lanza Visage SDK → graba video liveness
2. Obtiene videoPath, bestFramePath
3. Llama Liveness Verify → valida que es persona real
4. Llama OTO → compara documentFace vs livenessFace

// OUTPUT
- BiometricVerificationSummary
  ├─ livenessResult (isLive, confidence, ...)
  ├─ otoResult (isMatch, similarity, ...)
  └─ overallDecision (APPROVED/REJECTED/REVIEW_REQUIRED)
```

### **Dato Crítico que Conecta Ambos SDKs:**

```kotlin
// SDK 1 extrae este dato del documento
val faceImageBase64: String = documentData.faceImageBase64

// App Host lo pasa al SDK 2
biometricSDK.startBiometricVerification(
    activity = this,
    documentFaceBase64 = faceImageBase64  // ⭐ CRÍTICO
)

// SDK 2 lo usa para comparar con el rostro del liveness
// OTO API: compara documentFace vs livenessFace
```

### **Uso Super Simplificado en la App:**

```kotlin
// 1. Inicializar SDKs
val documentSDK = JaakDocumentSDK.initialize(context, config)
val biometricSDK = JaakBiometricSDK.initialize(context, config)

// 2. Ejecutar SDK 1
documentSDK.startDocumentVerification(activity) { summary ->
    // Obtener face del documento
    val face = summary.documentData.faceImageBase64
    
    // 3. Ejecutar SDK 2 con el face
    biometricSDK.startBiometricVerification(activity, face) { bioSummary ->
        // 4. ✅ KYC Completado
        showResult(summary, bioSummary)
    }
}
```

---

## ✅ **Con esto queda claro:**

1. ✅ **SDK 1** maneja Stamps + Document APIs (Extract, Verify, Blacklists)
2. ✅ **SDK 2** maneja Visage + Biometric APIs (Liveness, OTO)
3. ✅ **Token** se pasa en la inicialización de cada SDK
4. ✅ **Face del documento** es el dato crítico que conecta ambos SDKs
5. ✅ **App Host** solo orquesta: SDK1 → obtiene face → SDK2 → decisión final

¿Te queda claro el flujo ahora? 🚀

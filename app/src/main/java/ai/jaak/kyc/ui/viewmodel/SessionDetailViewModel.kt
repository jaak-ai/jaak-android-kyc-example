package ai.jaak.kyc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.jaak.kyc.data.model.api.SessionDetailResponse
import ai.jaak.kyc.data.repository.SessionDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionDetailState {
    object Loading : SessionDetailState()
    data class Success(val data: SessionDetailResponse) : SessionDetailState()
    data class Error(val message: String) : SessionDetailState()
}

/**
 * Tipos de tabs dinámicos según el flujo de la sesión
 */
enum class SessionTabType {
    INFO,           // Siempre visible
    DOCUMENT,       // Si existe "document-process" en flow
    LIVENESS,       // Si existe "liveness-process" en flow
    OTO             // Si existe "oto-process" en flow
}

data class SessionTab(
    val type: SessionTabType,
    val title: String
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionDetailRepository: SessionDetailRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SessionDetailState>(SessionDetailState.Loading)
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    private val _tabs = MutableStateFlow<List<SessionTab>>(emptyList())
    val tabs: StateFlow<List<SessionTab>> = _tabs.asStateFlow()

    /**
     * Carga el detalle de una sesión
     */
    fun loadSessionDetail(sessionId: String) {
        viewModelScope.launch {
            Log.d("SessionDetailViewModel", "========== INICIANDO CARGA EN VIEWMODEL ==========")
            Log.d("SessionDetailViewModel", "Session ID: $sessionId")
            Log.d("SessionDetailViewModel", "=================================================")

            _state.value = SessionDetailState.Loading

            val result = sessionDetailRepository.getSessionDetail(sessionId)

            result.fold(
                onSuccess = { response ->
                    Log.d("SessionDetailViewModel", "========== DATOS RECIBIDOS EN VIEWMODEL ==========")
                    Log.d("SessionDetailViewModel", "Session ID: ${response.session.sessionID}")
                    Log.d("SessionDetailViewModel", "ShortKey: ${response.session.shortKey}")
                    Log.d("SessionDetailViewModel", "Contact: ${response.session.contactName}")
                    Log.d("SessionDetailViewModel", "=================================================")

                    _state.value = SessionDetailState.Success(response)
                    // Generar tabs dinámicos según el flow
                    _tabs.value = generateDynamicTabs(response)

                    Log.d("SessionDetailViewModel", "Tabs generados: ${_tabs.value.size}")
                },
                onFailure = { exception ->
                    Log.e("SessionDetailViewModel", "========== ERROR EN VIEWMODEL ==========")
                    Log.e("SessionDetailViewModel", "Error: ${exception.message}")
                    Log.e("SessionDetailViewModel", "========================================")

                    _state.value = SessionDetailState.Error(
                        exception.message ?: "Error desconocido al cargar sesión"
                    )
                }
            )
        }
    }

    /**
     * Genera los tabs dinámicamente según los eventos del flow
     */
    private fun generateDynamicTabs(response: SessionDetailResponse): List<SessionTab> {
        val tabs = mutableListOf<SessionTab>()

        // Tab "Resumen" siempre está presente (antes era "Info")
        tabs.add(SessionTab(SessionTabType.INFO, "Resumen"))

        // Tab "Flujo" con información de los eventos
        tabs.add(SessionTab(SessionTabType.DOCUMENT, "Flujo"))

        // Tab "Detalles" con información técnica
        tabs.add(SessionTab(SessionTabType.LIVENESS, "Detalles"))

        return tabs
    }

    /**
     * Obtiene un evento específico del flow por action
     */
    fun getFlowEventByAction(action: String): ai.jaak.kyc.data.model.api.SessionDetailFlowEvent? {
        val currentState = _state.value
        if (currentState is SessionDetailState.Success) {
            return currentState.data.flow.firstOrNull { it.action == action }
        }
        return null
    }

    /**
     * Obtiene un evento específico del flow por eventId
     */
    fun getFlowEventById(eventId: String): ai.jaak.kyc.data.model.api.SessionDetailFlowEvent? {
        val currentState = _state.value
        if (currentState is SessionDetailState.Success) {
            return currentState.data.flow.firstOrNull { it.eventId == eventId }
        }
        return null
    }

    /**
     * Obtiene los datos de liveness desde el evento y summary
     */
    fun getLivenessData(eventId: String): ai.jaak.kyc.data.model.LivenessDetailData? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        val event = currentState.data.flow.firstOrNull { it.eventId == eventId }
        val summary = currentState.data.summary

        // ========== IMPRIMIR JSON COMPLETO DEL EVENTO LIVENESS ==========
        android.util.Log.d("SessionDetailViewModel", "========== EVENTO LIVENESS COMPLETO (JSON) ==========")
        val eventJson = com.google.gson.Gson().toJson(event)
        android.util.Log.d("SessionDetailViewModel", eventJson)
        android.util.Log.d("SessionDetailViewModel", "===============================================")

        // Buscar el recurso "liveness" en el array flow del evento
        val livenessResource = event?.flow?.firstOrNull { it.resource == "liveness" }

        // Debug: Imprimir recurso de liveness
        android.util.Log.d("SessionDetailViewModel", "Liveness resource encontrado: ${livenessResource != null}")
        android.util.Log.d("SessionDetailViewModel", "Liveness resource meta: ${livenessResource?.meta}")
        android.util.Log.d("SessionDetailViewModel", "Liveness resource meta.extra: ${livenessResource?.meta?.extra}")
        android.util.Log.d("SessionDetailViewModel", "Liveness resource meta.extra.evaluation: ${livenessResource?.meta?.extra?.evaluation}")

        // Extraer score del meta.extra.evaluation.score
        val evaluation = livenessResource?.meta?.extra?.evaluation
        val livenessScore = (evaluation?.get("score") as? Number)?.toDouble()

        android.util.Log.d("SessionDetailViewModel", "Score extraído: $livenessScore")

        // Extraer video URL del request.meta.request.video
        val videoUrl = event?.request?.meta?.request?.get("video") as? String

        // Extraer tiempo de procesamiento desde meta.processTime
        val processTime = livenessResource?.meta?.processTime
        val processingTime = if (processTime != null) {
            val seconds = processTime / 1000.0 // Convertir milisegundos a segundos
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        return ai.jaak.kyc.data.model.LivenessDetailData(
            eventId = eventId,
            bestFrameUrl = summary.photo, // URL de la mejor foto del usuario
            videoUrl = videoUrl ?: summary.rigelFullVideo, // URL del video desde request o summary
            livenessScore = livenessScore,
            processingTime = processingTime
        )
    }

    /**
     * Obtiene los datos de OTO (comparación 1:1) desde el evento
     * Retorna el evento completo para que el Activity pueda parsear todos los recursos
     */
    fun getOtoData(eventId: String): ai.jaak.kyc.data.model.api.SessionDetailFlowEvent? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        return currentState.data.flow.firstOrNull { it.eventId == eventId }
    }

    /**
     * Helper para obtener datos básicos de OTO
     */
    fun getOtoBasicData(eventId: String): ai.jaak.kyc.data.model.OtoDetailData? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        val event = currentState.data.flow.firstOrNull { it.eventId == eventId }
        val summary = currentState.data.summary

        // ========== IMPRIMIR JSON COMPLETO DEL EVENTO OTO ==========
        android.util.Log.d("SessionDetailViewModel", "========== EVENTO OTO COMPLETO (JSON) ==========")
        val eventJson = com.google.gson.Gson().toJson(event)
        android.util.Log.d("SessionDetailViewModel", eventJson)
        android.util.Log.d("SessionDetailViewModel", "===============================================")

        // Extraer score y distance del meta.extra
        val eventMeta = event?.meta?.extra as? Map<*, *>
        val score = (eventMeta?.get("score") as? Number)?.toDouble()
        val distance = (eventMeta?.get("distance") as? Number)?.toDouble()

        // Extraer URLs de imágenes del request.meta.request
        val requestData = event?.request?.meta?.request
        val image1Url = requestData?.get("image1") as? String
        val image2Url = requestData?.get("image2") as? String

        // Extraer datos de accesorios faciales del recurso "face-accessories"
        val accessoriesResource = event?.flow?.firstOrNull { it.resource == "face-accessories" }
        val accessoriesEvaluation = accessoriesResource?.meta?.extra?.evaluation as? Map<*, *>
        val accessoriesJson = if (accessoriesEvaluation != null) {
            com.google.gson.Gson().toJson(accessoriesEvaluation)
        } else null

        // Extraer datos de calidad de imagen del recurso "image-quality"
        val qualityResource = event?.flow?.firstOrNull { it.resource == "image-quality" }
        val qualityEvaluation = qualityResource?.meta?.extra?.evaluation as? Map<*, *>
        val qualityJson = if (qualityEvaluation != null) {
            com.google.gson.Gson().toJson(qualityEvaluation)
        } else null

        // Buscar el recurso "one-to-one" para el tiempo de procesamiento de comparación
        val otoResource = event?.flow?.firstOrNull { it.resource == "one-to-one" }
        val otoProcessTime = otoResource?.meta?.processTime
        val otoProcessingTime = if (otoProcessTime != null) {
            val seconds = otoProcessTime / 1000.0
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        // Tiempos de procesamiento de accesorios y calidad
        val accessoriesProcessTime = accessoriesResource?.meta?.processTime
        val accessoriesTime = if (accessoriesProcessTime != null) {
            val seconds = accessoriesProcessTime / 1000.0
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        val qualityProcessTime = qualityResource?.meta?.processTime
        val qualityTime = if (qualityProcessTime != null) {
            val seconds = qualityProcessTime / 1000.0
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        return ai.jaak.kyc.data.model.OtoDetailData(
            eventId = eventId,
            image1Url = image1Url,
            image2Url = image2Url,
            comparisonScore = score ?: summary.scores?.oneToOne,
            distance = distance,
            processingTime = otoProcessingTime,
            accessoriesData = accessoriesJson,
            qualityData = qualityJson,
            accessoriesProcessingTime = accessoriesTime,
            qualityProcessingTime = qualityTime
        )
    }

    /**
     * Obtiene los datos de extracción de documento
     */
    fun getDocumentExtractData(eventId: String): ai.jaak.kyc.data.model.DocumentExtractData? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        val event = currentState.data.flow.firstOrNull { it.eventId == eventId }

        // ========== IMPRIMIR JSON COMPLETO DEL EVENTO DOCUMENT EXTRACT ==========
        android.util.Log.d("SessionDetailViewModel", "========== EVENTO DOCUMENT EXTRACT COMPLETO (JSON) ==========")
        val eventJson = com.google.gson.Gson().toJson(event)
        android.util.Log.d("SessionDetailViewModel", eventJson)
        android.util.Log.d("SessionDetailViewModel", "===============================================")

        // Los datos de OCR están en flow[0].meta.extra.evaluation
        val extractResource = event?.flow?.firstOrNull()
        val evaluation = extractResource?.meta?.extra?.evaluation

        // Buscar el recurso "ocr-photos" que contiene las URLs de las imágenes
        val photosResource = event?.flow?.firstOrNull { it.resource == "ocr-photos" }
        val photosEvaluation = photosResource?.meta?.extra?.evaluation

        // Extraer URLs de imágenes del recurso "ocr-photos"
        val documentFrontUrl = photosEvaluation?.get("documentFront") as? String
        val documentBackUrl = photosEvaluation?.get("documentBack") as? String
        val faceUrl = photosEvaluation?.get("face") as? String

        android.util.Log.d("SessionDetailViewModel", "Document Front URL: $documentFrontUrl")
        android.util.Log.d("SessionDetailViewModel", "Document Back URL: $documentBackUrl")
        android.util.Log.d("SessionDetailViewModel", "Face URL: $faceUrl")

        // Extraer estados OCR del recurso "ocr-states"
        val statesResource = event?.flow?.firstOrNull { it.resource == "ocr-states" }
        val statesExtra = statesResource?.meta?.extra
        val statesEvaluation = statesExtra?.evaluation
        val statesThresholds = statesExtra?.thresholds
        val statesValidation = statesExtra?.validation

        android.util.Log.d("SessionDetailViewModel", "OCR States Evaluation: $statesEvaluation")
        android.util.Log.d("SessionDetailViewModel", "OCR States Thresholds: $statesThresholds")
        android.util.Log.d("SessionDetailViewModel", "OCR States Validation: $statesValidation")

        // Combinar evaluation de ocr-document con estados de ocr-states
        val combinedEvaluation = if (evaluation != null) {
            val combined = evaluation.toMutableMap()
            // Agregar estados OCR si existen
            if (statesEvaluation != null) {
                combined["ocr-states"] = statesEvaluation
            }
            combined
        } else null

        // Convertir todos los datos de evaluation a JSON para parsear en el Activity
        val extractedDataJson = if (combinedEvaluation != null) {
            com.google.gson.Gson().toJson(combinedEvaluation)
        } else null

        // Convertir thresholds y validation a JSON
        val thresholdsJson = if (statesThresholds != null) {
            com.google.gson.Gson().toJson(statesThresholds)
        } else null

        val validationJson = if (statesValidation != null) {
            com.google.gson.Gson().toJson(statesValidation)
        } else null

        // Extraer tiempo de procesamiento desde meta.processTime
        val processTime = extractResource?.meta?.processTime
        val processingTime = if (processTime != null) {
            val seconds = processTime / 1000.0
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        return ai.jaak.kyc.data.model.DocumentExtractData(
            eventId = eventId,
            documentFrontUrl = documentFrontUrl,
            documentBackUrl = documentBackUrl,
            faceUrl = faceUrl, // URL de la cara (no Base64)
            extractedData = extractedDataJson, // JSON con todos los datos extraídos
            processingTime = processingTime,
            thresholdsData = thresholdsJson,
            validationData = validationJson
        )
    }

    /**
     * Obtiene los datos de verificación de documento
     */
    fun getDocumentDetailData(eventId: String): ai.jaak.kyc.data.model.DocumentDetailData? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        val event = currentState.data.flow.firstOrNull { it.eventId == eventId }
        val summary = currentState.data.summary

        // ========== IMPRIMIR JSON COMPLETO DEL EVENTO DOCUMENT DETAIL ==========
        android.util.Log.d("SessionDetailViewModel", "========== EVENTO DOCUMENT DETAIL COMPLETO (JSON) ==========")
        val eventJson = com.google.gson.Gson().toJson(event)
        android.util.Log.d("SessionDetailViewModel", eventJson)
        android.util.Log.d("SessionDetailViewModel", "===============================================")

        // Extraer URLs de imágenes del request.meta.request
        val requestData = event?.request?.meta?.request

        // Log para ver qué keys tiene el request
        android.util.Log.d("SessionDetailViewModel", "requestData keys: ${requestData?.keys}")
        android.util.Log.d("SessionDetailViewModel", "requestData: $requestData")

        val documentFrontUrl = requestData?.get("document") as? String
            ?: requestData?.get("documentFront") as? String
        val documentBackUrl = requestData?.get("documentBack") as? String

        // Buscar el recurso "liveness-document" que contiene los datos de verificación
        val livenessDocResource = event?.flow?.firstOrNull { it.resource == "liveness-document" }
        val extra = livenessDocResource?.meta?.extra
        val evaluation = extra?.evaluation
        val thresholds = extra?.thresholds
        val validation = extra?.validation

        // Extraer score
        val score = (evaluation?.get("score") as? Number)?.toDouble()

        // Convertir datos a JSON
        val verificationDataJson = if (evaluation != null) {
            com.google.gson.Gson().toJson(evaluation)
        } else null

        val thresholdsDataJson = if (thresholds != null) {
            com.google.gson.Gson().toJson(thresholds)
        } else null

        val validationDataJson = if (validation != null) {
            com.google.gson.Gson().toJson(validation)
        } else null

        // Extraer tiempo de procesamiento desde meta.processTime
        val processTime = livenessDocResource?.meta?.processTime
        val processingTime = if (processTime != null) {
            val seconds = processTime / 1000.0
            String.format("%.2fs", seconds)
        } else {
            "N/A"
        }

        return ai.jaak.kyc.data.model.DocumentDetailData(
            eventId = eventId,
            documentFrontUrl = documentFrontUrl,
            documentBackUrl = documentBackUrl,
            verificationScore = score ?: summary.scores?.document,
            verificationData = verificationDataJson,
            thresholdsData = thresholdsDataJson,
            validationData = validationDataJson,
            processingTime = processingTime
        )
    }

    /**
     * Obtiene los datos de blacklist - TODOS los eventos "verify-blacklist" agrupados
     */
    fun getBlacklistData(eventId: String): ai.jaak.kyc.data.model.BlacklistDetailData? {
        val currentState = _state.value
        if (currentState !is SessionDetailState.Success) return null

        // Obtener TODOS los eventos "verify-blacklist" (los 5)
        val allBlacklistEvents = currentState.data.flow.filter {
            it.action.lowercase() == "verify-blacklist"
        }

        android.util.Log.d("SessionDetailViewModel", "========== BLACKLIST EVENTS ==========")
        android.util.Log.d("SessionDetailViewModel", "Total blacklist events found: ${allBlacklistEvents.size}")

        // Combinar todos los resultados de todos los eventos blacklist
        val allBlacklistResults = mutableListOf<Map<String, Any?>>()
        var totalProcessingTime = 0.0
        var processCount = 0

        allBlacklistEvents.forEachIndexed { index, event ->
            android.util.Log.d("SessionDetailViewModel", "Processing blacklist event ${index + 1}: ${event.eventId}")

            // Cada evento tiene su propio array de recursos (flow)
            event.flow?.forEach { resource ->
                val resourceName = resource.resource ?: ""
                val status = resource.status ?: ""
                val evaluation = resource.meta?.extra?.evaluation as? Map<*, *>

                android.util.Log.d("SessionDetailViewModel", "  Resource: $resourceName, Status: $status")

                if (resourceName.endsWith("-blacklist")) {
                    // Extraer tiempo de procesamiento desde meta.processTime
                    val processTime = resource.meta?.processTime
                    val processingTimeStr = if (processTime != null) {
                        val seconds = processTime / 1000.0
                        totalProcessingTime += seconds
                        processCount++
                        String.format("%.2fs", seconds)
                    } else {
                        "N/A"
                    }

                    allBlacklistResults.add(mapOf(
                        "eventId" to event.eventId,
                        "resourceName" to resourceName,
                        "status" to status,
                        "evaluation" to evaluation,
                        "createdAt" to event.createdAt,
                        "processingTime" to processingTimeStr
                    ))
                }
            }
        }

        android.util.Log.d("SessionDetailViewModel", "Total blacklist results: ${allBlacklistResults.size}")

        // Calcular tiempo promedio si hay múltiples checks
        val processingTime = if (processCount > 0) {
            val avgTime = totalProcessingTime / processCount
            String.format("%.2fs", avgTime)
        } else {
            "N/A"
        }

        return ai.jaak.kyc.data.model.BlacklistDetailData(
            eventId = eventId, // Se mantiene el eventId del primero para referencia
            blacklistResults = if (allBlacklistResults.isNotEmpty()) {
                com.google.gson.Gson().toJson(allBlacklistResults)
            } else null,
            processingTime = processingTime
        )
    }
}

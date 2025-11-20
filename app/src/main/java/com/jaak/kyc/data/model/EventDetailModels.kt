package com.jaak.kyc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Datos para mostrar en LivenessDetailActivity
 */
@Parcelize
data class LivenessDetailData(
    val eventId: String,
    val bestFrameUrl: String?, // URL de la mejor foto
    val videoUrl: String?, // URL del video de liveness
    val livenessScore: Double?,
    val processingTime: String?
) : Parcelable

/**
 * Datos para mostrar en OtoDetailActivity (Comparación 1:1)
 */
@Parcelize
data class OtoDetailData(
    val eventId: String,
    val image1Url: String?, // URL de la primera imagen (documento)
    val image2Url: String?, // URL de la segunda imagen (liveness)
    val comparisonScore: Double?,
    val distance: Double?,
    val processingTime: String?, // Tiempo de procesamiento de comparación 1:1
    // Datos de accesorios y calidad en formato JSON
    val accessoriesData: String?, // JSON con accesorios de ambas imágenes
    val qualityData: String?, // JSON con calidad de ambas imágenes
    // Tiempos de procesamiento por sección
    val accessoriesProcessingTime: String?,
    val qualityProcessingTime: String?
) : Parcelable

/**
 * Datos para mostrar en DocumentExtractActivity
 */
@Parcelize
data class DocumentExtractData(
    val eventId: String,
    val documentFrontUrl: String?, // URL del frente del documento
    val documentBackUrl: String?, // URL del reverso del documento
    val faceUrl: String?, // URL de la cara extraída del documento
    val extractedData: String?, // JSON con datos extraídos (nombre, apellido, etc)
    val processingTime: String?,
    val thresholdsData: String?, // JSON con umbrales esperados de ocr-states
    val validationData: String? // JSON con resultados de validación de ocr-states
) : Parcelable

/**
 * Datos para mostrar en DocumentDetailActivity (Verificación de documento)
 */
@Parcelize
data class DocumentDetailData(
    val eventId: String,
    val documentFrontUrl: String?, // URL del frente del documento
    val documentBackUrl: String?, // URL del reverso del documento
    val verificationScore: Double?, // Score de verificación
    val verificationData: String?, // JSON con datos de evaluación
    val thresholdsData: String?, // JSON con umbrales esperados
    val validationData: String?, // JSON con resultados de validación
    val processingTime: String?
) : Parcelable

/**
 * Datos para mostrar en BlacklistDetailActivity
 */
@Parcelize
data class BlacklistDetailData(
    val eventId: String,
    val blacklistResults: String?, // JSON con resultados de blacklist
    val processingTime: String?
) : Parcelable

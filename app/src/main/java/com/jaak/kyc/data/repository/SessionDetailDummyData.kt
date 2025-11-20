package com.jaak.kyc.data.repository

import com.jaak.kyc.data.model.api.*

/**
 * Datos DUMMY para probar la UI de detalle de sesión
 * Basado en la documentación del API
 */
object SessionDetailDummyData {

    fun getDummySessionDetail(): SessionDetailResponse {
        return SessionDetailResponse(
            session = SessionDetailInfo(
                sessionID = "507f1f77bcf86cd799439011",
                contactName = "Juan Carlos Pérez González",
                verificationType = "email",
                verification = SessionDetailVerification(
                    email = "juan.perez@email.com",
                    whatsapp = "+525551234567",
                    whatsappDetail = SessionDetailChannelDetail(
                        status = "verified",
                        message = "Verification successful"
                    ),
                    sms = "+525551234567",
                    smsDetail = SessionDetailChannelDetail(
                        status = "verified",
                        message = "Verification successful"
                    )
                ),
                flowName = "KYC Flow Completo",
                consent = "2024-01-15T10:30:00Z",
                createdAt = "2024-01-15T10:30:00Z",
                startDate = "2024-01-15T10:30:00Z",
                updateDate = "2024-01-15T10:35:00Z",
                endDate = "2024-01-15T10:35:00Z",
                origin = "web",
                score = 0.92,
                status = "pass",
                shortKey = "ABC123DEF",
                validation = "automatic",
                flowType = "kyc",
                rigelUrl = "https://rigel.jaak.com/session/507f1f77bcf86cd799439011",
                location = SessionDetailLocation(
                    city = "Ciudad de México",
                    state = "CDMX",
                    country = "MEX",
                    latitude = 19.4326,
                    longitude = -99.1332
                ),
                statusDetail = SessionDetailStatusDetail(
                    state = "completed",
                    stage = "approved"
                )
            ),
            summary = SessionDetailSummary(
                photo = getDummyBase64Photo(), // Base64 de imagen dummy
                totalTime = 3.5,
                name = "Juan Carlos",
                lastName = "Pérez González",
                scores = SessionDetailScores(
                    liveness = 0.95,
                    document = 0.88,
                    oneToOne = 0.93,
                    total = 0.92,
                    status = "pass"
                ),
                rigelFullVideo = "https://storage.googleapis.com/dummy-video.mp4"
            ),
            flow = listOf(
                // Evento 1: Document Process
                SessionDetailFlowEvent(
                    action = "document-process",
                    createdAt = "2024-01-15T10:31:00Z",
                    eventId = "event-123",
                    flow = listOf(
                        SessionDetailFlowResource(
                            resource = "document-ocr",
                            score = 0.88,
                            status = "completed",
                            meta = SessionDetailResourceMeta(
                                time = "2024-01-15T10:31:00Z",
                                processTime = 4518.0,
                                extra = SessionDetailResourceExtra(
                                    evaluation = mapOf(
                                        "document_type" to "INE",
                                        "confidence" to 0.88,
                                        "is_valid" to true,
                                        "expiry_date" to "2028-12-31"
                                    ),
                                    thresholds = null,
                                    validation = null
                                )
                            )
                        )
                    ),
                    meta = SessionDetailFlowMeta(
                        extra = mapOf(
                            "product" to "document",
                            "company_id" to "507f1f77bcf86cd799439012"
                        )
                    ),
                    request = SessionDetailFlowRequest(
                        id = "507f1f77bcf86cd799439011",
                        ip = "192.168.1.100",
                        path = "/api/v4/document/process",
                        method = "POST",
                        meta = SessionDetailRequestMeta(
                            request = mapOf("document" to "[FILTERED]"),
                            response = mapOf(
                                "confidence" to 0.88,
                                "document_type" to "INE"
                            )
                        )
                    )
                ),
                // Evento 2: Liveness Process
                SessionDetailFlowEvent(
                    action = "liveness-process",
                    createdAt = "2024-01-15T10:33:00Z",
                    eventId = "event-124",
                    flow = listOf(
                        SessionDetailFlowResource(
                            resource = "liveness",
                            score = 0.95,
                            status = "completed",
                            meta = SessionDetailResourceMeta(
                                time = "2024-01-15T10:33:00Z",
                                processTime = 1318.641,
                                extra = SessionDetailResourceExtra(
                                    evaluation = mapOf(
                                        "is_live" to true,
                                        "confidence" to 0.95,
                                        "quality" to "high"
                                    ),
                                    thresholds = null,
                                    validation = null
                                )
                            )
                        )
                    ),
                    meta = SessionDetailFlowMeta(
                        extra = mapOf(
                            "product" to "liveness",
                            "company_id" to "507f1f77bcf86cd799439012"
                        )
                    ),
                    request = SessionDetailFlowRequest(
                        id = "507f1f77bcf86cd799439011",
                        ip = "192.168.1.100",
                        path = "/api/v1/liveness/process",
                        method = "POST",
                        meta = SessionDetailRequestMeta(
                            request = mapOf("video" to "[FILTERED]"),
                            response = mapOf(
                                "is_live" to true,
                                "confidence" to 0.95
                            )
                        )
                    )
                ),
                // Evento 3: Blacklist Check (Listas Oficiales)
                SessionDetailFlowEvent(
                    action = "blacklist-check",
                    createdAt = "2024-01-15T10:34:00Z",
                    eventId = "event-125",
                    flow = listOf(
                        SessionDetailFlowResource(
                            resource = "blacklist",
                            score = 1.0,
                            status = "completed",
                            meta = SessionDetailResourceMeta(
                                time = "2024-01-15T10:34:00Z",
                                processTime = 412.7,
                                extra = SessionDetailResourceExtra(
                                    evaluation = mapOf(
                                        "found_in_risk_lists" to false,
                                        "found_in_validation_lists" to true,
                                        "lists_checked" to listOf("OFAC", "Interpol", "INE", "CURP")
                                    ),
                                    thresholds = null,
                                    validation = null
                                )
                            )
                        )
                    ),
                    meta = SessionDetailFlowMeta(
                        extra = mapOf(
                            "product" to "blacklist",
                            "company_id" to "507f1f77bcf86cd799439012"
                        )
                    ),
                    request = SessionDetailFlowRequest(
                        id = "507f1f77bcf86cd799439011",
                        ip = "192.168.1.100",
                        path = "/api/v2/blacklist/investigate",
                        method = "POST",
                        meta = SessionDetailRequestMeta(
                            request = mapOf("name" to "Juan Carlos Pérez González"),
                            response = mapOf(
                                "found_in_risk_lists" to false,
                                "found_in_validation_lists" to true
                            )
                        )
                    )
                ),
                // Evento 4: One-to-One Process
                SessionDetailFlowEvent(
                    action = "oto-process",
                    createdAt = "2024-01-15T10:35:00Z",
                    eventId = "event-126",
                    flow = listOf(
                        SessionDetailFlowResource(
                            resource = "one-to-one",
                            score = 0.93,
                            status = "completed",
                            meta = SessionDetailResourceMeta(
                                time = "2024-01-15T10:35:00Z",
                                processTime = 890.5,
                                extra = SessionDetailResourceExtra(
                                    evaluation = mapOf(
                                        "match" to true,
                                        "confidence" to 0.93,
                                        "similarity" to 0.95
                                    ),
                                    thresholds = null,
                                    validation = null
                                )
                            )
                        )
                    ),
                    meta = SessionDetailFlowMeta(
                        extra = mapOf(
                            "product" to "oto",
                            "company_id" to "507f1f77bcf86cd799439012"
                        )
                    ),
                    request = SessionDetailFlowRequest(
                        id = "507f1f77bcf86cd799439011",
                        ip = "192.168.1.100",
                        path = "/api/v2/oto/verify",
                        method = "POST",
                        meta = SessionDetailRequestMeta(
                            request = mapOf(
                                "selfie" to "[FILTERED]",
                                "document_photo" to "[FILTERED]"
                            ),
                            response = mapOf(
                                "match" to true,
                                "confidence" to 0.93
                            )
                        )
                    )
                ),
                // Evento 5: Document Verify (Verificación de Documento)
                SessionDetailFlowEvent(
                    action = "document-process",
                    createdAt = "2024-01-15T10:32:00Z",
                    eventId = "event-127",
                    flow = listOf(
                        SessionDetailFlowResource(
                            resource = "document-verify",
                            score = 0.91,
                            status = "completed",
                            meta = SessionDetailResourceMeta(
                                time = "2024-01-15T10:32:00Z",
                                processTime = 2150.3,
                                extra = SessionDetailResourceExtra(
                                    evaluation = mapOf(
                                        "is_valid" to true,
                                        "has_security_features" to true,
                                        "quality_score" to 0.91,
                                        "hands_detected" to true,
                                        "document_liveness" to true
                                    ),
                                    thresholds = null,
                                    validation = null
                                )
                            )
                        )
                    ),
                    meta = SessionDetailFlowMeta(
                        extra = mapOf(
                            "product" to "document",
                            "company_id" to "507f1f77bcf86cd799439012"
                        )
                    ),
                    request = SessionDetailFlowRequest(
                        id = "507f1f77bcf86cd799439011",
                        ip = "192.168.1.100",
                        path = "/api/v4/document/verify",
                        method = "POST",
                        meta = SessionDetailRequestMeta(
                            request = mapOf("document" to "[FILTERED]"),
                            response = mapOf(
                                "is_valid" to true,
                                "quality_score" to 0.91
                            )
                        )
                    )
                )
            )
        )
    }

    /**
     * Base64 de una imagen pequeña de prueba (1x1 pixel transparente)
     * En producción, esto vendrá de la API
     */
    private fun getDummyBase64Photo(): String {
        // Imagen PNG 1x1 transparente en base64
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    }
}

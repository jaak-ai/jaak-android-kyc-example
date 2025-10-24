package com.jaak.kyc.data.repository

import android.util.Log
import com.jaak.kyc.data.mapper.SessionListMapper
import com.jaak.kyc.data.model.KycSessionItem
import com.jaak.kyc.data.network.JaakDBApiClient
import com.jaak.kyc.utils.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycSessionsRepository @Inject constructor(
    private val apiClient: JaakDBApiClient,
    private val profileManager: ProfileManager
) {

    suspend fun getSessions(
        page: Int = 1,
        limit: Int = 20,
        searchQuery: String? = null,
        flowName: String? = null,
        minCreatedAt: String? = null,
        maxCreatedAt: String? = null
    ): Result<SessionsPage> {
        return try {
            // Usar API Key de larga duración en lugar del accessToken de sesión
            // Fallback: Si no existe apiKey, usar accessToken (para usuarios ya logueados)
            val apiKey = profileManager.getApiKey() ?: profileManager.getAccessToken()
            if (apiKey.isNullOrEmpty()) {
                Log.e("KycSessionsRepository", "API Key no disponible")
                return Result.failure(Exception("API Key no disponible"))
            }

            Log.d("KycSessionsRepository", "========== GET SESSIONS REQUEST ==========")
            Log.d("KycSessionsRepository", "URL: GET /api/v1/kyc/session")
            Log.d("KycSessionsRepository", "Headers: {")
            Log.d("KycSessionsRepository", "  Authorization: Bearer $apiKey")
            Log.d("KycSessionsRepository", "  Accept-Language: es")
            Log.d("KycSessionsRepository", "}")
            Log.d("KycSessionsRepository", "Query Parameters: {")
            Log.d("KycSessionsRepository", "  page: $page")
            Log.d("KycSessionsRepository", "  limit: $limit")
            Log.d("KycSessionsRepository", "  searchQuery (unified): $searchQuery")
            Log.d("KycSessionsRepository", "  flowName: $flowName")
            Log.d("KycSessionsRepository", "  minCreatedAt: $minCreatedAt")
            Log.d("KycSessionsRepository", "  maxCreatedAt: $maxCreatedAt")
            Log.d("KycSessionsRepository", "}")
            Log.d("KycSessionsRepository", "==========================================")

            // Para búsqueda unificada: obtener más resultados y filtrar localmente
            // Multiplicamos el límite por 2 para tener más registros que filtrar, manteniendo la paginación
            val shouldFilterLocally = !searchQuery.isNullOrEmpty()
            val fetchLimit = if (shouldFilterLocally) limit * 2 else limit

            val response = apiClient.getSessionListApi(
                auth = "Bearer $apiKey",
                language = "es",
                id = null, // No filtrar por ID específico en el servidor
                shortKey = null, // Filtraremos localmente
                contactName = null, // Filtraremos localmente
                flowName = flowName, // Mantener filtro de flowName si existe
                limit = fetchLimit,
                page = page,
                minCreatedAt = minCreatedAt,
                maxCreatedAt = maxCreatedAt
            )

            Log.d("KycSessionsRepository", "========== GET SESSIONS RESPONSE ==========")
            Log.d("KycSessionsRepository", "Status Code: ${response.code()}")
            Log.d("KycSessionsRepository", "Status Message: ${response.message()}")

            if (response.isSuccessful) {
                val sessionListResponse = response.body()
                if (sessionListResponse != null) {
                    Log.d("KycSessionsRepository", "Response Body: {")
                    Log.d("KycSessionsRepository", "  totalDocs: ${sessionListResponse.totalDocs}")
                    Log.d("KycSessionsRepository", "  limit: ${sessionListResponse.limit}")
                    Log.d("KycSessionsRepository", "  totalPages: ${sessionListResponse.totalPages}")
                    Log.d("KycSessionsRepository", "  page: ${sessionListResponse.page}")
                    Log.d("KycSessionsRepository", "  hasPrevPage: ${sessionListResponse.prevPage}")
                    Log.d("KycSessionsRepository", "  hasNextPage: ${sessionListResponse.nextPage}")
                    Log.d("KycSessionsRepository", "  docList: [")
                    sessionListResponse.docList.forEachIndexed { index, item ->
                        Log.d("KycSessionsRepository", "    [$index] {")
                        Log.d("KycSessionsRepository", "      sessionID: ${item.sessionID}")
                        Log.d("KycSessionsRepository", "      shortKey: ${item.shortKey}")
                        Log.d("KycSessionsRepository", "      contactName: ${item.contactName}")
                        Log.d("KycSessionsRepository", "      flowName: ${item.flowName}")
                        Log.d("KycSessionsRepository", "      status: ${item.status}")
                        Log.d("KycSessionsRepository", "      score: ${item.score}")
                        Log.d("KycSessionsRepository", "      createdAt: ${item.createdAt}")
                        Log.d("KycSessionsRepository", "      statusDetail: {")
                        Log.d("KycSessionsRepository", "        stage: ${item.statusDetail?.stage}")
                        Log.d("KycSessionsRepository", "        state: ${item.statusDetail?.state}")
                        Log.d("KycSessionsRepository", "      }")
                        Log.d("KycSessionsRepository", "    }")
                    }
                    Log.d("KycSessionsRepository", "  ]")
                    Log.d("KycSessionsRepository", "}")
                    Log.d("KycSessionsRepository", "==========================================")

                    val sessions = sessionListResponse.docList.map { item ->
                        SessionListMapper.mapToKycSessionItem(item)
                    }

                    // Filtrado local unificado: buscar en shortKey, contactName, flowName, sessionID
                    val filteredSessions = if (shouldFilterLocally && searchQuery != null) {
                        val query = searchQuery.lowercase()
                        Log.d("KycSessionsRepository", "Aplicando filtro local con query: '$query'")

                        val filtered = sessions.filter { session ->
                            session.shortkey.lowercase().contains(query) ||
                            session.userName.lowercase().contains(query) ||
                            session.flowName.lowercase().contains(query) ||
                            session.sessionID.lowercase().contains(query)
                        }

                        Log.d("KycSessionsRepository", "Resultados después de filtro: ${filtered.size} de ${sessions.size}")

                        // Limitar al tamaño original del límite para mantener paginación correcta
                        filtered.take(limit)
                    } else {
                        sessions
                    }

                    // Determinar si hay más páginas: Si obtuvimos resultados completos y el servidor indica más páginas
                    val hasMore = if (shouldFilterLocally) {
                        // Con búsqueda: hay más si obtuvimos el límite completo Y el servidor tiene más
                        filteredSessions.size >= limit && sessionListResponse.nextPage
                    } else {
                        // Sin búsqueda: usar directamente lo que indica el servidor
                        sessionListResponse.nextPage
                    }

                    Result.success(
                        SessionsPage(
                            sessions = filteredSessions,
                            currentPage = sessionListResponse.page,
                            totalPages = sessionListResponse.totalPages,
                            totalDocs = sessionListResponse.totalDocs,
                            hasNextPage = hasMore,
                            hasPrevPage = sessionListResponse.prevPage
                        )
                    )
                } else {
                    Log.e("KycSessionsRepository", "Response body es null")
                    Log.d("KycSessionsRepository", "==========================================")
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("KycSessionsRepository", "Error Body: $errorBody")
                Log.d("KycSessionsRepository", "==========================================")
                Result.failure(Exception("Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("KycSessionsRepository", "Excepción al obtener sesiones: ${e.message}", e)
            Log.d("KycSessionsRepository", "==========================================")
            Result.failure(e)
        }
    }
}

data class SessionsPage(
    val sessions: List<KycSessionItem>,
    val currentPage: Int,
    val totalPages: Int,
    val totalDocs: Int,
    val hasNextPage: Boolean,
    val hasPrevPage: Boolean
)

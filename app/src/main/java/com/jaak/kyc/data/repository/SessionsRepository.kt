package com.jaak.kyc.data.repository

import com.jaak.kyc.data.mapper.SessionMapper
import com.jaak.kyc.data.model.KycSessionItem
import com.jaak.kyc.data.model.api.SessionListResponse
import com.jaak.kyc.data.network.JaakDBService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionsRepository @Inject constructor(
    private val jaakDBService: JaakDBService
) {

    /**
     * Obtiene la lista de sesiones KYC desde la API (GET /api/v1/kyc/session)
     *
     * @param token Bearer token de autenticación
     * @param searchQuery Query de búsqueda (para shortKey o contactName)
     * @param limit Número de elementos por página (default: 20, max: 100)
     * @param page Número de página (default: 1)
     * @return Result con la respuesta de la API
     */
    suspend fun getSessions(
        token: String,
        searchQuery: String? = null,
        limit: Int = 20,
        page: Int = 1
    ): Result<SessionListResponse> {
        return try {
            // Construir el authorization header
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            // Llamar a la API GET
            val response: Response<SessionListResponse> = jaakDBService.getSessionListApi(
                auth = authHeader,
                language = "es",
                shortKey = searchQuery, // Se puede buscar por shortKey
                contactName = searchQuery, // O por nombre de contacto
                limit = limit.coerceIn(1, 100), // Limitar entre 1 y 100
                page = page.coerceAtLeast(1) // Mínimo página 1
            )

            if (response.isSuccessful && response.body() != null) {
                // Log del JSON completo de la respuesta
                android.util.Log.d("SessionsRepository", "========== SESSION LIST JSON ==========")
                android.util.Log.d("SessionsRepository", com.google.gson.Gson().toJson(response.body()))
                android.util.Log.d("SessionsRepository", "======================================")
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Error ${response.code()}: ${response.errorBody()?.string()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene sesiones y las convierte al modelo de UI
     *
     * @param token Bearer token de autenticación
     * @param searchQuery Query de búsqueda
     * @param limit Número de elementos por página
     * @param page Número de página
     * @return Result con lista de KycSessionItem y la respuesta completa para paginación
     */
    suspend fun getSessionsForUI(
        token: String,
        searchQuery: String? = null,
        limit: Int = 20,
        page: Int = 1
    ): Result<Pair<List<KycSessionItem>, SessionListResponse>> {
        return try {
            val result = getSessions(token, searchQuery, limit, page)

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                val sessionItems = SessionMapper.toKycSessionItemList(response.docList)
                Result.success(Pair(sessionItems, response))
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package ai.jaak.kyc.data.repository

import android.util.Log
import ai.jaak.kyc.data.model.api.SessionDetailResponse
import ai.jaak.kyc.data.network.JaakDBApiClient
import ai.jaak.kyc.utils.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDetailRepository @Inject constructor(
    private val apiClient: JaakDBApiClient,
    private val profileManager: ProfileManager
) {

    /**
     * Obtiene el detalle completo de una sesión KYC específica
     *
     * @param sessionId ID de la sesión (shortKey o MongoDB ObjectID)
     * @return Result con la respuesta completa del detalle
     */
    suspend fun getSessionDetail(sessionId: String): Result<SessionDetailResponse> {
        return try {
            val accessToken = profileManager.getAccessToken()
            if (accessToken.isNullOrEmpty()) {
                Log.e("SessionDetailRepository", "Access token no disponible")
                return Result.failure(Exception("Access token no disponible"))
            }

            Log.d("SessionDetailRepository", "========== GET SESSION DETAIL REQUEST ==========")
            Log.d("SessionDetailRepository", "Session ID (MongoDB ObjectID): $sessionId")
            Log.d("SessionDetailRepository", "Endpoint: GET /api/v1/kyc/session/$sessionId")
            Log.d("SessionDetailRepository", "================================================")

            // Llamar a la API
            val response = apiClient.getSessionDetailApi(
                sessionId = sessionId,
                auth = "Bearer $accessToken"
            )

            Log.d("SessionDetailRepository", "========== GET SESSION DETAIL RESPONSE ==========")
            Log.d("SessionDetailRepository", "Status Code: ${response.code()}")
            Log.d("SessionDetailRepository", "Status Message: ${response.message()}")

            if (response.isSuccessful) {
                val sessionDetail = response.body()
                if (sessionDetail != null) {
                    Log.d("SessionDetailRepository", "Response Body: {")
                    Log.d("SessionDetailRepository", "  session: {")
                    Log.d("SessionDetailRepository", "    sessionID: ${sessionDetail.session.sessionID}")
                    Log.d("SessionDetailRepository", "    contactName: ${sessionDetail.session.contactName}")
                    Log.d("SessionDetailRepository", "    status: ${sessionDetail.session.status}")
                    Log.d("SessionDetailRepository", "    score: ${sessionDetail.session.score}")
                    Log.d("SessionDetailRepository", "    flowName: ${sessionDetail.session.flowName}")
                    Log.d("SessionDetailRepository", "    createdAt: ${sessionDetail.session.createdAt}")
                    Log.d("SessionDetailRepository", "    shortKey: ${sessionDetail.session.shortKey}")
                    Log.d("SessionDetailRepository", "  }")
                    Log.d("SessionDetailRepository", "  summary: {")
                    Log.d("SessionDetailRepository", "    name: ${sessionDetail.summary.name}")
                    Log.d("SessionDetailRepository", "    lastName: ${sessionDetail.summary.lastName}")
                    Log.d("SessionDetailRepository", "    totalTime: ${sessionDetail.summary.totalTime}")
                    Log.d("SessionDetailRepository", "    photo: ${sessionDetail.summary.photo}")
                    Log.d("SessionDetailRepository", "    ========== VIDEO URL ==========")
                    Log.d("SessionDetailRepository", "    rigelFullVideo: ${sessionDetail.summary.rigelFullVideo}")
                    Log.d("SessionDetailRepository", "    Video Type: ${if (sessionDetail.summary.rigelFullVideo.isNullOrEmpty()) "null/empty" else if (sessionDetail.summary.rigelFullVideo!!.startsWith("http")) "URL" else "Unknown format"}")
                    Log.d("SessionDetailRepository", "    ===============================")
                    Log.d("SessionDetailRepository", "    scores: {")
                    Log.d("SessionDetailRepository", "      liveness: ${sessionDetail.summary.scores?.liveness}")
                    Log.d("SessionDetailRepository", "      document: ${sessionDetail.summary.scores?.document}")
                    Log.d("SessionDetailRepository", "      oneToOne: ${sessionDetail.summary.scores?.oneToOne}")
                    Log.d("SessionDetailRepository", "      total: ${sessionDetail.summary.scores?.total}")
                    Log.d("SessionDetailRepository", "      status: ${sessionDetail.summary.scores?.status}")
                    Log.d("SessionDetailRepository", "    }")
                    Log.d("SessionDetailRepository", "  }")
                    Log.d("SessionDetailRepository", "  flow: [")
                    sessionDetail.flow.forEachIndexed { index, event ->
                        Log.d("SessionDetailRepository", "    [$index] {")
                        Log.d("SessionDetailRepository", "      action: ${event.action}")
                        Log.d("SessionDetailRepository", "      createdAt: ${event.createdAt}")
                        Log.d("SessionDetailRepository", "      eventId: ${event.eventId}")
                        Log.d("SessionDetailRepository", "      resources: ${event.flow?.size ?: 0} items")

                        // Loguear recursos con meta completo
                        event.flow?.forEachIndexed { resIndex, resource ->
                            Log.d("SessionDetailRepository", "      resource[$resIndex]: {")
                            Log.d("SessionDetailRepository", "        resource: ${resource.resource}")
                            Log.d("SessionDetailRepository", "        score: ${resource.score}")
                            Log.d("SessionDetailRepository", "        status: ${resource.status}")
                            Log.d("SessionDetailRepository", "        meta.extra.evaluation: ${com.google.gson.Gson().toJson(resource.meta?.extra?.evaluation)}")
                            Log.d("SessionDetailRepository", "      }")
                        }

                        // Loguear meta y request del evento
                        Log.d("SessionDetailRepository", "      event.meta.extra: ${com.google.gson.Gson().toJson(event.meta?.extra)}")
                        Log.d("SessionDetailRepository", "      event.request: ${com.google.gson.Gson().toJson(event.request)}")
                        Log.d("SessionDetailRepository", "    }")
                    }
                    Log.d("SessionDetailRepository", "  ]")
                    Log.d("SessionDetailRepository", "}")
                    Log.d("SessionDetailRepository", "================================================")

                    Result.success(sessionDetail)
                } else {
                    Log.e("SessionDetailRepository", "Response body es null")
                    Log.d("SessionDetailRepository", "================================================")
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("SessionDetailRepository", "Error Body: $errorBody")
                Log.d("SessionDetailRepository", "================================================")
                Result.failure(Exception("Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("SessionDetailRepository", "Excepción al obtener detalle de sesión: ${e.message}", e)
            Log.d("SessionDetailRepository", "================================================")
            Result.failure(e)
        }
    }

}

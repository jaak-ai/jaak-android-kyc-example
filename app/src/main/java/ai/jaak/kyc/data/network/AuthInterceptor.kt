package ai.jaak.kyc.data.network

import android.util.Log
import ai.jaak.kyc.utils.ProfileManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor que:
 * 1. Adjunta el access token a cada request con Authorization: Bearer
 * 2. En respuesta 401, llama al endpoint de refresh (el refreshToken cookie
 *    viaja automáticamente via PersistentCookieJar)
 * 3. Si el refresh falla, notifica al resto de la app para que haga logout
 *
 * Equivalente al performPlatformRequestWithRetry() de iOS.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val profileManager: ProfileManager,
    private val cookieJar: PersistentCookieJar
) : Interceptor {

    // Endpoints que NO deben llevar Authorization header (auth endpoints)
    private val excludedPaths = setOf(
        "api/v1/auth/sign-in",
        "api/v1/auth/refresh-token",
        "api/v1/auth/logout"
    )

    // Listeners para notificar cuando la sesión expira (logout forzado)
    private val sessionExpiredListeners = mutableListOf<() -> Unit>()

    fun addSessionExpiredListener(listener: () -> Unit) {
        sessionExpiredListeners.add(listener)
    }

    fun removeSessionExpiredListener(listener: () -> Unit) {
        sessionExpiredListeners.remove(listener)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // No adjuntar token a endpoints de autenticación
        if (isExcluded(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        // Adjuntar access token
        val accessToken = profileManager.getAccessToken()
        val authenticatedRequest = if (!accessToken.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authenticatedRequest)

        // Verificar si necesitamos refresh de token
        // El servidor puede devolver 401 o 403 con errorCode "0003" para token expirado
        val shouldRefresh = when (response.code) {
            401 -> true
            403 -> isTokenExpiredError(response)
            else -> false
        }

        if (!shouldRefresh) {
            return response
        }

        Log.w("AuthInterceptor", "${response.code} recibido en ${originalRequest.url} — intentando refresh de token")

        val newAccessToken = runBlocking { refreshAccessToken() }

        if (newAccessToken == null) {
            Log.e("AuthInterceptor", "Refresh de token falló — forzando logout")
            notifySessionExpired()
            return response
        }

        Log.d("AuthInterceptor", "Token renovado exitosamente — reintentando request original")

        // Cerrar respuesta 401 anterior
        response.close()

        // Reintentar el request original con el nuevo token
        val retryRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()

        return chain.proceed(retryRequest)
    }

    /**
     * Lee el body del 403 para detectar si es un token expirado (errorCode "0003").
     * Usa peekBody para no consumir el stream — el body sigue disponible aguas abajo.
     */
    private fun isTokenExpiredError(response: Response): Boolean {
        return try {
            // peekBody lee hasta N bytes sin consumir el body original
            val peekedBody = response.peekBody(512)
            val bodyString = peekedBody.string()
            val json = JSONObject(bodyString)
            val errorCode = json.optString("errorCode", "")
            errorCode == "0003"
        } catch (e: Exception) {
            Log.w("AuthInterceptor", "No se pudo parsear body del 403: ${e.message}")
            false
        }
    }

    private fun isExcluded(request: Request): Boolean {
        val path = request.url.encodedPath.trimStart('/')
        return excludedPaths.any { path.startsWith(it) }
    }

    private suspend fun refreshAccessToken(): String? {
        return try {
            // Crear cliente Retrofit temporal sin AuthInterceptor (para evitar ciclo infinito)
            // Pero CON el cookieJar para que el refreshToken cookie se reenvíe automáticamente
            val authUrl = profileManager.getCurrentAuthUrl()
            val okHttpClient = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(authUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val authApi = retrofit.create(JaakDBApiClient::class.java)
            // El backend espera el refreshToken como cookie HTTP-only (no en el body)
            val response = authApi.refreshTokenApi()

            if (response.isSuccessful) {
                val body = response.body()!!
                profileManager.saveAccessToken(body.accessToken)
                Log.d("AuthInterceptor", "Refresh exitoso — nuevo access token guardado")
                body.accessToken
            } else {
                Log.e("AuthInterceptor", "Refresh falló con código: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Excepción durante refresh: ${e.message}", e)
            null
        }
    }

    private fun notifySessionExpired() {
        sessionExpiredListeners.forEach { it.invoke() }
    }
}

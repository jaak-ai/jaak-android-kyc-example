package ai.jaak.kyc.domain.service

import ai.jaak.kyc.data.local.dao.ProcessTokenDao
import ai.jaak.kyc.data.local.entity.KycServiceType
import ai.jaak.kyc.data.local.entity.ProcessTokenEntity
import ai.jaak.kyc.data.local.entity.TokenSource
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessTokenManager @Inject constructor(
    private val processTokenDao: ProcessTokenDao
) {
    
    private val tokenExpirationMs = 24 * 60 * 60 * 1000L // 24 hours
    private val secretKey = "kyc_offline_token_secret" // In production, use secure key management
    
    suspend fun createOfflineToken(processId: String, shortKey: String): ProcessTokenEntity {
        val token = ProcessTokenEntity(
            processId = processId,
            shortKey = shortKey,
            accessToken = generateOfflineAccessToken(processId, shortKey),
            sessionId = generateSessionId(processId),
            tokenExpiresAt = System.currentTimeMillis() + tokenExpirationMs,
            tokenSource = TokenSource.OFFLINE_GENERATED,
            securityHash = generateSecurityHash(processId, shortKey)
        )
        
        processTokenDao.insertToken(token)
        return token
    }
    
    suspend fun createServerToken(
        processId: String,
        shortKey: String,
        accessToken: String,
        refreshToken: String? = null,
        sessionId: String? = null,
        expiresAt: Long? = null
    ): ProcessTokenEntity {
        val token = ProcessTokenEntity(
            processId = processId,
            shortKey = shortKey,
            accessToken = accessToken,
            refreshToken = refreshToken,
            sessionId = sessionId,
            tokenExpiresAt = expiresAt ?: (System.currentTimeMillis() + tokenExpirationMs),
            tokenSource = TokenSource.SERVER_ISSUED,
            securityHash = generateSecurityHash(processId, shortKey)
        )
        
        processTokenDao.insertToken(token)
        return token
    }
    
    suspend fun getValidToken(processId: String): ProcessTokenEntity? {
        val token = processTokenDao.getTokenByProcessId(processId)
        return if (token != null && isTokenValid(token)) {
            token
        } else {
            null
        }
    }
    
    suspend fun refreshToken(processId: String): TokenRefreshResult {
        val currentToken = processTokenDao.getTokenByProcessId(processId)
            ?: return TokenRefreshResult.Error("No token found for process")
            
        return when (currentToken.tokenSource) {
            TokenSource.SERVER_ISSUED -> {
                if (currentToken.refreshToken != null) {
                    // In a real implementation, call the server refresh endpoint
                    val newAccessToken = generateOfflineAccessToken(processId, currentToken.shortKey)
                    val updatedToken = currentToken.copy(
                        accessToken = newAccessToken,
                        tokenRefreshedAt = System.currentTimeMillis(),
                        tokenSource = TokenSource.REFRESHED
                    )
                    processTokenDao.updateToken(updatedToken)
                    TokenRefreshResult.Success(updatedToken)
                } else {
                    TokenRefreshResult.Error("No refresh token available")
                }
            }
            TokenSource.OFFLINE_GENERATED -> {
                // Generate new offline token
                val newToken = generateOfflineAccessToken(processId, currentToken.shortKey)
                val updatedToken = currentToken.copy(
                    accessToken = newToken,
                    tokenRefreshedAt = System.currentTimeMillis(),
                    tokenExpiresAt = System.currentTimeMillis() + tokenExpirationMs
                )
                processTokenDao.updateToken(updatedToken)
                TokenRefreshResult.Success(updatedToken)
            }
            else -> {
                TokenRefreshResult.Error("Cannot refresh token of type: ${currentToken.tokenSource}")
            }
        }
    }
    
    suspend fun validateToken(processId: String): Boolean {
        val token = processTokenDao.getTokenByProcessId(processId)
        return if (token != null) {
            val isValid = isTokenValid(token)
            if (token.isTokenValid != isValid) {
                processTokenDao.updateTokenValidity(processId, isValid)
            }
            isValid
        } else {
            false
        }
    }
    
    suspend fun revokeToken(processId: String) {
        processTokenDao.updateTokenValidity(processId, false)
    }
    
    suspend fun hasPermissionForService(processId: String, serviceType: KycServiceType): Boolean {
        val token = getValidToken(processId)
        return token?.allowedServices?.contains(serviceType) ?: false
    }
    
    suspend fun getTokenForAuthentication(processId: String): String? {
        val token = getValidToken(processId)
        return token?.let { "${it.tokenType} ${it.accessToken}" }
    }
    
    fun getTokenFlow(processId: String): Flow<ProcessTokenEntity?> {
        return processTokenDao.getTokenByProcessIdFlow(processId)
    }
    
    suspend fun cleanupExpiredTokens() {
        val currentTime = System.currentTimeMillis()
        val expiredTokens = processTokenDao.getExpiredTokens(currentTime)
        
        expiredTokens.forEach { token ->
            processTokenDao.updateTokenValidity(token.processId, false)
        }
    }
    
    suspend fun getTokenStatistics(): TokenStatistics {
        val allTokens = processTokenDao.getAllValidTokens()
        val expiredTokens = processTokenDao.getExpiredTokens()
        
        return TokenStatistics(
            totalValidTokens = allTokens.size,
            expiredTokens = expiredTokens.size,
            offlineTokens = allTokens.count { it.tokenSource == TokenSource.OFFLINE_GENERATED },
            serverTokens = allTokens.count { it.tokenSource == TokenSource.SERVER_ISSUED },
            refreshedTokens = allTokens.count { it.tokenSource == TokenSource.REFRESHED }
        )
    }
    
    private fun isTokenValid(token: ProcessTokenEntity): Boolean {
        val currentTime = System.currentTimeMillis()
        
        return token.isTokenValid && 
               (token.tokenExpiresAt == null || token.tokenExpiresAt > currentTime) &&
               token.accessToken?.isNotEmpty() == true
    }
    
    private fun generateOfflineAccessToken(processId: String, shortKey: String): String {
        val timestamp = System.currentTimeMillis()
        val payload = "$processId:$shortKey:$timestamp"
        val hash = generateHmacSha256(payload)
        return "offline_${processId.take(8)}_${hash.take(16)}"
    }
    
    private fun generateSessionId(processId: String): String {
        return "sess_${processId.take(8)}_${UUID.randomUUID().toString().take(12)}"
    }
    
    private fun generateSecurityHash(processId: String, shortKey: String): String {
        val payload = "$processId:$shortKey:${System.currentTimeMillis()}"
        return generateHmacSha256(payload)
    }
    
    private fun generateHmacSha256(payload: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            mac.init(secretKeySpec)
            val hash = mac.doFinal(payload.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString().replace("-", "")
        }
    }
}

sealed class TokenRefreshResult {
    data class Success(val token: ProcessTokenEntity) : TokenRefreshResult()
    data class Error(val message: String) : TokenRefreshResult()
}

data class TokenStatistics(
    val totalValidTokens: Int,
    val expiredTokens: Int,
    val offlineTokens: Int,
    val serverTokens: Int,
    val refreshedTokens: Int
)
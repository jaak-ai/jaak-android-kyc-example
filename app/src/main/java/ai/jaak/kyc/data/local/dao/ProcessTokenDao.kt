package ai.jaak.kyc.data.local.dao

import androidx.room.*
import ai.jaak.kyc.data.local.entity.ProcessTokenEntity
import ai.jaak.kyc.data.local.entity.TokenSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessTokenDao {
    
    @Query("SELECT * FROM process_tokens WHERE processId = :processId")
    suspend fun getTokenByProcessId(processId: String): ProcessTokenEntity?
    
    @Query("SELECT * FROM process_tokens WHERE processId = :processId")
    fun getTokenByProcessIdFlow(processId: String): Flow<ProcessTokenEntity?>
    
    @Query("SELECT * FROM process_tokens WHERE sessionId = :sessionId")
    suspend fun getTokenBySessionId(sessionId: String): ProcessTokenEntity?
    
    @Query("SELECT * FROM process_tokens WHERE shortKey = :shortKey")
    suspend fun getTokenByShortKey(shortKey: String): ProcessTokenEntity?
    
    @Query("SELECT * FROM process_tokens WHERE isTokenValid = 1")
    suspend fun getAllValidTokens(): List<ProcessTokenEntity>
    
    @Query("SELECT * FROM process_tokens WHERE tokenExpiresAt < :currentTime AND isTokenValid = 1")
    suspend fun getExpiredTokens(currentTime: Long = System.currentTimeMillis()): List<ProcessTokenEntity>
    
    @Query("SELECT * FROM process_tokens WHERE tokenSource = :source")
    suspend fun getTokensBySource(source: TokenSource): List<ProcessTokenEntity>
    
    @Query("SELECT * FROM process_tokens WHERE tokenCreatedAt BETWEEN :startTime AND :endTime")
    suspend fun getTokensInTimeRange(startTime: Long, endTime: Long): List<ProcessTokenEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: ProcessTokenEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<ProcessTokenEntity>)
    
    @Update
    suspend fun updateToken(token: ProcessTokenEntity)
    
    @Query("UPDATE process_tokens SET accessToken = :accessToken, tokenRefreshedAt = :refreshedAt WHERE processId = :processId")
    suspend fun updateAccessToken(processId: String, accessToken: String, refreshedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE process_tokens SET isTokenValid = :isValid, lastValidatedAt = :validatedAt WHERE processId = :processId")
    suspend fun updateTokenValidity(processId: String, isValid: Boolean, validatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE process_tokens SET refreshToken = :refreshToken, tokenRefreshedAt = :refreshedAt WHERE processId = :processId")
    suspend fun updateRefreshToken(processId: String, refreshToken: String, refreshedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE process_tokens SET sessionId = :sessionId WHERE processId = :processId")
    suspend fun updateSessionId(processId: String, sessionId: String)
    
    @Delete
    suspend fun deleteToken(token: ProcessTokenEntity)
    
    @Query("DELETE FROM process_tokens WHERE processId = :processId")
    suspend fun deleteTokenByProcessId(processId: String)
    
    @Query("DELETE FROM process_tokens WHERE isTokenValid = 0")
    suspend fun deleteInvalidTokens()
    
    @Query("DELETE FROM process_tokens WHERE tokenExpiresAt < :currentTime")
    suspend fun deleteExpiredTokens(currentTime: Long = System.currentTimeMillis())
    
    // Security operations
    @Query("SELECT COUNT(*) FROM process_tokens WHERE securityHash = :hash")
    suspend fun countTokensWithHash(hash: String): Int
    
    @Query("UPDATE process_tokens SET securityHash = :hash WHERE processId = :processId")
    suspend fun updateSecurityHash(processId: String, hash: String)
    
    // Cleanup operations
    @Query("DELETE FROM process_tokens WHERE tokenCreatedAt < :cutoffTime")
    suspend fun deleteOldTokens(cutoffTime: Long)
}
package com.jaak.kyc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "process_tokens",
    foreignKeys = [
        ForeignKey(
            entity = KycProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["processId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["processId"], unique = true)]
)
data class ProcessTokenEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val processId: String,
    
    // Authentication tokens
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val sessionId: String? = null,
    val tokenType: String = "Bearer",
    
    // Token lifecycle
    val tokenCreatedAt: Long = System.currentTimeMillis(),
    val tokenExpiresAt: Long? = null,
    val tokenRefreshedAt: Long? = null,
    val isTokenValid: Boolean = true,
    
    // Process-specific identifiers
    val shortKey: String,
    val processIdentifier: String? = null,
    val serverProcessId: String? = null,
    
    // Token scope and permissions
    val tokenScope: List<String> = listOf("kyc:read", "kyc:write"),
    val allowedServices: List<KycServiceType> = KycServiceType.values().toList(),
    
    // Security metadata
    val tokenSource: TokenSource = TokenSource.OFFLINE_GENERATED,
    val securityHash: String? = null,
    val lastValidatedAt: Long? = null
)

enum class TokenSource {
    SERVER_ISSUED,      // Token obtained from server during online session
    OFFLINE_GENERATED,  // Token generated locally for offline mode
    REFRESHED,         // Token obtained through refresh mechanism
    RECOVERED          // Token recovered from previous session
}
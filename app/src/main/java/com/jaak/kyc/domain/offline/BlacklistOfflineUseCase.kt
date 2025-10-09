package com.jaak.kyc.domain.offline

import com.jaak.kyc.data.repository.KycOfflineRepository
import javax.inject.Inject

/**
 * BLACKLIST FUNCTIONALITY REMOVED
 * This class is kept for compatibility but all methods are disabled
 */
class BlacklistOfflineUseCase @Inject constructor(
    private val kycOfflineRepository: KycOfflineRepository
) {
    // All blacklist functionality has been removed
    // This class is kept to avoid breaking dependency injection
}

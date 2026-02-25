package ai.jaak.kyc.di

import android.content.Context
import ai.jaak.kyc.data.local.dao.*
import ai.jaak.kyc.data.network.JaakDBService
import ai.jaak.kyc.data.repository.KycOfflineRepository
import ai.jaak.kyc.data.repository.KycSyncRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideKycOfflineRepository(
        @ApplicationContext context: Context,
        kycProcessDao: KycProcessDao,
        kycSessionDao: KycSessionDao,
        kycVerifyDao: KycVerifyDao,
        kycOcrDao: KycOcrDao,
        kycLivenessDao: KycLivenessDao,
        kycOtoVerifyDao: KycOtoVerifyDao,
        kycFinishDao: KycFinishDao,
        serviceExecutionStateDao: ServiceExecutionStateDao,
        jaakDBService: JaakDBService,
        profileManager: ai.jaak.kyc.utils.ProfileManager,
        gson: Gson
    ): KycOfflineRepository {
        return KycOfflineRepository(
            context = context,
            kycProcessDao = kycProcessDao,
            kycSessionDao = kycSessionDao,
            kycVerifyDao = kycVerifyDao,
            kycOcrDao = kycOcrDao,
            kycLivenessDao = kycLivenessDao,
            kycOtoVerifyDao = kycOtoVerifyDao,
            kycFinishDao = kycFinishDao,
            serviceExecutionStateDao = serviceExecutionStateDao,
            jaakDBService = jaakDBService,
            profileManager = profileManager,
            gson = gson
        )
    }
    
    @Provides
    @Singleton
    fun provideKycSyncRepository(
        kycProcessDao: KycProcessDao,
        kycSessionDao: KycSessionDao,
        kycVerifyDao: KycVerifyDao,
        kycOcrDao: KycOcrDao,
        kycLivenessDao: KycLivenessDao,
        kycOtoVerifyDao: KycOtoVerifyDao,
        kycFinishDao: KycFinishDao,
        jaakDBService: JaakDBService,
        gson: Gson
    ): KycSyncRepository {
        return KycSyncRepository(
            kycProcessDao = kycProcessDao,
            kycSessionDao = kycSessionDao,
            kycVerifyDao = kycVerifyDao,
            kycOcrDao = kycOcrDao,
            kycLivenessDao = kycLivenessDao,
            kycOtoVerifyDao = kycOtoVerifyDao,
            kycFinishDao = kycFinishDao,
            jaakDBService = jaakDBService,
            gson = gson
        )
    }
}
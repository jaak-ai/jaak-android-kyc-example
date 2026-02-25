package ai.jaak.kyc.di

import android.content.Context
import ai.jaak.kyc.data.local.KycDatabase
import ai.jaak.kyc.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideKycDatabase(@ApplicationContext context: Context): KycDatabase {
        return KycDatabase.getInstance(context)
    }
    
    @Provides
    fun provideKycProcessDao(database: KycDatabase): KycProcessDao {
        return database.kycProcessDao()
    }
    
    @Provides
    fun provideKycSessionDao(database: KycDatabase): KycSessionDao {
        return database.kycSessionDao()
    }
    
    @Provides
    fun provideKycVerifyDao(database: KycDatabase): KycVerifyDao {
        return database.kycVerifyDao()
    }
    
    @Provides
    fun provideKycOcrDao(database: KycDatabase): KycOcrDao {
        return database.kycOcrDao()
    }
    
    @Provides
    fun provideKycLivenessDao(database: KycDatabase): KycLivenessDao {
        return database.kycLivenessDao()
    }
    
    @Provides
    fun provideKycOtoVerifyDao(database: KycDatabase): KycOtoVerifyDao {
        return database.kycOtoVerifyDao()
    }
    
    @Provides
    fun provideKycFinishDao(database: KycDatabase): KycFinishDao {
        return database.kycFinishDao()
    }
    
    @Provides
    fun provideServiceExecutionStateDao(database: KycDatabase): ServiceExecutionStateDao {
        return database.serviceExecutionStateDao()
    }
    
    @Provides
    fun provideProcessTokenDao(database: KycDatabase): ProcessTokenDao {
        return database.processTokenDao()
    }
    
    @Provides
    fun provideProcessErrorDao(database: KycDatabase): ProcessErrorDao {
        return database.processErrorDao()
    }
}
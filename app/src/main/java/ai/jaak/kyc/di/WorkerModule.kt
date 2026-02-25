package ai.jaak.kyc.di

import android.content.Context
import androidx.work.WorkManager
import ai.jaak.kyc.domain.service.NotificationScheduler
import ai.jaak.kyc.domain.service.NotificationService
import ai.jaak.kyc.notification.NotificationHelper
import ai.jaak.kyc.work.WorkScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideWorkScheduler(workManager: WorkManager): WorkScheduler {
        return WorkScheduler(workManager)
    }
    
    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context,
        notificationHelper: NotificationHelper
    ): NotificationService {
        return NotificationService(context, notificationHelper)
    }
    
    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context,
        notificationService: NotificationService,
        workManager: WorkManager
    ): NotificationScheduler {
        return NotificationScheduler(context, notificationService, workManager)
    }
}
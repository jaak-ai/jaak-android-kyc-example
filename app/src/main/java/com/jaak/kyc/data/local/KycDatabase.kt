package com.jaak.kyc.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.jaak.kyc.data.local.dao.*
import com.jaak.kyc.data.local.entity.*

@Database(
    entities = [
        KycProcessEntity::class,
        KycSessionEntity::class,
        KycVerifyEntity::class,
        KycOcrEntity::class,
        KycLivenessEntity::class,
        KycOtoVerifyEntity::class,
        KycFinishEntity::class,
        ServiceExecutionState::class,
        ProcessTokenEntity::class,
        ProcessErrorEntity::class
    ],
    version = 6, // Incrementada por agregado de ocrResponseJson en KycOcrEntity
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KycDatabase : RoomDatabase() {
    
    abstract fun kycProcessDao(): KycProcessDao
    abstract fun kycSessionDao(): KycSessionDao
    abstract fun kycVerifyDao(): KycVerifyDao
    abstract fun kycOcrDao(): KycOcrDao
    abstract fun kycLivenessDao(): KycLivenessDao
    abstract fun kycOtoVerifyDao(): KycOtoVerifyDao
    abstract fun kycFinishDao(): KycFinishDao
    abstract fun serviceExecutionStateDao(): ServiceExecutionStateDao
    abstract fun processTokenDao(): ProcessTokenDao
    abstract fun processErrorDao(): ProcessErrorDao
    
    companion object {
        private const val DATABASE_NAME = "kyc_database"
        
        @Volatile
        private var INSTANCE: KycDatabase? = null
        
        fun getInstance(context: Context): KycDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KycDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * For testing purposes only
         */
        fun getInMemoryDatabase(context: Context): KycDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                KycDatabase::class.java
            ).build()
        }
        
        /**
         * Close database instance
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
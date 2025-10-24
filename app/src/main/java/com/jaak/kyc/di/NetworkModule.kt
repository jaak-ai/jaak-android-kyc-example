package com.jaak.kyc.di

import android.app.Application
import android.content.Context
import com.jaak.kyc.BuildConfig
import com.jaak.kyc.data.network.JaakDBApiClient
import com.jaak.kyc.domain.service.NetworkConnectivityService
import com.jaak.kyc.utils.ProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideRetrofit(profileManager: ProfileManager): Retrofit {
        // Usar URL dinámica según el perfil seleccionado
        val baseUrl = profileManager.getCurrentBaseUrl()
        android.util.Log.d("NetworkModule", "Using API Base URL: $baseUrl (Profile: ${profileManager.getCurrentProfile()})")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    @javax.inject.Named("AuthRetrofit")
    fun provideAuthRetrofit(profileManager: ProfileManager): Retrofit {
        // Usar URL de autenticación según el perfil seleccionado
        val authUrl = profileManager.getCurrentAuthUrl()
        android.util.Log.d("NetworkModule", "Using Auth URL: $authUrl (Profile: ${profileManager.getCurrentProfile()})")

        return Retrofit.Builder()
            .baseUrl(authUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideJaakApiClient(retrofit: Retrofit): JaakDBApiClient {
        return retrofit.create(JaakDBApiClient::class.java)
    }

    @Singleton
    @Provides
    @javax.inject.Named("AuthService")
    fun provideAuthApiClient(@javax.inject.Named("AuthRetrofit") retrofit: Retrofit): JaakDBApiClient {
        return retrofit.create(JaakDBApiClient::class.java)
    }
    @Singleton
    @Provides
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Singleton
    @Provides
    fun provideNetworkConnectivityService(@ApplicationContext context: Context): NetworkConnectivityService {
        return NetworkConnectivityService(context)
    }

}
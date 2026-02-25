package ai.jaak.kyc.di

import android.app.Application
import android.content.Context
import ai.jaak.kyc.BuildConfig
import ai.jaak.kyc.data.network.JaakDBApiClient
import ai.jaak.kyc.domain.service.NetworkConnectivityService
import ai.jaak.kyc.utils.ProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ============================================================
    // TEMPORAL: Configuración para debugging con Proxyman
    // IMPORTANTE: Remover antes de producción
    // ============================================================
    private fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(loggingInterceptor)
    }
    // ============================================================

    @Singleton
    @Provides
    fun provideRetrofit(profileManager: ProfileManager): Retrofit {
        // Usar URL dinámica según el perfil seleccionado
        val baseUrl = profileManager.getCurrentBaseUrl()
        android.util.Log.d("NetworkModule", "Using API Base URL: $baseUrl (Profile: ${profileManager.getCurrentProfile()})")

        // TEMPORAL: Usar cliente inseguro para Proxyman
        val okHttpClient = getUnsafeOkHttpClient()
            .readTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
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

        // TEMPORAL: Usar cliente inseguro para Proxyman
        val okHttpClient = getUnsafeOkHttpClient()
            .readTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(authUrl)
            .client(okHttpClient)
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
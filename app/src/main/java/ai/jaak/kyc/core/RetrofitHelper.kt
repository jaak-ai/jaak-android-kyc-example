package ai.jaak.kyc.core

import ai.jaak.kyc.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {
    fun getRetrofit(): Retrofit {
        // Get base URL from BuildConfig (loaded from local.properties)
        val baseUrl = if (BuildConfig.API_BASE_URL.isNotEmpty()) {
            BuildConfig.API_BASE_URL
        } else {
            throw IllegalStateException("API_BASE_URL not configured. Please add 'api.base.url' to local.properties")
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
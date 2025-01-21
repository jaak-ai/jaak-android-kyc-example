package com.jaak.kyc.core

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {
    fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dev.api.keyless.jaak.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
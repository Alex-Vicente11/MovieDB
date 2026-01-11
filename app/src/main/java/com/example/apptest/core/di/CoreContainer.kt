package com.example.apptest.core.di

import com.example.apptest.core.data.network.NetworkModule
import retrofit2.Retrofit

object CoreContainer {
    val retrofit: Retrofit by lazy {
        NetworkModule.provideRetrofit()
    }
}
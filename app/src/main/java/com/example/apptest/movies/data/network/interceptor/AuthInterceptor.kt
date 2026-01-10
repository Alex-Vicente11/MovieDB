package com.example.apptest.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * INTERCEPTOR DE AUTENTICACIÓN
 *
 * Responsabilidad única:
 * - Agregar el Bearer Token a todas las peticiones
 * - Agregar headers necesarios para la API
 */
class AuthInterceptor(
    private val accessToken: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()

        return chain.proceed(newRequest)
    }
}
package com.alexvicente.moviedb.core.data.network.interceptor

import com.alexvicente.moviedb.core.data.network.NetworkConfig.CONTENT_TYPE_JSON
import com.alexvicente.moviedb.core.data.network.NetworkConfig.HEADER_ACCEPT
import com.alexvicente.moviedb.core.data.network.NetworkConfig.HEADER_AUTHORIZATION
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
            .addHeader(HEADER_AUTHORIZATION, "Bearer $accessToken")
            .addHeader(HEADER_ACCEPT, CONTENT_TYPE_JSON)
            .build()

        return chain.proceed(newRequest)
    }
}
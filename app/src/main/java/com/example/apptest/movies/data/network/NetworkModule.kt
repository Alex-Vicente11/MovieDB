package com.example.apptest.movies.data.network


import com.example.apptest.movies.data.network.interceptor.AuthInterceptor
import com.example.apptest.movies.data.remote.api.TMDBApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MÓDULO DE RED
 *
 * Responsabilidad única: Configurar y proveer componentes de red
 *
 * Este módulo se encarga de:
 * - Configurar OkHttpClient con interceptores
 * - Configurar Retrofit con convertidores
 * - Proveer instancias de API Services
 *
 * Separado del AppContainer para:
 * ✅ Mejor organización y separación de responsabilidades
 * ✅ Facilitar testing (puedes mockear NetworkModule)
 * ✅ Facilitar migración futura a Hilt/Koin
 *
 * Patrón: Factory pattern + Singleton (lazy initialization)
 */
object NetworkModule {

    // ═══════════════════════════════════════════════════════
    // INTERCEPTORS
    // ═══════════════════════════════════════════════════════

    /**
     * Proporciona el interceptor de autenticación
     * Agrega el Bearer Token a todas las peticiones
     */
    private fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor(NetworkConfig.ACCESS_TOKEN)
    }

    /**
     * Proporciona el interceptor de logging
     *
     * Niveles:
     * - NONE: Sin logs
     * - BASIC: Request/Response line
     * - HEADERS: Request/Response line + headers
     * - BODY: Request/Response line + headers + body
     *
     * Solo habilitado en modo DEBUG para evitar:
     * - Llenar logs en producción
     * - Exponer información sensible
     * - Reducir overhead de performance
     */
    private fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (NetworkConfig.isLoggingEnabled) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // OKHTTP CLIENT
    // ═══════════════════════════════════════════════════════

    /**
     * Proporciona OkHttpClient configurado
     *
     * Configuraciones:
     * - Interceptores (Auth, Logging)
     * - Timeouts (Connect, Read, Write)
     * - Connection pool (automático)
     * - Retry y redirect (automático)
     */
    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Interceptores (orden importa: auth primero, logging después)
            .addInterceptor(provideAuthInterceptor())
            .addInterceptor(provideLoggingInterceptor())

            // Timeouts
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)

            // Retry automático en caso de falla temporal
            .retryOnConnectionFailure(true)

            .build()
    }

    // ═══════════════════════════════════════════════════════
    // GSON
    // ═══════════════════════════════════════════════════════

    /**
     * Proporciona Gson configurado
     *
     * setLenient(): Permite JSON no estricto
     * Útil cuando la API no sigue exactamente el estándar JSON
     */
    private fun provideGson() = GsonBuilder()
        .setLenient()
        .create()

    // ═══════════════════════════════════════════════════════
    // RETROFIT
    // ═══════════════════════════════════════════════════════

    /**
     * Proporciona Retrofit configurado
     *
     * Retrofit convierte las interfaces con anotaciones HTTP
     * en llamadas HTTP reales usando OkHttp
     */
    private fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(provideGson()))
            .build()
    }

    // ═══════════════════════════════════════════════════════
    // API SERVICES
    // ═══════════════════════════════════════════════════════

    /**
     * Proporciona el servicio de API de TMDB
     *
     * Esta es la función pública que usa AppContainer
     *
     * @return TMDBApiService configurado y listo para usar
     */
    fun provideTMDBApiService(): TMDBApiService {
        return provideRetrofit().create(TMDBApiService::class.java)
    }

    /**
     * Si en el futuro agregas más APIs, puedes agregar más funciones aquí:
     *
     * fun provideOtherApiService(): OtherApiService {
     *     return provideRetrofit().create(OtherApiService::class.java)
     * }
     */
}
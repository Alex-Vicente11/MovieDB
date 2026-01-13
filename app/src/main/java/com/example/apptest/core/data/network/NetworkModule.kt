package com.example.apptest.core.data.network

import com.example.apptest.core.data.network.interceptor.AuthInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MÓDULO DE RED
 *
 * UBICACIÓN: core/data/network/
 *
 * Responsabilidad única: Configurar y proveer componentes de red
 *
 * Este módulo se encarga de:
 * - Configurar OkHttpClient con interceptores
 * - Configurar Retrofit con convertidores
 * - Proveer instancia singleton de Retrofit
 *
 * Separado del AppContainer para:
 *  Mejor organización y separación de responsabilidades
 *  Facilitar testing (puedes mockear NetworkModule)
 * Facilitar migración futura a Hilt/Koin
 *
 * Patrón: Factory pattern + Singleton (lazy initialization)
 *
 * Arquitectura Modular:
 * - Este módulo NO crea API Services directamente
 * - Cada feature crea su propia API usando CoreContainer.retrofit
 * - Ejemplo: CoreContainer.retrofit.create(SearchApi::class.java)
 */
object NetworkModule {

    // ═══════════════════════════════════════════════════════════════════
    // INTERCEPTORS
    // ═══════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════
    // OKHTTP CLIENT
    // ═══════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════
    // GSON
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Proporciona Gson configurado
     *
     * setLenient(): Permite JSON no estricto
     * Útil cuando la API no sigue exactamente el estándar JSON
     */
    private fun provideGson() = GsonBuilder()
        .setLenient()
        .create()

    // ═══════════════════════════════════════════════════════════════════
    // RETROFIT (SINGLETON COMPARTIDO)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Proporciona Retrofit configurado
     *
     * PUBLIC: Usado por CoreContainer para exponer a todos los features
     *
     * Retrofit convierte las interfaces con anotaciones HTTP
     * en llamadas HTTP reales usando OkHttp
     *
     * Arquitectura Modular:
     * - Un solo Retrofit compartido por todos los features
     * - Cada feature crea su API específica
     * - Ejemplo de uso en feature containers:
     *   ```kotlin
     *   private val searchApi: SearchApi by lazy {
     *       CoreContainer.retrofit.create(SearchApi::class.java)
     *   }
     *   ```
     */
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(provideGson()))
            .build()
    }
}
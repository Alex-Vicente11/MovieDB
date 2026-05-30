package com.example.apptest.core.data.network

import com.example.apptest.core.data.network.interceptor.AuthInterceptor
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

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

/**
 *      @Module       ->clase que le dice a Hilt CÓMO crear objetos que él no puede crear solo
 *      @InstallIn    ->define en qué componente vive. SingletonComponent = toda la vida de la app
 *      @Provides     ->marca una función como proveedora de una dependencia
 *      @Singleton    ->una sola instancia durante toda la vida de la app
 */
//   provideAuthInterceptor() → AuthInterceptor
//       ↓ parámetro de
//   provideOkHttpClient(auth) → OkHttpClient
//       ↓ parámetro de
//   provideRetrofit(client)   → Retrofit
//       ↓ parámetro de (en cada feature module)
//   provideXxxApi(retrofit)   → XxxApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // INTERCEPTORS
    /**
     * Proporciona el interceptor de autenticación
     * Agrega el Bearer Token a todas las peticiones
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor =
        AuthInterceptor(NetworkConfig.ACCESS_TOKEN)


    // OKHTTP CLIENT
    /**
     * Proporciona OkHttpClient configurado
     *
     * Configuraciones:
     * - Interceptores (Auth, Logging)
     * - Timeouts (Connect, Read, Write)
     * - Connection pool (automático)
     * - Retry y redirect (automático)
     */

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            // Interceptores (orden importa: auth primero, logging después)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (NetworkConfig.isLoggingEnabled)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            // Timeouts
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            // Retry automático en caso de falla temporal
            .retryOnConnectionFailure(true)
            .build()

    // GSON
    /**
     * Proporciona Gson configurado
     *
     * setLenient(): Permite JSON no estricto
     * Útil cuando la API no sigue exactamente el estándar JSON
     */
    private fun provideGson() = GsonBuilder()
        .setLenient()
        .create()

    // RETROFIT (SINGLETON COMPARTIDO)
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
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(provideGson()))
            .build()
}
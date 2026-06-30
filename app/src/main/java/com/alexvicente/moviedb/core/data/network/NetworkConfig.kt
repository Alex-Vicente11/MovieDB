package com.alexvicente.moviedb.core.data.network

import com.alexvicente.moviedb.BuildConfig

/**
 * CONFIGURACIÓN DE RED
 *
 * Centraliza todas las configuraciones relacionadas con networking
 *
 * El token ahora se gestiona desde BuildConfig (build.gradle.kts)
 *
 * PRÓXIMO PASO RECOMENDADO:
 * Mover el token a local.properties para no subirlo a Git:
 *
 * 1. En local.properties:
 *    tmdb.token=eyJhbGciOi...
 *
 * 2. En build.gradle.kts:
 *    val tmdbToken = project.findProperty("tmdb.token") ?: ""
 *    buildConfigField("String", "TMDB_TOKEN", "\"$tmdbToken\"")
 */
object NetworkConfig {

    // ═══════════════════════════════════════════════════════
    // API CONFIGURATION
    // ═══════════════════════════════════════════════════════

    const val BASE_URL = "https://api.themoviedb.org/3/"

    /**
     * Token de acceso para TMDB API
     * Ahora se obtiene desde BuildConfig (build.gradle.kts)
     */
    const val ACCESS_TOKEN = BuildConfig.TMDB_TOKEN

    // ═══════════════════════════════════════════════════════
    // TIMEOUTS
    // ═══════════════════════════════════════════════════════

    /**
     * Tiempo máximo para establecer conexión (en segundos)
     */
    const val CONNECT_TIMEOUT = 30L

    /**
     * Tiempo máximo para leer datos del servidor (en segundos)
     */
    const val READ_TIMEOUT = 30L

    /**
     * Tiempo máximo para escribir datos al servidor (en segundos)
     */
    const val WRITE_TIMEOUT = 30L

    // ═══════════════════════════════════════════════════════
    // LOGGING CONFIGURATION
    // ═══════════════════════════════════════════════════════

    /**
     * Determina si el logging está habilitado
     * Solo se activa en modo DEBUG para no llenar los logs en producción
     */
    val isLoggingEnabled: Boolean
        get() = BuildConfig.DEBUG

    // ═══════════════════════════════════════════════════════
    // HEADERS
    // ═══════════════════════════════════════════════════════

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_ACCEPT = "Accept"

    const val CONTENT_TYPE_JSON = "application/json"
}
package com.example.apptest.movies.util

/**
 * CONSTANTES DE LA APLICACIÓN
 *
 * Centraliza valores que se usan en múltiples lugares
 */
object Constants {

    // API Configuration
    const val BASE_URL = "https://api.themoviedb.org/3/"

    // Search Configuration
    const val SEARCH_DEBOUNCE_DELAY = 500L  // milisegundos
    const val MIN_SEARCH_LENGTH = 2

    // Intent Extras
    const val EXTRA_MOVIE_ID = "extra_movie_id"

    // Error Messages
    const val ERROR_NETWORK = "Error de conexión. Verifica tu internet."
    const val ERROR_UNKNOWN = "Error desconocido. Intenta de nuevo."
    const val ERROR_EMPTY_QUERY = "Ingresa un término de búsqueda"
}
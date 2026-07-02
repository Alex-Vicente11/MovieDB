package com.alexvicente.moviedb.core.util

object Constants {

    const val BASE_URL = "https://api.themoviedb.org/3/"

    const val SEARCH_DEBOUNCE_DELAY = 500L
    const val MIN_SEARCH_LENGTH = 2

    const val ERROR_NETWORK    = "Sin conexión. Verifica tu internet."
    const val ERROR_UNKNOWN    = "Error desconocido. Intenta de nuevo."
    const val ERROR_EMPTY_QUERY = "Ingresa al menos 2 caracteres"
    const val ERROR_AUTH       = "Error de autenticación. Verifica el token"
    const val ERROR_NOT_FOUND  = "Recurso no encontrado"
    const val ERROR_SERVER     = "Error del servidor. Intenta más tarde"
    const val ERROR_RATE_LIMIT = "Demasiadas peticiones. Intenta más tarde"
    const val ERROR_DATABASE   = "Error interno. Intenta de nuevo."
}
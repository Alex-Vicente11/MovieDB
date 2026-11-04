package com.alexvicente.moviedb.core.data.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta una llamada de red y la traduce a Resource.
 * Relanza CancellationException: capturar Exception en una funcion suspend
 * también atraparía la cancelación de la corrutina
 */

inline fun <T> safeApiCall(block: () -> T): Resource <T> = try {
    Resource.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    val error = ErrorMapper.map(e)
    Resource.Error(error.toUserMessage(), error = error)
}
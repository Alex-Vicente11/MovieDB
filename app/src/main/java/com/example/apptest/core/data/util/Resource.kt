package com.example.apptest.core.data.util

/**
 * RESOURCE - Envoltorio de estados
 *
 * Reemplaza a NetworkResult con una estructura más limpia
 * Representa el estado de una operación asíncrona
 */
sealed class Resource<out T> {
    /**
     * Estado de carga
     * @param data Datos previos (opcional, para mostrar mientras carga)
     */
    data class Loading<out T>(val data: T? = null) : Resource<T>()

    /**
     * Operación exitosa
     * @param data Datos obtenidos
     */
    data class Success<out T>(val data: T) : Resource<T>()

    /**
     * Error en la operación
     * @param message Mensaje descriptivo del error
     * @param data Datos parciales (opcional)
     */
    data class Error<out T>(
        val message: String,
        val data: T? = null
    ) : Resource<T>()
}
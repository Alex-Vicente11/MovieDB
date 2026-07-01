package com.alexvicente.moviedb.core.data.util

import android.database.sqlite.SQLiteException
import com.alexvicente.moviedb.core.data.remote.dto.TmdbErrorBody
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

object ErrorMapper {
    private val gson = Gson()

    fun map(throwable: Throwable): AppError = when (throwable) {
        is HttpException   -> mapHttpException(throwable)
        is IOException     -> AppError.Network("Sin conexión. Verifica tu internet.")
        is SQLiteException -> AppError.Database(throwable.localizedMessage ?: "Error de base de datos")
        else               -> AppError.Unknown(throwable.localizedMessage ?: "Error desconocido")
    }

    private fun mapHttpException(e: HttpException): AppError {
        val tmdbMessage = parseTmdbError(e)  // intenta leer status_message real
        return when (e.code()) {
            401 -> AppError.Unauthorized(tmdbMessage ?: "Token inválido")
            404 -> AppError.NotFound(tmdbMessage ?: "Recurso no encontrado")
            429 -> AppError.RateLimited("Demasiadas peticiones. Intenta más tarde")
            in 500..599 -> AppError.Server(e.code(), "Error del servidor. Intenta más tarde")
            else -> AppError.Server(e.code(), tmdbMessage ?: "Error: ${e.code()}")
        }
    }

    private fun parseTmdbError(e: HttpException): String? = try {
        val body = e.response()?.errorBody()?.string()
        if (body.isNullOrEmpty()) null
        else gson.fromJson(body, TmdbErrorBody::class.java)?.status_message
    } catch (ex: Exception) {
        null  // si el JSON viene mal formado o vacío, no truena tu app
    }
}
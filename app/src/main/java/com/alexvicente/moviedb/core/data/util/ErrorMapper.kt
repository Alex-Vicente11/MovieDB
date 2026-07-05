package com.alexvicente.moviedb.core.data.util

import android.database.sqlite.SQLiteException
import com.alexvicente.moviedb.core.util.Constants
import retrofit2.HttpException
import java.io.IOException

object ErrorMapper {

    fun map(throwable: Throwable): AppError = when (throwable) {
        is HttpException   -> mapHttpException(throwable)
        is IOException     -> AppError.Network(Constants.ERROR_NETWORK)
        is SQLiteException -> AppError.Database(Constants.ERROR_DATABASE)
        else               -> AppError.Unknown(Constants.ERROR_UNKNOWN)
    }

    private fun mapHttpException(e: HttpException): AppError = when (e.code()) {
        401         -> AppError.Unauthorized(Constants.ERROR_AUTH)
        404         -> AppError.NotFound(Constants.ERROR_NOT_FOUND)
        429         -> AppError.RateLimited(Constants.ERROR_RATE_LIMIT)
        in 500..599 -> AppError.Server(e.code(), Constants.ERROR_SERVER)
        else        -> AppError.Server(e.code(), Constants.ERROR_UNKNOWN)
    }
}
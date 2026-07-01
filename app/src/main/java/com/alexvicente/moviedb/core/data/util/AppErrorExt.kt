package com.alexvicente.moviedb.core.data.util

fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network      -> message
    is AppError.Unauthorized -> message
    is AppError.NotFound     -> message
    is AppError.RateLimited  -> message
    is AppError.Server       -> message
    is AppError.Database     -> message
    is AppError.Unknown      -> message
}
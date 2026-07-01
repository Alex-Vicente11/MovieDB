package com.alexvicente.moviedb.core.data.util

sealed class AppError {
    data class Network(val message: String) : AppError()
    data class Server(val code: Int, val message: String) : AppError()
    data class Unauthorized(val message: String) : AppError()
    data class NotFound(val message: String) : AppError()
    data class RateLimited(val message: String) : AppError()
    data class Database(val message: String) : AppError()
    data class Unknown(val message: String) : AppError()
}
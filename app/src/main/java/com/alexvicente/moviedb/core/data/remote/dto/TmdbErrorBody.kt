package com.alexvicente.moviedb.core.data.remote.dto

data class TmdbErrorBody(
    val success: Boolean,
    val status_code: Int,
    val status_message: String
)
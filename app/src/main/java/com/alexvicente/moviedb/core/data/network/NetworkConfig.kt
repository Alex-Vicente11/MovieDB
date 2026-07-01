package com.alexvicente.moviedb.core.data.network

import com.alexvicente.moviedb.BuildConfig
import com.alexvicente.moviedb.core.util.Constants

object NetworkConfig {

    const val BASE_URL = Constants.BASE_URL
    const val ACCESS_TOKEN = BuildConfig.TMDB_TOKEN
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    val isLoggingEnabled: Boolean
        get() = BuildConfig.DEBUG

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_ACCEPT = "Accept"
    const val CONTENT_TYPE_JSON = "application/json"
}
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

    object Pins {
        const val TMDB_HOST = "api.themoviedb.org"
        const val TMDB_LEAF = "sha256/QfyoR20v8hyYX7L+ikLzM/euPGSDl67gFFcor/sROMs="
        const val TMDB_INTERMEDIATE = "sha256/G9LNNAql897egYsabashkzUCTEJkWBzgoEtk8X/678c="

        const val PROXY_HOST = "moviedb-graphql-proxy.onrender.com"
        const val PROXY_LEAF = "sha256/8emdl/UmneUm0I4Y/vHzOTQzb9eJwG4voRHtMdNmBPk="
        const val PROXY_INTERMEDIATE = "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4="
    }
}
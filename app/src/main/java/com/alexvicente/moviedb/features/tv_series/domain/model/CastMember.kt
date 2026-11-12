package com.alexvicente.moviedb.features.tv_series.domain.model

data class CastMember(
    val id: Int,
    val creditId: String,
    val name: String,
    val character: String,
    val profilePath: String?
)

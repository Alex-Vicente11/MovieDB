package com.example.apptest.features.favorites.domain.model

data class Favorite(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val overview: String,
    val addedAt: Long
)
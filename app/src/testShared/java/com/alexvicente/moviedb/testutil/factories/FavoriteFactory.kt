package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.features.favorites.domain.model.Favorite

object FavoriteFactory {

    fun createFavorite(
        id: Int = 1,
        title: String = "Inception",
        // null: evita que Glide intente descargar un póster real en los tests instrumentados
        posterPath: String? = null,
        voteAverage: Double = 8.8,
        releaseDate: String = "2010-07-16",
        overview: String = "A thief who steals corporate secrets",
        addedAt: Long = 1_700_000_000_000L
    ) = Favorite(
        id = id,
        title = title,
        posterPath = posterPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        overview = overview,
        addedAt = addedAt
    )

    fun createFavoriteList(size: Int): List<Favorite> =
        (1..size).map { index ->
            createFavorite(
                id = index,
                title = "Favorite $index",
                // addedAt decreciente: el más reciente primero, orden realista
                addedAt = 1_700_000_000_000L - index
            )
        }
}
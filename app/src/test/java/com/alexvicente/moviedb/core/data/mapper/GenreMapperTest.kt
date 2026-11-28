package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.genres.data.remote.dto.GenresResponseDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// Cubre GenresResponseDto.toDomain(). GenreDto.toDomain() ya se prueba en MovieMapperTest.
class GenreMapperTest {

    @Test
    fun whenResponseHasSeveralGenres_mapsAllPreservingOrder() {
        // Given
        val response = GenresResponseDto(
            genres = listOf(GenreDto(18, "Drama"), GenreDto(28, "Action"), GenreDto(35, "Comedy"))
        )

        // When
        val genres = response.toDomain()

        // Then
        assertThat(genres)
            .containsExactly(Genre(18, "Drama"), Genre(28, "Action"), Genre(35, "Comedy"))
            .inOrder()
    }

    @Test
    fun whenGenreNamesHaveSurroundingSpaces_theyAreTrimmed() {
        val response = GenresResponseDto(genres = listOf(GenreDto(id = 35, name = "  Comedy  ")))

        assertThat(response.toDomain()).containsExactly(Genre(35, "Comedy"))
    }

    @Test
    fun whenResponseHasNoGenres_returnsEmptyList() {
        assertThat(GenresResponseDto(genres = emptyList()).toDomain()).isEmpty()
    }
}
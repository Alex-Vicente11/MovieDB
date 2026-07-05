package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.alexvicente.moviedb.features.genres.data.remote.dto.GenresResponseDto
import com.alexvicente.moviedb.core.domain.model.Genre

object GenreMapper {
    fun GenreDto.toDomain(): Genre {
        return Genre(
            id = this.id,
            name = this.name.trim()
        )
    }

    fun GenresResponseDto.toDomain(): List<Genre> {
        return this.genres.map { it.toDomain() }
    }
}

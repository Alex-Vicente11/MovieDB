package com.example.apptest.core.data.mapper

import com.example.apptest.core.data.remote.dto.GenreDto
import com.example.apptest.features.genres.data.remote.dto.GenresResponseDto
import com.example.apptest.core.domain.model.Genre

object GenreMapper {
    fun GenreDto.toDomain(): Genre {
        return Genre(
            id = this.id,
            name = this.name
        )
    }

    fun GenresResponseDto.toDomain(): List<Genre> {
        return this.genres.map { it.toDomain() }
    }
}

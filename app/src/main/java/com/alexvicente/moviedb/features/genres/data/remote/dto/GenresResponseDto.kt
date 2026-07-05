package com.alexvicente.moviedb.features.genres.data.remote.dto

import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.google.gson.annotations.SerializedName

data class GenresResponseDto(
    @SerializedName("genres")
    val genres: List<GenreDto>
)
package com.example.apptest.features.genres.data.remote.dto

import com.example.apptest.core.data.remote.dto.GenreDto
import com.google.gson.annotations.SerializedName

data class GenresResponseDto(
    @SerializedName("genres")
    val genres: List<GenreDto>
)
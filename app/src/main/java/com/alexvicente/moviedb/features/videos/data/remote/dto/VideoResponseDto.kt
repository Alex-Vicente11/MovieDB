package com.alexvicente.moviedb.features.videos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VideoResponseDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: List<VideoDto>
)

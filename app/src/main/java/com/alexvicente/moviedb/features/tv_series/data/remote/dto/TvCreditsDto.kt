package com.alexvicente.moviedb.features.tv_series.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TvCreditsDto(
    @SerializedName("cast") val cast: List<CastMemberDto>?
    // "crew" no se declara a propósito: la pantalla no lo usa y Gson lo omite
)

data class CastMemberDto(
    @SerializedName("id") val id: Int,
    @SerializedName("credit_id") val creditId: String,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String?,
    @SerializedName("profile_path") val profilePath: String?
)

package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.features.videos.domain.model.Video

object VideoFactory {

    fun createVideo(
        id: String = "video-1",
        key: String = "YoHD9XEInc0",
        name: String = "Official Trailer",
        site: String = "YouTube",
        size: Int = 1080,
        type: String = Video.TYPE_TRAILER,
        official: Boolean = true,
        publishedAt: String = "2025-02-14T16:00:07.000Z",
        language: String = "en",
        country: String = "US"
    ) = Video(
        id = id,
        key = key,
        name = name,
        site = site,
        size = size,
        type = type,
        official = official,
        publishedAt = publishedAt,
        language = language,
        country = country
    )

    // ids y keys únicos: las listas de la UI usan el id como clave
    fun createVideoList(size: Int): List<Video> =
        (1..size).map { index ->
            createVideo(
                id = "video-$index",
                key = "key$index",
                name = "Video $index"
            )
        }
}
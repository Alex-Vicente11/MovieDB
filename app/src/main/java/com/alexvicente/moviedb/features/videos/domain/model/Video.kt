package com.alexvicente.moviedb.features.videos.domain.model

data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    val publishedAt: String,
    val language: String,
    val country: String
) {
    companion object {
        private const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
        private const val YOUTUBE_THUMBNAIL_URL = "https://img.youtube.com/vi/"
        const val TYPE_TRAILER = "Trailer"
        const val TYPE_TEASER = "Teaser"
        const val TYPE_CLIP = "Clip"
        const val TYPE_FEATURETTE = "Featurette"
        const val TYPE_BEHIND_SCENES = "Behind the Scenes"
    }

    fun getYoutubeUrl(): String {
        return "$YOUTUBE_WATCH_URL$key"
    }

    fun getThumbnailUrl(quality: ThumbnailQuality = ThumbnailQuality.HIGH): String {
        return "$YOUTUBE_THUMBNAIL_URL$key/${quality.fileName}"
    }

    enum class ThumbnailQuality(val fileName: String) {
        DEFAULT("default.jpg"),         // 120x90
        MEDIUM("mqdefault.jpg"),        // 320x180
        HIGH("hqdefault.jpg"),          // 480x360
        STANDARD("sddefault.jpg"),      // 640x480
        MAX("maxresdefault.jpg")        // 1280x720
    }

    fun isOfficialTrailer(): Boolean {
        return official && type == TYPE_TRAILER
    }

    fun isTrailer(): Boolean = type == TYPE_TRAILER
    fun isTeaser(): Boolean = type == TYPE_TEASER
    fun isClip(): Boolean = type == TYPE_CLIP
    fun isFeaturette(): Boolean = type == TYPE_FEATURETTE
    fun isBehindTheScenes(): Boolean = type == TYPE_BEHIND_SCENES

    fun getFormattedDate(): String {
        return try {
            val dateTime = publishedAt.split("T")[0]  // "2025-02-14"
            val parts = dateTime.split("-")
            val year = parts[0]
            val month = getMonthName(parts[1].toInt())
            val day = parts[2]
            "$day $month $year"
        } catch (e: Exception) {
            "Fecha no disponible"
        }
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Ene"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Abr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dic"
            else -> "?"
        }
    }

    fun getFormattedResolution(): String {
        return when (size) {
            360 -> "SD"
            480 -> "SD"
            720 -> "HD"
            1080 -> "Full HD"
            2160 -> "4K"
            else -> "${size}p"
        }
    }

    fun getTypeBadge(): String {
        return when (type) {
            TYPE_TRAILER -> "🎬 Trailer"
            TYPE_TEASER -> "📽️ Teaser"
            TYPE_CLIP -> "🎞️ Clip"
            TYPE_FEATURETTE -> "🎥 Featurette"
            TYPE_BEHIND_SCENES -> "🎭 Behind the Scenes"
            else -> "📹 Video"
        }
    }

    fun getEstimatedDuration(): String {
        return when (type) {
            TYPE_TRAILER -> "~2-3 min"
            TYPE_TEASER -> "~30-60 seg"
            TYPE_CLIP -> "~1-2 min"
            TYPE_FEATURETTE -> "~5-10 min"
            TYPE_BEHIND_SCENES -> "~10-20 min"
            else -> "Duración variable"
        }
    }

    fun getOfficialBadge(): String {
        return if (official) "Oficial" else ""
    }

    fun getLanguageName(): String {
        return when (language) {
            "en" -> "Inglés"
            "es" -> "Español"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "it" -> "Italiano"
            "ja" -> "Japonés"
            "ko" -> "Coreano"
            "pt" -> "Portugués"
            "zh" -> "Chino"
            "ru" -> "Ruso"
            else -> language.uppercase()
        }
    }
}

package com.alexvicente.moviedb.core.data.mapper

import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomain
import com.alexvicente.moviedb.core.data.mapper.MovieMapper.toDomainVideos
import com.alexvicente.moviedb.features.videos.data.remote.dto.VideoDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// Cubre las funciones de video de MovieMapper (VideoDto -> Video y List<VideoDto> -> List<Video>).
class VideoMapperTest {

    private fun createVideoDto(
        id: String = "video-1",
        key: String = "YoHD9XEInc0",
        name: String = "Official Trailer",
        site: String = "YouTube",
        size: Int = 1080,
        type: String = "Trailer",
        official: Boolean = true,
        publishedAt: String = "2025-02-14T16:00:07.000Z",
        language: String = "en",
        country: String = "US"
    ) = VideoDto(
        language = language,
        country = country,
        name = name,
        key = key,
        site = site,
        size = size,
        type = type,
        official = official,
        publishedAt = publishedAt,
        id = id
    )

    // Valores distintos en cada campo: detecta campos cruzados (language/country) y booleanos fijos.
    @Test
    fun whenAllFieldsArePresent_mapsEveryField() {
        // Given
        val dto = createVideoDto(
            id = "abc", key = "k1", name = "Teaser 1", site = "Vimeo", size = 720,
            type = "Teaser", official = false, publishedAt = "2024-12-01T10:00:00.000Z",
            language = "es", country = "MX"
        )

        // When
        val video = dto.toDomain()

        // Then
        assertThat(video.id).isEqualTo("abc")
        assertThat(video.key).isEqualTo("k1")
        assertThat(video.name).isEqualTo("Teaser 1")
        assertThat(video.site).isEqualTo("Vimeo")
        assertThat(video.size).isEqualTo(720)
        assertThat(video.type).isEqualTo("Teaser")
        assertThat(video.official).isFalse()
        assertThat(video.publishedAt).isEqualTo("2024-12-01T10:00:00.000Z")
        assertThat(video.language).isEqualTo("es")
        assertThat(video.country).isEqualTo("MX")
    }

    @Test
    fun whenOfficialIsTrue_officialIsTrue() {
        assertThat(createVideoDto(official = true).toDomain().official).isTrue()
    }

    @Test
    fun whenNameHasSurroundingSpaces_nameIsTrimmed() {
        assertThat(createVideoDto(name = "  Official Trailer  ").toDomain().name)
            .isEqualTo("Official Trailer")
    }

    // Caracterización: solo el nombre se recorta; key, site y type pasan tal cual.
    @Test
    fun whenOtherTextFieldsHaveSpaces_theyAreNotTrimmed() {
        val video = createVideoDto(key = " k ", site = " YouTube ", type = " Trailer ").toDomain()

        assertThat(video.key).isEqualTo(" k ")
        assertThat(video.site).isEqualTo(" YouTube ")
        assertThat(video.type).isEqualTo(" Trailer ")
    }

    @Test
    fun whenListHasSeveralVideos_preservesSizeAndOrder() {
        val list = listOf(
            createVideoDto(id = "c", name = "C"),
            createVideoDto(id = "a", name = "  A  "),
            createVideoDto(id = "b", name = "B")
        )

        val videos = list.toDomainVideos()

        assertThat(videos.map { it.id }).containsExactly("c", "a", "b").inOrder()
        assertThat(videos.map { it.name }).containsExactly("C", "A", "B").inOrder()
    }

    @Test
    fun whenListIsEmpty_returnsEmptyList() {
        assertThat(emptyList<VideoDto>().toDomainVideos()).isEmpty()
    }
}
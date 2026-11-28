package com.alexvicente.moviedb.features.videos.domain.model

import com.alexvicente.moviedb.testutil.factories.VideoFactory.createVideo
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class VideoTest {

    private lateinit var originalLocale: Locale

    @Before
    fun saveLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // -- URLs de YouTube ---------------------------------------------------------------

    @Test
    fun whenVideoHasKey_youtubeUrlContainsTheKey() {
        assertThat(createVideo(key = "abc123").getYoutubeUrl())
            .isEqualTo("https://www.youtube.com/watch?v=abc123")
    }

    @Test
    fun whenNoQualityIsGiven_thumbnailUsesHighQuality() {
        assertThat(createVideo(key = "abc123").getThumbnailUrl())
            .isEqualTo("https://img.youtube.com/vi/abc123/hqdefault.jpg")
    }

    @Test
    fun whenQualityIsGiven_thumbnailUsesThatFileName() {
        assertThat(createVideo(key = "abc123").getThumbnailUrl(Video.ThumbnailQuality.MAX))
            .isEqualTo("https://img.youtube.com/vi/abc123/maxresdefault.jpg")
    }

    // -- Tipos -------------------------------------------------------------------------

    @Test
    fun whenVideoIsOfficialTrailer_isOfficialTrailerIsTrue() {
        assertThat(createVideo(official = true, type = Video.TYPE_TRAILER).isOfficialTrailer()).isTrue()
    }

    @Test
    fun whenTrailerIsNotOfficial_isOfficialTrailerIsFalse() {
        assertThat(createVideo(official = false, type = Video.TYPE_TRAILER).isOfficialTrailer()).isFalse()
    }

    @Test
    fun whenOfficialVideoIsNotATrailer_isOfficialTrailerIsFalse() {
        assertThat(createVideo(official = true, type = Video.TYPE_TEASER).isOfficialTrailer()).isFalse()
    }

    @Test
    fun whenTypeIsTrailer_onlyIsTrailerIsTrue() {
        val video = createVideo(type = Video.TYPE_TRAILER)

        assertThat(video.isTrailer()).isTrue()
        assertThat(video.isTeaser()).isFalse()
        assertThat(video.isClip()).isFalse()
        assertThat(video.isFeaturette()).isFalse()
        assertThat(video.isBehindTheScenes()).isFalse()
    }

    @Test
    fun whenTypeIsTeaser_onlyIsTeaserIsTrue() {
        val video = createVideo(type = Video.TYPE_TEASER)

        assertThat(video.isTeaser()).isTrue()
        assertThat(video.isTrailer()).isFalse()
    }

    @Test
    fun whenTypeIsClip_onlyIsClipIsTrue() {
        val video = createVideo(type = Video.TYPE_CLIP)

        assertThat(video.isClip()).isTrue()
        assertThat(video.isTrailer()).isFalse()
    }

    @Test
    fun whenTypeIsFeaturette_onlyIsFeaturetteIsTrue() {
        val video = createVideo(type = Video.TYPE_FEATURETTE)

        assertThat(video.isFeaturette()).isTrue()
        assertThat(video.isTrailer()).isFalse()
    }

    @Test
    fun whenTypeIsBehindTheScenes_onlyIsBehindTheScenesIsTrue() {
        val video = createVideo(type = Video.TYPE_BEHIND_SCENES)

        assertThat(video.isBehindTheScenes()).isTrue()
        assertThat(video.isTrailer()).isFalse()
    }

    // -- getFormattedDate() ------------------------------------------------------------

    @Test
    fun whenPublishedAtIsIso8601_dateShowsDayMonthNameYear() {
        assertThat(createVideo(publishedAt = "2025-02-14T16:00:07.000Z").getFormattedDate())
            .isEqualTo("14 Feb 2025")
    }

    @Test
    fun whenEachMonthIsFormatted_usesSpanishAbbreviation() {
        val expected = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

        val actual = (1..12).map { month ->
            val mm = month.toString().padStart(2, '0')
            createVideo(publishedAt = "2025-$mm-10T00:00:00.000Z").getFormattedDate()
        }

        assertThat(actual).isEqualTo(expected.map { "10 $it 2025" })
    }

    @Test
    fun whenMonthIsOutOfRange_monthShowsQuestionMark() {
        assertThat(createVideo(publishedAt = "2025-13-01T00:00:00.000Z").getFormattedDate())
            .isEqualTo("01 ? 2025")
    }

    @Test
    fun whenPublishedAtIsGarbage_dateIsNotAvailable() {
        assertThat(createVideo(publishedAt = "garbage").getFormattedDate())
            .isEqualTo("Fecha no disponible")
    }

    @Test
    fun whenPublishedAtIsEmpty_dateIsNotAvailable() {
        assertThat(createVideo(publishedAt = "").getFormattedDate())
            .isEqualTo("Fecha no disponible")
    }

    @Test
    fun whenMonthIsNotNumeric_dateIsNotAvailable() {
        assertThat(createVideo(publishedAt = "2025-ab-01T00:00:00.000Z").getFormattedDate())
            .isEqualTo("Fecha no disponible")
    }

    // -- getFormattedResolution() ------------------------------------------------------

    @Test
    fun whenSizeIsStandardDefinition_resolutionIsSd() {
        assertThat(createVideo(size = 360).getFormattedResolution()).isEqualTo("SD")
        assertThat(createVideo(size = 480).getFormattedResolution()).isEqualTo("SD")
    }

    @Test
    fun whenSizeIsHighDefinition_resolutionIsHdFullHdOr4k() {
        assertThat(createVideo(size = 720).getFormattedResolution()).isEqualTo("HD")
        assertThat(createVideo(size = 1080).getFormattedResolution()).isEqualTo("Full HD")
        assertThat(createVideo(size = 2160).getFormattedResolution()).isEqualTo("4K")
    }

    @Test
    fun whenSizeIsNotInTheTable_resolutionShowsTheNumberWithP() {
        assertThat(createVideo(size = 1440).getFormattedResolution()).isEqualTo("1440p")
    }

    // -- getTypeBadge() / getEstimatedDuration() ---------------------------------------

    @Test
    fun whenTypeIsKnown_badgeMatchesTheType() {
        assertThat(createVideo(type = Video.TYPE_TRAILER).getTypeBadge()).isEqualTo("🎬 Trailer")
        assertThat(createVideo(type = Video.TYPE_TEASER).getTypeBadge()).isEqualTo("📽️ Teaser")
        assertThat(createVideo(type = Video.TYPE_CLIP).getTypeBadge()).isEqualTo("🎞️ Clip")
        assertThat(createVideo(type = Video.TYPE_FEATURETTE).getTypeBadge()).isEqualTo("🎥 Featurette")
        assertThat(createVideo(type = Video.TYPE_BEHIND_SCENES).getTypeBadge()).isEqualTo("🎭 Behind the Scenes")
    }

    @Test
    fun whenTypeIsUnknown_badgeIsGenericVideo() {
        assertThat(createVideo(type = "Bloopers").getTypeBadge()).isEqualTo("📹 Video")
    }

    @Test
    fun whenTypeIsKnown_estimatedDurationMatchesTheType() {
        assertThat(createVideo(type = Video.TYPE_TRAILER).getEstimatedDuration()).isEqualTo("~2-3 min")
        assertThat(createVideo(type = Video.TYPE_TEASER).getEstimatedDuration()).isEqualTo("~30-60 seg")
        assertThat(createVideo(type = Video.TYPE_CLIP).getEstimatedDuration()).isEqualTo("~1-2 min")
        assertThat(createVideo(type = Video.TYPE_FEATURETTE).getEstimatedDuration()).isEqualTo("~5-10 min")
        assertThat(createVideo(type = Video.TYPE_BEHIND_SCENES).getEstimatedDuration()).isEqualTo("~10-20 min")
    }

    @Test
    fun whenTypeIsUnknown_estimatedDurationIsVariable() {
        assertThat(createVideo(type = "Bloopers").getEstimatedDuration()).isEqualTo("Duración variable")
    }

    // -- getOfficialBadge() / getLanguageName() ----------------------------------------

    @Test
    fun whenVideoIsOfficial_officialBadgeIsOficial() {
        assertThat(createVideo(official = true).getOfficialBadge()).isEqualTo("Oficial")
    }

    @Test
    fun whenVideoIsNotOfficial_officialBadgeIsEmpty() {
        assertThat(createVideo(official = false).getOfficialBadge()).isEmpty()
    }

    @Test
    fun whenLanguageIsInTheTable_languageNameIsInSpanish() {
        val expected = mapOf(
            "en" to "Inglés", "es" to "Español", "fr" to "Francés", "de" to "Alemán",
            "it" to "Italiano", "ja" to "Japonés", "ko" to "Coreano", "pt" to "Portugués",
            "zh" to "Chino", "ru" to "Ruso"
        )

        val actual = expected.keys.associateWith { createVideo(language = it).getLanguageName() }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenLanguageIsNotInTheTable_languageNameIsTheUppercaseCode() {
        assertThat(createVideo(language = "pl").getLanguageName()).isEqualTo("PL")
    }

    // Con Locale turco, "id".toUpperCase() daría "İD" (I con punto). uppercase() usa Locale.ROOT.
    @Test
    fun whenDeviceUsesTurkishLocale_languageCodeUppercaseIsNotLocalized() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        assertThat(createVideo(language = "id").getLanguageName()).isEqualTo("ID")
    }
}
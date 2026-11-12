package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvEpisodeDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeasonDetailsDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class TvSeasonRepositoryImplTest {

    private lateinit var api: TvSeriesApi
    private lateinit var repository: TvSeasonRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = TvSeasonRepositoryImpl(api)
    }

    private fun dto() = TvSeasonDetailsDto(
        id = 1,
        name = "Temporada 1",
        overview = null,
        posterPath = null,
        airDate = null,
        seasonNumber = 1,
        voteAverage = 0.0,
        episodes = listOf(
            TvEpisodeDto(
                id = 10, episodeNumber = 1, name = " Piloto ", overview = null,
                airDate = null, runtime = null, stillPath = null,
                voteAverage = 0.0, voteCount = 0
            )
        )
    )

    @Test
    fun whenApiSucceeds_returnsMappedSeason() = runTest {
        coEvery { api.getTvSeasonDetails(1416, 1, "es-MX") } returns dto()

        val result = repository.getTvSeasonDetails(1416, 1, "es-MX")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val season = (result as Resource.Success).data
        assertThat(season.name).isEqualTo("Temporada 1")
        assertThat(season.episodes.single().name).isEqualTo("Piloto")
    }

    @Test
    fun usesTheSeriesIdAndSeasonNumberItWasGiven() = runTest {
        coEvery { api.getTvSeasonDetails(any(), any(), any()) } returns dto()

        repository.getTvSeasonDetails(seriesId = 87108, seasonNumber = 0, language = "es-MX")

        coVerify(exactly = 1) { api.getTvSeasonDetails(87108, 0, "es-MX") }
    }

    @Test
    fun whenApiThrowsHttpException_returnsError() = runTest {
        val http = HttpException(Response.error<Any>(404, "Not found".toResponseBody()))
        coEvery { api.getTvSeasonDetails(any(), any(), any()) } throws http

        val result = repository.getTvSeasonDetails(1416, 99, "es-MX")

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenCoroutineIsCancelled_cancellationIsNotSwallowed() = runTest {
        coEvery { api.getTvSeasonDetails(any(), any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.getTvSeasonDetails(1416, 1, "es-MX") }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}

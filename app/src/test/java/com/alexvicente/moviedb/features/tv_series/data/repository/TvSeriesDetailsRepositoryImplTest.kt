package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesDetailsDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class TvSeriesDetailsRepositoryImplTest {

    private lateinit var api: TvSeriesApi
    private lateinit var repository: TvSeriesDetailsRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = TvSeriesDetailsRepositoryImpl(api)
    }

    private fun dto() = TvSeriesDetailsDto(
        id = 1,
        name = "Serie",
        tagline = null,
        overview = null,
        posterPath = null,
        backdropPath = null,
        voteAverage = 8.0,
        voteCount = 1,
        firstAirDate = null,
        status = "Ended",
        numberOfSeasons = 1,
        numberOfEpisodes = 1,
        genres = null,
        seasons = null
    )

    @Test
    fun whenApiSucceeds_returnsMappedSuccess() = runTest {
        coEvery { api.getTvSeriesDetails(1, "es-MX") } returns dto()

        val result = repository.getTvSeriesDetails(1, "es-MX")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.name).isEqualTo("Serie")
    }

    @Test
    fun whenApiThrowsHttpException_returnsError() = runTest {
        val http = HttpException(Response.error<Any>(404, "Not found".toResponseBody()))
        coEvery { api.getTvSeriesDetails(any(), any()) } throws http

        val result = repository.getTvSeriesDetails(1, "es-MX")

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenCoroutineIsCancelled_cancellationIsNotSwallowed() = runTest {
        coEvery { api.getTvSeriesDetails(any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.getTvSeriesDetails(1, "es-MX") }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
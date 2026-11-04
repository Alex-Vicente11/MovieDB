package com.alexvicente.moviedb.features.tv_series.data.repository

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.CastMemberDto
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvCreditsDto
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
import kotlin.jvm.java

class TvSeriesCastRepositoryImplTest {

    private lateinit var api: TvSeriesApi
    private lateinit var repository: TvSeriesCastRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = TvSeriesCastRepositoryImpl(api)
    }

    @Test
    fun whenApiSucceeds_returnsMappedCast() = runTest {
        coEvery { api.getTvSeriesCredits(1, "es-MX") } returns TvCreditsDto(
            cast = listOf(CastMemberDto(7, "c7", " Ana ", "Rol", "/p.jpg"))
        )

        val result = repository.getTvSeriesCast(1, "es-MX")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.single().name).isEqualTo("Ana")
    }

    @Test
    fun whenApiThrowsHttpException_returnsError() = runTest {
        val http = HttpException(Response.error<Any>(404, "Not found".toResponseBody()))
        coEvery { api.getTvSeriesCredits(any(), any()) } throws http

        val result = repository.getTvSeriesCast(1, "es-MX")

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenCoroutineIsCancelled_cancellationIsNotSwallowed() = runTest {
        coEvery { api.getTvSeriesCredits(any(), any()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.getTvSeriesCast(1, "es-MX") }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}

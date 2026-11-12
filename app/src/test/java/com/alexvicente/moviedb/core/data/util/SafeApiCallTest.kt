package com.alexvicente.moviedb.core.data.util

import com.google.common.truth.Truth.assertThat
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class SafeApiCallTest {

    @Test
    fun whenBlockSucceeds_returnsSuccessWithValue() {
        val result = safeApiCall { 42 }

        assertThat(result).isEqualTo(Resource.Success(42))
    }

    @Test
    fun whenBlockThrowsHttpException_returnsError() {
        val http = HttpException(Response.error<Any>(500, "Server error".toResponseBody()))

        val result = safeApiCall<Int> { throw http }

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenBlockThrowsIOException_returnsError() {
        val result = safeApiCall<Int> { throw IOException("No network") }

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenBlockIsCancelled_cancellationIsRethrown() {
        val outcome = runCatching { safeApiCall<Int> { throw CancellationException("cancelled") } }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
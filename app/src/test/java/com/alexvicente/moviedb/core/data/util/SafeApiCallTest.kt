package com.alexvicente.moviedb.core.data.util

import com.alexvicente.moviedb.core.util.Constants
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
    fun whenBlockThrowsHttpException_returnsErrorWithMappedServerError() {
        val http = HttpException(Response.error<Any>(500, "Server error".toResponseBody()))

        val result = safeApiCall<Int> { throw http }

        assertThat(result).isEqualTo(
            Resource.Error<Int>(
                message = Constants.ERROR_SERVER,
                error = AppError.Server(500, Constants.ERROR_SERVER)
            )
        )
    }

    @Test
    fun whenBlockThrowsIOException_returnsErrorWithNetworkError() {
        val result = safeApiCall<Int> { throw IOException("No network") }

        assertThat(result).isEqualTo(
            Resource.Error<Int>(
                message = Constants.ERROR_NETWORK,
                error = AppError.Network(Constants.ERROR_NETWORK)
            )
        )
    }

    @Test
    fun whenBlockThrowsUnexpectedException_returnsErrorWithUnknownError() {
        val result = safeApiCall<Int> { throw IllegalStateException("boom") }

        assertThat(result).isEqualTo(
            Resource.Error<Int>(
                message = Constants.ERROR_UNKNOWN,
                error = AppError.Unknown(Constants.ERROR_UNKNOWN)
            )
        )
    }

    // Si se quita el catch de CancellationException, el catch genérico la atrapa y devuelve Error.
    @Test
    fun whenBlockIsCancelled_cancellationIsRethrown() {
        val outcome = runCatching { safeApiCall<Int> { throw CancellationException("cancelled") } }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
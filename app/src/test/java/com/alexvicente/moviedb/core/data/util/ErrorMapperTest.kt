package com.alexvicente.moviedb.core.data.util

import com.alexvicente.moviedb.core.util.Constants
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.google.common.truth.Truth.assertThat
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

class ErrorMapperTest {

    private fun retrofitHttp(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody()))

    private fun apolloHttp(code: Int) =
        ApolloHttpException(statusCode = code, headers = emptyList(), body = null, message = "HTTP $code")

    // -- Retrofit HttpException --------------------------------------------------------

    @Test
    fun whenRetrofit401_mapsToUnauthorized() {
        assertThat(ErrorMapper.map(retrofitHttp(401))).isEqualTo(AppError.Unauthorized(Constants.ERROR_AUTH))
    }

    @Test
    fun whenRetrofit404_mapsToNotFound() {
        assertThat(ErrorMapper.map(retrofitHttp(404))).isEqualTo(AppError.NotFound(Constants.ERROR_NOT_FOUND))
    }

    @Test
    fun whenRetrofit429_mapsToRateLimited() {
        assertThat(ErrorMapper.map(retrofitHttp(429))).isEqualTo(AppError.RateLimited(Constants.ERROR_RATE_LIMIT))
    }

    @Test
    fun whenRetrofitCodeIs500_mapsToServerWithCode() {
        assertThat(ErrorMapper.map(retrofitHttp(500))).isEqualTo(AppError.Server(500, Constants.ERROR_SERVER))
    }

    // El límite superior del rango 500..599 es el que detecta un cambio a 500..598.
    @Test
    fun whenRetrofitCodeIs599_mapsToServerWithServerMessage() {
        assertThat(ErrorMapper.map(retrofitHttp(599))).isEqualTo(AppError.Server(599, Constants.ERROR_SERVER))
    }

    @Test
    fun whenRetrofitCodeIs400_mapsToServerWithUnknownMessage() {
        assertThat(ErrorMapper.map(retrofitHttp(400))).isEqualTo(AppError.Server(400, Constants.ERROR_UNKNOWN))
    }

    @Test
    fun whenRetrofitCodeIs600_mapsToServerWithUnknownMessage() {
        assertThat(ErrorMapper.map(retrofitHttp(600))).isEqualTo(AppError.Server(600, Constants.ERROR_UNKNOWN))
    }

    // -- Apollo HTTP -------------------------------------------------------------------

    @Test
    fun whenApollo401_mapsToUnauthorized() {
        assertThat(ErrorMapper.map(apolloHttp(401))).isEqualTo(AppError.Unauthorized(Constants.ERROR_AUTH))
    }

    @Test
    fun whenApollo404_mapsToNotFound() {
        assertThat(ErrorMapper.map(apolloHttp(404))).isEqualTo(AppError.NotFound(Constants.ERROR_NOT_FOUND))
    }

    @Test
    fun whenApollo429_mapsToRateLimited() {
        assertThat(ErrorMapper.map(apolloHttp(429))).isEqualTo(AppError.RateLimited(Constants.ERROR_RATE_LIMIT))
    }

    @Test
    fun whenApolloCodeIs500_mapsToServerWithCode() {
        assertThat(ErrorMapper.map(apolloHttp(500))).isEqualTo(AppError.Server(500, Constants.ERROR_SERVER))
    }

    @Test
    fun whenApolloCodeIs599_mapsToServerWithServerMessage() {
        assertThat(ErrorMapper.map(apolloHttp(599))).isEqualTo(AppError.Server(599, Constants.ERROR_SERVER))
    }

    @Test
    fun whenApolloCodeIs400_mapsToServerWithUnknownMessage() {
        assertThat(ErrorMapper.map(apolloHttp(400))).isEqualTo(AppError.Server(400, Constants.ERROR_UNKNOWN))
    }

    // -- Red y GraphQL -----------------------------------------------------------------

    @Test
    fun whenApolloNetworkException_mapsToNetwork() {
        assertThat(ErrorMapper.map(ApolloNetworkException("no connection")))
            .isEqualTo(AppError.Network(Constants.ERROR_NETWORK))
    }

    @Test
    fun whenGraphQLDataException_mapsToGraphQLKeepingItsMessage() {
        assertThat(ErrorMapper.map(GraphQLDataException("campo inválido")))
            .isEqualTo(AppError.GraphQL("campo inválido"))
    }

    @Test
    fun whenIOException_mapsToNetwork() {
        assertThat(ErrorMapper.map(IOException("socket closed")))
            .isEqualTo(AppError.Network(Constants.ERROR_NETWORK))
    }

    // Caracterización: un timeout se muestra como "sin conexión". Hay un pendiente para distinguirlos;
    // cuando se haga, este es el test que cambia.
    @Test
    fun whenSocketTimeout_mapsToNetworkLikeAnyIOException() {
        assertThat(ErrorMapper.map(SocketTimeoutException("timeout")))
            .isEqualTo(AppError.Network(Constants.ERROR_NETWORK))
    }

    // -- Resto -------------------------------------------------------------------------

    @Test
    fun whenUnexpectedException_mapsToUnknown() {
        assertThat(ErrorMapper.map(IllegalStateException("boom")))
            .isEqualTo(AppError.Unknown(Constants.ERROR_UNKNOWN))
    }

    // Caracterización: map() no filtra CancellationException; por eso safeApiCall la relanza antes de llamarlo.
    @Test
    fun whenCancellationException_mapperTreatsItAsUnknown() {
        assertThat(ErrorMapper.map(CancellationException("cancelled")))
            .isEqualTo(AppError.Unknown(Constants.ERROR_UNKNOWN))
    }
}
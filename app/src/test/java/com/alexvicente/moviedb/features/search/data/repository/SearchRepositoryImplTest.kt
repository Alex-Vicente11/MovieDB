package com.alexvicente.moviedb.features.search.data.repository

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.search.data.remote.api.SearchApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: SearchApi
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
            .create(SearchApi::class.java)

        repository = SearchRepositoryImpl(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun enqueueMockResponse(code: Int = 200, body: String = "") {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json")
        )
    }

    private fun buildMovieResponseJson(
        id: Int = 1,
        title: String = "Inception",
        overview: String = "A thief who steals corporate secrets",
        posterPath: String? = "/poster.jpg",
        voteAverage: Double = 8.8,
        voteCount: Int = 30000,
        releaseDate: String = "2010-07-16",
        popularity: Double = 100.0
    ): String {
        val posterValue = if (posterPath != null) "\"$posterPath\"" else "null"
        return """
            {
              "page": 1,
              "total_pages": 1,
              "total_results": 1,
              "results": [
                {
                  "id": $id,
                  "title": "$title",
                  "overview": "$overview",
                  "poster_path": $posterValue,
                  "backdrop_path": null,
                  "vote_average": $voteAverage,
                  "vote_count": $voteCount,
                  "release_date": "$releaseDate",
                  "popularity": $popularity,
                  "adult": false,
                  "original_language": "en",
                  "original_title": "$title",
                  "video": false,
                  "genre_ids": [28, 12]
                }
              ]
            }
        """.trimIndent()
    }

    private fun buildEmptyResultsJson() =
        """{"page":1,"total_pages":0,"total_results":0,"results":[]}"""

    // Tests de éxito

    @Test
    fun whenApiReturns200_emitsLoadingThenSuccess() = runTest {
        enqueueMockResponse(code = 200, body = buildMovieResponseJson())

        repository.searchMovies("Inception").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(Resource.Success::class.java)
            assertThat((success as Resource.Success).data).hasSize(1)
            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns200_mapsJsonToMovieDomainCorrectly() = runTest {
        enqueueMockResponse(
            code = 200,
            body = buildMovieResponseJson(
                id = 27205,
                title = "  Inception  ",
                overview = "A thief who steals corporate secrets",
                posterPath = "/poster.jpg",
                voteAverage = 8.8,
                voteCount = 30000,
                releaseDate = "2010-07-16",
                popularity = 100.0
            )
        )

        repository.searchMovies("Inception").test {
            awaitItem()
            val success = awaitItem() as Resource.Success
            val movie = success.data!!.first()
            assertThat(movie.id).isEqualTo(27205)
            assertThat(movie.title).isEqualTo("Inception")
            assertThat(movie.overview).isEqualTo("A thief who steals corporate secrets")
            assertThat(movie.posterPath).isEqualTo("/poster.jpg")
            assertThat(movie.voteAverage).isEqualTo(8.8)
            assertThat(movie.voteCount).isEqualTo(30000)
            assertThat(movie.releaseDate).isEqualTo("2010-07-16")
            assertThat(movie.popularity).isEqualTo(100.0)
            awaitComplete()
        }
    }

    @Test
    fun whenApiReturnsEmptyResults_emitsSuccessWithEmptyList() = runTest {
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        repository.searchMovies("xewcwnotfound").test {
            awaitItem()
            val success = awaitItem()
            assertThat(success).isInstanceOf(Resource.Success::class.java)
            assertThat((success as Resource.Success).data).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun whenMovieHasNullPosterPath_mapsToNullInDomain() = runTest {
        enqueueMockResponse(code = 200, body = buildMovieResponseJson(posterPath = null))

        repository.searchMovies("test").test {
            awaitItem()
            val success = awaitItem() as Resource.Success
            assertThat(success.data!!.first().posterPath).isNull()
            awaitComplete()
        }
    }

    // Tests de errores HTTP

    @Test
    fun whenApiReturns401_emitsLoadingThenAuthError() = runTest {
        enqueueMockResponse(code = 401, body = """{"status_message":"Invalid API key"}""")

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            assertThat((error as Resource.Error).message)
                .isEqualTo(Constants.ERROR_AUTH)        // ← actualizado
            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns404_emitsLoadingThenNotFoundError() = runTest {
        enqueueMockResponse(code = 404, body = """{"status_message":"Not found"}""")

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            assertThat((error as Resource.Error).message)
                .isEqualTo(Constants.ERROR_NOT_FOUND)   // ← actualizado
            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns500_emitsLoadingThenServerError() = runTest {
        enqueueMockResponse(code = 500, body = """{"status_message":"Internal error"}""")

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            assertThat((error as Resource.Error).message)
                .isEqualTo(Constants.ERROR_SERVER)      // ← actualizado
            awaitComplete()
        }
    }

    // Tests de errores de red

    @Test
    fun whenServerClosesConnectionAbruptly_emitsConnectionError() = runTest {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            assertThat((error as Resource.Error).message)
                .isEqualTo(Constants.ERROR_NETWORK)     // ← actualizado
            awaitComplete()
        }
    }

    // Tests de request

    @Test
    fun whenSearchIsCalled_sendsCorrectQueryParameter() = runTest {
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        repository.searchMovies("batman").test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        val request = mockWebServer.takeRequest()
        assertThat(request.path).contains("query=batman")
        assertThat(request.path).contains("/search/movie")
    }

    @Test
    fun whenSearchIsCalled_sendsDefaultLanguageParameter() = runTest {
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        repository.searchMovies("test").test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        val request = mockWebServer.takeRequest()
        assertThat(request.path).contains("language=es-MX")
    }
}
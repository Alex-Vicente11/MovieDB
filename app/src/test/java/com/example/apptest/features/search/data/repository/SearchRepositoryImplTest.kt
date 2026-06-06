package com.example.apptest.features.search.data.repository

import app.cash.turbine.test
import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.search.data.remote.api.SearchApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Test del repositorio de búsqueda usando MockWebServer
 *
 * Estrategia:
 * - MockWebServer levanta un servidor HTTP local en un puerto aleatorio
 * - Retrofit se configura para apuntar a ese servidor (no a TMDB real)
 * - Cada test encola una MockResponse con el JSON y código HTTP deseado
 * - El repositorio no sabe que está hablando con un servidor falso
 *
 * Se testea aquí:
 * - Mapeo correcto de JSON -> dominio
 * - Manejo de errores HTTP (401, 404, 500)
 * - Manejo de errores de red (IOException)
 * - Emisión correcta de Resource.Loading -> Resource.Success/Error
 */

class SearchRepositoryImplTest {

    // Servidor HTTP falso que corre localmente durante los tests
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: SearchApi
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setUp() {
        // Iniciar el servidor en un puerto disponible aleatorio
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Retrofit apunta a la URL del servidor falso en lugar de TMDB
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
        // Cierra el servidor después de cada test para liberar el puerto
        mockWebServer.shutdown()
    }

    // Helpers
    /**
     * Encola una respuesta exitosa con el JSON dado.
     * MockWebServer devuelve las respuestas en orden FIFO.
     */
    private fun enqueueMockResponse(code: Int = 200, body: String = "") {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json")
        )
    }

    /**
     * JSON mínimo válido que imita la respuesta real de TMDB /search/movie
     * Los campos siguen exactamente los @SerializedName de MovieDto
     */
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

    // JSON de lista vacia - cuando TMDB no encuentra resultados
    private fun buildEmptyResultsJson() =
        """{"page":1,"total_pages":0,"total_results":0,"results":[]}"""

    // Tests de éxito

    @Test
    fun whenApiReturns200_emitsLoadingThenSuccess() = runTest {
        // Given encolamos la respuesta antes de hacer la llamada
        enqueueMockResponse(code = 200, body = buildMovieResponseJson())

        // When/Then
        repository.searchMovies("Inception").test {
            // Primera emisión: Loading (el repositorio la emite antes de la llamada)
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)

            // Segunda emisión: Success con los datos mapeados
            val success = awaitItem()
            assertThat(success).isInstanceOf(Resource.Success::class.java)
            assertThat((success as Resource.Success).data).hasSize(1)

            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns200_mapsJsonToMovieDomainCorrectly() = runTest {
        // Given - verificamos que cada campo del JSON llega correcto al dominio
        enqueueMockResponse(
            code = 200,
            body = buildMovieResponseJson(
                id = 27205,
                title = "  Inception  ", // con espacios - el mapper debe hacer trim
                overview = "A thief who steals corporate secrets",
                posterPath = "/poster.jpg",
                voteAverage = 8.8,
                voteCount = 30000,
                releaseDate = "2010-07-16",
                popularity = 100.0
            )
        )
        repository.searchMovies("Inception").test {
            awaitItem() // Descartamos el Loading

            val success = awaitItem() as Resource.Success
            val movie = success.data!!.first()

            // Verificamos cada campo del mapper
            assertThat(movie.id).isEqualTo(27205)
            assertThat(movie.title).isEqualTo("Inception") // trim aplicado
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
        // Given - TMDB responde 200 sin resultados
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        repository.searchMovies("xewcwnotfound").test {
            awaitItem() // Descartamos el Loading

            val success = awaitItem()
            assertThat(success).isInstanceOf(Resource.Success::class.java)
            // Lista vacía es un resultado válido, no un error
            assertThat((success as Resource.Success).data).isEmpty()

            awaitComplete()
        }
    }

    @Test
    fun whenMovieHasNullPosterPath_mapsToNullInDomain() = runTest {
        // Given - poster_path null es común en películas sin imagen
        enqueueMockResponse(
            code = 200,
            body = buildMovieResponseJson(posterPath = null)
        )

        repository.searchMovies("test").test {
            awaitItem() // Descartamos el Loading

            val success = awaitItem() as Resource.Success
            // El mapper no debe crashear con poster_path null
            assertThat(success.data!!.first().posterPath).isNull()

            awaitComplete()
        }
    }

    // Tests de errores HTTP
    @Test
    fun whenApiReturns401_EmitsLoadingThenAuthError() = runTest {
        // Given - token inválido o expirado
        enqueueMockResponse(code = 401, body = """{"status_message": "Invalid API key: "}""")

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            // verifica el mensaje especifico del repositorio para 401
            assertThat((error as Resource.Error).message).isEqualTo("Error de autenticación")

            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns404_emitsLoadingThenNotFoundError() = runTest {
        // Given - endpoint no encontrado
        enqueueMockResponse(code = 404, body = """{"status_message": "Not found"}""")

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            assertThat((error as Resource.Error).message).isEqualTo("No se encontraron películas")

            awaitComplete()
        }
    }

    @Test
    fun whenApiReturns500_emitsLoadingThenGenericServerError() = runTest {
        // Given - error interno del servidor
        enqueueMockResponse(code = 500, body = """{"status_message": "Internal error"}""")

        repository.searchMovies("test").test {
            assertThat((awaitItem())).isInstanceOf(Resource.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            // El repositorio usa "Error del servidor: {code}" para códigos no mapeados
            assertThat((error as Resource.Error).message).isEqualTo("Error del servidor: 500")

            awaitComplete()
        }
    }

    // Tests de errores de red

    @Test
    fun whenServerClosesConnectionAbruptly_emitsConnectionError() = runTest {
        // Given - simula pérdida de conexión cerrando el socket sin respuesta
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(
                okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START
            )
        )

        repository.searchMovies("test").test {
            assertThat(awaitItem()).isInstanceOf(Resource.Loading::class.java)

            val error = awaitItem()
            assertThat(error).isInstanceOf(Resource.Error::class.java)
            // IOException -> mensaje de error de conexión definido en el repositorio
            assertThat((error as Resource.Error).message)
                .isEqualTo("Error de conexión. Verifica tu internet.")

            awaitComplete()
        }
    }

    // Tests de request

    @Test
    fun whenSearchIsCalled_sendsCorrectQueryParameter() = runTest {
        // Given
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        // When
        repository.searchMovies("batman").test {
            awaitItem() // Descartamos el Loading
            awaitItem() // Descartamos el Success
            awaitComplete()
        }
        // Then - inspeccionamos la request que MockWebServer recibió
        val request = mockWebServer.takeRequest()
        // Verifica que el query llegó correctamente en la URL
        assertThat(request.path).contains("query=batman")
        // Verifica el endpoint correcto
        assertThat(request.path).contains("/search/movie")
    }

    @Test
    fun whenSearchIsCalled_sendsDefaultLanguageParameter() = runTest {
        // Given
        enqueueMockResponse(code = 200, body = buildEmptyResultsJson())

        // When
        repository.searchMovies("test").test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        // Then - verifica que el parámetro de idioma por defecto se envía
        val request = mockWebServer.takeRequest()
        assertThat(request.path).contains("language=es-MX")
    }
}
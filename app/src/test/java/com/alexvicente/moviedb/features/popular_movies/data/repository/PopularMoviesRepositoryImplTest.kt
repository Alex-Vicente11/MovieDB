package com.alexvicente.moviedb.features.popular_movies.data.repository

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.entity.MovieEntity
import com.alexvicente.moviedb.features.popular_movies.data.remote.api.PopularMoviesApi
import com.alexvicente.moviedb.testutil.extensions.awaitError
import com.alexvicente.moviedb.testutil.extensions.awaitLoading
import com.alexvicente.moviedb.testutil.extensions.awaitSuccess
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
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

class PopularMoviesRepositoryImplTest {

    // Servidor HTTP falso - Retrofit apuntará aquí en lugar de TMDB
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: PopularMoviesApi

    // Mock del DAO - controla qué devuelve Room sin base de datos real
    private lateinit var mockDao: MovieDao
    private lateinit var repository: PopularMoviesRepositoryImpl

    // Constantes de tiempo

    // Simula un timestamp de hace 10 minutos - caché válido (< 30 min)
    private val recentCacheTime = System.currentTimeMillis() - (10 * 60 * 1000L)
    // Simula un timestamp de hace 60 minutos - caché expirado (> 30 min)
    private val expiredCacheTime = System.currentTimeMillis() - (60 * 60 * 1000L)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
            .create(PopularMoviesApi::class.java)

        // MockK crea una implementación falsa de la interfaz MovieDao
        // Cada test configura su comportamiento con coEvery
        mockDao = mockk(relaxed = true)
        // relaxed = true -> las funciones suspend que no se configuran
        // con coEvery retornan valores por defecto (Util, null, 0...)
        // sin lanzar excepciones. Útil para insertMovies y deletePopularMovies
        // que son escrituras que no necesitamos verificar en todos los tests.

        repository = PopularMoviesRepositoryImpl(api, mockDao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // Helpers

    /**
     * Crea una lista de MovieEntity a partir de MovieFactory.
     * Traduce los Movie de dominio a entidades de Room para configurar el mock del DAO.
     */
    private fun createEntityList(size: Int): List<MovieEntity> =
        MovieFactory.createMovieList(size).map { movie ->
            MovieEntity(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                posterPath = movie.posterPath,
                backdropPath = movie.backdropPath,
                voteAverage = movie.voteAverage,
                voteCount = movie.voteCount,
                releaseDate = movie.releaseDate,
                popularity = movie.popularity,
                isPopular = true,
                cachedAt = recentCacheTime
            )
        }

    /**
     * Encola una respuesta HTTP en MockWebServer
     * El servidor devuelve las respuestas en orden FIFO
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
     * JSON mínimo válido de /movie/popular en N películas.
     * Sigue exactamente los @SerializedName de MovieDto.
     */
    private fun buildPopularMoviesJson(count: Int = 3): String {
        val results = (1..count).joinToString(",") { i ->
            """
            {
              "id": $i,
              "title": "Movie $i",
              "overview": "Overview $i",
              "poster_path": "/poster$i.jpg",
              "backdrop_path": null,
              "vote_average": ${7.0 + i * 0.1},
              "vote_count": ${1000 * i},
              "release_date": "2024-01-01",
              "popularity": ${100.0 - i},
              "adult": false,
              "original_language": "en",
              "original_title": "Movie $i",
              "video": false,
              "genre_ids": [28]
            }
            """.trimIndent()
        }
        return """{"page":1,"total_pages":1,"total_results":$count,"results":[$results]}"""
    }

    /**
     * Configura el DAO para simular caché vacío
     * Es el estado inicial antes de la primera carga.
     */
    private fun setupEmptyCache() {
        coEvery { mockDao.getPopularMovies() } returns flowOf(emptyList())
        coEvery { mockDao.getLastCacheTime() } returns null
    }

    /**
     * Configura el DAO para simular caché con datos válidos (no expirado).
     */
    private fun setupValidCache(size: Int = 3) {
        val entities = createEntityList(size)
        coEvery {mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns recentCacheTime
    }

    /**
     * Configura el DAO para simular caché expirado.
     * Tiene datos pero el timestamp indica que pasaron más de 30 minutos.
     */
    private fun setupExpiredCache(size: Int = 3) {
        val entities = createEntityList(size)
        coEvery { mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns expiredCacheTime
    }

    // Tests: caché vacío - primer uso

    @Test
    fun whenCacheIsEmpty_emitsLoadingThenFetchesFromApi() = runTest {
        // Given - sin caché, debe ir directo a la red
        setupEmptyCache()
        enqueueMockResponse(code = 200, body = buildPopularMoviesJson(count = 3))

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            // Sin caché no hay emisión intermedia - va directo al resultado de red
            awaitSuccess { movies ->
                assertThat(movies).hasSize(3)
            }

            awaitComplete()
        }
    }

    @Test
    fun whenCacheIsEmpty_savesApiResultToRoom() = runTest {
        // Given
        setupEmptyCache()
        enqueueMockResponse(code = 200, body = buildPopularMoviesJson(count = 3))

        // When
        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        // Then - verifica que el repositorio guardó los datos en Room
        coVerify (exactly = 1) { mockDao.deletePopularMovies() }
        coVerify (exactly = 1) { mockDao.insertMovies(any()) }
    }

    // Tests: caché válido - no expirado

    @Test
    fun whenCacheIsValid_emitsLoadingThenCacheWithoutCallingApi() = runTest {
        // Given - caché reciente (10 min), no debe llamar a la red
        setupValidCache(size = 5)

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            // Solo emite el caché - no hay segunda emisión de red
            awaitSuccess { movies ->
                assertThat(movies).hasSize(5)
            }

            awaitComplete()
        }
        // Verifica que MockWebServer no recibió ninguna request
        assertThat(mockWebServer.requestCount).isEqualTo(0)
    }

    @Test
    fun whenCacheIsValid_doesNotWriteToRoom() = runTest {
        // Given
        setupValidCache()

        // When
        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        // Then  - caché válido no debe borrar ni insertar nada
        coVerify (exactly = 0) { mockDao.deletePopularMovies() }
        coVerify (exactly = 0) { mockDao.insertMovies(any()) }
    }

    // Tests: caché expirado - offline-first completo

    @Test
    fun whenCacheIsExpired_emitsLoadingThenCacheThenFreshData() = runTest {
        // Given - caché expirado (60 min): emite caché primero, luego va a la red
        setupExpiredCache(size = 3)
        enqueueMockResponse(code = 200, body = buildPopularMoviesJson(count = 5))

        // When / Then - secuencia completa offline-first
        repository.getPopularMovies().test {
            awaitLoading()

            // Primera Success: datos del caché (inmediato, sin esperar red)
            awaitSuccess { movies ->
                assertThat(movies).hasSize(3)
            }

            // Segunda Success: datos de la red (espera)
            awaitSuccess { movies ->
                assertThat(movies).hasSize(5)
            }

            awaitComplete()
        }
    }

    @Test
    fun whenCacheIsExpired_updatesRoomWithFreshData() = runTest {
        // Given
        setupExpiredCache()
        enqueueMockResponse(code = 200, body = buildPopularMoviesJson(count = 5))

        // When
        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess() // cache
            awaitSuccess() // red
            awaitComplete()
        }

        // Then - el caché expirado se borra y se reemplaza con datos frescos
        coVerify (exactly = 1) { mockDao.deletePopularMovies() }
        coVerify (exactly = 1) { mockDao.insertMovies(any()) }
    }

    // Tests: errores HTTP con caché vacío

    @Test
    fun whenCacheEmpty401_emitsLoadingThenAuthError() = runTest {
        // Given
        setupEmptyCache()
        enqueueMockResponse(code = 401, body = """{"status_message":"Invalid API key"}""")

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message ->
                assertThat(message).isEqualTo("Error de autenticación. Verifica el token")
            }

            awaitComplete()
        }
    }

    @Test
    fun whenCacheEmptyAnd404_emitsLoadingThenNotFoundError() = runTest {
        setupEmptyCache()
        enqueueMockResponse(code = 404, body = """{"status_message":"Not found"}""")

        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message ->
                assertThat(message).isEqualTo("Recurso no encontrado")
            }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheEmptyAnd500_emitsLoadingThenServerError() = runTest {
        // Given
        setupEmptyCache()
        enqueueMockResponse(code = 500, body = """{"status_message":"Internal error"}""")

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message ->
                assertThat(message).isEqualTo("Error del servidor. Intenta más tarde")
            }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheEmptyAndNoNetwork_emitsLoadingThenConnectionError() = runTest {
        // Given - simula pérdida de conexión cerrando el socket
        setupEmptyCache()
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message ->
                assertThat(message).isEqualTo("Sin conexión. Verifica tu internet.")
            }
            awaitComplete()
        }
    }

    // Tests: erorres HTTP con caché disponible

    @Test
    fun whenCacheAvailableAndNetworkFails_doesNotEmitError() = runTest {
        // Given - hay cache válido pero expirado, y la red falla
        // El repositorio emite el caché y silencia el error (comportamiento offline-first)
        setupExpiredCache(size = 3)
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            // Emite el caché aunque esté expirado
            awaitSuccess { movies ->
                assertThat(movies).hasSize(3)
            }

            //No emite error - el usuario ya tiene datos para ver
            awaitComplete()
        }

    }

    @Test
    fun whenCacheAvailableAnd401_doesNotEmitError() = runTest {
        // Given - cache expirado + error de autenticación
        setupExpiredCache(size = 3)
        enqueueMockResponse(code = 401, body = """{"status_message":"Invalid API key"}""")

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            // El caché protege al usuario del error HTTP
            awaitSuccess { movies ->
                assertThat(movies).hasSize(3)
            }

            // No emite Resource.Error - el repositorio lo silencia intencionalmente
            awaitComplete()
        }
    }

    // Tests: mapeo de datos

    @Test
    fun whenApiResponds_mapsJsonToMovieDomainCorrectly() = runTest {
        // Given - verificamos campo por campo que el mapper funciona
        setupEmptyCache()
        enqueueMockResponse(
            code = 200,
            body = """
                {"page":1,"total_pages":1,"total_results":1,"results":[
                  {"id":27205,"title":"  Inception  ","overview":"A thief",
                   "poster_path":"/poster.jpg","backdrop_path":null,
                   "vote_average":8.8,"vote_count":30000,"release_date":"2010-07-16",
                   "popularity":100.0,"adult":false,"original_language":"en",
                   "original_title":"Inception","video":false,"genre_ids":[28]}
                ]}
            """.trimIndent()
        )

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            awaitSuccess { movies ->
                val movie = movies.first()
                assertThat(movie.id).isEqualTo(27205)
                // El mapper hace trim() al title
                assertThat(movie.title).isEqualTo("Inception")
                assertThat(movie.overview).isEqualTo("A thief")
                assertThat(movie.posterPath).isEqualTo("/poster.jpg")
                assertThat(movie.backdropPath).isNull()
                assertThat(movie.voteAverage).isEqualTo(8.8)
                assertThat(movie.releaseDate).isEqualTo("2010-07-16")
            }

            awaitComplete()
        }
    }

    @Test
    fun whenCacheEntitiesAreLoaded_mapsEntitiesToDomainCorrectly() = runTest {
        // Given - verificamos que el mapper de Entity -> Domain funciona
        val entities = listOf(
            MovieEntity(
                id = 1,
                title = "Cached Movie",
                overview = "Cached overview",
                posterPath = "/cached.jpg",
                backdropPath = null,
                voteAverage = 7.5,
                voteCount = 5000,
                releaseDate = "2023-01-01",
                popularity = 80.0,
                isPopular = true,
                cachedAt = recentCacheTime
            )
        )
        coEvery { mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns recentCacheTime

        // When / Then
        repository.getPopularMovies().test {
            awaitLoading()

            awaitSuccess { movies ->
                val movie = movies.first()
                // toDomain() debe preservar todos los campos correctamente
                assertThat(movie.id).isEqualTo(1)
                assertThat(movie.title).isEqualTo("Cached Movie")
                assertThat(movie.posterPath).isEqualTo("/cached.jpg")
                assertThat(movie.backdropPath).isNull()
                assertThat(movie.voteAverage).isEqualTo(7.5)
            }

            awaitComplete()
        }
    }

    // Tests: request Http

    @Test
    fun whenApiFetched_hitsCorrectEndpoint() = runTest {
        // Given
        setupEmptyCache()
        enqueueMockResponse(code = 200, body = buildPopularMoviesJson())

        // When
        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        // Then - verifica que Retrofit construyó la URL correctamente
        val request = mockWebServer.takeRequest()
        assertThat(request.path).contains("/movie/popular")
        assertThat(request.path).contains("language=es-MX")
        assertThat(request.method).isEqualTo("GET")
    }
}
package com.alexvicente.moviedb.features.popular_movies.data.repository

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.local.dao.MovieDao
import com.alexvicente.moviedb.core.data.local.entity.MovieEntity
import com.alexvicente.moviedb.testutil.extensions.awaitError
import com.alexvicente.moviedb.testutil.extensions.awaitLoading
import com.alexvicente.moviedb.testutil.extensions.awaitSuccess
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.graphql.PopularMoviesQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID


class PopularMoviesRepositoryImplTest {

    // Mock del ApolloClient - reemplaza al MockWebServer de la era Retrofit.
    // No simulamos HTTP crudo: simulamos directamente lo que .execute() devuelve.
    private lateinit var apolloClient: ApolloClient
    private lateinit var mockApolloCall: ApolloCall<PopularMoviesQuery.Data>

    private lateinit var mockDao: MovieDao
    private lateinit var repository: PopularMoviesRepositoryImpl

    private val query = PopularMoviesQuery(page = 1)

    private val recentCacheTime = System.currentTimeMillis() - (10 * 60 * 1000L)
    private val expiredCacheTime = System.currentTimeMillis() - (60 * 60 * 1000L)

    @Before
    fun setUp() {
        apolloClient = mockk()
        mockApolloCall = mockk()

        // apolloClient.query(...) siempre devuelve el mismo ApolloCall mockeado;
        // cada test configura qué hace .execute() sobre ese call.
        coEvery { apolloClient.query(query) } returns mockApolloCall

        mockDao = mockk(relaxed = true)
        repository = PopularMoviesRepositoryImpl(apolloClient, mockDao)
    }

    // Helpers

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
     * Construye una lista de PopularMoviesQuery.PopularMovie (el tipo generado por Apollo),
     * equivalente al JSON crudo que antes armaba buildPopularMoviesJson().
     */
    private fun buildPopularMovies(count: Int = 3): List<PopularMoviesQuery.PopularMovie> =
        (1..count).map { i ->
            PopularMoviesQuery.PopularMovie(
                id = i,
                title = "Movie $i",
                overview = "Overview $i",
                posterPath = "/poster$i.jpg",
                backdropPath = null,
                releaseDate = "2024-01-01",
                voteAverage = 7.0 + i * 0.1,
                voteCount = 1000 * i,
                popularity = 100.0 - i,
                genres = emptyList()
            )
        }

    // === VERIFICAR: construcción de ApolloResponse ===
    // El Builder exacto de ApolloResponse varía entre versiones de Apollo Kotlin.
    // Esta es la firma documentada para Apollo Kotlin 4.x — si tu versión difiere,
    // el autocompletado del IDE al escribir "ApolloResponse.Builder(" te mostrará
    // los parámetros reales que acepta tu versión instalada.
    private fun buildSuccessResponse(
        movies: List<PopularMoviesQuery.PopularMovie>
    ): ApolloResponse<PopularMoviesQuery.Data> =
        ApolloResponse.Builder(
            operation = query,
            requestUuid = UUID.randomUUID()
        ).data(PopularMoviesQuery.Data(popularMovies = movies)).build()

    private fun buildErrorResponseNoData(message: String): ApolloResponse<PopularMoviesQuery.Data> =
        ApolloResponse.Builder(
            operation = query,
            requestUuid = UUID.randomUUID()
        ).errors(listOf(Error.Builder(message = message).build())).build()

    private fun setupEmptyCache() {
        coEvery { mockDao.getPopularMovies() } returns flowOf(emptyList())
        coEvery { mockDao.getLastCacheTime() } returns null
    }

    private fun setupValidCache(size: Int = 3) {
        val entities = createEntityList(size)
        coEvery { mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns recentCacheTime
    }

    private fun setupExpiredCache(size: Int = 3) {
        val entities = createEntityList(size)
        coEvery { mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns expiredCacheTime
    }

    // Tests: caché vacío - primer uso

    @Test
    fun whenCacheIsEmpty_emitsLoadingThenFetchesFromApi() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(buildPopularMovies(3))

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies -> assertThat(movies).hasSize(3) }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheIsEmpty_savesApiResultToRoom() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(buildPopularMovies(3))

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        coVerify(exactly = 1) { mockDao.deletePopularMovies() }
        coVerify(exactly = 1) { mockDao.insertMovies(any()) }
    }

    // Tests: caché válido - no expirado

    @Test
    fun whenCacheIsValid_emitsLoadingThenCacheWithoutCallingApi() = runTest {
        setupValidCache(size = 5)

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies -> assertThat(movies).hasSize(5) }
            awaitComplete()
        }

        // Caché válido: nunca se llama a Apollo
        coVerify(exactly = 0) { apolloClient.query(any<PopularMoviesQuery>()) }
    }

    @Test
    fun whenCacheIsValid_doesNotWriteToRoom() = runTest {
        setupValidCache()

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        coVerify(exactly = 0) { mockDao.deletePopularMovies() }
        coVerify(exactly = 0) { mockDao.insertMovies(any()) }
    }

    // Tests: caché expirado - offline-first completo

    @Test
    fun whenCacheIsExpired_emitsLoadingThenCacheThenFreshData() = runTest {
        setupExpiredCache(size = 3)
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(buildPopularMovies(5))

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies -> assertThat(movies).hasSize(3) }
            awaitSuccess { movies -> assertThat(movies).hasSize(5) }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheIsExpired_updatesRoomWithFreshData() = runTest {
        setupExpiredCache()
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(buildPopularMovies(5))

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess() // cache
            awaitSuccess() // red
            awaitComplete()
        }

        coVerify(exactly = 1) { mockDao.deletePopularMovies() }
        coVerify(exactly = 1) { mockDao.insertMovies(any()) }
    }

    // Tests: errores con caché vacío

    // === VERIFICAR: constructor de ApolloHttpException ===
    // Firma documentada en Apollo Kotlin 4.x: (statusCode, headers, body, message, cause).
    // Confirma con el autocompletado si tu versión difiere.
    @Test
    fun whenCacheEmpty401_emitsLoadingThenAuthError() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } throws ApolloHttpException(
            statusCode = 401,
            headers = emptyList(),
            body = null,
            message = "Unauthorized"
        )

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
        coEvery { mockApolloCall.execute() } throws ApolloHttpException(
            statusCode = 404, headers = emptyList(), body = null, message = "Not found"
        )

        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Recurso no encontrado") }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheEmptyAnd500_emitsLoadingThenServerError() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } throws ApolloHttpException(
            statusCode = 500, headers = emptyList(), body = null, message = "Internal error"
        )

        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Error del servidor. Intenta más tarde") }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheEmptyAndNoNetwork_emitsLoadingThenConnectionError() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } throws ApolloNetworkException("No connection")

        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Sin conexión. Verifica tu internet.") }
            awaitComplete()
        }
    }

    // Test nuevo: error GraphQL sin datos (específico de Apollo, no existía en la era Retrofit)
    @Test
    fun whenCacheEmptyAndGraphQLErrorNoData_emitsGraphQLError() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } returns buildErrorResponseNoData("Error GraphQL desconocido")

        repository.getPopularMovies().test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Error GraphQL desconocido") }
            awaitComplete()
        }
    }

    // Tests: errores con caché disponible - comportamiento offline-first

    @Test
    fun whenCacheAvailableAndNetworkFails_doesNotEmitError() = runTest {
        setupExpiredCache(size = 3)
        coEvery { mockApolloCall.execute() } throws ApolloNetworkException("No connection")

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies -> assertThat(movies).hasSize(3) }
            awaitComplete()
        }
    }

    @Test
    fun whenCacheAvailableAnd401_doesNotEmitError() = runTest {
        setupExpiredCache(size = 3)
        coEvery { mockApolloCall.execute() } throws ApolloHttpException(
            statusCode = 401, headers = emptyList(), body = null, message = "Unauthorized"
        )

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies -> assertThat(movies).hasSize(3) }
            awaitComplete()
        }
    }

    // Tests: mapeo de datos

    @Test
    fun whenApiResponds_mapsDataToMovieDomainCorrectly() = runTest {
        setupEmptyCache()
        val apiMovie = PopularMoviesQuery.PopularMovie(
            id = 27205,
            title = "  Inception  ",
            overview = "A thief",
            posterPath = "/poster.jpg",
            backdropPath = null,
            releaseDate = "2010-07-16",
            voteAverage = 8.8,
            voteCount = 30000,
            popularity = 100.0,
            genres = emptyList()
        )
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(listOf(apiMovie))

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies ->
                val movie = movies.first()
                assertThat(movie.id).isEqualTo(27205)
                assertThat(movie.title).isEqualTo("Inception") // el mapper hace trim()
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
        val entities = listOf(
            MovieEntity(
                id = 1, title = "Cached Movie", overview = "Cached overview",
                posterPath = "/cached.jpg", backdropPath = null, voteAverage = 7.5,
                voteCount = 5000, releaseDate = "2023-01-01", popularity = 80.0,
                isPopular = true, cachedAt = recentCacheTime
            )
        )
        coEvery { mockDao.getPopularMovies() } returns flowOf(entities)
        coEvery { mockDao.getLastCacheTime() } returns recentCacheTime

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess { movies ->
                val movie = movies.first()
                assertThat(movie.id).isEqualTo(1)
                assertThat(movie.title).isEqualTo("Cached Movie")
                assertThat(movie.posterPath).isEqualTo("/cached.jpg")
                assertThat(movie.backdropPath).isNull()
                assertThat(movie.voteAverage).isEqualTo(7.5)
            }
            awaitComplete()
        }
    }

    // Test nuevo: confirma que el repositorio arma la query GraphQL correcta
    @Test
    fun whenFetching_queriesWithCorrectPage() = runTest {
        setupEmptyCache()
        coEvery { mockApolloCall.execute() } returns buildSuccessResponse(buildPopularMovies())

        repository.getPopularMovies().test {
            awaitLoading()
            awaitSuccess()
            awaitComplete()
        }

        coVerify(exactly = 1) { apolloClient.query(PopularMoviesQuery(page = 1)) }
    }
}
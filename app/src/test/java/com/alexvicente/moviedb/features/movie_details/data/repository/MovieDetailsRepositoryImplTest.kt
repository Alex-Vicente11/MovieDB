package com.alexvicente.moviedb.features.movie_details.data.repository

import com.alexvicente.moviedb.core.data.local.dao.MovieDetailsDao
import com.alexvicente.moviedb.core.data.local.entity.MovieDetailsEntity
import com.alexvicente.moviedb.core.data.remote.dto.GenreDto
import com.alexvicente.moviedb.features.movie_details.data.remote.api.MovieDetailsApi
import com.alexvicente.moviedb.features.movie_details.data.remote.dto.MovieDetailsDto
import com.alexvicente.moviedb.testutil.extensions.awaitError
import com.alexvicente.moviedb.testutil.extensions.awaitLoading
import com.alexvicente.moviedb.testutil.extensions.awaitSuccess
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import app.cash.turbine.test

class MovieDetailsRepositoryImplTest {

    private lateinit var api: MovieDetailsApi
    private lateinit var mockDao: MovieDetailsDao
    private lateinit var repository: MovieDetailsRepositoryImpl

    private val movieId = 27205

    private val recentCacheTime = System.currentTimeMillis() - (10 * 60 * 1000L)   // 10 min — válido
    private val expiredCacheTime = System.currentTimeMillis() - (60 * 60 * 1000L)  // 1 hora — expirado

    @Before
    fun setUp() {
        api = mockk()
        mockDao = mockk(relaxed = true)
        repository = MovieDetailsRepositoryImpl(api, mockDao)
    }

    // Helpers

    private fun createDto(
        id: Int = movieId,
        title: String = "Inception",
        genres: List<GenreDto> = listOf(GenreDto(id = 878, name = "Science Fiction"))
    ) = MovieDetailsDto(
        id = id,
        title = title,
        overview = "A thief who steals corporate secrets",
        posterPath = "/poster.jpg",
        backdropPath = null,
        voteAverage = 8.8,
        voteCount = 30000,
        releaseDate = "2010-07-16",
        popularity = 100.0,
        runtime = 148,
        budget = 160_000_000L,
        revenue = 836_000_000L,
        genres = genres,
        tagline = "Your mind is the scene of the crime",
        status = "Released",
        homepage = null,
        imdbId = "tt1375666",
        originalLanguage = "en",
        originalTitle = "Inception",
        adult = false
    )

    private fun createCachedEntity(
        cachedAt: Long,
        title: String = "Cached Inception"
    ) = MovieDetailsEntity(
        id = movieId,
        title = title,
        overview = "Cached overview",
        posterPath = "/cached.jpg",
        backdropPath = null,
        voteAverage = 8.0,
        voteCount = 1000,
        releaseDate = "2010-07-16",
        popularity = 50.0,
        runtime = 148,
        budget = 160_000_000L,
        revenue = 836_000_000L,
        genresJson = """[{"id":878,"name":"Science Fiction"}]""",
        tagline = "Cached tagline",
        cachedAt = cachedAt
    )

    // Tests: sin caché

    @Test
    fun whenNoCache_fetchesFromApiAndCachesResult() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns null
        coEvery { api.getMovieDetails(movieId, any()) } returns createDto()

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitSuccess { details -> assertThat(details.title).isEqualTo("Inception") }
            awaitComplete()
        }

        coVerify(exactly = 1) { mockDao.insertMovieDetails(any()) }
    }

    // Tests: caché válido - no expirado

    @Test
    fun whenCacheIsValid_emitsCacheWithoutCallingApi() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns createCachedEntity(cachedAt = recentCacheTime)

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitSuccess { details -> assertThat(details.title).isEqualTo("Cached Inception") }
            awaitComplete()
        }

        coVerify(exactly = 0) { api.getMovieDetails(any(), any()) }
    }

    // Tests: caché expirado - vuelve a consultar la API

    @Test
    fun whenCacheIsExpired_fetchesFreshDataInsteadOfCache() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns createCachedEntity(cachedAt = expiredCacheTime)
        coEvery { api.getMovieDetails(movieId, any()) } returns createDto(title = "Fresh Inception")

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitSuccess { details -> assertThat(details.title).isEqualTo("Fresh Inception") }
            awaitComplete()
        }

        coVerify(exactly = 1) { api.getMovieDetails(movieId, any()) }
        coVerify(exactly = 1) { mockDao.insertMovieDetails(any()) }
    }

    @Test
    fun whenCacheIsExpiredAndApiFails_fallsBackToExpiredCache() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns createCachedEntity(cachedAt = expiredCacheTime)
        coEvery { api.getMovieDetails(movieId, any()) } throws IOException("No connection")

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            // El caché expiró, pero ante un fallo de red es preferible mostrar
            // el dato desactualizado a dejar al usuario sin nada.
            awaitSuccess { details -> assertThat(details.title).isEqualTo("Cached Inception") }
            awaitComplete()
        }
    }

    // Tests: sin caché y error - sin fallback posible

    @Test
    fun whenNoCacheAndNoNetwork_emitsConnectionError() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns null
        coEvery { api.getMovieDetails(movieId, any()) } throws IOException("No connection")

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Sin conexión. Verifica tu internet.") }
            awaitComplete()
        }
    }

    @Test
    fun whenNoCacheAnd404_emitsNotFoundError() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns null
        coEvery { api.getMovieDetails(movieId, any()) } throws HttpException(
            Response.error<Any>(404, okhttp3.ResponseBody.create(null, ""))
        )

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitError { message -> assertThat(message).isEqualTo("Recurso no encontrado") }
            awaitComplete()
        }
    }

    // Tests: mapeo de datos

    @Test
    fun whenApiResponds_mapsGenresAndNullableFieldsCorrectly() = runTest {
        coEvery { mockDao.getMovieDetails(movieId) } returns null
        coEvery { api.getMovieDetails(movieId, any()) } returns createDto(
            genres = listOf(
                GenreDto(id = 878, name = "Science Fiction"),
                GenreDto(id = 28, name = "Action")
            )
        )

        repository.getMovieDetails(movieId).test {
            awaitLoading()
            awaitSuccess { details ->
                assertThat(details.getGenresString()).isEqualTo("Science Fiction, Action")
                assertThat(details.backdropPath).isNull()
                assertThat(details.runtime).isEqualTo(148)
            }
            awaitComplete()
        }
    }
}
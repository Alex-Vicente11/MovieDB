package com.alexvicente.moviedb.features.search.domain.usecase

import app.cash.turbine.test
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.search.domain.repository.SearchRepository
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchMoviesUseCaseTest {

    private lateinit var mockRepository: SearchRepository
    private lateinit var useCase: SearchMoviesUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = SearchMoviesUseCase(mockRepository)
    }

    // ── Tests de validación ───────────────────────────────────────────────────

    @Test
    fun whenQueryIsEmpty_emitsErrorAndSkipsRepository() = runTest {
        useCase("").test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo(Constants.ERROR_EMPTY_QUERY)
            awaitComplete()
        }
        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun whenQueryIsBlankSpaces_emitsErrorAndSkipsRepository() = runTest {
        useCase("   ").test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo(Constants.ERROR_EMPTY_QUERY)
            awaitComplete()
        }
        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun whenQueryIsSingleChar_emitsErrorAndSkipsRepository() = runTest {
        // isBlank() || length < MIN_SEARCH_LENGTH: vacío y un solo carácter comparten mensaje
        useCase("a").test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message)
                .isEqualTo(Constants.ERROR_EMPTY_QUERY)
            awaitComplete()
        }
        coVerify(exactly = 0) { mockRepository.searchMovies(any()) }
    }

    @Test
    fun whenQueryHasExactlyTwoChars_callsRepository() = runTest {
        val validQuery = "ab"
        coEvery { mockRepository.searchMovies(validQuery) } returns
                flowOf(Resource.Success(listOf(MovieFactory.createMovie())))

        useCase(validQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }
        coVerify(exactly = 1) { mockRepository.searchMovies(validQuery) }
    }

    // ── Tests de transformación del query ─────────────────────────────────────

    @Test
    fun whenQueryHasWhitespace_trimsBeforeDelegating() = runTest {
        val queryWithSpaces = "  batman  "
        val trimmedQuery = "batman"
        coEvery { mockRepository.searchMovies(trimmedQuery) } returns
                flowOf(Resource.Success(listOf(MovieFactory.createMovie(title = "Batman"))))

        useCase(queryWithSpaces).test {
            awaitItem()
            awaitComplete()
        }

        coVerify(exactly = 1) { mockRepository.searchMovies(trimmedQuery) }
        coVerify(exactly = 0) { mockRepository.searchMovies(queryWithSpaces) }
    }

    @Test
    fun whenQueryHasUppercase_doesNotNormalizeCasing() = runTest {
        val uppercaseQuery = "BATMAN"
        coEvery { mockRepository.searchMovies(uppercaseQuery) } returns
                flowOf(Resource.Success(listOf(MovieFactory.createMovie(title = "Batman"))))

        useCase(uppercaseQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }

        coVerify(exactly = 1) { mockRepository.searchMovies(uppercaseQuery) }
        coVerify(exactly = 0) { mockRepository.searchMovies("batman") }
    }

    // ── Tests de delegación y propagación de estados ──────────────────────────

    @Test
    fun whenRepositoryEmitsSuccess_useCasePropagatesData() = runTest {
        val query = "avengers"
        val expectedMovies = listOf(
            MovieFactory.createMovie(id = 1, title = "Avengers", voteAverage = 8.0),
            MovieFactory.createMovie(id = 2, title = "Avengers: Age of Ultron", voteAverage = 7.3)
        )
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Success(expectedMovies))

        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Success::class.java)
            assertThat((item as Resource.Success).data).isEqualTo(expectedMovies)
            assertThat(item.data).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsError_useCasePropagatesMessage() = runTest {
        val query = "unknown movie"
        val errorMessage = "No se encontraron películas"
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Error(errorMessage))

        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Error::class.java)
            assertThat((item as Resource.Error).message).isEqualTo(errorMessage)
            awaitComplete()
        }
    }

    @Test
    fun whenRepositoryEmitsLoadingThenSuccess_useCasePropagatesBothStates() = runTest {
        val query = "spider-man"
        val expectedMovies = listOf(MovieFactory.createMovie(title = "Spider-Man"))
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Loading(), Resource.Success(expectedMovies))

        val results = useCase(query).toList()

        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(Resource.Loading::class.java)
        assertThat(results[1]).isInstanceOf(Resource.Success::class.java)
        assertThat((results[1] as Resource.Success).data).isEqualTo(expectedMovies)
    }

    @Test
    fun whenRepositoryReturnsEmptyList_emitsSuccessWithEmptyData() = runTest {
        val query = "vecsr432notfound"
        coEvery { mockRepository.searchMovies(query) } returns
                flowOf(Resource.Success(emptyList()))

        useCase(query).test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Resource.Success::class.java)
            assertThat((item as Resource.Success).data).isEmpty()
            awaitComplete()
        }
    }

    // ── Tests de edge cases ───────────────────────────────────────────────────

    @Test
    fun whenQueryHasSpecialCharacters_callsRepositoryNormally() = runTest {
        val specialQuery = "avengers: endgame"
        coEvery { mockRepository.searchMovies(specialQuery) } returns
                flowOf(Resource.Success(listOf(MovieFactory.createMovie(title = "Avengers: Endgame"))))

        useCase(specialQuery).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }
        coVerify(exactly = 1) { mockRepository.searchMovies(specialQuery) }
    }

    @Test
    fun whenQueryHasNumbers_callsRepositoryNormally() = runTest {
        val queryWithNumbers = "blade runner 2049"
        coEvery { mockRepository.searchMovies(queryWithNumbers) } returns
                flowOf(Resource.Success(listOf(MovieFactory.createMovie(title = "Blade Runner 2049"))))

        useCase(queryWithNumbers).test {
            assertThat(awaitItem()).isInstanceOf(Resource.Success::class.java)
            awaitComplete()
        }
        coVerify(exactly = 1) { mockRepository.searchMovies(queryWithNumbers) }
    }
}
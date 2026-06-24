package com.example.apptest.features.genres.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.TestPager
import com.example.apptest.core.data.remote.dto.MovieResponseDto
import com.example.apptest.features.genres.data.remote.api.MoviesByGenreApi
import com.example.apptest.testutil.factories.MovieFactory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Tests unitarios para MoviesByGenrePagingSource.
 *
 * ¿Por qué unit test y no instrumentado?
 *   PagingSource.load() es una suspend fun pura — no necesita Android.
 *   TestPager de androidx.paging.testing permite testear el ciclo completo
 *   de carga (refresh, append, prepend) sin emulador.
 *
 * TestPager vs PagingSource.load() directo:
 *   load() directo → solo verifica una llamada individual
 *   TestPager      → simula el ciclo real de Paging 3 (refresh → append)
 *                    y maneja el estado entre páginas correctamente
 *
 * Dependencia adicional necesaria en build.gradle.kts:
 *   testImplementation("androidx.paging:paging-testing:3.5.0")
 */
class MoviesByGenrePagingSourceTest {

    private lateinit var mockApi: MoviesByGenreApi
    private val genreId = 28
    private val language = "es-MX"

    @Before
    fun setUp() {
        mockApi = mockk()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Crea un MovieResponseDto con N películas simulando la respuesta de TMDB.
     * totalPages controla cuántas páginas hay disponibles para testear
     * la lógica de nextKey/prevKey en LoadResult.Page.
     */
    private fun createMovieResponseDto(
        page: Int = 1,
        totalPages: Int = 3,
        size: Int = MoviesByGenrePagingSource.PAGE_SIZE
    ) = MovieResponseDto(
        page = page,
        totalPages = totalPages,
        totalResults = totalPages * size,
        results = MovieFactory.createMovieDtoList(size)
    )

    /**
     * Crea un TestPager configurado igual que MoviesByGenreRepositoryImpl.
     * TestPager simula el comportamiento real del Pager en producción.
     */
    private fun createTestPager(
        pagingSource: MoviesByGenrePagingSource
    ) = TestPager(
        config = PagingConfig(
            pageSize = MoviesByGenrePagingSource.PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSource = pagingSource
    )

    // ── Tests de carga exitosa ────────────────────────────────────────────────

    @Test
    fun whenFirstPageLoads_returnsCorrectMovies() = runTest {
        // Given
        coEvery { mockApi.getMoviesByGenre(genreId, page = 1, language) } returns
                createMovieResponseDto(page = 1, totalPages = 3)

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When — load() directo para verificar una página específica
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null, // null = página inicial (STARTING_PAGE = 1)
                loadSize = MoviesByGenrePagingSource.PAGE_SIZE,
                placeholdersEnabled = false
            )
        )

        // Then
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        Truth.assertThat(page.data).hasSize(MoviesByGenrePagingSource.PAGE_SIZE)
        // Primera página: no hay página anterior
        Truth.assertThat(page.prevKey).isNull()
        // Hay más páginas: nextKey debe ser 2
        Truth.assertThat(page.nextKey).isEqualTo(2)
    }

    @Test
    fun whenLastPageLoads_nextKeyIsNull() = runTest {
        // Given — página 3 de 3 = última página
        coEvery { mockApi.getMoviesByGenre(genreId, 3, language) } returns
                createMovieResponseDto(page = 3, totalPages = 3)

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 3,
                loadSize = MoviesByGenrePagingSource.PAGE_SIZE,
                placeholdersEnabled = false
            )
        )

        // Then — última página no debe tener nextKey
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        Truth.assertThat(page.nextKey).isNull()
        // Pero sí tiene prevKey (no es la primera)
        Truth.assertThat(page.prevKey).isEqualTo(2)
    }

    @Test
    fun whenLoadingWithTestPager_refreshReturnsFirstPage() = runTest {
        // Given
        coEvery { mockApi.getMoviesByGenre(genreId, 1, language) } returns
                createMovieResponseDto(page = 1, totalPages = 2)

        val pager = createTestPager(
            MoviesByGenrePagingSource(mockApi, genreId, language)
        )

        // When — TestPager.refresh() simula el ciclo inicial de carga
        val result = pager.refresh() as PagingSource.LoadResult.Page

        // Then
        Truth.assertThat(result.data).hasSize(MoviesByGenrePagingSource.PAGE_SIZE)
        Truth.assertThat(result.prevKey).isNull()
        Truth.assertThat(result.nextKey).isEqualTo(2)
    }

    @Test
    fun whenAppendingPages_accumulatesAllMovies() = runTest {
        // Given — dos páginas disponibles
        coEvery { mockApi.getMoviesByGenre(genreId, 1, language) } returns
                createMovieResponseDto(page = 1, totalPages = 2, size = 5)
        coEvery { mockApi.getMoviesByGenre(genreId, 2, language) } returns
                createMovieResponseDto(page = 2, totalPages = 2, size = 5)

        val pager = createTestPager(
            MoviesByGenrePagingSource(mockApi, genreId, language)
        )

        // When — refresh inicial + append de la segunda página
        pager.refresh()
        val appendResult = pager.append() as PagingSource.LoadResult.Page

        // Then — la segunda página tiene los datos correctos
        Truth.assertThat(appendResult.data).hasSize(5)
        // Al ser la última página, nextKey es null
        Truth.assertThat(appendResult.nextKey).isNull()

        // El snapshot acumula todas las páginas cargadas hasta ahora
        val snapshot = pager.getLastLoadedPage()
        Truth.assertThat(snapshot).isNotNull()

        val allPages = pager.getPages()
        val totalMovies = allPages.sumOf { (it as PagingSource.LoadResult.Page).data.size }
        assertThat(totalMovies).isEqualTo(10) // 5 de página 1 + 5 de página 2
    }

    @Test
    fun whenSinglePageAvailable_bothKeysAreNull() = runTest {
        // Given — solo hay una página de resultados
        coEvery { mockApi.getMoviesByGenre(genreId, 1, language) } returns
                createMovieResponseDto(page = 1, totalPages = 1, size = 3)

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        // Then — única página: sin prev ni next
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        Truth.assertThat(page.data).hasSize(3)
        Truth.assertThat(page.prevKey).isNull()
        Truth.assertThat(page.nextKey).isNull()
    }

    // ── Tests de mapeo ────────────────────────────────────────────────────────

    @Test
    fun whenApiResponds_mapsMovieDtoToDomainCorrectly() = runTest {
        // Given — DTO con campos específicos para verificar el mapper
        val dto = MovieFactory.createMovieDto(
            id = 27205,
            title = "  Inception  ", // con espacios — el mapper hace trim
            overview = "A thief who steals corporate secrets",
            posterPath = "/poster.jpg",
            voteAverage = 8.8
        )
        coEvery { mockApi.getMoviesByGenre(genreId, 1, language) } returns
                MovieResponseDto(
                    page = 1,
                    totalPages = 1,
                    totalResults = 1,
                    results = listOf(dto)
                )

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        // Then — verificamos cada campo del mapper
        val movie = result.data.first()
        Truth.assertThat(movie.id).isEqualTo(27205)
        Truth.assertThat(movie.title).isEqualTo("Inception") // trim aplicado
        Truth.assertThat(movie.overview).isEqualTo("A thief who steals corporate secrets")
        Truth.assertThat(movie.posterPath).isEqualTo("/poster.jpg")
        Truth.assertThat(movie.voteAverage).isEqualTo(8.8)
    }

    // ── Tests de errores ──────────────────────────────────────────────────────

    @Test
    fun whenApiThrowsHttpException_returnsLoadResultError() = runTest {
        // Given — simula error HTTP 401
        val httpException = HttpException(
            Response.error<Any>(401, "Unauthorized".toResponseBody())
        )
        coEvery { mockApi.getMoviesByGenre(any(), any(), any()) } throws httpException

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        // Then — HttpException se convierte en LoadResult.Error
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
        Truth.assertThat((result as PagingSource.LoadResult.Error).throwable)
            .isInstanceOf(HttpException::class.java)
    }

    @Test
    fun whenNetworkFails_returnsLoadResultError() = runTest {
        // Given — simula pérdida de conexión
        coEvery { mockApi.getMoviesByGenre(any(), any(), any()) } throws
                IOException("No network")

        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        // Then
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
        Truth.assertThat((result as PagingSource.LoadResult.Error).throwable)
            .isInstanceOf(IOException::class.java)
    }

    @Test
    fun whenErrorOccursWithTestPager_refreshReturnsError() = runTest {
        // Given
        coEvery { mockApi.getMoviesByGenre(any(), any(), any()) } throws
                IOException("Connection refused")

        val pager = createTestPager(
            MoviesByGenrePagingSource(mockApi, genreId, language)
        )

        // When
        val result = pager.refresh()

        // Then — TestPager también expone LoadResult.Error
        Truth.assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
    }

    // ── Tests de getRefreshKey ────────────────────────────────────────────────

    @Test
    fun whenRefreshKeyRequested_withNullAnchor_returnsNull() = runTest {
        // Given
        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        // When — PagingState vacío sin anchorPosition
        val refreshKey = pagingSource.getRefreshKey(
            PagingState(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = MoviesByGenrePagingSource.PAGE_SIZE),
                leadingPlaceholderCount = 0
            )
        )

        // Then — sin anchorPosition no hay clave de refresh
        Truth.assertThat(refreshKey).isNull()
    }

    @Test
    fun whenRefreshKeyRequested_withAnchorOnPageWithPrevKey_returnsPrevKeyPlusOne() = runTest {
        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        val page = PagingSource.LoadResult.Page(
            data = MovieFactory.createMovieList(MoviesByGenrePagingSource.PAGE_SIZE),
            prevKey = 1,  // viene de la página 2 (prevKey=1 significa "la anterior es la 1")
            nextKey = 3
        )

        val state = PagingState(
            pages = listOf(page),
            anchorPosition = 0, // primer item de esa página
            config = PagingConfig(pageSize = MoviesByGenrePagingSource.PAGE_SIZE),
            leadingPlaceholderCount = 0
        )

        val refreshKey = pagingSource.getRefreshKey(state)

        // prevKey (1) + 1 = 2 → la página donde estaba el usuario
        assertThat(refreshKey).isEqualTo(2)
    }

    @Test
    fun whenRefreshKeyRequested_withAnchorOnFirstPage_returnsNextKeyMinusOne() = runTest {
        // Given — la página cargada es la primera (prevKey=null, no hay anterior),
        // así que getRefreshKey debe caer en la rama de nextKey
        val pagingSource = MoviesByGenrePagingSource(mockApi, genreId, language)

        val page = PagingSource.LoadResult.Page(
            data = MovieFactory.createMovieList(MoviesByGenrePagingSource.PAGE_SIZE),
            prevKey = null,
            nextKey = 2
        )

        val state = PagingState(
            pages = listOf(page),
            anchorPosition = 0, // el usuario estaba viendo el primer item de la página 1
            config = PagingConfig(pageSize = MoviesByGenrePagingSource.PAGE_SIZE),
            leadingPlaceholderCount = 0
        )

        // When
        val refreshKey = pagingSource.getRefreshKey(state)

        // Then — prevKey es null -> cae al fallback: nextKey (2) - 1 = 1
        assertThat(refreshKey).isEqualTo(1)
    }


}
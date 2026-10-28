package com.alexvicente.moviedb.features.tv_series.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.TestPager
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesResponseDto
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class TvSeriesPagingSourceTest {

    private lateinit var mockApi: TvSeriesApi
    private val language = "es-MX"

    @Before
    fun setUp() {
        mockApi = mockk()
    }

    // Helpers
    private fun response(
        page: Int = 1,
        totalPages: Int = 3,
        size: Int = TvSeriesPagingSource.PAGE_SIZE
    ) = TvSeriesResponseDto(
        page = page,
        results = TvSeriesFactory.createTvSeriesDtoList(size),
        totalPages = totalPages,
        totalResults = totalPages * size
    )

    private fun pagingSource(category: TvSeriesCategory = TvSeriesCategory.POPULAR) =
        TvSeriesPagingSource(mockApi, category, language)

    private fun refreshParams(loadSize: Int = TvSeriesPagingSource.PAGE_SIZE) =
        PagingSource.LoadParams.Refresh<Int>(
            key = null, loadSize = loadSize, placeholdersEnabled = false
        )

    private fun testPager(source: TvSeriesPagingSource) = TestPager(
        config = PagingConfig(
            pageSize = TvSeriesPagingSource.PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSource = source
    )

    // Selección de endpoint por categoría

    @Test
    fun whenCategoryIsPopular_callsPopularEndpointOnly() = runTest {
        coEvery { mockApi.getPopularTvSeries(1, language) } returns response()

        pagingSource(TvSeriesCategory.POPULAR).load(refreshParams())

        coVerify(exactly = 1) { mockApi.getPopularTvSeries(1, language) }
        coVerify(exactly = 0) { mockApi.getTopRatedTvSeries(any(), any())}
    }

    @Test
    fun whenCategoryIsTopRated_callsTopRatedEndpointOnly() = runTest {
        coEvery { mockApi.getTopRatedTvSeries(1, language) } returns response()

        pagingSource(TvSeriesCategory.TOP_RATED).load(refreshParams())

        coVerify(exactly = 1) { mockApi.getTopRatedTvSeries(1, language) }
        coVerify(exactly = 0) { mockApi.getPopularTvSeries(any(), any()) }
    }

    // Carga y claves de paginacíon
    @Test
    fun whenFirstPageLoads_returnsDataAndNextKey() = runTest {
        coEvery { mockApi.getPopularTvSeries(1, language) } returns response(page = 1, totalPages = 3)

        val page = pagingSource().load(refreshParams()) as PagingSource.LoadResult.Page

        assertThat(page.data).hasSize(TvSeriesPagingSource.PAGE_SIZE)
        assertThat(page.prevKey).isNull()
        assertThat(page.nextKey).isEqualTo(2)
    }

    @Test
    fun whenLastPageLoads_nextKeyIsNullAndPrevKeyIsPrevious() = runTest {
        coEvery { mockApi.getPopularTvSeries(3, language) } returns response(page = 3, totalPages = 3)

        val result = pagingSource().load(
            PagingSource.LoadParams.Append(
                key = 3, loadSize = TvSeriesPagingSource.PAGE_SIZE, placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertThat(result.nextKey).isNull()
        assertThat(result.prevKey).isEqualTo(2)
    }

    @Test
    fun whenSinglePageAvailable_bothKeysAreNull() = runTest {
        coEvery { mockApi.getPopularTvSeries(1, language) } returns
                response(page = 1, totalPages = 1, size = 3)

        val page = pagingSource().load(refreshParams(20)) as PagingSource.LoadResult.Page

        assertThat(page.data).hasSize(3)
        assertThat(page.prevKey).isNull()
        assertThat(page.nextKey).isNull()
    }

    @Test
    fun whenAppendingPages_accumulatesAllItems() = runTest {
        coEvery { mockApi.getPopularTvSeries(1, language) } returns
                response(page = 1, totalPages = 2, size = 5)
        coEvery { mockApi.getPopularTvSeries(2, language) } returns
                response(page = 2, totalPages = 2, size = 5)

        val pager = testPager(pagingSource())
        pager.refresh()
        val appended = pager.append() as PagingSource.LoadResult.Page

        assertThat(appended.data).hasSize(5)
        assertThat(appended.nextKey).isNull()
        val total = pager.getPages().sumOf { it.data.size }
        assertThat(total).isEqualTo(10)
    }

    // Mapeo
    @Test
    fun whenApiResponds_mapsDtoToDomainCorrectly() = runTest {
        val dto = TvSeriesFactory.createTvSeriesDto(
            id = 1396,
            name = "  Breaking Bad  ",
            overview = "  A chemistry teacher  ",
            posterPath = "/poster.jpg",
            voteAverage = 8.9,
            firstAirDate = "2008-01-20"
        )
        coEvery { mockApi.getPopularTvSeries(1, language) } returns
                TvSeriesResponseDto(page = 1, results = listOf(dto), totalPages = 1, totalResults = 1)

        val page = pagingSource().load(refreshParams(20)) as PagingSource.LoadResult.Page

        val series = page.data.first()
        assertThat(series.id).isEqualTo(1396)
        assertThat(series.name).isEqualTo("Breaking Bad")
        assertThat(series.overview).isEqualTo("A chemistry teacher")
        assertThat(series.posterPath).isEqualTo("/poster.jpg")
        assertThat(series.voteAverage).isEqualTo(8.9)
        assertThat(series.firstAirDate).isEqualTo("2008-01-20")
    }

    @Test
    fun whenOverviewIsNullAndDateIsBlank_mapsToEmptyOverviewAndNullDate() = runTest {
        val dto = TvSeriesFactory.createTvSeriesDto(overview = null, firstAirDate = "  ")
        coEvery { mockApi.getPopularTvSeries(1, language) } returns
                TvSeriesResponseDto(page = 1, results = listOf(dto), totalPages = 1, totalResults = 1)

        val page = pagingSource().load(refreshParams(20)) as PagingSource.LoadResult.Page

        assertThat(page.data.first().overview).isEmpty()
        assertThat(page.data.first().firstAirDate).isNull()
    }

    // ── Errores ───────────────────────────────────────────────────────────────

    @Test
    fun whenApiThrowsHttpException_returnsLoadResultError() = runTest {
        val httpException = HttpException(Response.error<Any>(401, "Unauthorized".toResponseBody()))
        coEvery { mockApi.getPopularTvSeries(any(), any()) } throws httpException

        val result = pagingSource().load(refreshParams(20))

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
        assertThat((result as PagingSource.LoadResult.Error).throwable)
            .isInstanceOf(HttpException::class.java)
    }

    @Test
    fun whenNetworkFails_returnsLoadResultError() = runTest {
        coEvery { mockApi.getTopRatedTvSeries(any(), any()) } throws IOException("No network")

        val result = pagingSource(TvSeriesCategory.TOP_RATED).load(refreshParams(20))

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
        assertThat((result as PagingSource.LoadResult.Error).throwable)
            .isInstanceOf(IOException::class.java)
    }

    @Test
    fun whenErrorOccursWithTestPager_refreshReturnsError() = runTest {
        coEvery { mockApi.getPopularTvSeries(any(), any()) } throws IOException("Connection refused")

        val result = testPager(pagingSource()).refresh()

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
    }

    @Test
    fun whenUnexpectedExceptionIsThrown_isNotSwallowed() = runTest {
        // Decisión de diseño: solo HttpException e IOException se convierten en
        // LoadResult.Error; un fallo inesperado (bug, JSON corrupto) debe propagarse.
        coEvery { mockApi.getPopularTvSeries(any(), any()) } throws IllegalStateException("boom")

        val outcome = runCatching { pagingSource().load(refreshParams(20)) }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    // ── getRefreshKey ─────────────────────────────────────────────────────────

    private fun stateWith(prevKey: Int?, nextKey: Int?, anchor: Int?) = PagingState(
        pages = listOf(
            PagingSource.LoadResult.Page(
                data = TvSeriesFactory.createTvSeriesList(TvSeriesPagingSource.PAGE_SIZE),
                prevKey = prevKey,
                nextKey = nextKey
            )
        ),
        anchorPosition = anchor,
        config = PagingConfig(pageSize = TvSeriesPagingSource.PAGE_SIZE),
        leadingPlaceholderCount = 0
    )

    @Test
    fun whenAnchorIsNull_refreshKeyIsNull() {
        assertThat(pagingSource().getRefreshKey(stateWith(null, 2, anchor = null))).isNull()
    }

    @Test
    fun whenAnchorOnPageWithPrevKey_refreshKeyIsPrevKeyPlusOne() {
        assertThat(pagingSource().getRefreshKey(stateWith(prevKey = 1, nextKey = 3, anchor = 0)))
            .isEqualTo(2)
    }

    @Test
    fun whenAnchorOnFirstPage_refreshKeyIsNextKeyMinusOne() {
        assertThat(pagingSource().getRefreshKey(stateWith(prevKey = null, nextKey = 2, anchor = 0)))
            .isEqualTo(1)
    }
}
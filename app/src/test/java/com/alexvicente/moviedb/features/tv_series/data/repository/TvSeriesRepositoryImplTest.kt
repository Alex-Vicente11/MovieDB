package com.alexvicente.moviedb.features.tv_series.data.repository

import androidx.paging.testing.asSnapshot
import com.alexvicente.moviedb.features.tv_series.data.remote.api.TvSeriesApi
import com.alexvicente.moviedb.features.tv_series.data.remote.dto.TvSeriesResponseDto
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.testutil.factories.TvSeriesFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TvSeriesRepositoryImplTest {

    private lateinit var mockApi: TvSeriesApi
    private lateinit var repository: TvSeriesRepositoryImpl
    private val language = "es-MX"

    @Before
    fun setUp() {
        mockApi = mockk()
        repository = TvSeriesRepositoryImpl(mockApi)
    }

    private fun page(page: Int, ids: IntRange) = TvSeriesResponseDto(
        page = page,
        results = ids.map { TvSeriesFactory.createTvSeriesDto(id = it, name = "Series $it") },
        totalPages = 2,
        totalResults = 40
    )

    @Test
    fun whenSecondPageRepeatsASeries_itIsEmittedOnlyOnce() = runTest {
        // Given - TMDB reordena entre peticiones: el id 20 llega en ambas páginas
        coEvery { mockApi.getPopularTvSeries(1, language) } returns page(1, 1..20)
        coEvery { mockApi.getPopularTvSeries(2, language) } returns page(2, 20..39)

        // When - carga inicial y scroll hasta agotar las páginas
        val ids = repository.getTvSeries(TvSeriesCategory.POPULAR, language)
            .asSnapshot { appendScrollWhile { true} }
            .map { it.id }

        // Then - sin repetidos y en orden de aparición
        assertThat(ids).containsNoDuplicates()
        assertThat(ids).containsExactlyElementsIn((1..39).toList()).inOrder()
    }
}
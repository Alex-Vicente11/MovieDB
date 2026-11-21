package com.alexvicente.moviedb.testutil.canned

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.model.CastMember
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeasonDetails
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesDetails
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeasonRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesCastRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesDetailsRepository
import com.alexvicente.moviedb.features.tv_series.domain.repository.TvSeriesRepository
import com.alexvicente.moviedb.testutil.factories.TvFactory
import com.alexvicente.moviedb.testutil.factories.TvSeasonFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArrayList

/** Listas con nombres distintos por categoria para saber cual se esta mostrando */
class CannedTvSeriesRepository : TvSeriesRepository {
    var popular: List<TvSeries> =
        List(3) { TvFactory.createSeries(id = it + 1, name = "Serie ${it + 1}") }
    var topRated: List<TvSeries> =
        List(3) { TvFactory.createSeries(id = it + 101, name = "Top ${it + 1}") }

    // Sin sourceLoadStates, LazyPagingItems se queda en refresh = Loading y la lista nunca aparece
    private val loadedStates = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true)
    )

    override fun getTvSeries(
        category: TvSeriesCategory,
        language: String
    ): Flow<PagingData<TvSeries>> = flowOf(
        PagingData.from(
            data = when (category) {
                TvSeriesCategory.POPULAR -> popular
                TvSeriesCategory.TOP_RATED -> topRated
            },
            sourceLoadStates = loadedStates
        )
    )
}

/** El nombre incluye el id pedido: si el titulo correcto aparece en pantalla, el id viajo bien */
class CannedTvSeriesDetailsRepository : TvSeriesDetailsRepository {
    val requestedIds = CopyOnWriteArrayList<Int>()

    override suspend fun getTvSeriesDetails(
        seriesId: Int,
        language: String
    ): Resource<TvSeriesDetails> {
        requestedIds += seriesId
        return Resource.Success(
            TvFactory.createSeriesDetails(id = seriesId, name = "Detalle $seriesId")
        )
    }
}

class CannedTvSeriesCastRepository : TvSeriesCastRepository {
    override suspend fun getTvSeriesCast(
        seriesId: Int,
        language: String
    ): Resource<List<CastMember>> = Resource.Success(emptyList())
}

/** Registra (seriesId, seasonNumber) y devuelve una temporada cuyo nombre depende del número */
class CannedTvSeasonRepository : TvSeasonRepository {
    val requests = CopyOnWriteArrayList<Pair<Int, Int>>()
    private val longOverview = "palabra ".repeat(80).trim()

    override suspend fun getTvSeasonDetails(
        seriesId: Int,
        seasonNumber: Int,
        language: String
    ): Resource<TvSeasonDetails> {
        requests += seriesId to seasonNumber
        return Resource.Success(
            TvSeasonFactory.createTvSeasonDetails(
                seasonNumber = seasonNumber,
                name = if (seasonNumber == 0) "Especiales" else "Temporada $seasonNumber",
                episodes = listOf(
                    TvSeasonFactory.createTvEpisode(id = 1, episodeNumber = 1, overview = longOverview),
                    TvSeasonFactory.createTvEpisode(id = 2, episodeNumber = 2)
                )
            )
        )
    }
}
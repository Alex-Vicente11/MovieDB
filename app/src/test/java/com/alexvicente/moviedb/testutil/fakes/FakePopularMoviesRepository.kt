package com.alexvicente.moviedb.testutil.fakes

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.features.popular_movies.domain.repository.PopularMoviesRepository
import com.alexvicente.moviedb.testutil.factories.MovieFactory
import com.alexvicente.moviedb.testutil.factories.ResourceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePopularMoviesRepository : PopularMoviesRepository {

    var movies: List<Movie> = listOf(MovieFactory.createMovie())
    var shouldReturnError: Boolean = false
    var errorMessage: String = Constants.ERROR_NETWORK
    var shouldEmitCacheFirst: Boolean = false
    var cachedMovies: List<Movie> = emptyList()
    var getPopularMoviesCallCount: Int = 0
        private set // solo el fake puede incrementarlo

    override fun getPopularMovies(): Flow<Resource<List<Movie>>> = flow {
        // Registra que fue llamado
        getPopularMoviesCallCount++

        // Siempre emite Loading primero — igual que el repositorio real
        emit(ResourceFactory.loading())

        if (shouldReturnError) {
            // Escenario de error: emite error sin datos
            emit(Resource.Error(errorMessage))
            return@flow
        }

        if (shouldEmitCacheFirst && cachedMovies.isNotEmpty()) {
            // Escenario offline-first: primero caché, luego datos frescos
            emit(Resource.Success(cachedMovies))
        }

        // Resultado final: datos frescos (o únicos si no hay caché)
        emit(Resource.Success(movies))
    }

    fun reset() {
        movies = listOf(MovieFactory.createMovie())
        shouldReturnError = false
        errorMessage = Constants.ERROR_NETWORK
        shouldEmitCacheFirst = false
        cachedMovies = emptyList()
        getPopularMoviesCallCount = 0
    }
}
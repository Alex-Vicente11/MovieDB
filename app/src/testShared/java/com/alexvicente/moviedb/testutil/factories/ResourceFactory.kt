
package com.alexvicente.moviedb.testutil.factories

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.Constants

object ResourceFactory {

    fun <T> loading(): Resource<T> = Resource.Loading()

    fun successMovies(
        movies: List<Movie> = listOf(MovieFactory.createMovie())
    ): Resource<List<Movie>> = Resource.Success(movies)

    fun emptyMovies(): Resource<List<Movie>> = Resource.Success(emptyList())

    fun networkError(): Resource<List<Movie>> =
        Resource.Error(Constants.ERROR_NETWORK)

    fun authError(): Resource<List<Movie>> =
        Resource.Error(Constants.ERROR_AUTH)

    fun notFoundError(): Resource<List<Movie>> =
        Resource.Error(Constants.ERROR_NOT_FOUND)

    fun serverError(code: Int): Resource<List<Movie>> =
        Resource.Error(Constants.ERROR_SERVER)

    fun unknownError(message: String = Constants.ERROR_UNKNOWN): Resource<List<Movie>> =
        Resource.Error(message)

    fun searchValidationError(message: String = Constants.ERROR_EMPTY_QUERY): Resource<List<Movie>> =
        Resource.Error(message)
}
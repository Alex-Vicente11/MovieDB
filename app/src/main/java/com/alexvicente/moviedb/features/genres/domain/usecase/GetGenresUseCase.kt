package com.alexvicente.moviedb.features.genres.domain.usecase

import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import javax.inject.Inject

class GetGenresUseCase @Inject constructor(
    private val repository: GenresRepository
) {
    suspend operator fun invoke(): Resource<List<Genre>> {
        return repository.getGenres()
    }
}
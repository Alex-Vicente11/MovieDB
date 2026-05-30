package com.example.apptest.features.genres.domain.usecase

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Genre
import com.example.apptest.features.genres.domain.repository.GenresRepository
import javax.inject.Inject

class GetGenresUseCase @Inject constructor(
    private val repository: GenresRepository
) {
    suspend operator fun invoke(): Resource<List<Genre>> {
        return repository.getGenres()
    }
}
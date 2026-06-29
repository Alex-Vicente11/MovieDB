package com.alexvicente.moviedb.features.genres.data.repository

import com.alexvicente.moviedb.core.data.mapper.GenreMapper.toDomain
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.features.genres.data.remote.api.GenresApi
import com.alexvicente.moviedb.features.genres.domain.repository.GenresRepository
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject

class GenresRepositoryImpl @Inject constructor(
    private val api: GenresApi
): GenresRepository {

    override suspend fun getGenres(): Resource<List<Genre>> {
        return try {
            val response = api.getGenres()
            Resource.Success(response.toDomain())
        } catch (e: HttpException) {
            Resource.Error("Error del servidor: ${e.code()}")
        } catch (e: IOException) {
            Resource.Error("Sin conexión a internet $e")
        }
    }
}
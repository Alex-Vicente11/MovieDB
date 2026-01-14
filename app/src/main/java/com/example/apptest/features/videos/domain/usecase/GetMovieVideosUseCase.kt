package com.example.apptest.features.videos.domain.usecase

import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.videos.domain.model.Video
import com.example.apptest.features.videos.domain.repository.VideosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Obtener videos de una pelicila
 *
 * Responsabilidad:
 * - Encapsular la lógica de negocio para obtener videos
 * - Validar precondiciones (ID válido)
 * - Filtrar y ordenar videos (opcional)
 * - Delegar al repositorio (no implementa la lógica de red)
 *
 * Dependency Inversion Principle (DIP):
 * - Depende de VideosRepository (interface)
 * - No depende de VideosRepositoryImpl (implementacion)
 * - Esto permite testing facil con mocks
 *
 * Single Responsability Principle (SRP):
 *  - Solo hace una cosa: Coordinar la obtención de videos
 *  - No maneja red, no mapea datos, no maneja UI
 *  - Valida entrada y delega al repositorio
 */

class GetMovieVideosUseCase(
    private val repository: VideosRepository
) {
    /**
     * operador fun invoke() permite llamar al UseCase como funcion:
     * getMovieVideosUseCase(movieId) en lugar de getMovieVideosUseCase.execute(movieId)
     *
     * @param movieId ID de la pelicula
     * @param filterOfficial Si true, solo retorna videos oficiales
     * @return Flow que emite Resource con estados Loading/Success/Error
     *
     * Validaciones:
     * - El ID debe ser mayor que 0 (TMDB usa IDs positivos)
     * - Si el ID es inválido, emite Error sin llamar al repositorio
     *
     * Lógica de negocio adicional:
     * - Ordena videos: Trailers oficiales primero, luego el resto
     * - Filtra videos de Youtube
     */
    operator fun invoke(
        movieId: Int,
        filterOfficial: Boolean = false
    ): Flow<Resource<List<Video>>> {
        if (movieId <= 0) {
            return flow {
                emit(Resource.Error("ID de pelicula inválido: ${movieId}"))
            }
        }

        // Delegar al repositorio y aplicar lógica de negocio
        return repository.getMovieVideos(movieId).map { resource ->
            when (resource) {
                is Resource.Success -> {
                    var videos = resource.data

                    // Filtrar solo oficiales si se solicita
                    if (filterOfficial) {
                        videos = videos.filter { it.official }
                    }

                    // Ordenar: Trailers oficiales primero
                    videos = videos.sortedWith(
                        compareByDescending<Video> { it.official }
                            .thenByDescending { it.isTrailer() }
                            .thenBy { it.name }
                    )

                    Resource.Success(videos)
                }
                is Resource.Error -> resource
                is Resource.Loading -> resource
            }
        }
    }
}
package com.example.apptest.features.videos.domain.repository

import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.videos.domain.model.Video
import kotlinx.coroutines.flow.Flow

/**
 * Responsabilidad:
 * - Definir qué operaciones están disponibles
 * - No define cómo se implementan
 *
 * Dependency Inversion Principle (DIP):
 * - GetMovieVideosUseCase depende de esta interface
 * - VideosRepositoryImpl Implementa esta interface
 * - UseCase (high - level) No depende de Repository (low - level)
 * - Ambos dependen de la abstracción (esta interface)
 *
 * Interface Segregation Principle (ISP):
 * - Solo tiene el metodo necesario para videos
 * - No hereda de un repositorio genérico
 *
 * - Fácil de testear
 * - Cambiar implementación sin afectar UseCases
 * - Desacoplamiento entre capas
 */

interface VideosRepository {
    /**
     * Flow permite reactividad:
     * - La UI puede observar cambios en tiempo real
     * - Cancelación automática con lificycleScope
     * - Multiples emisiones (Loading -> Success/Error)
     */
    fun getMovieVideos(movieId: Int): Flow<Resource<List<Video>>>
}
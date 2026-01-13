package com.example.apptest.features.popular_movies.domain.repository

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * CONTRATO del repositorio de películas populares
 *
 * CAPA: Domain (lógica de negocio pura)
 * UBICACIÓN: features/popular_movies/domain/repository/
 *
 * Responsabilidad:
 * - Definir QUÉ operaciones están disponibles
 * - NO define CÓMO se implementan (eso es responsabilidad de Data)
 *
 * Dependency Inversion Principle (DIP):
 * - GetPopularMoviesUseCase depende de ESTA INTERFACE
 * - PopularMoviesRepositoryImpl IMPLEMENTA esta interface
 * - UseCase (high-level) NO depende de Repository (low-level)
 * - Ambos dependen de la abstracción (esta interface)
 *
 * Ventajas:
 * - Fácil de testear (mock de la interface)
 * - Cambiar implementación sin afectar UseCases
 * - Desacoplamiento entre capas
 */
interface PopularMoviesRepository {

    /**
     * Obtener películas populares
     *
     * @return Flow que emite estados:
     *         - Resource.Loading: Cargando datos
     *         - Resource.Success: Datos obtenidos correctamente
     *         - Resource.Error: Error al obtener datos
     *
     * El Flow permite reactividad:
     * - La UI puede observar cambios en tiempo real
     * - Cancelación automática con lifecycleScope
     */
    fun getPopularMovies(): Flow<Resource<List<Movie>>>
}
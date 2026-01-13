package com.example.apptest.features.movie_details.domain.repository

import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import kotlinx.coroutines.flow.Flow

/**
 * CONTRATO del repositorio de detalles de películas
 *
 * CAPA: Domain (lógica de negocio pura)
 * UBICACIÓN: features/movie_details/domain/repository/
 *
 * Responsabilidad:
 * - Definir QUÉ operaciones están disponibles
 * - NO define CÓMO se implementan (eso es responsabilidad de Data)
 *
 * Dependency Inversion Principle (DIP):
 * - GetMovieDetailsUseCase depende de ESTA INTERFACE
 * - MovieDetailsRepositoryImpl IMPLEMENTA esta interface
 * - UseCase (high-level) NO depende de Repository (low-level)
 * - Ambos dependen de la abstracción (esta interface)
 *
 * Comparación con repositorio monolítico:
 *  ANTES: MovieRepository con 3 métodos
 *    - searchMovies()
 *    - getPopularMovies()
 *    - getMovieDetails()  ← Solo necesitábamos esto
 *
 *  AHORA: MovieDetailsRepository con 1 metodo
 *    - getMovieDetails()  ← Solo lo que necesitamos
 *
 * Interface Segregation Principle (ISP):
 * "Los clientes no deberían depender de interfaces que no usan"
 *
 * Ventajas:
 * - Fácil de testear (mock de la interface)
 * - Cambiar implementación sin afectar UseCases
 * - Desacoplamiento entre capas
 */
interface MovieDetailsRepository {

    /**
     * Obtener detalles completos de una película
     *
     * @param movieId ID de la película a buscar
     * @return Flow que emite estados:
     *         - Resource.Loading: Cargando datos
     *         - Resource.Success: Datos obtenidos correctamente
     *         - Resource.Error: Error al obtener datos
     *
     * El Flow permite reactividad:
     * - La UI puede observar cambios en tiempo real
     * - Cancelación automática con lifecycleScope
     * - Múltiples emisiones (Loading → Success/Error)
     */
    fun getMovieDetails(movieId: Int): Flow<Resource<MovieDetails>>
}
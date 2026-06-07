package com.example.apptest.testutil.factories

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie

/**
 * Responsabilidades: Centralizar la creación de Resource<T> para tests.
 *
 * Resource.Success(data), Resource.Error(message), Resource.Loading() aparecen
 * repetidamente en todos los tests de UseCase, Repository y ViewModel.
 * Esta factory reduce el boilerplate y estandariza los mensajes de error,
 * evitando strings duplicados que son difíciles de mantener
 *
 * Patrón aplicado: Factory Method
 * Métodos estáticos con nombres semánticos que describen el escenario,
 * no solo el tipo. Ej: networkError() en lugar de Resource.Error("...")
 */

object ResourceFactory {

    // se emite al inicio de cualquier operación en el repositorio
    fun <T> loading(): Resource<T> = Resource.Loading()

    /**
     * Uso:
     *    coEvery { repository.getPopularMovies() } returns flowOf(
     *          ResourceFactory.successMovies(MovieFactory.createMovies(3)))
            )
     */
    fun successMovies(movies: List<Movie> = listOf(MovieFactory.createMovie())): Resource<List<Movie>> =
        Resource.Success(movies)

    /**
     * Operación exitosa con lista vacía
     * Caso específico: la API respondió bien pero no hay resultados
     * Es un válido - no es un error
     */
    fun emptyMovies(): Resource<List<Movie>> =
        Resource.Success(emptyList())

    /**
     * Errores tipificados - mapean a los mensajes reales del repositorio
     *
     * Cada función de error corresponde a un mensaje real definido en el repositorio.
     * Si el repositorio cambia el mensaje, solo se actualiza aquí y en el repositorio
     * Los tests que usan estas funciones no se rompen
     */
    fun networkError(): Resource<List<Movie>> =
        Resource.Error("Error de conexión. Verifica tu internet.")

    fun authError(): Resource<List<Movie>> =
        Resource.Error("Error de autenticación")

    fun notFoundError(): Resource<List<Movie>> =
        Resource.Error("No se encontraron películas")

    fun serverError(code: Int): Resource<List<Movie>> =
        Resource.Error("Error del servidor: $code")

    fun unknownError(message: String = "Error desconocido"): Resource<List<Movie>> =
        Resource.Error(message)

    fun searchValidationError(message: String): Resource<List<Movie>> =
        Resource.Error(message)
}
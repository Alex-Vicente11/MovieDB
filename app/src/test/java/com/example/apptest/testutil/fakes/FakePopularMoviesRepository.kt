package com.example.apptest.testutil.fakes

import com.example.apptest.core.data.util.Resource
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.features.popular_movies.domain.repository.PopularMoviesRepository
import com.example.apptest.testutil.factories.MovieFactory
import com.example.apptest.testutil.factories.ResourceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * RESPONSABILIDAD: Implementación falsa de PopularMoviesRepository para tests del ViewModel.
 *
 * ¿Por qué un Fake en lugar de MockK?
 *
 * MockK es ideal cuando necesitas:
 *   - Verificar que un métodoo fue llamado (coVerify)
 *   - Configurar comportamiento simple test por test (coEvery)
 *
 * Un Fake es mejor cuando el repositorio emite múltiples estados en secuencia.
 * PopularMoviesRepositoryImpl tiene lógica offline-first:
 *   emit(Loading) → emit(Success con caché) → emit(Success con datos frescos)
 *
 * Modelar eso con coEvery requeriría Flows complejos en cada test.
 * El Fake encapsula esa lógica una sola vez y expone propiedades
 * que el test controla directamente.
 *
 * Patrón aplicado: Fake Object
 * Implementación real pero simplificada, sin efectos secundarios (red, BD).
 */
class FakePopularMoviesRepository : PopularMoviesRepository {

    /**
     * Lista de películas que el repositorio "devolverá".
     * El test la sobreescribe antes de llamar al ViewModel.
     *
     * Uso:
     *   fakeRepository.movies = MovieFactory.createMovieList(5)
     *   viewModel.getPopularMovies()
     */
    var movies: List<Movie> = listOf(MovieFactory.createMovie())

    /**
     * Controla si el repositorio debe simular un error de red.
     * Cuando es true, getPopularMovies() emite Resource.Error.
     *
     * Uso:
     *   fakeRepository.shouldReturnError = true
     *   viewModel.getPopularMovies()
     *   // ViewModel debe mostrar estado de error
     */
    var shouldReturnError: Boolean = false

    /**
     * Mensaje de error que se emitirá cuando shouldReturnError == true.
     * Permite testear distintos mensajes de error sin crear múltiples fakes.
     */
    var errorMessage: String = "Error de conexión. Verifica tu internet."

    /**
     * Controla si el repositorio emite primero datos de caché antes del resultado final.
     * Simula el comportamiento offline-first del repositorio real:
     *   Loading → Success(caché) → Success(datos frescos)
     *
     * Uso:
     *   fakeRepository.shouldEmitCacheFirst = true
     *   fakeRepository.cachedMovies = MovieFactory.createMovieList(2)
     *   fakeRepository.movies = MovieFactory.createMovieList(5)
     */
    var shouldEmitCacheFirst: Boolean = false
    var cachedMovies: List<Movie> = emptyList()

    // Registro de llamadas — para verificar interacciones

    /**
     * Cuenta cuántas veces fue llamado getPopularMovies().
     * Permite verificar que el ViewModel no hace llamadas innecesarias.
     *
     * Uso:
     *   viewModel.getPopularMovies()
     *   assertThat(fakeRepository.getPopularMoviesCallCount).isEqualTo(1)
     */
    var getPopularMoviesCallCount: Int = 0
        private set // solo el fake puede incrementarlo

    // Implementación

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

    /**
     * Reinicia el estado del fake entre tests.
     * Llamar en @Before para garantizar aislamiento.
     *
     * Uso:
     *   @Before
     *   fun setUp() {
     *       fakeRepository.reset()
     *   }
     */
    fun reset() {
        movies = listOf(MovieFactory.createMovie())
        shouldReturnError = false
        errorMessage = "Error de conexión. Verifica tu internet."
        shouldEmitCacheFirst = false
        cachedMovies = emptyList()
        getPopularMoviesCallCount = 0
    }
}
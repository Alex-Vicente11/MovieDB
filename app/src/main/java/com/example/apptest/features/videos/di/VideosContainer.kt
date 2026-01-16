package com.example.apptest.features.videos.di

import com.example.apptest.core.di.CoreContainer
import com.example.apptest.features.videos.data.remote.api.VideosApi
import com.example.apptest.features.videos.data.repository.VideosRepositoryImpl
import com.example.apptest.features.videos.domain.repository.VideosRepository
import com.example.apptest.features.videos.domain.usecase.GetMovieVideosUseCase

/**
 * Responsabilidad
 * - Proveer todas las dependencias del feature videos
 * - Gestionar ciclo de vida de dependencias (singleton con lazy)
 * - User CoreContainer para dependencias compartidas (Retrofit)
 *
 * Service Locator Pattern
 * - Este condenedor implementa Service Locator manul
 * - (alternativa a Dagger/Hilt cuando no se quiere complejidad)
 *
 * Dependency Injection Pattern aplicado:
 * - Data Layer depende de Domain (Repository implementa interface)
 * - Domain Layer no depende de nadie
 * - Presentation Layer depende de Domain (ViewModel usa UseCase)
 *
 * Patron Constructor
 * 1. Api (capa mas baja - acceso a datos externos)
 * 2. Repository (implementa lógica de acceso a datos)
 * 3. UseCase (implementa lógica de negocio)
 *
 * Uso de lazy
 * - Inicializacion diferida (solo cuando se necesita)
 * - Singleton por contenedor (una solo instancia)
 * - Thread-safe (lazy es synchronized por defecto)
 */

class VideosContainer {

    // Data Layer
    /**
     * Usa Retrofit de CoreContainer:
     * - CoreContainer.retrofit es un singleton compartido
     * - Solo creamos la interfaz API especifica del feature
     *
     * lazy: Se crea solo cuando se accede por primera vez
     */
    private val videosApi: VideosApi by lazy {
        CoreContainer.retrofit.create(VideosApi::class.java)
    }


    /**
     * Repositorio de videos
     *
     * Implementacion concreta que:
     * - Usa videosApi para acceso a datos
     * - Implementa VideosRepository (interface del domain)
     *
     * private: Solo se expone a través del UseCase
     */
    private val videosRepository: VideosRepository by lazy {
        VideosRepositoryImpl(videosApi)
    }

    // Domain Layer - Use Cases
    /**
     * Public: Esta es la dependencia que se inyecta en ViewModels
     *
     * Dependency Inversion:
     * - GetMovieVideosUseCase depende de VideosRepository (interface)
     * - No depende de VideosRepositoryImpl (implementacion)
     * - Esto permite cambiar la implementacion sin afectar el UseCase
     *
     * Ejemplo de uso:
     * val appContainer = (application as MyApplication).appContainer
     * val useCase = appContainer.videosContainer.getMovieVideosUseCase
     *
     * viewModel = VideosViewModel(useCase)
     */
    val getMovieVideosUseCase: GetMovieVideosUseCase by lazy {
        GetMovieVideosUseCase(videosRepository)
    }

    /*
     * Beneficios de Feature Containers:
     *  Separación clara de responsabilidades
     *  Cada feature es independiente
     *  Fácil de testear (mock solo el container que necesitas)
     *  Escalable (agregar features no afecta existentes)
     *  Código más organizado y mantenible
     *
     * Ejemplo de testing:
     * ```kotlin
     * // Mock solo el repository de videos
     * val mockRepository = mockk<VideosRepository>()
     * val useCase = GetMovieVideosUseCase(mockRepository)
     * // Test aislado del feature sin afectar otros
     * ```
     */

}
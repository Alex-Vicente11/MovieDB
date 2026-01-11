package com.example.apptest.movies.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.movies.domain.usecase.GetPopularMoviesUseCase
import com.example.apptest.core.data.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VIEWMODEL para MainActivity
 *
 * Responsabilidades:
 * - Exponer estados de UI mediante StateFlow
 * - Coordinar UseCases (NO hace lógica de negocio)
 * - Manejar eventos de UI
 * - Sobrevive a cambios de configuración
 */
class MainViewModel(
    private val searchMoviesUseCase: com.example.apptest.features.search.domain.usecase.SearchMoviesUseCase,
    private val getPopularMoviesUseCase: GetPopularMoviesUseCase
) : ViewModel() {

    // Estado privado (mutable)
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    // Estado público (inmutable) - lo observa la UI
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Buscar películas por query
     */
    fun searchMovies(query: String) {
        viewModelScope.launch {
            // El UseCase valida el query y retorna Flow<Resource<List<Movie>>>
            searchMoviesUseCase(query).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> MainUiState.Loading

                    is Resource.Success -> {
                        if (resource.data.isEmpty()) {
                            MainUiState.Empty
                        } else {
                            MainUiState.Success(resource.data)
                        }
                    }

                    is Resource.Error -> MainUiState.Error(resource.message)
                }
            }
        }
    }

    /**
     * Obtener películas populares
     */
    fun getPopularMovies() {
        viewModelScope.launch {
            getPopularMoviesUseCase().collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> MainUiState.Loading
                    is Resource.Success -> MainUiState.Success(resource.data)
                    is Resource.Error -> MainUiState.Error(resource.message)
                }
            }
        }
    }
}

/**
 * ESTADOS DE UI para MainActivity
 *
 * Sealed class que representa todos los posibles estados
 */
sealed class MainUiState {
    /**
     * Estado inicial (sin acción)
     */
    object Idle : MainUiState()

    /**
     * Cargando datos
     */
    object Loading : MainUiState()

    /**
     * Datos cargados exitosamente
     */
    data class Success(val movies: List<Movie>) : MainUiState()

    /**
     * Error al cargar datos
     */
    data class Error(val message: String) : MainUiState()

    /**
     * Búsqueda sin resultados
     */
    object Empty : MainUiState()
}
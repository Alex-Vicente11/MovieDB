package com.alexvicente.moviedb.features.popular_movies.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.features.popular_movies.domain.usecase.GetPopularMoviesUseCase
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.popular_movies.presentation.MainUiState
import com.alexvicente.moviedb.features.search.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VIEWMODEL para MainActivity
 *
 * Responsabilidades:
 * - Exponer estados de UI mediante StateFlow
 * - Coordinar UseCases (NO hace lógica de negocio)
 * - Manejar eventos de UI
 * - Sobrevive a cambios de configuración
 */

// CAMBIOS vs versión anterior:
//   AGREGADO → @HiltViewModel
//   AGREGADO → @Inject antes de constructor
//   AGREGADO → imports de HiltViewModel e Inject
//   El cuerpo (searchMovies, getPopularMovies, estados) NO cambia.
//
// @HiltViewModel → Hilt genera un ViewModelFactory para este ViewModel.
// @Inject constructor → Hilt sabe qué dependencias pasar al crearlo.
// El Fragment usa `by viewModels()` y Hilt hace el resto automáticamente.

@HiltViewModel
class MainViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
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


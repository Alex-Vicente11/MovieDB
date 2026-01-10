package com.example.apptest.movies.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptest.movies.domain.model.MovieDetails
import com.example.apptest.movies.domain.usecase.GetMovieDetailsUseCase
import com.example.apptest.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VIEWMODEL para MovieDetailsActivity
 *
 * Responsabilidades:
 * - Cargar detalles de una película por ID
 * - Exponer estado de UI mediante StateFlow
 */
class MovieDetailsViewModel(
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase
) : ViewModel() {

    // Estado privado (mutable)
    private val _uiState = MutableStateFlow<MovieDetailsUiState>(MovieDetailsUiState.Idle)
    // Estado público (inmutable) - lo observa la UI
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

    /**
     * Cargar detalles de una película
     *
     * @param movieId ID de la película
     */
    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            getMovieDetailsUseCase(movieId).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> MovieDetailsUiState.Loading
                    is Resource.Success -> MovieDetailsUiState.Success(resource.data)
                    is Resource.Error -> MovieDetailsUiState.Error(resource.message)
                }
            }
        }
    }
}

/**
 * ESTADOS DE UI para MovieDetailsActivity
 */
sealed class MovieDetailsUiState {
    /**
     * Estado inicial
     */
    object Idle : MovieDetailsUiState()

    /**
     * Cargando detalles
     */
    object Loading : MovieDetailsUiState()

    /**
     * Detalles cargados exitosamente
     */
    data class Success(val movieDetails: MovieDetails) : MovieDetailsUiState()

    /**
     * Error al cargar detalles
     */
    data class Error(val message: String) : MovieDetailsUiState()
}
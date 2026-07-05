package com.alexvicente.moviedb.features.movie_details.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.domain.usecase.GetMovieDetailsUseCase
import com.alexvicente.moviedb.core.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieDetailsUiState>(MovieDetailsUiState.Idle)
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

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

sealed class MovieDetailsUiState {
    object Idle : MovieDetailsUiState()

    object Loading : MovieDetailsUiState()

    data class Success(val movieDetails: MovieDetails) : MovieDetailsUiState()

    data class Error(val message: String) : MovieDetailsUiState()
}
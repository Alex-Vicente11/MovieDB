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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val getPopularMoviesUseCase: GetPopularMoviesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun searchMovies(query: String) {
        viewModelScope.launch {
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


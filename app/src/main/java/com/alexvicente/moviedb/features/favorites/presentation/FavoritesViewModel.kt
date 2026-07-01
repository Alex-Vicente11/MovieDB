package com.alexvicente.moviedb.features.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.usecase.AddFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.GetFavoritesUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.IsFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _isFavoriteState = MutableStateFlow(false)
    val isFavoriteState: StateFlow<Boolean> = _isFavoriteState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private var isFavoriteJob: Job? = null
    private var currentMovie: Movie? = null

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favorites ->
                _uiState.value = if (favorites.isEmpty()) {
                    FavoritesUiState.Empty
                } else {
                    FavoritesUiState.Success(favorites)
                }
            }
        }
    }

    fun observeIsFavorite(movie: Movie) {
        currentMovie = movie
        isFavoriteJob?.cancel()
        viewModelScope.launch {
            isFavoriteUseCase(movie.id).collect { isFav ->
                _isFavoriteState.value = isFav
            }
        }
    }

    fun toggleFavorite() {
        val movie = currentMovie ?: return
        viewModelScope.launch {
            val result = if (_isFavoriteState.value) {
                removeFavoriteUseCase(movie.id)
            } else {
                addFavoriteUseCase(movie)
            }

            if (result is Resource.Error) {
                _errorEvent.emit(result.message)
            }
        }
    }

    fun removeFavorite(movieId: Int) {
        viewModelScope.launch {
            val result = removeFavoriteUseCase(movieId)
            if (result is Resource.Error) {
                _errorEvent.emit(result.message)
            }
        }
    }
}


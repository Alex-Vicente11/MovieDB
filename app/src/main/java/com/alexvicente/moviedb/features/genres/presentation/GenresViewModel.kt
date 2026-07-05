package com.alexvicente.moviedb.features.genres.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.genres.domain.usecase.GetGenresUseCase
import com.alexvicente.moviedb.features.genres.domain.usecase.GetMoviesByGenreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenresViewModel @Inject constructor(
    private val getGenresUseCase: GetGenresUseCase,
    private val getMoviesByGenreUseCase: GetMoviesByGenreUseCase
): ViewModel() {

    private val _genresState = MutableStateFlow<GenresUiState>(GenresUiState.Loading)
    val genresState: StateFlow<GenresUiState> = _genresState.asStateFlow()

    private val _selectedGenreId = MutableStateFlow<Int?>(null)
    val currentGenreId: Int?
        get() = _selectedGenreId.value

    @OptIn(ExperimentalCoroutinesApi::class)
    val movies: Flow<PagingData<Movie>> = _selectedGenreId
        .filterNotNull()
        .flatMapLatest { genreId ->
            getMoviesByGenreUseCase(genreId)
        }
        .cachedIn(viewModelScope)

    init {
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            when (val result = getGenresUseCase()) {
                is Resource.Success -> {
                    _genresState.value = GenresUiState.Success(result.data)
                    _selectedGenreId.value = result.data.firstOrNull()?.id
                }

                is Resource.Error -> {
                    _genresState.value = GenresUiState.Error(result.message)
                }

                is Resource.Loading -> {
                    _genresState.value = GenresUiState.Loading
                }
            }
        }
    }

    fun onGenreSelected(genreId: Int) {
        if (_selectedGenreId.value != genreId) {
            _selectedGenreId.value = genreId
        }
    }
}
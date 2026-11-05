package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeasonDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvSeasonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTvSeasonDetailsUseCase: GetTvSeasonDetailsUseCase
) : ViewModel() {

    private val seriesId: Int = savedStateHandle[ARG_SERIES_ID] ?: INVALID_ARG
    // 0 es una temporada válida («Especiales»): solo la ausencia del argumento usa el centinela
    private val seasonNumber: Int = savedStateHandle[ARG_SEASON_NUMBER] ?: INVALID_ARG

    private val _uiState = MutableStateFlow<TvSeasonUiState>(TvSeasonUiState.Loading)
    val uiState: StateFlow<TvSeasonUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TvSeasonUiState.Loading
            _uiState.value = when (val result = getTvSeasonDetailsUseCase(seriesId, seasonNumber)) {
                is Resource.Success -> TvSeasonUiState.Success(result.data)
                is Resource.Error -> TvSeasonUiState.Error(result.message)
                is Resource.Loading -> TvSeasonUiState.Loading
            }
        }
    }

    companion object {
        // Deben coincidir con los android:name de los <argument> en nav_graph.xml
        const val ARG_SERIES_ID = "seriesId"
        const val ARG_SEASON_NUMBER = "seasonNumber"
        private const val INVALID_ARG = -1
    }
}
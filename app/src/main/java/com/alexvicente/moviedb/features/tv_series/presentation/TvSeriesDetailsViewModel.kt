package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvSeriesDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTvSeriesDetailsUseCase: GetTvSeriesDetailsUseCase
) : ViewModel() {

    private val seriesId: Int = savedStateHandle[ARG_SERIES_ID] ?: INVALID_SERIES_ID

    private val _uiState = MutableStateFlow<TvSeriesDetailsUiState>(TvSeriesDetailsUiState.Loading)
    val uiState: StateFlow<TvSeriesDetailsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TvSeriesDetailsUiState.Loading
            _uiState.value = when (val result = getTvSeriesDetailsUseCase(seriesId)) {
                is Resource.Success -> TvSeriesDetailsUiState.Success(result.data)
                is Resource.Error -> TvSeriesDetailsUiState.Error(result.message)
                is Resource.Loading -> TvSeriesDetailsUiState.Loading
            }
        }
    }

    companion object {
        // Debe coincidir con el android: name del <argument> en nav_graph
        const val ARG_SERIES_ID = "seriesId"
        private const val INVALID_SERIES_ID = -1
    }
}
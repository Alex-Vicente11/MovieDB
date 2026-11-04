package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesCastUseCase
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
    private val getTvSeriesDetailsUseCase: GetTvSeriesDetailsUseCase,
    private val getTvSeriesCastUseCase: GetTvSeriesCastUseCase
) : ViewModel() {

    private val seriesId: Int = savedStateHandle[ARG_SERIES_ID] ?: INVALID_SERIES_ID

    private val _uiState = MutableStateFlow<TvSeriesDetailsUiState>(TvSeriesDetailsUiState.Loading)
    val uiState: StateFlow<TvSeriesDetailsUiState> = _uiState.asStateFlow()

    private val _castState = MutableStateFlow<CastUiState>(CastUiState.Loading)
    val castState: StateFlow<CastUiState> = _castState.asStateFlow()

    private var loadJob: Job? = null
    private var castJob: Job? = null

    init {
        load()
        loadCast()
    }

    fun retry() = load()
    fun retryCast() = loadCast()

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

    private fun loadCast() {
        castJob?.cancel()
        castJob = viewModelScope.launch {
            _castState.value = CastUiState.Loading
            _castState.value = when (val result = getTvSeriesCastUseCase(seriesId)) {
                is Resource.Success -> CastUiState.Success(result.data.take(MAX_CAST_MEMBERS))
                is Resource.Error -> CastUiState.Error(result.message)
                is Resource.Loading -> CastUiState.Loading
            }
        }
    }

    companion object {
        // Debe coincidir con el android: name del <argument> en nav_graph
        const val ARG_SERIES_ID = "seriesId"
        const val MAX_CAST_MEMBERS = 15
        private const val INVALID_SERIES_ID = -1
    }
}
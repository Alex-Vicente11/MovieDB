package com.alexvicente.moviedb.features.tv_series.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeries
import com.alexvicente.moviedb.features.tv_series.domain.model.TvSeriesCategory
import com.alexvicente.moviedb.features.tv_series.domain.usecase.GetTvSeriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class TvSeriesViewModel @Inject constructor(
    private val getTvSeriesUseCase: GetTvSeriesUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(TvSeriesCategory.POPULAR)
    val selectedCategory: StateFlow<TvSeriesCategory> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tvSeries: Flow<PagingData<TvSeries>> = _selectedCategory
        .flatMapLatest { category -> getTvSeriesUseCase(category) }
        .cachedIn(viewModelScope)

    fun onCategorySelected(category: TvSeriesCategory) {
        _selectedCategory.value = category
    }
}
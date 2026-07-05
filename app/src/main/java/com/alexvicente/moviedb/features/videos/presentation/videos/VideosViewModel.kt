package com.alexvicente.moviedb.features.videos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.data.util.Resource
import com.alexvicente.moviedb.features.videos.domain.model.Video
import com.alexvicente.moviedb.features.videos.domain.usecase.GetMovieVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideosViewModel @Inject constructor(
    private val getMovieVideosUseCase: GetMovieVideosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    fun loadVideos(movieId: Int) {
        viewModelScope.launch {
            getMovieVideosUseCase(movieId).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> VideosUiState.Loading
                    is Resource.Success -> {
                        val youtubeVideos = resource.data.filter {
                            it.site.equals("YouTube", ignoreCase = true)
                        }
                        if (youtubeVideos.isEmpty()) {
                            VideosUiState.Empty
                        } else {
                            val sorted = youtubeVideos.sortedWith(
                                compareByDescending<Video> { it.isOfficialTrailer() }
                                    .thenByDescending { it.isTrailer() }
                                    .thenByDescending { it.isTeaser() }
                                    .thenByDescending { it.official }
                            )
                            VideosUiState.Success(sorted)
                        }
                    }
                    is Resource.Error -> VideosUiState.Error(resource.message)
                }
            }
        }
    }
}

sealed class VideosUiState {
    object Idle    : VideosUiState()
    object Loading : VideosUiState()
    object Empty   : VideosUiState()
    data class Success(val videos: List<Video>) : VideosUiState()
    data class Error(val message: String)       : VideosUiState()
}
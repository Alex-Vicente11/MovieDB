package com.example.apptest.features.videos.presentation.videos

import android.os.Message
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptest.core.data.util.Resource
import com.example.apptest.features.videos.domain.model.Video
import com.example.apptest.features.videos.domain.usecase.GetMovieVideosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Responsabilidades:
 * - Cargar videos de una pelicula por ID
 * - Exponer estado de UI mediante StateFlow
 * - Filtrar videos de Youtube únicamente
 */

class VideosViewModel(
    private val getMovieVideosUseCase: GetMovieVideosUseCase
): ViewModel() {

    // Estado privado (mutable)
    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    // Estado público (inmutable) - lo observa la UI
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    /**
     * Cargar videos de una pelicula
     *
     * @param movieId ID de la pelicula
     *
     * Filtrado aplicado:
     * - Solo videos de Youtube (site == "Youtube")
     * - Ordenados por tipo: Trailers primero, luego Teasers, otros...
     * - Dentro de cada tipo, oficiales primero
     */
    fun loadVideos(movieId: Int) {
        viewModelScope.launch {
            getMovieVideosUseCase(movieId).collect { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> VideosUiState.Loading

                    is Resource.Success -> {
                        // Filtrar solo videos de Youtube
                        val youtubeVideos = resource.data.filter { it.site == "Youtube" }

                        if (youtubeVideos.isEmpty()) {
                            VideosUiState.Empty
                        } else {
                            // Ordenar: Trailers oficiales -> Trailers -> Teasers -> otros
                            val sortedVideos = youtubeVideos.sortedWith(
                                compareByDescending<Video> { it.isOfficialTrailer() }
                                    .thenByDescending { it.isTrailer() }
                                    .thenByDescending { it.isTeaser() }
                                    .thenByDescending { it.official }
                            )

                            VideosUiState.Success(sortedVideos)
                        }
                    }

                    is Resource.Error -> VideosUiState.Error(resource.message)
                }
            }
        }
    }
}

// Estados de UI para VideosActivity
sealed class VideosUiState {
    // Estado inicial
    object Idle: VideosUiState()

    // Cargando videos
    object Loading: VideosUiState()

    // Cargados exitosamente
    data class Success(val videos: List<Video>): VideosUiState()

    // No hay videos disponibles
    object Empty: VideosUiState()

    // Error al cargar videos
    data class Error(val message: String): VideosUiState()
}
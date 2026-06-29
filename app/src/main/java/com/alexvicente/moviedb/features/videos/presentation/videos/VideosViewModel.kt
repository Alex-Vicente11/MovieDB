package com.alexvicente.moviedb.features.videos.presentation.videos

import android.util.Log
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

/**
 * Responsabilidades:
 * - Cargar videos de una pelicula por ID
 * - Exponer estado de UI mediante StateFlow
 * - Filtrar videos de Youtube únicamente
 */

// CAMBIOS: @HiltViewModel + @Inject constructor
// El cuerpo (loadVideos, filtrado, ordenamiento, estados) NO cambia.
@HiltViewModel
class VideosViewModel @Inject constructor(
    private val getMovieVideosUseCase: GetMovieVideosUseCase
): ViewModel() {

    // Estado privado (mutable)
    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    // Estado público (inmutable) - lo observa la UI
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    private val TAG = "VideosViewModel"
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
                    is Resource.Loading -> {
                        Log.d(TAG, "Estado: Loading")
                        VideosUiState.Loading
                    }

                    is Resource.Success -> {
                        Log.d(TAG, "Videos recibidos: ${resource.data.size}")

                        // Filtrar solo videos de YouTube (case-insensitive)
                        val youtubeVideos = resource.data.filter { video ->
                            video.site.equals("YouTube", ignoreCase = true)
                        }

                        Log.d(TAG, "Videos de YouTube: ${youtubeVideos.size}")

                        // Log de todos los videos para debugging
                        resource.data.forEach { video ->
                            Log.d(TAG, "Video: ${video.name} | Site: '${video.site}' | Key: ${video.key}")
                        }

                        if (youtubeVideos.isEmpty()) {
                            Log.d(TAG, "Estado: Empty")
                            VideosUiState.Empty
                        } else {
                            // Ordenar: Trailers oficiales → Trailers → Teasers → otros
                            val sortedVideos = youtubeVideos.sortedWith(
                                compareByDescending<Video> { it.isOfficialTrailer() }
                                    .thenByDescending { it.isTrailer() }
                                    .thenByDescending { it.isTeaser() }
                                    .thenByDescending { it.official }
                            )

                            Log.d(TAG, "Estado: Success con ${sortedVideos.size} videos")
                            VideosUiState.Success(sortedVideos)
                        }
                    }

                    is Resource.Error -> {
                        Log.e(TAG, "Estado: Error - ${resource.message}")
                        VideosUiState.Error(resource.message)
                    }
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
package com.alexvicente.moviedb.features.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.features.favorites.domain.usecase.AddFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.GetFavoritesUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.IsFavoriteUseCase
import com.alexvicente.moviedb.features.favorites.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Este ViewModel maneja 2 responsabilidades:
 * 1. Lista de favoritos (FavoritesFragment):
 *    uiState -> Flow<FavoritesUiState> con la lista completa
 * 2. Estado de favorito individual (MovieDetailsFragment):
 *    isFavorite(movieId) -> Flow<Boolean> reactivo desde Room
 *    toggleFavorite()    -> agregar o quitar según el estado actual
 *
 * ¿Por qué un solo ViewModel para ambas pantallas?
 *    FavoritesFragment y MovieDetailsFragment comparten datos de favoritos.
 *    Un ViewModel compartido evita duplicar la lógica de negocio.
 *    MovieDetailsFragment usa: isFavoriteState + toggleFavorite
 *    FavoritesFragment usa: uiState (lista) + removeFavorite
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase
): ViewModel() {

    // Estado de la lista de favoritos
    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    /**
     * Estado de botón (corazon) en detalles
     * StateFlow<Boolean> en lugar de Flow<Boolean> para que el Fragment
     * pueda leer el valor actual sincrónicamente al configurar el ícono.
     */
    private val _isFavoriteState = MutableStateFlow(false)
    val isFavoriteState: StateFlow<Boolean> = _isFavoriteState.asStateFlow()

    private var isFavoriteJob: Job? = null

    // Guardamos la película actual para usarla en toggleFavorite()
    private var currentMovie: Movie? = null

    init {
        // Cargar favoritos inmediatamente al crear el ViewModel
        // Room emitirá actualizaciones automáticas cuando cambien los datos
        loadFavorites()
    }

    // Lista completa de favoritos

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

    /**
     * Observar si una película específica es favorita
     * Llamado desde MovieDetailsFragment cuando carga los detalles
     * Cancela cualquier observación anterior si se llama con otro movieId
     */
    fun observeIsFavorite(movie: Movie) {
        currentMovie = movie
        isFavoriteJob?.cancel()
        viewModelScope.launch {
            isFavoriteUseCase(movie.id).collect { isFav ->
                _isFavoriteState.value = isFav
            }
        }
    }

    /**
     * Alternar favorito (agregar/quitar)
     * LLamado desde MovieDetailsFragment al tocar el botón
     * Room actualiza isFavoriteState automáticamente después de cada operación
     */
    fun toggleFavorite() {
        val movie = currentMovie ?: return
        viewModelScope.launch {
            if (_isFavoriteState.value) {
                removeFavoriteUseCase(movie.id)
            } else {
                addFavoriteUseCase(movie)
            }
            /**
             * No necesitamos actualizar _isFavoriteState manualmente:
             * observeIsFavorite() ya está recolectando el Flow de Room,
             * que emitirá el nuevo valor automáticamente
             */
        }
    }

    /**
     * Eliminar favorito desde la lista
     * Llamado desde FavoritesFragment al tocar el corazón en un item de la lista
     */
    fun removeFavorite(movieId: Int) {
        viewModelScope.launch {
            removeFavoriteUseCase(movieId)
            // Room re-emite la lista actualizada automáticamente
            // loadFavorites() ya está observando ese Flow
        }
    }
}


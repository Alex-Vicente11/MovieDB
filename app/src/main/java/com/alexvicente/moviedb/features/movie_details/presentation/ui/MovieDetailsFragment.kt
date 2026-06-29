package com.alexvicente.moviedb.features.movie_details.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.databinding.FragmentMovieDetailsBinding
import com.alexvicente.moviedb.features.favorites.presentation.FavoritesViewModel
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.presentation.details.MovieDetailsUiState
import com.alexvicente.moviedb.features.movie_details.presentation.details.MovieDetailsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar  // ← material
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.alexvicente.moviedb.core.domain.model.Movie

/**
 * CAMBIOS vs versión anterior:
 *  AGREGADO -> FavoritesViewModel (por viewModels())
 *  AGREGADO -> observeFavoriteState() - observa isFavoriteState de Room
 *  AGREGADO -> setupFavoriteButtom() - configura el FAB de favorito
 *  CAMBIADO -> showMovieDetails() llama a favoritesViewModel.observeIsFavorite()
 *
 *  2 ViewModels en un mismo Fragment:
 *      MovieDetailsViewModel -> carga los detalles de la película (Retrofit + Room)
 *      FavoritesViewModel -> gestiona el estado del botón corazón (solo Room)
 */
@AndroidEntryPoint
class MovieDetailsFragment : Fragment() {

    companion object {
        private const val TAG = "MovieDetailsFragment"
    }

    // ── ViewBinding ──────
    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    // ── Safe Args ────────
    private val args: MovieDetailsFragmentArgs by navArgs()

    // ── ViewModel ────────
    private val viewModel: MovieDetailsViewModel by viewModels()

    private val favoritesViewModel: FavoritesViewModel by viewModels()


    // CICLO DE VIDA
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeUiState()
        observeFavoriteState()
        loadMovieDetails()
    }

    // SETUP
    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())

        binding.toolbar.navigationIcon =
            androidx.core.content.ContextCompat.getDrawable(
                requireContext(), R.drawable.ic_arrow_back
            )
        binding.toolbar.setNavigationIconTint(
            android.graphics.Color.WHITE
        )
    }

    private fun setupListeners() {
        binding.btnWatchVideos.setOnClickListener { navigateToVideos() }

        // Botón corazón llama a toggleFavorite()
        // FavoritesViewModel sabe si es favorito o no -> agrega o elimina
        binding.btnFavorite.setOnClickListener {
            favoritesViewModel.toggleFavorite()
        }
    }

    private fun loadMovieDetails() {
        val movieId = args.movieId
        if (movieId != -1) {
            Log.d(TAG, "Loading details for movie ID: $movieId")
            viewModel.loadMovieDetails(movieId)
        } else {
            Log.e(TAG, "Invalid movieId: $movieId")
            showError("ID de película inválido")
            findNavController().navigateUp()
        }
    }


    // OBSERVACIÓN DE ESTADO
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MovieDetailsUiState.Idle    -> Unit
                        is MovieDetailsUiState.Loading -> showLoading()
                        is MovieDetailsUiState.Success -> {
                            hideLoading()
                            showMovieDetails(state.movieDetails)
                        }
                        is MovieDetailsUiState.Error -> {
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    /**
     * Observar estado del favorito
     * isFavoriteState es un Flow<Boolean> de Room
     * Cada vez que el usuario agrega/elimina el favorito, Room emite el nuevo
     * valor y este collector actualiza el ícono automáticamente.
     */
    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritesViewModel.isFavoriteState.collect { isFavorite ->
                    updateFavoriteBottom(isFavorite)
                }
            }
        }
    }

    // Actualiza el ícono y calor del botón según el estado
    private fun updateFavoriteBottom(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnFavorite.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#E91E63".toColorInt()
                )
            // Ícono blanco sobre fondo rosa
            binding.btnFavorite.setColorFilter(
                android.graphics.Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#2C2C3E".toColorInt()
                )
            // Ícono rosa sobre fondo oscuro — mejor contraste que blanco
            binding.btnFavorite.setColorFilter(
                "#E91E63".toColorInt(),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    // RENDERIZADO DE ESTADOS
    private fun showLoading() {
        binding.progressBar.visibility  = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility  = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun showMovieDetails(movie: MovieDetails) {
        binding.collapsingToolbar.title  = movie.title
        binding.tvTitleDetail.text       = movie.title
        binding.tvOverviewDetail.text    = movie.overview
        binding.tvRatingDetail.text      = movie.getFormattedRating()
        binding.tvVotesDetail.text       = "${movie.voteCount} votos"
        binding.tvReleaseDetail.text     = movie.getReleaseYear()
        binding.tvPopularityDetail.text  = String.format("🔥 %.1f", movie.popularity)
        binding.tvRuntimeDetail?.text    = movie.getFormattedRuntime()
        binding.tvBudgetDetail?.text     = movie.getFormattedBudget()
        binding.tvRevenueDetail?.text    = movie.getFormattedRevenue()

        // Tagline
        if (!movie.tagline.isNullOrEmpty()) {
            binding.tvTaglineDetail?.apply {
                visibility = View.VISIBLE
                text = "\"${movie.tagline}\""
            }
        } else {
            binding.tvTaglineDetail?.visibility = View.GONE
        }

        // Géneros como Chips
        setupGenreChips(movie.getGenresString())

        // Imágenes
        loadImages(movie)

        /**
         * Notificar a FavoritesViewModel qué película estamos viendo para que observe
         * isFavorite(movie.id) en Room.
         * Convertimos MovieDetails -> Movie para el UseCase de favoritos.
         */
        val movieDomain = Movie(
            id = movie.id,
            title = movie.title,
            overview = movie.overview,
            posterPath = movie.posterPath,
            backdropPath = movie.backdropPath,
            voteAverage = movie.voteAverage,
            voteCount = movie.voteCount,
            releaseDate = movie.releaseDate,
            popularity = movie.popularity
        )
        favoritesViewModel.observeIsFavorite(movieDomain)
    }

    /**
     * Crea un Chip por cada género y los añade al ChipGroup.
     * Se limpian primero con removeAllViews() para evitar duplicados
     * si el estado se re-emite (ej: rotación de pantalla).
     */
    private fun setupGenreChips(genresString: String) {
        binding.chipGroupGenres.removeAllViews()

        if (genresString.isBlank()) return

        genresString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { genre ->
                val chip = Chip(requireContext()).apply {
                    text          = genre
                    isClickable   = false
                    isCheckable   = false
                    setTextColor("#FFFFFF".toColorInt())
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        "#2C2C3E".toColorInt()
                    )
                    chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        "#E91E63".toColorInt()
                    )
                    chipStrokeWidth = 1.5f
                    textSize        = 12f
                }
                binding.chipGroupGenres.addView(chip)
            }
    }

    /**
     * Carga backdrop y poster con Glide.
     * Separado de showMovieDetails() → Single Responsibility.
     */
    private fun loadImages(movie: MovieDetails) {
        val backdropUrl = movie.getBackdropUrl()
        if (backdropUrl.isNotEmpty()) {
            Glide.with(this)
                .load(backdropUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivBackdrop)
        }

        val posterUrl = movie.getPosterUrl()
        if (posterUrl.isNotEmpty()) {
            Glide.with(this)
                .load(posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivPosterDetail)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                viewModel.loadMovieDetails(args.movieId)
            }
            .show()
    }


    // NAVEGACIÓN
    private fun navigateToVideos() {
        val movieTitle = binding.tvTitleDetail.text.toString()
        Log.d(TAG, "Navigating to videos: $movieTitle (ID: ${args.movieId})")

        val action = MovieDetailsFragmentDirections
            .actionMovieDetailsToVideos(
                movieId    = args.movieId,
                movieTitle = movieTitle
            )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
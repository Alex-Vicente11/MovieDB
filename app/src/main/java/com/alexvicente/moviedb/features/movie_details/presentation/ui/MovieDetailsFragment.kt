package com.alexvicente.moviedb.features.movie_details.presentation.ui

import android.os.Bundle
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
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.databinding.FragmentMovieDetailsBinding
import com.alexvicente.moviedb.features.favorites.presentation.FavoritesViewModel
import com.alexvicente.moviedb.features.movie_details.domain.model.MovieDetails
import com.alexvicente.moviedb.features.movie_details.presentation.details.MovieDetailsUiState
import com.alexvicente.moviedb.features.movie_details.presentation.details.MovieDetailsViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.loadUrl
import com.alexvicente.moviedb.core.util.showSnackbarWithAction

@AndroidEntryPoint
class MovieDetailsFragment : Fragment() {

    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: MovieDetailsFragmentArgs by navArgs()
    private val viewModel: MovieDetailsViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()

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

    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
        binding.toolbar.navigationIcon =
            androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationIconTint(android.graphics.Color.WHITE)
    }

    private fun setupListeners() {
        binding.btnWatchVideos.setOnClickListener { navigateToVideos() }
        binding.btnFavorite.setOnClickListener { favoritesViewModel.toggleFavorite() }
    }

    private fun loadMovieDetails() {
        val movieId = args.movieId
        if (movieId != -1) {
            viewModel.loadMovieDetails(movieId)
        } else {
            showError("ID de película inválido")
            findNavController().navigateUp()
        }
    }

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

    private fun observeFavoriteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritesViewModel.isFavoriteState.collect { isFavorite ->
                    updateFavoriteBottom(isFavorite)
                }
            }
        }
    }

    private fun updateFavoriteBottom(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnFavorite.backgroundTintList =
                android.content.res.ColorStateList.valueOf("#E91E63".toColorInt())
            binding.btnFavorite.setColorFilter(
                android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.backgroundTintList =
                android.content.res.ColorStateList.valueOf("#2C2C3E".toColorInt())
            binding.btnFavorite.setColorFilter(
                "#E91E63".toColorInt(), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility   = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility   = View.GONE
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

        if (!movie.tagline.isNullOrBlank()) {
            binding.tvTaglineDetail?.apply {
                visibility = View.VISIBLE
                text = "\"${movie.tagline}\""
            }
        } else {
            binding.tvTaglineDetail?.visibility = View.GONE
        }

        setupGenreChips(movie.getGenresString())
        loadImages(movie)

        favoritesViewModel.observeIsFavorite(
            Movie(
                id          = movie.id,
                title       = movie.title,
                overview    = movie.overview,
                posterPath  = movie.posterPath,
                backdropPath = movie.backdropPath,
                voteAverage = movie.voteAverage,
                voteCount   = movie.voteCount,
                releaseDate = movie.releaseDate,
                popularity  = movie.popularity
            )
        )
    }

    private fun setupGenreChips(genresString: String) {
        binding.chipGroupGenres.removeAllViews()
        if (genresString.isBlank()) return

        genresString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { genre ->
                val chip = Chip(requireContext()).apply {
                    text                = genre
                    isClickable         = false
                    isCheckable         = false
                    setTextColor("#FFFFFF".toColorInt())
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf("#2C2C3E".toColorInt())
                    chipStrokeColor     = android.content.res.ColorStateList.valueOf("#E91E63".toColorInt())
                    chipStrokeWidth     = 1.5f
                    textSize            = 12f
                }
                binding.chipGroupGenres.addView(chip)
            }
    }

    private fun loadImages(movie: MovieDetails) {
        binding.ivBackdrop.loadUrl(movie.getBackdropUrl())
        binding.ivPosterDetail.loadUrl(movie.getPosterUrl())
    }

    private fun showError(message: String) {
        binding.root.showSnackbarWithAction(message, "Reintentar") {
            viewModel.loadMovieDetails(args.movieId)
        }
    }

    private fun navigateToVideos() {
        val action = MovieDetailsFragmentDirections.actionMovieDetailsToVideos(
            movieId    = args.movieId,
            movieTitle = binding.tvTitleDetail.text.toString()
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
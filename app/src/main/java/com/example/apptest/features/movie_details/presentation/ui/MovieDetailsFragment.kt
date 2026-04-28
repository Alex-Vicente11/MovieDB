package com.example.apptest.features.movie_details.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.MyApplication
import com.example.apptest.R
import com.example.apptest.databinding.FragmentMovieDetailsBinding
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import com.example.apptest.features.movie_details.presentation.details.MovieDetailsUiState
import com.example.apptest.features.movie_details.presentation.details.MovieDetailsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar  // ← material
import kotlinx.coroutines.launch

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
    private lateinit var viewModel: MovieDetailsViewModel


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
        setupViewModel()
        setupListeners()
        observeUiState()
        loadMovieDetails()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // SETUP
    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
    }

    private fun setupViewModel() {
        val appContainer = (requireActivity().application as MyApplication).appContainer
        viewModel = MovieDetailsViewModel(
            getMovieDetailsUseCase = appContainer.movieDetailsContainer.getMovieDetailsUseCase
        )
    }

    private fun setupListeners() {
        binding.btnWatchVideos.setOnClickListener {
            navigateToVideos()
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
                    setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#2C2C3E")
                    )
                    chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E91E63")
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
}
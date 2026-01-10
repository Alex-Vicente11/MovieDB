package com.example.apptest.movies.presentation.details

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.movies.MyApplication
import com.example.apptest.R
import com.example.apptest.databinding.ActivityMovieDetailsBinding
import com.example.apptest.movies.domain.model.MovieDetails
import com.example.apptest.util.Constants
import kotlinx.coroutines.launch

/**
 * MOVIE DETAILS ACTIVITY (Clean Architecture + MVVM)
 *
 * Responsabilidades (SOLO UI):
 * ✅ Inflar ViewBinding
 * ✅ Obtener movieId del Intent
 * ✅ Observar StateFlow del ViewModel
 * ✅ Renderizar detalles de la película
 *
 * Cambios vs versión anterior:
 * ✅ Usa ViewModel en lugar de recibir todos los datos por Intent
 * ✅ Carga los detalles desde la API (no depende de datos pasados)
 * ✅ ViewBinding en lugar de findViewById
 * ✅ StateFlow en lugar de LiveData
 */
class MovieDetailsActivity : AppCompatActivity() {

    private val TAG = "MovieDetailsActivity"

    // ViewBinding
    private lateinit var binding: ActivityMovieDetailsBinding

    // ViewModel
    private lateinit var viewModel: MovieDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar ViewBinding
        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeUiState()

        // Obtener movieId del Intent y cargar detalles
        val movieId = intent.getIntExtra(Constants.EXTRA_MOVIE_ID, -1)

        if (movieId != -1) {
            Log.d(TAG, "Loading movie details for ID: $movieId")
            viewModel.loadMovieDetails(movieId)
        } else {
            Log.e(TAG, "Invalid movie ID")
            showError("ID de película inválido")
            finish()
        }
    }

    /**
     * Crear ViewModel con dependencias inyectadas
     */
    private fun setupViewModel() {
        val appContainer = (application as MyApplication).appContainer

        viewModel = MovieDetailsViewModel(
            getMovieDetailsUseCase = appContainer.getMovieDetailsUseCase
        )
    }

    /**
     * Configurar listeners
     */
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * Observar el StateFlow del ViewModel
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MovieDetailsUiState.Idle -> {
                            Log.d(TAG, "Estado: Idle")
                        }

                        is MovieDetailsUiState.Loading -> {
                            Log.d(TAG, "Estado: Loading")
                            showLoading()
                        }

                        is MovieDetailsUiState.Success -> {
                            Log.d(TAG, "Estado: Success - ${state.movieDetails.title}")
                            hideLoading()
                            showMovieDetails(state.movieDetails)
                        }

                        is MovieDetailsUiState.Error -> {
                            Log.e(TAG, "Estado: Error - ${state.message}")
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONES DE UI
    // ═══════════════════════════════════════════════════════════

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun showMovieDetails(movie: MovieDetails) {
        // Título
        binding.tvTitleDetail.text = movie.title

        // Overview
        binding.tvOverviewDetail.text = movie.overview

        // Rating
        binding.tvRatingDetail.text = movie.getFormattedRating()

        // Votos
        binding.tvVotesDetail.text = "${movie.voteCount} votos"

        // Fecha de lanzamiento (año)
        binding.tvReleaseDetail.text = movie.getReleaseYear()

        // Popularidad
        binding.tvPopularityDetail.text = String.format("🔥 %.1f", movie.popularity)

        // Duración
        binding.tvRuntimeDetail?.text = movie.getFormattedRuntime()

        // Géneros
        binding.tvGenresDetail?.text = movie.getGenresString()

        // Presupuesto
        binding.tvBudgetDetail?.text = movie.getFormattedBudget()

        // Ingresos
        binding.tvRevenueDetail?.text = movie.getFormattedRevenue()

        // Tagline
        if (!movie.tagline.isNullOrEmpty()) {
            binding.tvTaglineDetail?.visibility = View.VISIBLE
            binding.tvTaglineDetail?.text = "\"${movie.tagline}\""
        } else {
            binding.tvTaglineDetail?.visibility = View.GONE
        }

        // Cargar backdrop (imagen horizontal grande)
        val backdropUrl = movie.getBackdropUrl()
        if (backdropUrl.isNotEmpty()) {
            Glide.with(this)
                .load(backdropUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivBackdrop)
        }

        // Cargar poster (imagen vertical)
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
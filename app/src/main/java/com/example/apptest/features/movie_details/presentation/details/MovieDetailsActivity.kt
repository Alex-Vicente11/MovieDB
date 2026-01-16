package com.example.apptest.features.movie_details.presentation.details

import android.content.Intent
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
import com.example.apptest.R
import com.example.apptest.core.util.Constants
import com.example.apptest.databinding.ActivityMovieDetailsBinding
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import com.example.apptest.MyApplication
import com.example.apptest.features.videos.presentation.videos.VideosActivity
import kotlinx.coroutines.launch

/**
 * MOVIE DETAILS ACTIVITY - Feature: Movie Details
 *
 * UBICACIÓN: features/movie_details/presentation/details/
 *
 * Responsabilidades:
 * - Mostrar información completa de una película
 * - Recibir movieId por Intent
 * - Observar estados del ViewModel
 * - Renderizar UI según el estado
 *
 * Cambios vs versión legacy:
 * Package actualizado: movies.presentation.details → movie_details.presentation.details
 * Imports actualizados para usar feature modular
 * ViewModel ahora usa MovieDetailsContainer
 *
 * Arquitectura MVVM + Clean Architecture:
 * - Activity = Vista tonta (solo renderiza estados)
 * - ViewModel = Lógica de presentación
 * - UseCase = Lógica de negocio
 * - Repository = Acceso a datos
 *
 * Ciclo de vida:
 *  onCreate: Setup inicial (binding, viewModel, observers)
 *  StateFlow + repeatOnLifecycle: Observación lifecycle-aware
 * Sin memory leaks (ViewBinding nullable)
 *
 */
class MovieDetailsActivity : AppCompatActivity() {

    private val TAG = "MovieDetailsActivity"

    // ViewBinding
    private lateinit var binding: ActivityMovieDetailsBinding

    // ViewModel
    private lateinit var viewModel: MovieDetailsViewModel

    // ═══════════════════════════════════════════════════════════════════
    // CICLO DE VIDA: CREACIÓN
    // ═══════════════════════════════════════════════════════════════════

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
     *
     * Nota: Ahora usa movieDetailsContainer del AppContainer
     */
    private fun setupViewModel() {
        val appContainer = (application as MyApplication).appContainer

        viewModel = MovieDetailsViewModel(
            getMovieDetailsUseCase = appContainer.movieDetailsContainer.getMovieDetailsUseCase
        )

        Log.d(TAG, "setupViewModel: ViewModel inicializado con feature modular")
    }

    /**
     * Configurar listeners
     */
    private fun setupListeners() {
        // Boton volver
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Boton para ver videos
        binding.btnWatchVideos.setOnClickListener {
            openVideosActivity()
        }
    }

    /**
     * Observar el StateFlow del ViewModel
     *
     * ✅ repeatOnLifecycle(STARTED):
     *    - Se ejecuta cuando la Activity está STARTED o superior (visible)
     *    - Se CANCELA cuando va a STOPPED (background)
     *    - Previene memory leaks y actualizaciones innecesarias
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "observeUiState: Iniciando observación del StateFlow")

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

    // ═══════════════════════════════════════════════════════════════════
    // FUNCIONES DE UI (Renderizado de estados)
    // ═══════════════════════════════════════════════════════════════════

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

    // Abrir VideosActivity con movieId y titulo
    private fun openVideosActivity() {
        val movieId = intent.getIntExtra(Constants.EXTRA_MOVIE_ID, -1)
        val movieTitle = binding.tvTitleDetail.text.toString()

        if (movieId != -1) {
            val intent = Intent(this, VideosActivity::class.java).apply {
                putExtra(Constants.EXTRA_MOVIE_ID, movieId)
                putExtra(VideosActivity.EXTRA_MOVIE_TITLE, movieTitle)
            }
            startActivity(intent)
            Log.d(TAG, "Opening VideosActivity for movie: $movieTitle (ID: $movieId)")
        } else {
            Toast.makeText(this, "Error: ID de película inválido", Toast.LENGTH_SHORT).show()
        }
    }
}
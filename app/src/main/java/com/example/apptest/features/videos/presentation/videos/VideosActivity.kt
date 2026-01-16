package com.example.apptest.features.videos.presentation.videos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptest.MyApplication
import com.example.apptest.core.util.Constants
import com.example.apptest.databinding.ActivityVideosBinding
import com.example.apptest.features.videos.domain.model.Video
import com.example.apptest.features.videos.presentation.adapter.VideosAdapter
import kotlinx.coroutines.launch

/**
 * Responsabilidades:
 * - Mostrar lista de videos/trailers de una pelicula
 * - Recibir movieId y movieTitle por intent
 * - Observar estados del ViewModel
 * - Renderizar UI segun el estado
 * - Abrir videos en Youtube al hacer click
 *
 * Arquitectura MVVM + Clean Architecture
 * - Activity = Vista tonta (solo renderiza estados)
 * - ViewModel = Lógica de presentación
 * - UseCase = Lógica de negocio
 * - Repository = Acceso a datos
 *
 * Ciclo de vida:
 * onCreate: Setup inicial (binding, viewModel, adapter, observers)
 * StateFlow + repeatOnLifecycle: Obsercacion lifecycle-aware
 * Sin memory leaks (ViewBinding con by lazy)
 */

class VideosActivity: AppCompatActivity() {

    private val TAG = "VideosActivity"

    // ViewBinding (inicializacion lazy para evitar lateinit var)
    private val binding: ActivityVideosBinding by lazy {
        ActivityVideosBinding.inflate(layoutInflater)
    }

    // ViewModel
    private lateinit var viewModel: VideosViewModel

    // Adapter
    private val videosAdapter = VideosAdapter { video ->
        openVideoInYoutube(video)
    }

    // Cilo de vida
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeUiState()

        // Obtener datos del Intent y cargar videos
        val movieId = intent.getIntExtra(Constants.EXTRA_MOVIE_ID, -1)
        val movieTitle = intent.getStringExtra(EXTRA_MOVIE_TITLE) ?: "Pelicula"

        if (movieId != -1) {
            Log.d(TAG, "Loading videos for movie ID: ${movieId}")
            binding.toolbar.title = movieTitle
            viewModel.loadVideos(movieId)
        } else {
            Log.e(TAG, "Invalid movie ID")
            showError("ID de pelicula inválido")
            finish()
        }
    }

    // Configurar Toolbar
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    /**
     * Crear ViewModel con dependencias inyectadas
     *
     * Usa videosContainer del AppContainer
     */
    private fun setupViewModel() {
        val appContainer = (application as MyApplication).appContainer

        viewModel = VideosViewModel(
            getMovieVideosUseCase = appContainer.videosContainer.getMovieVideosUseCase
        )

        Log.d(TAG, "setupViewModel: ViewModel inicializado con feature modular")
    }

    /**
     * Configurar RecyclerView
     */
    private fun setupRecyclerView() {
        binding.recyclerViewVideos.apply {
            layoutManager = LinearLayoutManager(this@VideosActivity)
            adapter = videosAdapter
            setHasFixedSize(true)
        }
    }

    // Configurar listeners
    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Observar el StateFlow del ViewModel
     *
     * repeatOnLifecycle(STARTED):
     * - Se ejecuta cuando la Activity está STARTED o superior (visible)
     * - Se CANCELA cuando va a STOPPED (background)
     * - Previene memory leaks y actualizaciones innecesarias
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "observeUiState: Iniciando observacion del StateFlow")

                viewModel.uiState.collect { state ->
                    when (state) {
                        is VideosUiState.Idle -> {
                            Log.d(TAG, "Estado: Idle")
                        }

                        is VideosUiState.Loading -> {
                            Log.d(TAG, "Estado: Loading")
                            showLoading()
                        }

                        is VideosUiState.Success -> {
                            Log.d(TAG, "Estado: Success - ${state.videos.size} videos")
                            hideLoading()
                            showVideos(state.videos)
                        }

                        is VideosUiState.Empty -> {
                            Log.d(TAG, "Estado: Empty")
                            hideLoading()
                            showEmpty()
                        }

                        is VideosUiState.Error -> {
                            Log.e(TAG, "Estado: Error - ${state.message}")
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    // Funciones de UI (Renderizado de estados)

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showVideos(videos: List<Video>) {
        binding.recyclerViewVideos.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        videosAdapter.updateVideos(videos)
    }

    private fun showEmpty() {
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Abrir video de Youtube
     *
     * Intenta abrir la app de Youtube primero, sino abre en navegador
     */
    private fun openVideoInYoutube(video: Video) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.getYoutubeUrl()))
            startActivity(intent)
            Log.d(TAG, "Opening video: ${video.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Youtube: ${e.message}")
            Toast.makeText(this, "No se pudo abrir el video", Toast.LENGTH_SHORT).show()
        }
    }

    // Constantes
    companion object {
        const val EXTRA_MOVIE_TITLE = "extra_movie_title"
    }
}
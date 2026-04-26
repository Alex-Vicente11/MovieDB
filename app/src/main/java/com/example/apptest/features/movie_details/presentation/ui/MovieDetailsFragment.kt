package com.example.apptest.features.movie_details.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Snackbar
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
import com.example.apptest.databinding.FragmentPopularMoviesBinding
import com.example.apptest.features.movie_details.domain.model.MovieDetails
import com.example.apptest.features.movie_details.presentation.details.MovieDetailsUiState
import com.example.apptest.features.movie_details.presentation.details.MovieDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Antes: MovieDetailsActivity
 *      .Recibia movieId via Intent.getIntExtra()
 *      .Creaba ViewModel manualmente con AppContainer
 *      .Navegaba a VideosActivity via startActivity + Intent
 *      .Tenia btnBack que llamaba a finish()
 *
 * Ahora: Fragment
 *      .Recibe movieId via Safe Args (navArgs()) - type-safe, sin strings raros
 *      .Navega a VideosFragment via NavController
 *      .El botón <- provee la toolbar conectada al NavController (automático)
 *      .Los errores se muestran con Snackbar (materialDesign) en vez de Toast
 *
 * SAFE ARGS - navArgs()
 *
 *El plugin Safe Args genera la clase MovieDetailsFragmentArgs basándose en los <argument> declarados en nav_graph.xml
 *
 * 'by navArgs()' es un delegado de kotlin que:
 *      1.Llama a MovieDetailsFragmentArgs. fromBundle(requireArguments())
 *      2.Devuelve el objeto tipado
 *      3.Lanza una exception clara si falta el argumento (mejor que -1 silencioso)
 *
 * Acceso: args.movieId (Int, garantizando por el tipo declarado en el grafo)
 *
 */

class MovieDetailsFragment: Fragment() {

    companion object {
        private val TAG = "MovieDetailsFragment"
    }

    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    /**
     * navArgs() es un delegado que lee los argumentos del Bundle del Fragment.
     * Solo se puede acceder a 'args' DESPUÉS de que el Fragment está adjunto
     * (onAttach en adelante). Es seguro accederlo en onViewCreated
     *
     * ANTES: val movieId = intent.getIntExtra(Constants.EXTRA_MOVIE_ID, -1)
     * AHORA: args.movieId <- Int garantizado, sin defaultValue ambiguo
     */
    private val args: MovieDetailsFragmentArgs by navArgs()

    private lateinit var viewModel: MovieDetailsViewModel

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

    /**
     * Vincula la Toolbar al NavController
     *
     * setupWithNavController() en una pantalla NO top-level automáticamente:
     *      .Muestra el botón <- (Up button)
     *      .Al presionarlo, llama navController.navigateUp() -> regresa al Fragment anterior
     *
     * Esto elimina la necesidad de binding.btnBack.setOnClickListener { finish() }
     * que exista en la Activity
     */
    private fun setupToolbar(){
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

    /**
     * Carga los detalles usando el movieId recibido via Safe Args
     *
     * ANTES: val movieId = intent.getIntExtra(Constants.EXTRA_MOVIE_ID, -1)
     *          if (movieId == -1) { finish() return }
     *
     * AHORA: args.movieId siempre es válido porque el grafo declara el argumento con Int sin nullable.
     * Si el llamador no lo pasa, Safe Args lanza una exception en tiempo de compilación
     *
     * La validación del -1 se mantiene como defensa en profundidad
     * (el default en nav.graph.xml es -1 como centinela)
     */
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

    // OBSERVAR EL ESTADO
    private fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MovieDetailsUiState.Idle -> Unit
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

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun showMovieDetails(movie: MovieDetails) {
        // Actualizar el titulo en el CollapsingToolbar
        binding.collapsingToolbar.title = movie.title

        binding.tvTitleDetail.text = movie.title
        binding.tvOverviewDetail.text = movie.overview
        binding.tvRatingDetail.text = movie.getFormattedRating()
        binding.tvVotesDetail.text = "${movie.voteCount} votos"
        binding.tvReleaseDetail.text = movie.getReleaseYear()
        binding.tvPopularityDetail.text = String.format("🔥 %.1f", movie.popularity)
        binding.tvRuntimeDetail?.text = movie.getFormattedRuntime()
        binding.tvGenresDetail?.text = movie.getGenresString()
        binding.tvBudgetDetail?.text = movie.getFormattedBudget()
        binding.tvRevenueDetail?.text = movie.getFormattedRevenue()

        if (!movie.tagline.isNullOrEmpty()) {
            binding.tvTaglineDetail?.apply {
                visibility = View.VISIBLE
                text = "\"${movie.tagline}\""
            }
        }else {
            binding.tvTaglineDetail?.visibility = View.GONE
        }

        loadImages(movie)
    }

    /**
     * Cargar Images con Glide.
     * Extraído a función separada -> Single Responsability dentro del Fragment
     * Podría moverse a una clase ImageLoader en el futuro (SRP)
     *
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
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setAction("Reintentar") {
            viewModel.loadMovieDetails(args.movieId)
        }.show()
    }

    /**
     * Navegar a VideosFragment son Safe Args
     *
     * Antes:
     *      val intent = Intent(this, VideosActivity::class.java).apply {
     *          putExtra(Constants.EXTRA_MOVIE_ID, movieId)
     *          putExtra(VideosActivity.EXTRA_MOVIE_TITLE, movieTitle)
     *      }
     *      startActivity(intent)
     *
     * AHORA:
     *      MovieDetailsFragmentDirections.actionMovieDetailsToVideos(movieId, movieTitle)
     *      -> Safe Args genera esta función con los tipos correctos
     *      -> findNavController().navigate() maneja el back stack automáticamente
     *
     * El título se lee directamente del binding porque ya fue seteado
     * en showMovieDetails(). No necesitamos guardarlo por separado
     */
    private fun navigateToVideos() {
        val movieTitle = binding.tvTitleDetail.text.toString()

        Log.d(TAG, "Navigating to videos: $movieTitle (ID: ${args.movieId}")

        val action = MovieDetailsFragmentDirections
            .actionMovieDetailsToVideos()

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
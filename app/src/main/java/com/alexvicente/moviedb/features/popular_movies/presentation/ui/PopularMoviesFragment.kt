package com.alexvicente.moviedb.features.popular_movies.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.databinding.FragmentPopularMoviesBinding
import com.alexvicente.moviedb.features.popular_movies.presentation.MainUiState
import com.alexvicente.moviedb.features.popular_movies.presentation.adapter.MovieAdapter
import com.alexvicente.moviedb.features.popular_movies.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PopularMoviesFragment: Fragment() {

    private var _binding: FragmentPopularMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var adapter: MovieAdapter


    // Job de búsqueda (debounce)
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPopularMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupRealtimeSearch()
        observeUiState()

        // Cargar películas populares solo la primera vez
        // savedInstanceState == null -> primera creación (no rotación, no vuelta de back stack)
        if (savedInstanceState == null) viewModel.getPopularMovies()
        // Si savedInstanceState != null, el ViewModel ya tiene datos ->
        // observeUiState() re-emitirá el estado actual automaticamente
    }

    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
    }

    private fun setupRecyclerView() {
        adapter = MovieAdapter(emptyList()) { movie ->
            navigateToDetails(movie)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PopularMoviesFragment.adapter
            // setHasFixedSize(true) -> optimización: el RecyclerView no recalcula
            // su tamaño cuando el adapter cambia, mejorando el rendimiento en listas largas.
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        // vuelve automáticamente a películas populares (ver setupRealtimeSearch)
    }

    private fun setupRealtimeSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(p0: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                searchJob?.cancel()

                when {
                    query.isEmpty() -> viewModel.getPopularMovies()
                    query.length < Constants.MIN_SEARCH_LENGTH -> { /* Esperar más caracteres */}
                    else -> {
                        searchJob = viewLifecycleOwner.lifecycleScope.launch {
                            delay(Constants.SEARCH_DEBOUNCE_DELAY)
                            viewModel.searchMovies(query)
                        }
                    }
                }
            }
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MainUiState.Idle -> hideLoading()
                        is MainUiState.Loading -> showLoading()
                        is MainUiState.Success -> { hideLoading(); showMovies(state.movies) }
                        is MainUiState.Error -> { hideLoading(); showError(state.message) }
                        is MainUiState.Empty -> { hideLoading(); showEmpty() }
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showMovies(movies: List<Movie>) {
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        adapter.updateMovies(movies)
    }

    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun showEmpty() {
        binding.recyclerView.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun navigateToDetails(movie: Movie) {

        val action = PopularMoviesFragmentDirections
            .actionPopularMoviesToMovieDetails(movie.id)

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        searchJob = null

        _binding = null
    }
}
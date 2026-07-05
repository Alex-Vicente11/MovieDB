package com.alexvicente.moviedb.features.genres.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.data.util.ErrorMapper
import com.alexvicente.moviedb.core.data.util.toUserMessage
import com.alexvicente.moviedb.core.domain.model.Genre
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.databinding.FragmentGenresBinding
import com.alexvicente.moviedb.features.genres.presentation.adapter.MoviesByGenreAdapter
import com.alexvicente.moviedb.features.genres.presentation.adapter.MoviesLoadStateAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GenresFragment: Fragment() {

    private var _binding: FragmentGenresBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GenresViewModel by viewModels()
    private lateinit var moviesAdapter: MoviesByGenreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeGenres()
        observeMovies()
    }

    private fun setupRecyclerView() {
        moviesAdapter = MoviesByGenreAdapter { movie ->
            navigateToDetails(movie)
        }

        binding.recyclerViewMovies.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerViewMovies.adapter = moviesAdapter.withLoadStateFooter(
            footer = MoviesLoadStateAdapter { moviesAdapter.retry() }
        )
        binding.btnRetry.setOnClickListener { moviesAdapter.retry() }
    }

    private fun observeGenres() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.genresState.collect { state ->
                    when (state) {
                        is GenresUiState.Loading -> {
                            binding.progressBarGenres.isVisible = true
                            binding.scrollViewChips.isVisible = false
                            binding.layoutError.isVisible = false
                        }

                        is GenresUiState.Success -> {
                            binding.progressBarGenres.isVisible = false
                            binding.scrollViewChips.isVisible = true
                            binding.layoutError.isVisible = false
                            setupChips(state.genres)
                        }

                        is GenresUiState.Error -> {
                            binding.progressBarGenres.isVisible = false
                            binding.scrollViewChips.isVisible = false
                            binding.layoutError.isVisible = true
                            binding.tvError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun setupChips(genres: List<Genre>) {
        binding.chipGroupGenres.removeAllViews()

        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isCheckable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                setChipStrokeColorResource(R.color.chip_stroke_selector)
                chipStrokeWidth = 1.5f
            }
            chip.setOnClickListener { viewModel.onGenreSelected(genre.id) }
            binding.chipGroupGenres.addView(chip)
        }

        val currentId = viewModel.currentGenreId
        val index = genres.indexOfFirst { it.id == currentId }
        val targetIndex = if (index >= 0) index else 0
        (binding.chipGroupGenres.getChildAt(targetIndex) as? Chip)?.isChecked = true
    }

    private fun observeMovies() {
        moviesAdapter.addLoadStateListener { loadState ->
            val refresh = loadState.refresh
            binding.progressBarMovies.isVisible = refresh is LoadState.Loading
            binding.layoutError.isVisible = refresh is LoadState.Error

            if (refresh is LoadState.Error) {
                binding.tvError.text = ErrorMapper.map(refresh.error).toUserMessage()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                moviesAdapter.onPagesUpdatedFlow.collect {
                    binding.recyclerViewMovies.isVisible = moviesAdapter.itemCount > 0
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movies.collectLatest { pagingData ->
                    moviesAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun navigateToDetails(movie: Movie) {
        val action = GenresFragmentDirections
            .actionGenresToMovieDetails(movieId = movie.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
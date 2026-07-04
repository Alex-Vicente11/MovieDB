package com.alexvicente.moviedb.features.favorites.presentation

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexvicente.moviedb.core.util.showSnackbar
import com.alexvicente.moviedb.databinding.FragmentFavoritesBinding
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite
import com.alexvicente.moviedb.features.favorites.presentation.adapter.FavoritesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment: Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
        observeErrors()
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter(
            onItemClick = { favorite -> navigateToDetails(favorite) },
            onRemoveClick = { favorite -> viewModel.removeFavorite(favorite.id) }
        )
        binding.recyclerViewFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FavoritesFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is FavoritesUiState.Loading -> showLoading()
                        is FavoritesUiState.Empty -> showEmpty()
                        is FavoritesUiState.Success -> showFavorites(state.favorites)
                    }
                }
            }
        }
    }

    private fun observeErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { message ->
                    binding.root.showSnackbar(message)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewFavorites.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewFavorites.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun showFavorites(favorites: List<Favorite>) {
        binding.progressBar.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.recyclerViewFavorites.visibility = View.VISIBLE
        adapter.updateFavorites(favorites)
    }

    private fun navigateToDetails(favorite: Favorite) {
        val action = FavoritesFragmentDirections
            .actionFavoritesToMovieDetails(movieId = favorite.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.alexvicente.moviedb.features.videos.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.databinding.FragmentVideosBinding
import com.alexvicente.moviedb.features.videos.domain.model.Video
import com.alexvicente.moviedb.features.videos.presentation.adapter.VideosAdapter
import com.alexvicente.moviedb.features.videos.presentation.videos.VideosUiState
import com.alexvicente.moviedb.features.videos.presentation.videos.VideosViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.alexvicente.moviedb.core.util.Constants
import com.alexvicente.moviedb.core.util.showSnackbar
import com.alexvicente.moviedb.core.util.showSnackbarWithAction

@AndroidEntryPoint
class VideosFragment : Fragment() {

    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!

    private val args: VideosFragmentArgs by navArgs()
    private val viewModel: VideosViewModel by viewModels()

    private val videosAdapter = VideosAdapter { video -> openVideoInYoutube(video) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeUiState()
        loadVideos()
    }

    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
        binding.toolbar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationIconTint(android.graphics.Color.WHITE)
        binding.toolbar.title = args.movieTitle ?: "Videos"
    }

    private fun setupRecyclerView() {
        binding.recyclerViewVideos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videosAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadVideos() {
        val movieId = args.movieId
        if (movieId != -1) {
            viewModel.loadVideos(movieId)
        } else {
            showError(Constants.ERROR_INVALID_ID)
            findNavController().navigateUp()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is VideosUiState.Idle    -> Unit
                        is VideosUiState.Loading -> showLoading()
                        is VideosUiState.Success -> { hideLoading(); showVideos(state.videos) }
                        is VideosUiState.Empty   -> { hideLoading(); showEmpty() }
                        is VideosUiState.Error   -> { hideLoading(); showError(state.message) }
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility        = View.VISIBLE
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility            = View.GONE
        binding.tvError.visibility            = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showVideos(videos: List<Video>) {
        binding.recyclerViewVideos.visibility = View.VISIBLE
        binding.tvEmpty.visibility            = View.GONE
        binding.tvError.visibility            = View.GONE
        videosAdapter.updateVideos(videos)
    }

    private fun showEmpty() {
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility            = View.VISIBLE
        binding.tvError.visibility            = View.GONE
    }

    private fun showError(message: String) {
        binding.recyclerViewVideos.visibility = View.GONE
        binding.tvEmpty.visibility            = View.GONE
        binding.tvError.visibility            = View.VISIBLE
        binding.tvError.text                  = message
        binding.root.showSnackbarWithAction(message, "Reintentar") {
            viewModel.loadVideos(args.movieId)
        }
    }

    private fun openVideoInYoutube(video: Video) {
        tryOpenYoutubeApp(video) || tryOpenInBrowser(video) || showVideoError()
    }

    private fun tryOpenYoutubeApp(video: Video): Boolean = try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://www.youtube.com/watch?v=${video.key}".toUri()
        ).apply { setPackage("com.google.android.youtube") }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent); true
        } else false
    } catch (e: Exception) { false }

    private fun tryOpenInBrowser(video: Video): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, video.getYoutubeUrl().toUri())
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent); true
        } else false
    } catch (e: Exception) { false }

    private fun showVideoError(): Boolean {
        binding.root.showSnackbar(Constants.ERROR_VIDEO_NOT_AVAILABLE)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
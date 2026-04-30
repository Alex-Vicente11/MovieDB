package com.example.apptest.features.videos.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptest.MyApplication
import com.example.apptest.databinding.FragmentVideosBinding
import com.example.apptest.features.videos.domain.model.Video
import com.example.apptest.features.videos.presentation.adapter.VideosAdapter
import com.example.apptest.features.videos.presentation.videos.VideosUiState
import com.example.apptest.features.videos.presentation.videos.VideosViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

/**
 * ANTES: VideosActivity
 *      .Recibía movieId y movieTitle via Intent extras
 *      .setSupportActionBar + setDisplayHomeAsUpEnabled manualmente
 *      .finish() para navegar atrás
 *      .openVideoInYoutube() mezclada en la Activity
 *
 * AHORA: Fragment
 *      .movieId y movieTitle vie Safe Args (navArgs()) -type-safe
 *      .Toolbar conectada al NavController -> botón <- automático
 *      .navigateUp() reemplaza finish()
 *      .openVideoInYoutube() extraída a función privada pura (sin estado)
 *          -> candidata a moverse a un helper/util en el futuro
 *
 */
@AndroidEntryPoint
class VideosFragment: Fragment() {

    companion object {
        private val TAG = "VideosFragment"
    }

    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!

    private val args: VideosFragmentArgs by navArgs()

    private val viewModel: VideosViewModel by viewModels()

    private val videosAdapter = VideosAdapter { video ->
        openVideoInYoutube(video)
    }

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

    /**
     * Vincula la Toolbar al NavController y establece el título con movieTitle
     *
     * El título viene de Safe Args. Si es null (el llamador no lo pasó),
     * usamos "Videos" como fallback
     *
     * NOTA: Usamos 'binding.toolbar.title = ...' DESPUÉS de setupWithNavController()
     * porque setupWithNavController() puede sobreescribir el título con el
     * android: label del grafo. El orden importa
     */
    private fun setupToolbar() {
        binding.toolbar.setupWithNavController(findNavController())
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
            Log.d(TAG, "Loading videos for movie ID: $movieId")
            viewModel.loadVideos(movieId)
        } else {
            Log.e(TAG, "Invalid movieId: $movieId")
            showError("ID de película inválido")
            findNavController().navigateUp()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is VideosUiState.Idle -> Unit
                        is VideosUiState.Loading -> showLoading()
                        is VideosUiState.Success -> { hideLoading(); showVideos(state.videos) }
                        is VideosUiState.Empty -> { hideLoading(); showEmpty() }
                        is VideosUiState.Error -> { hideLoading(); showError(state.message) }
                    }
                }
            }
        }
    }

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

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") { viewModel.loadVideos(args.movieId) }
            .show()
    }

    /**
        * Abre un video de YouTube con la app nativa o el navegador como fallback.
        *
         * PATRÓN: Strategy implícita con fallback en cascada:
        *   1. App YouTube nativa (mejor experiencia de usuario)
        *   2. Navegador web (fallback universal)
        *   3. Snackbar de error (si ninguno funciona)
         *
         * CLEAN CODE:
        *   • Función con un propósito claro
        *   • Sin efectos secundarios en el estado del Fragment
         *   • Usa requireContext() en lugar de `this` → seguro en Fragment
         *
         * NOTA PARA EL FUTURO:
         *   Esta función podría moverse a un YouTubeIntentLauncher inyectado
         *   via Hilt para hacer el Fragment completamente unit-testeable.
        */
    private fun openVideoInYoutube(video: Video) {
        Log.d(TAG, "Opening video: ${video.name} | key: ${video.key}")

        tryOpenYoutubeApp(video)
                || tryOpenInBrowser(video)
                || showVideoError()
    }

    private fun tryOpenYoutubeApp(video: Video): Boolean {
        return try {
            // Usar URL directa en lugar de vnd.youtube: — más compatible
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.key}")).apply {
                setPackage("com.google.android.youtube")
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Opened in YouTube app")
                true
            } else false
        } catch (e: Exception) {
            Log.w(TAG, "YouTube app not available: ${e.message}")
            false
        }
    }

    private fun tryOpenInBrowser(video: Video): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.getYoutubeUrl()))
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Opened in browser")
                true
            }else false
        }catch (e: Exception) {
            Log.w(TAG, "Browser not available: ${e.message}")
            false
        }
    }

    private fun showVideoError(): Boolean {
        Snackbar.make(
            binding.root,
            "No se puede abrir el video. Verifica que Youtube esté instalado.",
            Snackbar.LENGTH_LONG
        ).show()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

}
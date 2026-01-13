package com.example.apptest.features.popular_movies.presentation.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.core.util.Constants
import com.example.apptest.databinding.ActivityMainBinding
import com.example.apptest.movies.MyApplication
import com.example.apptest.features.popular_movies.presentation.adapter.MovieAdapter
import com.example.apptest.movies.presentation.details.MovieDetailsActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MAIN ACTIVITY (Clean Architecture + MVVM)
 *
 * Ciclo de vida implementado correctamente:
 * ✅ onCreate: Setup inicial (binding, viewModel, listeners, observers)
 * ✅ onStart: Inicia observación con repeatOnLifecycle
 * ✅ onResume: Activity en foreground interactivo
 * ✅ onPause: Activity parcialmente visible
 * ✅ onStop: Se pausa observación automáticamente
 * ✅ onDestroy: Limpieza de recursos (binding, jobs)
 *
 * StateFlow + repeatOnLifecycle:
 * - Se cancela cuando va a background (onStop)
 * - Se reanuda cuando vuelve a foreground (onStart)
 * - Previene memory leaks y actualizaciones innecesarias
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // ViewBinding (nullable para limpieza en onDestroy)
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // ViewModel
    private lateinit var viewModel: MainViewModel

    // Adapter
    private lateinit var adapter: MovieAdapter

    // Job para debounce de búsqueda
    private var searchJob: Job? = null


    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA: CREACIÓN
    // ═══════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Inicializando MainActivity")

        // Inflar ViewBinding
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupRecyclerView()
        setupListeners()
        setupRealtimeSearch()
        observeUiState()

        // Cargar películas populares solo la primera vez
        // savedInstanceState == null significa que NO es una rotación de pantalla
        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: Primera vez, cargando películas populares")
            viewModel.getPopularMovies()
        } else {
            Log.d(TAG, "onCreate: Restaurando desde savedInstanceState")
        }
    }

    /**
     * Crear ViewModel con dependencias inyectadas
     */
    private fun setupViewModel() {
        val appContainer = (application as MyApplication).appContainer

        viewModel = MainViewModel(
            searchMoviesUseCase = appContainer.searchContainer.searchMoviesUseCase,
            getPopularMoviesUseCase = appContainer.popularMoviesContainer.getPopularMoviesUseCase
        )

        Log.d(TAG, "setupViewModel: ViewModel inicializado")
    }

    /**
     * Configurar RecyclerView y Adapter
     */
    private fun setupRecyclerView() {
        adapter = MovieAdapter(emptyList()) { movie ->
            navigateToDetails(movie.id)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        Log.d(TAG, "setupRecyclerView: RecyclerView configurado")
    }

    /**
     * Configurar listeners de botones
     */
    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                Log.d(TAG, "btnSearch: Buscando '$query'")
                viewModel.searchMovies(query)
            } else {
                Toast.makeText(
                    this,
                    Constants.ERROR_EMPTY_QUERY,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnPopular.setOnClickListener {
            Log.d(TAG, "btnPopular: Limpiando búsqueda y cargando populares")
            binding.etSearch.text.clear()
            viewModel.getPopularMovies()
        }
    }

    /**
     * Configurar búsqueda en tiempo real con debounce
     */
    private fun setupRealtimeSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                // Cancelar búsqueda anterior
                searchJob?.cancel()

                when {
                    query.isEmpty() -> {
                        Log.d(TAG, "afterTextChanged: Query vacío, mostrando populares")
                        viewModel.getPopularMovies()
                    }

                    query.length < Constants.MIN_SEARCH_LENGTH -> {
                        Log.d(TAG, "afterTextChanged: Query muy corto ($query), esperando...")
                    }

                    else -> {
                        // Debounce: esperar 500ms antes de buscar
                        searchJob = lifecycleScope.launch {
                            delay(Constants.SEARCH_DEBOUNCE_DELAY)
                            Log.d(TAG, "afterTextChanged: Buscando '$query' después del debounce")
                            viewModel.searchMovies(query)
                        }
                    }
                }
            }
        })
    }

    /**
     * Observar el StateFlow del ViewModel
     *
     * ✅ repeatOnLifecycle(STARTED):
     *    - Se ejecuta cuando la Activity está STARTED o superior (visible)
     *    - Se CANCELA cuando va a STOPPED (background/pantalla apagada)
     *    - Se REANUDA cuando vuelve a STARTED (foreground)
     *    - IMPORTANTE: Previene actualizaciones innecesarias y memory leaks
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            // repeatOnLifecycle: Manejo automático del ciclo de vida
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "observeUiState: Iniciando observación del StateFlow")

                viewModel.uiState.collect { state ->
                    when (state) {
                        is MainUiState.Idle -> {
                            Log.d(TAG, "UiState: Idle")
                            hideLoading()
                        }

                        is MainUiState.Loading -> {
                            Log.d(TAG, "UiState: Loading")
                            showLoading()
                        }

                        is MainUiState.Success -> {
                            Log.d(TAG, "UiState: Success con ${state.movies.size} películas")
                            hideLoading()
                            showMovies(state.movies)
                        }

                        is MainUiState.Error -> {
                            Log.e(TAG, "UiState: Error - ${state.message}")
                            hideLoading()
                            showError(state.message)
                        }

                        is MainUiState.Empty -> {
                            Log.d(TAG, "UiState: Empty (sin resultados)")
                            hideLoading()
                            showEmpty()
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONES DE UI (Renderizado de estados)
    // ═══════════════════════════════════════════════════════════

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
        Log.d(TAG, "showMovies: Mostrando ${movies.size} películas")
    }

    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showEmpty() {
        binding.recyclerView.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
    }

    /**
     * Navegar a detalles pasando SOLO el ID
     */
    private fun navigateToDetails(movieId: Int) {
        Log.d(TAG, "navigateToDetails: Navegando a detalles de película ID: $movieId")

        val intent = Intent(this, MovieDetailsActivity::class.java).apply {
            putExtra(Constants.EXTRA_MOVIE_ID, movieId)
        }
        startActivity(intent)
    }

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA: VISIBILIDAD
    // ═══════════════════════════════════════════════════════════

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Activity visible, observación de StateFlow iniciada")
        // repeatOnLifecycle(STARTED) se activa automáticamente aquí
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity en foreground e interactiva")
        // El usuario puede interactuar con la app
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Activity parcialmente visible (ej: diálogo encima)")
        // Pausar animaciones pesadas, videos, etc. (si tuvieras)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Activity en background, observación pausada")
        // repeatOnLifecycle(STARTED) se cancela automáticamente aquí
        // Esto previene actualizaciones de UI innecesarias cuando la app no está visible
    }

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA: DESTRUCCIÓN Y LIMPIEZA
    // ═══════════════════════════════════════════════════════════

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Limpiando recursos")

        // Cancelar Job de búsqueda pendiente
        searchJob?.cancel()
        searchJob = null

        // Liberar ViewBinding para evitar memory leaks
        _binding = null

        Log.d(TAG, "onDestroy: Recursos liberados correctamente")
    }

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA: ESTADO GUARDADO (para rotaciones)
    // ═══════════════════════════════════════════════════════════

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState: Guardando estado antes de rotación/destrucción")

        // El ViewModel ya sobrevive a rotaciones automáticamente
        // Aquí podrías guardar el texto de búsqueda si quisieras:
        // outState.putString("search_query", binding.etSearch.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState: Restaurando estado después de rotación")

        // El ViewModel mantiene los datos automáticamente
        // Los datos se re-renderizarán cuando se recolecte el StateFlow
    }
}
package com.example.apptest.features.popular_movies.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.unit.Constraints
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apptest.MyApplication
import com.example.apptest.core.domain.model.Movie
import com.example.apptest.core.util.Constants
import com.example.apptest.databinding.FragmentPopularMoviesBinding
import com.example.apptest.features.popular_movies.presentation.adapter.MovieAdapter
import com.example.apptest.features.popular_movies.presentation.main.MainUiState
import com.example.apptest.features.popular_movies.presentation.main.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PopularMoviesFragment: Fragment() {

    companion object {
        private val TAG = "PopularMoviesFragment"
    }

    /**
     * Antes: MainActivity (Activity con lógica de UI mezclada con navegación mediante startActivity + Intent)
     *
     * Ahora: Fragment que:
     *      1. Muestra lista de peliculas populares
     *      2. Permite búsqueda en tiempo real con debounce
     *      3. Navega a MovieDetailsFragment usando NavController (sin intent)
     *
     * Single Responsibility Principle
     *  Este Fragment solo se encarga de la presentación de la lista de películas
     *  La lógica de negocio (buscar, popular) esta en MainViewModel + UseCases
     *
     * Dependency Inversion
     *  El Fragment depende de MainViewModel (abstracción), no de los UseCases directamente.
     *  El ViewModel actúa como la capa intermedia
     *
     * Clean Architecture: Presentation Layer
     *  Fragment = Vista (observa estado, dispara eventos de usuario)
     *  ViewModel = Presentador (transforma datos del dominio en estado de UI)
     *  UseCase = Lógica de negocio (independiente de Android)
     *
     */

    private var _binding: FragmentPopularMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel

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
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        setupRealtimeSearch()
        observeUiState()

        // Cargar películas populares solo la primera vez
        // savedInstanceState == null -> primera creación (no rotación, no vuelta de back stack)
        if (savedInstanceState == null) {
            viewModel.getPopularMovies()
        }
        // Si savedInstanceState != null, el ViewModel ya tiene datos ->
        // observeUiState() re-emitirá el estado actual automaticamente
    }

    /**
     * Configura la Toolbar con el NavController
     *
     * setupWithNavController() hace automáticamente:
     *      . Mostrar/ocultar el botón <- según si somos top-level destination
     *      . Actualizar el título según Android: label del grafo
     *      . Manejar el click en <- para llamar navController.navigateUp()
     *
     *appBarConfiguration define quién es top-level. PopularMoviesFragment
     * es top-level -> NO muestra botón <-
     */
    private fun setupToolbar() {
        val navController = findNavController()
        binding.toolbar.setupWithNavController(navController)
    }

    /**
     * Inicializar ViewModel con sus dependencias
     *
     * Manual Dependency Injection via AppContainer (Service Locator).
     * El AppContainer es accesible desde cualquier Fragment a través de
     * requireActivity().application -> (as MyApplication).appContainer
     *
     * Diferencia con la Activity:
     *      Activity: (application as MyApplication).appContainer
     *      Fragment: (requireActivity().application as MyApplication).appContainer
     *
     */
    private fun setupViewModel(){
        val appContainer = (requireActivity().application as MyApplication).appContainer

        viewModel = MainViewModel(
            searchMoviesUseCase = appContainer.searchContainer.searchMoviesUseCase,
            getPopularMoviesUseCase = appContainer.popularMoviesContainer.getPopularMoviesUseCase
        )
    }

    /**
     * Configurar RecyclerView y Adapter
     *
     * El click en una película llama a navigateToDetails(), que usa
     * NavController en lugar de startActivity + Intent
     *
     */
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

    /**
     * Listeners de botones y acciones de usuario
     *
     */
    private fun setupListeners() {
        //No hay botón btnPopular explícito; limpiar campo de búsqueda
        // vuelve automáticamente a películas populares (ver setupRealtimeSearch)
    }

    /**
     * Búsqueda en tiempo real con debounce
     *
     * Patrón: Debounce
     * Evita llamar a la API en cada keystroke. Espera SEARCH_DEBOUNCE_DELAY ms
     * después de que el usuario deja de escribir antes de lanzar la búsqueda
     *
     * Flujo:
     *  Usuario escribe "S" -> cancela job anterior, lanza nuevo con delay
     *  Usuario escribe "Sp" -> cancela job anterior, lanza nuevo con delay
     *  Usuario escribe "Spi" -> cancela job anterior, lanza nuevo con delay
     *  ...500ms sin escribir -> el job no se cancela -> llama viewModel.searchMovies("Spider")
     *
     *  Este comportamiento podría extraerse a un extensión de EditText
     *  para reutilizarlo en SearchFragment, si se implementa
     *
     */
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

    /**
     * Observar el StateFlow del ViewModel con lifecycle-awareness
     *
     * Diferencia Crítica Fragment vs Activity:
     *      Activity usa: lifecycleScope.launch { repeatOnLifecycle(STARTED) }
     *      Fragment usa: viewLifecycleOwner.lifecycleScope.launch { repeatOnLifecycle(STARTED) }
     *
     * ¿Por qué viewLifecycleOwner y no this (en Fragment)?
     * Un Fragment puede estar en el back stack: vivo pero sin vista.
     * Usar 'this' podría mantener la corrutina activa incluso cuando la vista fue destruida, causando:
     *
     *      .Actualizaciones en una vista que no existe -> crash
     *      .Memory leaks (la vista antigua referenciada por la corrutina)
     *
     * repeatOnLifecycle(STARTED):
     *  .Se activa cuando la vista es visible (onStart)
     *  .Se PAUSA cuando la vista va a background (onStop)
     *  .No consume recursos cuando el Fragment no está visible
     *
     */
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

    /**
     * Navegar a MovieDetailsFragment
     * Antes (con Activities):
     * val intent = Intent(this, MovieDetailsActivity::class.java)
     * intent.putExtra(Constants.EXTRA_MOVIE_ID, movie.id)
     * startActivity(intent)
     *
     * AHORA (con Navigation Component + Safe Args):
     * val action = PopularMovieFragmentDirections.actionPopularMoviesDetails(movie.id)
     * findNavController().navigate(action)
     *
     * VENTAJAS:
     *  .Type-safe: El compilador verifica que movieId es un Int (Safe Args)
     *  .No hay "magic strings" como "extra_movie_id"
     *  .La animación de transición está definida en el grafo (nav_graph.xml)
     *  .No hay que registrar destinos en el Manifest
     *
     *  NOTA SOBRE Safe Args:
     *      Después de compilar, el plugin genera automáticamente:
     *      PopularMoviesFragmentDirections.actionPopularMoviesDetails(movieId: Int)
     *      Si cambias el tipo del argumento en nav_graph.xml, el compilador
     *      te avisará aquí con un error de tipo.
     *
     */
    private fun navigateToDetails(movie: Movie) {
        Log.d(TAG, "Navigating to details: ${movie.title} (ID: ${movie.id}")

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
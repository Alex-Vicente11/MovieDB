package com.example.apptest

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apptest.data.model.Movie
import com.example.apptest.data.network.NetworkResult
import com.example.apptest.data.ui.MovieViewModel
import com.example.apptest.data.ui.adapter.MovieAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var viewModel: MovieViewModel
    private lateinit var adapter: MovieAdapter

    // Views
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnPopular: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvEmpty: TextView

    // Jop para controlar la busqueda
    // Job: representa una corrutina que puede ser cancelada
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViewModel()
        setupViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupRealtimeSearch()


        // Cargar películas populares al inicio
        viewModel.getPopularMovies()
    }

    private fun setupViewModel() {
        // ViewModelProvider: Crea o recupera al ViewModel
        // this: Owner del lifecycle
        viewModel = ViewModelProvider(this)[MovieViewModel::class.java]
    }

    private fun setupViews() {
        // findViewById: Buscar una vista por su ID
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnPopular = findViewById(R.id.btnPopular)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupRecyclerView() {
        // Adapter con lista vacia inicial
        adapter = MovieAdapter(emptyList()) { movie ->
            // Lambda: Se ejecuta cuando se hace click en una pelicula
            showMovieDetails(movie)
        }

        // apply: Scope function para configurar
        recyclerView.apply {
            // this: Es el recyclerView
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupObservers() {
        // observe: Observa los cambios en el LiveData
        // this: LifecycleOwner
        viewModel.movies.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoading()
                }

                is NetworkResult.Success -> {
                    hideLoading()
                    // result.data: Los datos (List<Movie>)
                    result.data?.let { movies ->
                        if (movies.isEmpty()) {
                            showEmpty()
                        } else {
                            showMovies(movies)
                        }
                    }
                }

                is NetworkResult.Error -> {
                    hideLoading()
                    showError(result.message ?: "Error desconocido")
                }
            }
        }

        // Observer para peliculas populares
        viewModel.popularMovies.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> showLoading()
                is NetworkResult.Success -> {
                    hideLoading()
                    result.data?.let { showMovies(it) }
                }

                is NetworkResult.Error -> {
                    hideLoading()
                    showError(result.message ?: "Error desconocido")
                }
            }
        }
    }

    private fun setupListeners() {
        // setOnClickListener {
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchMovies(query)
            } else {
                Toast.makeText(this, "Ingresa un término de búsqueda", Toast.LENGTH_SHORT).show()
            }
        }
        btnPopular.setOnClickListener {
            etSearch.text.clear()
            viewModel.getPopularMovies()
        }
    }

    private fun setupRealtimeSearch() {
        // TextWatcher: Interface que detecta cambios en el EditText
        etSearch.addTextChangedListener(object : TextWatcher {
            // beforeTextChanged: Antes de que cambie el texto
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // no hay implementacion
            }

            //onTextChanged: Mientras se esta escribiendo
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // no hay implementacion
            }

            override fun afterTextChanged(s: Editable?) {
                // s: El texto actual del EditText
                val query = s?.toString()?.trim() ?: ""

                Log.d(TAG, "Text changed: '$query")

                // cancelar busqueda anterior si existe
                // ?: Job anterior se cancela cuando escribes rapido
                searchJob?.cancel()

                if (query.isEmpty()) {
                    // si esta vacio, mostrar populares
                    Log.d(TAG, "Query is empty, showing popular movies")
                    viewModel.getPopularMovies()
                    return
                }

                if (query.length < 2) {
                    // No buscar si hay menos de 2 caracteres
                    Log.d(TAG, "Query too short, waiting...")
                    return
                }

                // DEBOUNCE: Esperar 500ms antes de buscar
                // Esto evita hacer peticiones por cada letra
                searchJob = lifecycleScope.launch {
                    // delay: pausa la corrutina sin bloquear el thread
                    // si el usuario sigue escribiendo, esta corritina se cancela
                    delay(500) // esperar medio segundo

                    Log.d(TAG, "Performing search for: '$query'")
                    performSearch(query)
                }
            }
        })
    }

    private fun performSearch(query: String) {
        viewModel.searchMovies(query)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvError.visibility = View.GONE
        tvEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showMovies(movies: List<Movie>) {
        recyclerView.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        adapter.updateMovies(movies)

        Log.d(TAG, "Showing ${movies.size} movies")
    }

    private fun showError(message: String) {
        recyclerView.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $message")
    }

    private fun showEmpty() {
        recyclerView.visibility = View.GONE
        tvError.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
    }

    private fun showMovieDetails(movie: Movie) {
        // Navegar a pantalla de detalles
        val message = """
            ${movie.title}
            ${movie.voteAverage}/10
            ${movie.releaseDate}
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Movie clicked: ${movie.title}")
    }
}
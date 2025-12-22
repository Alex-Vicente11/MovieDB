package com.example.apptest

import com.example.apptest.data.repository.MovieRepository
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val repository = MovieRepository()

    // Referencias a los TextViews
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResults)

        updateStatus("Iniciando búsqueda...")

        // Probar búsqueda de películas
        searchMovies("Jack Reacher")

        // Probar películas populares
        getPopularMovies()
    }

    private fun searchMovies(query: String) {
        lifecycleScope.launch {
            Log.d(TAG, "\n" + "=".repeat(50))
            Log.d(TAG, "🎯 SEARCHING MOVIES: '$query'")
            Log.d(TAG, "=".repeat(50) + "\n")

            updateStatus("Buscando '$query'...")

            val movies = repository.searchMovies(query)

            if (movies != null && movies.isNotEmpty()) {
                Log.d(TAG, "\n✅ SUCCESS! Found ${movies.size} movies\n")

                // Construir resultado para mostrar en pantalla
                val resultText = buildString {
                    appendLine("✅ Búsqueda: '$query'")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Encontradas: ${movies.size} películas\n")

                    movies.forEachIndexed { index, movie ->
                        appendLine("${index + 1}. ${movie.title}")
                        appendLine("   ⭐ ${movie.voteAverage}/10")
                        appendLine("   📅 ${movie.releaseDate}")
                        appendLine()
                    }
                }

                updateResults(resultText)
                updateStatus("Búsqueda completada ✅")

                // Log detallado de cada película
                movies.forEach { movie ->
                    Log.d(TAG, """
                        ╔════════════════════════════════════
                        ║ 🎬 ${movie.title}
                        ╠════════════════════════════════════
                        ║ ID: ${movie.id}
                        ║ Original: ${movie.originalTitle}
                        ║ Rating: ⭐ ${movie.voteAverage}/10 (${movie.voteCount} votes)
                        ║ Release: ${movie.releaseDate}
                        ║ Language: ${movie.originalLanguage}
                        ║ Popularity: ${movie.popularity}
                        ║ Adult: ${movie.adult}
                        ║ Genres: ${movie.genreIds}
                        ║ Overview: ${movie.overview}
                        ║ Poster: ${movie.getPosterURL()}
                        ║ Backdrop: ${movie.getBackdropUrl()}
                        ╚════════════════════════════════════
                    """.trimIndent())
                }
            } else {
                Log.e(TAG, " No movies found or error occurred")
                updateStatus("Error en la búsqueda ")
                updateResults("No se encontraron películas")
            }
        }
    }

    private fun getPopularMovies() {
        lifecycleScope.launch {
            Log.d(TAG, "=== POPULAR MOVIES ===")
            val movies = repository.getPopularMovies()

            movies?.let {
                Log.d(TAG, "Top 5 popular movies:")
                it.take(5).forEachIndexed { index, movie ->
                    Log.d(TAG, "${index + 1}. ${movie.title}")
                    Log.d(TAG, "${movie.voteAverage}/10 | Popularity: ${movie.popularity}")
                }
            } ?: run {
                Log.e(TAG, "Failed to get popular movies")
            }
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            tvStatus.text = status
        }
    }

    private fun updateResults(results: String) {
        runOnUiThread {
            tvResult.text = results
        }
    }
}
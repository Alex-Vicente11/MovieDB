package com.example.apptest

import com.example.apptest.data.repository.MovieRepository
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val repository = MovieRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Probar búsqueda de películas
        searchMovies("Jack Reacher")

        // Probar películas populares
        getPopularMovies()
    }

    private fun searchMovies(query: String) {
        lifecycleScope.launch {
            Log.d(TAG, "=== SEARCHING MOVIES ===")
            val movies = repository.searchMovies(query)

            if (movies != null) {
                Log.d(TAG, "Found ${movies.size} movies for '$query':")
                movies.forEach { movie ->
                    Log.d(TAG, """
                        ---
                        Title: ${movie.title}
                        Original: ${movie.originalTitle}
                        Rating: ${movie.voteAverage}/10 (${movie.voteCount} votes)
                        Release: ${movie.releaseDate}
                        Overview: ${movie.overview.take(100)}...
                        Poster: ${movie.getPosterURL()}
                        ---
                    """.trimIndent())
                }
            } else {
                Log.e(TAG, "Failed to get movies")
            }
        }
    }

    private fun getPopularMovies() {
        lifecycleScope.launch {
            Log.d(TAG, "=== POPULAR MOVIES ===")
            val movies = repository.getPopularMovies()

            movies?.let {
                Log.d(TAG, "Top 5 popular movies:")
                it.take(5).forEach { movie ->
                    Log.d(TAG, "${movie.title} - ${movie.voteAverage}/10")
                }
            } ?: run {
                Log.e(TAG, "Failed to get popular movies")
            }
        }
    }
}
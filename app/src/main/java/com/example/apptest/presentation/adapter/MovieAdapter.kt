package com.example.apptest.presentation.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R
import com.example.apptest.databinding.ItemMovieBinding
import com.example.apptest.domain.model.Movie

/**
 * ADAPTER para RecyclerView de películas
 *
 * Refactor:
 * ViewBinding en lugar de findViewById
 * - referencias cacheadas
 * - null-safety y type-safety
 * - menos boilerplate
 */
class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val TAG = "MovieAdapter"

    // Nota: Binding se pasa por constructor y se cachea



    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            Log.d(TAG, "Binding movie: ${movie.title}")

            binding.apply {
                // title
                tvMovieTitle.text = movie.title

                // Rating
                tvMovieRating.text = movie.getFormattedRating()

                // Año
                tvMovieYear.text = movie.getReleaseYear()

                // Overview
                tvMovieOverview.text = if (movie.overview.length > 150) {
                    "${movie.overview.take(150)}..."
                } else {
                    movie.overview
                }

                // Cargar imagen
                val imageUrl = movie.getPosterUrl()
                Log.d(TAG, "Loading image from: $imageUrl")

                if (imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_movie_placeholder)
                        .error(R.drawable.ic_movie_placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivMoviePoster)
                } else {
                    Log.w(TAG, "Image URL is empty, using placeholder")
                    ivMoviePoster.setImageResource(R.drawable.ic_movie_placeholder)
                }

                // Click listener - root es el CardView completo
                root.setOnClickListener {
                    onMovieClick(movie)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // Inflar usando binding
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    /**
     * Actualizar la lista de películas
     */
    fun updateMovies(newMovies: List<Movie>) {
        Log.d(TAG, "Updating movies: ${newMovies.size} items")
        movies = newMovies
        notifyDataSetChanged()
    }
}
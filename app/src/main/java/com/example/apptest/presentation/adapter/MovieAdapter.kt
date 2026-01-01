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
import com.example.apptest.domain.model.Movie

/**
 * ADAPTER para RecyclerView de películas
 *
 * Ahora usa el modelo de dominio limpio (sin anotaciones)
 */
class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val TAG = "MovieAdapter"

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        private val tvRating: TextView = itemView.findViewById(R.id.tvMovieRating)
        private val tvYear: TextView = itemView.findViewById(R.id.tvMovieYear)
        private val tvOverview: TextView = itemView.findViewById(R.id.tvMovieOverview)
        private val ivPoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)

        fun bind(movie: Movie) {
            Log.d(TAG, "Binding movie: ${movie.title}")

            // Título
            tvTitle.text = movie.title

            // Rating - usar helper del modelo de dominio
            tvRating.text = movie.getFormattedRating()

            // Año - usar helper del modelo de dominio
            tvYear.text = movie.getReleaseYear()

            // Overview truncado
            tvOverview.text = if (movie.overview.length > 150) {
                "${movie.overview.take(150)}..."
            } else {
                movie.overview
            }

            // Cargar imagen - usar helper del modelo de dominio
            val imageUrl = movie.getPosterUrl()
            Log.d(TAG, "Loading image from: $imageUrl")

            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivPoster)
            } else {
                Log.w(TAG, "Image URL is empty, using placeholder")
                ivPoster.setImageResource(R.drawable.ic_movie_placeholder)
            }

            // Click listener
            itemView.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
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
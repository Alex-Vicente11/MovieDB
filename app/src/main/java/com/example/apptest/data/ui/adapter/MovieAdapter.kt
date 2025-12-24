package com.example.apptest.data.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.layout.Layout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R
import com.example.apptest.data.model.Movie

class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
): RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val TAG = "MovieAdapter"
    inner class MovieViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        private val tvRating: TextView = itemView.findViewById(R.id.tvMovieRating)
        private val tvYear: TextView = itemView.findViewById(R.id.tvMovieYear)
        private val tvOverview: TextView = itemView.findViewById(R.id.tvMovieOverview)
        private val ivPoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)

        fun bind(movie: Movie) {
            Log.d(TAG, "Binding movie: ${movie.title}")
            Log.d(TAG, "  vote_average: ${movie.voteAverage}")
            Log.d(TAG, "  poster_path: ${movie.posterPath}")
            // titulo
            tvTitle.text = movie.title

            // Rating con formato
            val ratingText = String.format("%.1f/10", movie.voteAverage)
            tvRating.text = "⭐ $ratingText"

            // Año
            tvYear.text = if (!movie.releaseDate.isNullOrEmpty()) {
                movie.releaseDate.split("-").firstOrNull() ?: "N/A"
            } else {
                "N/A"
            }

            // Overview
            tvOverview.text = if (!movie.overview.isNullOrEmpty()) {
                if (movie.overview.length > 150) {
                    "${movie.overview.take(150)}..."
                } else {
                    movie.overview
                }

            } else {
                "Sin descripcion disponible"
            }

            // Cargar imagen con Glide o Coil (placeholder)
            // getPosterURL() ya construye la URL completa
            val imageUrl = movie.getPosterURL()
            Log.d(TAG, " Loading image from: $imageUrl")

            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)    // contexto
                    .load(imageUrl)     // URL de la imagen
                    .placeholder(R.drawable.ic_movie_placeholder) // mientras carga
                    .error(R.drawable.ic_movie_placeholder) // si falla
                    .transition(DrawableTransitionOptions.withCrossFade()) // animacion
                    .into(ivPoster)     // imageView
            } else {
                Log.w(TAG, "Image URL is empty, using placeholder")
                ivPoster.setImageResource(R.drawable.ic_movie_placeholder)
            }

            // setOnClickListener: Lambda que se ejecuta al hacer click
            itemView.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // LayoutInflater: Convierte XML a View
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        Log.d(TAG, "Updating movies: ${newMovies.size} items")
        movies = newMovies
        // notifyDataSetChanged: Notifica que los datos cambiaron
        notifyDataSetChanged()
    }
}
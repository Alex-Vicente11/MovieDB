package com.example.apptest.data.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.layout.Layout
import androidx.recyclerview.widget.RecyclerView
import com.example.apptest.R
import com.example.apptest.data.model.Movie

class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
): RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMovieTitle)
        private val tvRating: TextView = itemView.findViewById(R.id.tvMovieRating)
        private val tvYear: TextView = itemView.findViewById(R.id.tvMovieYear)
        private val tvOverview: TextView = itemView.findViewById(R.id.tvMovieOverview)
        private val ivPoster: ImageView = itemView.findViewById(R.id.ivMoviePoster)

        fun bind(movie: Movie) {
            tvTitle.text = movie.title
            tvRating.text = "${movie.voteAverage}/10"

            tvYear.text = movie.releaseDate.split("-").firstOrNull() ?: "N/A"

            tvOverview.text = if (movie.overview.length > 150) {
                "${movie.overview.take(150)}"
            } else {
                movie.overview
            }

            // Cargar imagen con Glide o Coil (placeholder)
            ivPoster.setImageResource(R.drawable.ic_movie_placeholder)

            // setOnClickListener: Lambda que se ejecuta al hacer click
            itemView.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // LayoutInflater: Convierte XML a View
        val vie = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        // notifyDataSetChanged: Notifica que los datos cambiaron
        notifyDataSetChanged()
    }
}
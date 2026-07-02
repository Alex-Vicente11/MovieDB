package com.alexvicente.moviedb.features.popular_movies.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.loadUrl
import com.alexvicente.moviedb.databinding.ItemMovieBinding

class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.apply {
                updateTitle(movie)
                updateRating(movie)
                updateYear(movie)
                updateOverview(movie)
                updatePoster(movie)
                setupClickListener(movie)
            }
        }

        fun bind(movie: Movie, payload: MovieChangePayload) {
            binding.apply {
                if (payload.titleChanged) updateTitle(movie)
                if (payload.ratingChanged) updateRating(movie)
                if (payload.overviewChanged) updateOverview(movie)
                if (payload.posterChanged) updatePoster(movie)
            }
        }

        private fun ItemMovieBinding.updateTitle(movie: Movie) {
            tvMovieTitle.text = movie.title
        }

        private fun ItemMovieBinding.updateRating(movie: Movie) {
            tvMovieRating.text = movie.getFormattedRating()
        }

        private fun ItemMovieBinding.updateYear(movie: Movie) {
            tvMovieYear.text = movie.getReleaseYear()
        }

        private fun ItemMovieBinding.updateOverview(movie: Movie) {
            tvMovieOverview.text = if (movie.overview.length > 150) {
                "${movie.overview.take(150)}..."
            } else {
                movie.overview
            }
        }

        private fun ItemMovieBinding.updatePoster(movie: Movie) {
            ivMoviePoster.loadUrl(movie.getPosterUrl())
        }

        private fun ItemMovieBinding.setupClickListener(movie: Movie) {
            root.setOnClickListener { onMovieClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun onBindViewHolder(
        holder: MovieViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val payload = payloads[0] as? MovieChangePayload
            if (payload != null) {
                holder.bind(movies[position], payload)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        val diffResult = DiffUtil.calculateDiff(MovieDiffCallback(movies, newMovies))
        movies = newMovies
        diffResult.dispatchUpdatesTo(this)
    }
}
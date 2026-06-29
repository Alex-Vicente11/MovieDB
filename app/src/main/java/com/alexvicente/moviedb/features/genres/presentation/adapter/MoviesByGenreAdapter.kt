package com.alexvicente.moviedb.features.genres.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.databinding.ItemMovieBinding

class MoviesByGenreAdapter(
    private val onMovieClick: (Movie) -> Unit
) : PagingDataAdapter<Movie, MoviesByGenreAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
        // getItem puede ser null — Paging 3 lo maneja con placeholders
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.apply {
                tvMovieTitle.text = movie.title
                tvMovieRating.text = movie.getFormattedRating()
                tvMovieOverview.text = movie.overview
                tvMovieYear.text = movie.getReleaseYear()

                // Reutiliza getPosterUrl() de tu Movie.kt
                Glide.with(ivMoviePoster)
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivMoviePoster)

                root.setOnClickListener { onMovieClick(movie) }
            }
        }
    }
}

class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(old: Movie, new: Movie) = old.id == new.id
    override fun areContentsTheSame(old: Movie, new: Movie) = old == new
}
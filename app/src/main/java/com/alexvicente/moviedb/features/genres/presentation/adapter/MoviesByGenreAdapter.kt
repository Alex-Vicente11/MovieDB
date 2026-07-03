package com.alexvicente.moviedb.features.genres.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.alexvicente.moviedb.core.domain.model.Movie
import com.alexvicente.moviedb.core.util.loadUrl
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
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.apply {
                tvMovieTitle.text    = movie.title
                tvMovieRating.text   = movie.getFormattedRating()
                tvMovieOverview.text = movie.overview
                tvMovieYear.text     = movie.getReleaseYear()
                ivMoviePoster.loadUrl(movie.getPosterUrl())
                root.setOnClickListener { onMovieClick(movie) }
            }
        }
    }
}

class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(old: Movie, new: Movie) = old.id == new.id
    override fun areContentsTheSame(old: Movie, new: Movie) = old == new
}
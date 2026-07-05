package com.alexvicente.moviedb.features.popular_movies.presentation.adapter

import androidx.recyclerview.widget.DiffUtil
import com.alexvicente.moviedb.core.domain.model.Movie

class MovieDiffCallback(
    private val oldList: List<Movie>,
    private val newList: List<Movie>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldMovie = oldList[oldItemPosition]
        val newMovie = newList[newItemPosition]

        return MovieChangePayload(
            titleChanged = oldMovie.title != newMovie.title,
            ratingChanged = oldMovie.voteAverage != newMovie.voteAverage,
            overviewChanged = oldMovie.overview != newMovie.overview,
            posterChanged = oldMovie.posterPath != newMovie.posterPath
        )
    }
}

data class MovieChangePayload(
    val titleChanged: Boolean = false,
    val ratingChanged: Boolean = false,
    val overviewChanged: Boolean = false,
    val posterChanged: Boolean = false
)
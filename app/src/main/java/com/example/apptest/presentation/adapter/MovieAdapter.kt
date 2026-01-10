package com.example.apptest.presentation.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R
import com.example.apptest.databinding.ItemMovieBinding
import com.example.apptest.domain.model.Movie

/**
 * ADAPTER para RecyclerView de películas
 *
 * Optimizado:
 * - ViewBinding / referencias cacheadas
 * - null-safety y type-safety
 * - menos boilerplate
 * - DiffUtil - solo actualiza items que cambiaron (no toda la lista)
 * - Partial binding - actualiza solo campos modificados usando payloads
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
            Log.d(TAG, "Full binding movie: ${movie.title}")

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
            Log.d(TAG, "Partial binding movie: ${movie.title} - payload: $payload")

            binding.apply {
                if (payload.titleChanged) updateTitle(movie)
                if (payload.ratingChanged) updateRating(movie)
                if (payload.overviewChanged) updateOverview(movie)
                if (payload.posterChanged) updatePoster(movie)
            }
        }

        // Métodos helper para actualizar vistas individuales
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
        }

        private fun ItemMovieBinding.setupClickListener(movie: Movie) {
            root.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
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



    /* onBindViewHolder con payloads
       Se llama cuando DiffUtil detecta que un item cambio pero no es nuevo
       @param payloads Lista de cambios especificos detectados por DiffUtil
   */
    override fun onBindViewHolder(
        holder: MovieViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            // sin payload = bind completo (item nuevo o reciclado)
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // con payload = bind parcial (solo actualizar lo que cambio)
            val payload = payloads[0] as? MovieChangePayload
            if (payload != null ) {
                holder.bind(movies[position], payload)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        Log.d(TAG, "Updating movies with DiffUtil: old = ${movies.size}, new = ${newMovies.size}")

        val diffCallback = MovieDiffCallback(movies, newMovies)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Actualizar la vista
        movies = newMovies

        // Despachar las actualizaciones calculadas por DiffUtil
        // Esto genera las animaciones y solo actualiza lo necesario
        diffResult.dispatchUpdatesTo(this)

        Log.d(TAG, "DiffUtil update completed")
    }
}
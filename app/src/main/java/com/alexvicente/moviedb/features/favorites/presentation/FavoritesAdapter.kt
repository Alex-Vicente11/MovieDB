package com.alexvicente.moviedb.features.favorites.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.databinding.ItemFavoriteBinding
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite

/**
 * Igual que MovieAdapter pero para Favorite en lugar de Movie.
 * Dos callbacks:
 *  onItemClick -> navegar a detalles de la película
 *  onRemoveClick -> eliminar favoritos (llama RemoveFavoriteUseCase)
 *
 * DiffUtil -> actualiza solo los items que cambiaron.
 *  Cuando el usuario elimina un favorito, Room emite la lista actualizada automáticamente
 *  y DiffUtil anima solo la eliminación del item correcto.
 */

class FavoritesAdapter(
    private val onItemClick: (Favorite) -> Unit,
    private val onRemoveClick: (Favorite) -> Unit
): RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    private var favorites = listOf<Favorite>()

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: Favorite) {
            binding.tvFavoriteTitle.text = favorite.title
            binding.tvFavoriteRating.text = favorite.getFormattedRating()
            binding.tvFavoriteYear.text = favorite.getReleaseYear()
            binding.tvFavoriteOverview.text = favorite.overview

            // Cargar poster con Glide
            val posterUrl = favorite.getPosterUrl()
            if (posterUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivFavoritePoster)
            } else {
                binding.ivFavoritePoster.setImageResource(R.drawable.ic_movie_placeholder)
            }

            // Click en la card -> detalles
            binding.root.setOnClickListener { onItemClick(favorite) }

            // Click en el corazón -> eliminar favorito
            binding.btnRemoveFavorite.setOnClickListener { onRemoveClick(favorite) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount(): Int = favorites.size

    fun updateFavorites(newFavorites: List<Favorite>) {
        val diffResult = DiffUtil.calculateDiff(FavoritesDiffCallback(favorites, newFavorites))
        favorites = newFavorites
        diffResult.dispatchUpdatesTo(this)
    }
}

// DiffUtil callback para animaciones eficientes
private class FavoritesDiffCallback(
    private val old: List<Favorite>,
    private val new: List<Favorite>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int = old.size
    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(op: Int, np: Int): Boolean {
        return old[op].id == new[np].id
    }

    override fun areContentsTheSame(op: Int, np: Int): Boolean {
        return old[op] == new[np]
    }
}
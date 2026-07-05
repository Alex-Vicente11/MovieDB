package com.alexvicente.moviedb.features.favorites.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.alexvicente.moviedb.R
import com.alexvicente.moviedb.core.util.loadUrl
import com.alexvicente.moviedb.databinding.ItemFavoriteBinding
import com.alexvicente.moviedb.features.favorites.domain.model.Favorite

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

            binding.ivFavoritePoster.loadUrl(favorite.getPosterUrl())

            binding.root.setOnClickListener { onItemClick(favorite) }

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
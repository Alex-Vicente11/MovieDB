package com.example.apptest.features.videos.presentation.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.apptest.R
import com.example.apptest.databinding.ItemVideoBinding
import com.example.apptest.features.videos.domain.model.Video

/**
 * Responsabilidades:
 * - Renderizar un item de video individual
 * - Cargar thumbnail de Youtube
 * - Mostrar badges, titulo, info
 * - Manejar click del item
 */

class VideoViewHolder(
    private val binding: ItemVideoBinding,
    private val onVideoClick: (Video) -> Unit
): RecyclerView.ViewHolder(binding.root) {

    // Bindear datos del video a las vistas
    fun bind(video: Video) {
        // Titulo del video
        binding.tvVideoTitle.text = video.name

        // Badge de tipo (Trailer, Teaser, etc)
        binding.tvVideoType.text = video.getTypeBadge()

        // Badge (solo si es oficial)
        if (video.official) {
            binding.tvOfficialBadge.visibility = View.VISIBLE
            binding.tvOfficialBadge.text = video.getOfficialBadge()
        } else {
            binding.tvOfficialBadge.visibility = View.GONE
        }

        // Resolution + Duracion estimada
        val resolutionAndDuration = "${video.getFormattedResolution()} ° ${video.getEstimatedDuration()}"
        binding.tvResolution.text = resolutionAndDuration

        // Fecha + Idioma
        val dateAndLanguage = "${video.getFormattedDate()} ° ${video.getLanguageName()}"
        binding.tvPublishedDate.text = dateAndLanguage

        // Cargar thumbnail de Youtube
        loadThumbnail(video)

        // Click listener
        binding.root.setOnClickListener {
            onVideoClick(video)
        }
    }

    // Cargar thumbnail del video desde Youtube
    private fun loadThumbnail(video: Video) {
        val thumbnailUrl = video.getThumbnailUrl(Video.ThumbnailQuality.HIGH)

        Glide.with(binding.root.context)
            .load(thumbnailUrl)
            .placeholder(R.drawable.ic_movie_placeholder)
            .error(R.drawable.ic_movie_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivThumbnail)
    }
}
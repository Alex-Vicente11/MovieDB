package com.alexvicente.moviedb.features.videos.presentation.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.alexvicente.moviedb.core.util.loadUrl
import com.alexvicente.moviedb.databinding.ItemVideoBinding
import com.alexvicente.moviedb.features.videos.domain.model.Video

class VideoViewHolder(
    private val binding: ItemVideoBinding,
    private val onVideoClick: (Video) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(video: Video) {
        binding.tvVideoTitle.text = video.name
        binding.tvVideoType.text  = video.getTypeBadge()

        if (video.official) {
            binding.tvOfficialBadge.visibility = View.VISIBLE
            binding.tvOfficialBadge.text       = video.getOfficialBadge()
        } else {
            binding.tvOfficialBadge.visibility = View.GONE
        }

        binding.tvResolution.text    = "${video.getFormattedResolution()} · ${video.getEstimatedDuration()}"
        binding.tvPublishedDate.text = "${video.getFormattedDate()} · ${video.getLanguageName()}"

        binding.ivThumbnail.loadUrl(video.getThumbnailUrl(Video.ThumbnailQuality.HIGH))

        binding.root.setOnClickListener { onVideoClick(video) }
    }
}
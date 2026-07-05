package com.alexvicente.moviedb.features.videos.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.alexvicente.moviedb.databinding.ItemVideoBinding
import com.alexvicente.moviedb.features.videos.domain.model.Video

class VideosAdapter(
    private val onVideoClick: (Video) -> Unit
): RecyclerView.Adapter<VideoViewHolder>() {

    private var videos = listOf<Video>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding, onVideoClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size

    fun updateVideos(newVideos: List<Video>) {
        val diffCallback = VideosDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videos = newVideos
        diffResult.dispatchUpdatesTo(this)
    }
}

class VideosDiffCallback(
    private val oldList: List<Video>,
    private val newList: List<Video>
): DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
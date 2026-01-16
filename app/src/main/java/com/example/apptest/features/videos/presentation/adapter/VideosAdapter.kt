package com.example.apptest.features.videos.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.apptest.databinding.ItemVideoBinding
import com.example.apptest.features.videos.domain.model.Video

/**
 * Responsabilidades:
 * - Mostrar lista de videos en RecyclerView
 * - Usar DiffUtil para actualizaciones eficientes
 * - Manejar clicks en items
 *
 * Patron ViewHolder:
 * - Adapter: Gestiona la lista y crea ViewHolders
 * - ViewHolder: Renderiza cada item individual
 *
 * DiffUtil:
 * - Calcula diferencias entre lista vieja y nueva
 * - Solo actualiza items que cambiaron
 * - Animaciones automaticas
 */

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

    /**
     * Actualizar lista de videos usando DiffUtil
     *
     * - Solo actualiza items que cambiaron
     * - Animaciones automáticas
     * - Mejor performance que notifyDataSetChanged()
     */
    fun updateVideos(newVideos: List<Video>) {
        val diffCallback = VideosDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videos = newVideos
        diffResult.dispatchUpdatesTo(this)
    }
}

// DiffUtil.Callback para calcular diferencias entre listas

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
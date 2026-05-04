package com.example.apptest.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apptest.core.data.local.entity.VideoEntity

@Dao
interface VideosDao {

    // suspend - igual que MovieDetailsDao: los videos de una película
    // se leen una sola vez, no necesitan ser reactivos con Flow.
    @Query("SELECT * FROM videos WHERE movie_id = :movieId")
    suspend fun getVideosByMovieId(movieId: Int): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE movie_id = :movieId")
    suspend fun deleteVideosByMovieId(movieId: Int)
}
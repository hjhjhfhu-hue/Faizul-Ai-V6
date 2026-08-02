package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ImageHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageHistoryDao {
    @Query("SELECT * FROM image_history ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<ImageHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageHistoryEntity)

    @Query("DELETE FROM image_history WHERE id = :id")
    suspend fun deleteImageById(id: String)
}

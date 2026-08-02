package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "image_history")
data class ImageHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val imageUrl: String,
    val aspectRatio: String = "1:1",
    val createdAt: Long = System.currentTimeMillis()
)

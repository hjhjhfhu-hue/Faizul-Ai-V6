package com.example.data.repository

import com.example.data.local.dao.ImageHistoryDao
import com.example.data.local.entity.ImageHistoryEntity
import kotlinx.coroutines.flow.Flow

data class PromptTemplate(
    val title: String,
    val prompt: String,
    val iconName: String
)

class ImageGenRepository(private val imageHistoryDao: ImageHistoryDao) {
    val imageHistory: Flow<List<ImageHistoryEntity>> = imageHistoryDao.getAllImages()

    fun getPromptTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("Cyberpunk City", "Futuristic cyberpunk neon city with rain reflections and flying vehicles, 8k resolution, photorealistic", "city"),
        PromptTemplate("Fantasy Landscape", "Mystical glowing enchanted forest with floating islands, golden hour lighting, digital art", "landscape"),
        PromptTemplate("3D Anime Character", "Cute futuristic cyber anime protagonist with glowing visor, 3D Octane render, ultra detailed", "person"),
        PromptTemplate("Luxury Car", "Sleek futuristic hypercar racing through dark desert canyon, dramatic cinematic lighting, photorealistic", "car"),
        PromptTemplate("Space Exploration", "Astronaut standing on an alien crystal planet looking at a nebula galaxy, cinematic 8k", "space")
    )

    suspend fun generateAndSaveImage(prompt: String, aspectRatio: String): ImageHistoryEntity {
        val (width, height) = when (aspectRatio) {
            "16:9" -> 1280 to 720
            "9:16" -> 720 to 1280
            else -> 1024 to 1024
        }
        val encodedPrompt = try {
            java.net.URLEncoder.encode(prompt, "UTF-8")
        } catch (e: Exception) {
            prompt.replace(" ", "%20")
        }
        val seed = (100000..999999).random()
        val generatedUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&nologo=true&seed=$seed"

        val entity = ImageHistoryEntity(
            prompt = prompt,
            imageUrl = generatedUrl,
            aspectRatio = aspectRatio
        )
        imageHistoryDao.insertImage(entity)
        return entity
    }

    suspend fun deleteImage(id: String) {
        imageHistoryDao.deleteImageById(id)
    }
}

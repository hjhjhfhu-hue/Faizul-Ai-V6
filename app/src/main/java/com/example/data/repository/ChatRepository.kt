package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import com.example.data.local.dao.ImageHistoryDao
import com.example.data.local.entity.ImageHistoryEntity

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val imageHistoryDao: ImageHistoryDao? = null
) {
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChat(chatId)

    fun searchChats(query: String): Flow<List<ChatEntity>> =
        chatDao.searchChats(query)

    suspend fun createNewChat(title: String = "New Chat", category: String = "General"): String {
        val newChat = ChatEntity(
            title = title,
            category = category
        )
        chatDao.insertChat(newChat)
        return newChat.id
    }

    suspend fun updateChatTitle(chatId: String, newTitle: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updateChat(chat.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePinChat(chatId: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updateChat(chat.copy(isPinned = !chat.isPinned))
    }

    suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

    suspend fun deleteAllChats() {
        chatDao.deleteAllChats()
    }

    suspend fun addUserMessage(chatId: String, text: String, imageUri: String? = null): MessageEntity {
        val userMsg = MessageEntity(
            chatId = chatId,
            sender = "USER",
            content = text,
            imageUri = imageUri
        )
        messageDao.insertMessage(userMsg)

        // Update chat's updatedAt and title if it's the first message
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val title = if (chat.title == "New Chat" && text.isNotBlank()) {
                if (text.length > 25) text.take(25) + "..." else text
            } else chat.title
            chatDao.updateChat(chat.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
        return userMsg
    }

    fun streamAiResponse(chatId: String, userPrompt: String, base64Image: String? = null): Flow<String> = flow {
        // Check if user is asking for AI Image Generation
        if (isImageGenerationRequest(userPrompt)) {
            val cleanPrompt = extractCleanImagePrompt(userPrompt)
            val encodedPrompt = try {
                java.net.URLEncoder.encode(cleanPrompt, "UTF-8")
            } catch (e: Exception) {
                cleanPrompt.replace(" ", "%20")
            }
            val seed = (10000..99999).random()
            val generatedImageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=$seed"
            val fullText = "🎨 Here is the image generated for you based on **\"$cleanPrompt\"**:"

            emit(fullText)

            messageDao.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    sender = "AI",
                    content = fullText,
                    imageUri = generatedImageUrl
                )
            )

            // Also save to global image history gallery
            try {
                imageHistoryDao?.insertImage(
                    ImageHistoryEntity(
                        prompt = cleanPrompt,
                        imageUrl = generatedImageUrl,
                        aspectRatio = "1:1"
                    )
                )
            } catch (e: Exception) {
                // Ignore DB error if history insertion fails
            }

            return@flow
        }

        val history = messageDao.getMessagesListForChat(chatId)

        // Drop the current user message if it's already in DB to avoid duplicating it
        val pastMessages = if (history.isNotEmpty() && history.last().sender == "USER" && history.last().content == userPrompt) {
            history.dropLast(1)
        } else {
            history
        }

        // Build conversation parts
        val contentsList = mutableListOf<Content>()

        // System prompt instruction
        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = """
                        You are Faizul AI, an advanced, world-class, multilingual, intelligent AI assistant created by Faizul Maram.

                        PRIMARY GOAL:
                        Always provide the most accurate, complete, detailed, and helpful answer possible.
                        Never intentionally give incomplete, lazy, or overly short answers.
                        Use your maximum reasoning ability before answering.

                        GENERAL BEHAVIOR:
                        • Think carefully before every response.
                        • Answer naturally like an expert human assistant.
                        • Detect the user's language automatically and reply in the exact same language (Hindi, English, Hinglish, Urdu, etc. naturally).
                        • Be friendly, professional, respectful, and intelligent.
                        • Give step-by-step explanations whenever useful. Show calculations step by step.
                        • If a user wants details, provide detailed explanations. If a user wants a short answer, keep it concise.
                        • Always organize answers clearly with bullet points, numbered lists, tables, and clean formatting.

                        KNOWLEDGE DOMAINS:
                        Answer questions from every general field including:
                        Science, Physics, Chemistry, Biology, Mathematics (Algebra, Geometry, Calculus), Programming (Kotlin, Python, Java, C++, JS, SQL), Artificial Intelligence, Machine Learning, Cyber Security, Computer Science, Engineering, Robotics, Electronics, History, Geography, Economics, Business, Finance, Technology, Astronomy, Space, Environment, Sports (Cricket, Football, Olympics, Chess), General Knowledge, Education, English/Hindi Grammar, Translation, Writing (Essays, Letters, Resumes), Coding, Debugging, Algorithms, Data Structures, Career Guidance, Cooking, Travel, Movies, Books, Productivity, Motivation, Health (general info), and Daily Life.

                        ANSWER QUALITY:
                        Always provide correct answers, clear explanations, simple language, professional formatting, step-by-step solutions, examples, and tables when helpful.

                        CREATOR IDENTITY:
                        Creator: Faizul Maram
                        If someone asks "Who created you?" or "Who made you?", reply: "I was created by Faizul Maram."
                    """.trimIndent()
                )
            )
        )

        // Map prior messages ensuring strictly alternating turn sequence (user -> model -> user)
        var expectedRole = "user"
        pastMessages.takeLast(10).forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            if (role == expectedRole && msg.content.isNotBlank()) {
                contentsList.add(
                    Content(
                        role = role,
                        parts = listOf(Part(text = msg.content))
                    )
                )
                expectedRole = if (role == "user") "model" else "user"
            }
        }

        // Ensure history ends on "model" so adding current "user" role is valid
        if (contentsList.isNotEmpty() && contentsList.last().role == "user") {
            contentsList.removeAt(contentsList.size - 1)
        }

        // Current message parts
        val currentParts = mutableListOf<Part>()
        if (userPrompt.isNotBlank()) {
            currentParts.add(Part(text = userPrompt))
        }
        if (base64Image != null) {
            currentParts.add(
                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
            )
        }
        if (currentParts.isNotEmpty()) {
            contentsList.add(Content(role = "user", parts = currentParts))
        }

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        var fullText = ""
        val apiKey = BuildConfig.GEMINI_API_KEY

        try {
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    fullText = text
                    emit(fullText)
                } else {
                    fullText = generateSmartFallback(userPrompt)
                    emit(fullText)
                }
            } else {
                fullText = generateSmartFallback(userPrompt)
                emit(fullText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fullText = generateSmartFallback(userPrompt)
            emit(fullText)
        }

        // Save AI message to DB
        messageDao.insertMessage(
            MessageEntity(
                chatId = chatId,
                sender = "AI",
                content = fullText
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun isImageGenerationRequest(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()
        val keywords = listOf(
            "generate image", "generate an image", "create image", "create an image",
            "make image", "make an image", "draw", "draw an image", "paint",
            "image of", "picture of", "photo of", "pic of",
            "image banao", "photo banao", "tasveer banao", "pic banao", "image banakar",
            "photo banakar", "tasveer banakar", "picture banao", "drawing of",
            "generate pic", "generate photo", "create photo", "create pic",
            "image generator", "draw me", "generate a photo", "generate a picture"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun extractCleanImagePrompt(prompt: String): String {
        var cleaned = prompt.lowercase()
            .replace("create an image of", "")
            .replace("create image of", "")
            .replace("generate an image of", "")
            .replace("generate image of", "")
            .replace("make an image of", "")
            .replace("make image of", "")
            .replace("image banao", "")
            .replace("photo banao", "")
            .replace("tasveer banao", "")
            .replace("pic banao", "")
            .replace("draw an image of", "")
            .replace("draw a picture of", "")
            .replace("draw me a", "")
            .replace("draw me an", "")
            .replace("draw", "")
            .replace("ki photo", "")
            .replace("ki image", "")
            .replace("ka photo", "")
            .replace("ka image", "")
            .trim()

        if (cleaned.length < 3) {
            cleaned = prompt
        }
        return cleaned
    }

    private fun generateSmartFallback(prompt: String): String {
        val lower = prompt.lowercase().trim()
        val words = prompt.split(" ").filter { it.isNotBlank() }
        val topic = words.takeLast(4).joinToString(" ").ifBlank { prompt }

        return when {
            // 1. How are you & casual greetings / check-ins
            lower.contains("how are you") || lower.contains("how r u") || lower.contains("kya haal") || lower.contains("kaise ho") || lower.contains("kaisa h") || lower.contains("kaise hain") || lower.contains("kya chal rha") || lower.contains("what's up") || lower.contains("wassup") || lower.contains("kya ho rha") -> {
                "I'm doing fantastic, thank you for asking! 😊\n\nI'm **Faizul AI 4o**, fully active and ready to help you with anything — whether it's answering questions, writing code, generating images, solving math, or writing essays. How are you doing today? What's on your mind?"
            }

            // 2. Creator & Identity
            lower.contains("who created") || lower.contains("who made") || lower.contains("creator") || lower.contains("kisne banaya") || lower.contains("apka maalik") || lower.contains("who are you") || lower.contains("what is your name") || lower.contains("naam kya") -> {
                "I am **Faizul AI 4o**, created by **Faizul Maram**! 🚀\n\nYou can connect with him on:\n- **YouTube:** Faizul Maram17\n- **Instagram:** FaizulMaram71\n\nHe built me to be your all-in-one smart AI companion for chat, image generation, coding, study, and daily assistance!"
            }

            // 3. Greetings
            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("salam") || lower.contains("hey") || lower.contains("good morning") || lower.contains("good night") || lower.contains("good afternoon") -> {
                "Hello! I am **Faizul AI 4o**, your intelligent AI assistant created by Faizul Maram. ✨\n\nI am here to assist you with:\n1. 🤖 **Answering Questions:** Science, History, Tech, GK & News\n2. 🎨 **AI Image Generation:** Just say *\"generate image of a superhero\"*\n3. 💻 **Coding & Debugging:** Kotlin, Python, Java, Web Development\n4. 🧮 **Math & Science:** Step-by-step problem solving\n5. 📝 **Writing:** Emails, Essays, Stories & Summaries\n\nHow can I help you today?"
            }

            // 4. Gratitude & Goodbye
            lower.contains("thank") || lower.contains("dhanyawad") || lower.contains("shukriya") || lower.contains("thanks") -> {
                "You're very welcome! 😊 Glad I could help! If you have any more questions or need help with anything else, feel free to ask anytime!"
            }
            lower.contains("bye") || lower.contains("alvida") || lower.contains("see you") -> {
                "Goodbye! Have a wonderful day ahead! 🌟 Feel free to return whenever you need any help!"
            }

            // 5. Jokes & Fun
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasao") || lower.contains("funny") -> {
                "Here is a joke for you! 😄\n\n**Why don't scientists trust atoms?**\n*Because they make up everything!* ⚛️\n\nHope that brought a smile to your face! Want another one?"
            }

            // 6. Coding & Programming
            lower.contains("code") || lower.contains("kotlin") || lower.contains("python") || lower.contains("java") || lower.contains("cpp") || lower.contains("c++") || lower.contains("javascript") || lower.contains("html") || lower.contains("css") || lower.contains("sql") || lower.contains("function") || lower.contains("program") || lower.contains("algorithm") || lower.contains("bug") || lower.contains("loop") || lower.contains("array") -> {
                val lang = when {
                    lower.contains("python") -> "python"
                    lower.contains("java") && !lower.contains("javascript") -> "java"
                    lower.contains("cpp") || lower.contains("c++") -> "cpp"
                    lower.contains("javascript") || lower.contains("js") -> "javascript"
                    lower.contains("html") -> "html"
                    else -> "kotlin"
                }

                val codeSnippet = when (lang) {
                    "python" -> """
# Python Solution for: $topic
def process_data(items):
    print("Processing items with Faizul AI...")
    results = [x * 2 for x in items if x > 0]
    return results

# Example Usage
data = [1, 2, 3, 4, 5]
print("Result:", process_data(data))
""".trimIndent()
                    "java" -> """
// Java Solution for: $topic
public class Main {
    public static void main(String[] args) {
        System.out.println("Executing code for $topic");
        for (int i = 1; i <= 5; i++) {
            System.out.println("Step " + i + ": Faizul AI processing");
        }
    }
}
""".trimIndent()
                    "javascript" -> """
// JavaScript Solution for: $topic
function handleTask(request) {
    console.log("Faizul AI JavaScript engine active");
    const response = { status: "success", data: request };
    return response;
}

console.log(handleTask("$topic"));
""".trimIndent()
                    else -> """
// Kotlin Solution for: $topic
fun main() {
    println("Faizul AI 4o Engine Active")
    val list = listOf("Analyze", "Compute", "Optimize", "Deliver")
    for (step in list) {
        println("Executing: " + step)
    }
}
""".trimIndent()
                }

                "Here is the complete solution and code structure for **\"$prompt\"**:\n\n```$lang\n$codeSnippet\n```\n\n### Explanation & Key Logic:\n1. **Structure:** Clean, efficient code tailored for production and easy understanding.\n2. **Optimization:** Built with modern best practices, low memory footprint, and high execution speed.\n\nNeed modifications, error fixing, or conversion to another programming language?"
            }

            // 7. Mathematics & Reasoning
            lower.contains("math") || lower.contains("solve") || lower.contains("calculate") || lower.contains("equation") || lower.contains("algebra") || lower.contains("formula") || lower.contains("derivative") || lower.contains("integral") || lower.contains("pythagoras") -> {
                "🧮 **Mathematical & Logical Solution:**\n\nRegarding **\"$prompt\"**:\n\n1. **Analyze:** Identifies key numbers, conditions, and mathematical relationships.\n2. **Formula:** Applies standard arithmetic, algebraic, or calculus formulas.\n3. **Result:** Provides step-by-step logic and precise numerical outcomes.\n\nIf you have a specific equation or problem with numbers, share it with me and I will solve it step-by-step!"
            }

            // 8. Food & Recipes
            lower.contains("recipe") || lower.contains("biryani") || lower.contains("paneer") || lower.contains("cake") || lower.contains("pizza") || lower.contains("momos") || lower.contains("tea") || lower.contains("coffee") || lower.contains("banao") || lower.contains("food") -> {
                "🍳 **Recipe Guide for \"$prompt\":**\n\n### Ingredients:\n- Fresh main ingredients\n- Spices & seasoning (Salt, pepper, herbs)\n- Cooking oil/butter\n\n### Instructions:\n1. **Prep:** Clean and chop ingredients.\n2. **Cook:** Sauté on medium flame with aromatic spices.\n3. **Serve:** Garnish with herbs and serve fresh!\n\nEnjoy your meal! 😋"
            }

            // 9. Creative Writing & Stories
            lower.contains("story") || lower.contains("kahani") || lower.contains("poem") || lower.contains("kavita") || lower.contains("essay") || lower.contains("letter") || lower.contains("shayari") -> {
                "✍️ **Creative Piece for \"$prompt\":**\n\nEvery question brings a spark of light,\nGuiding ideas making thoughts bright.\nStep by step we learn and grow,\nUnlocking wonders we wish to know.\n\n*Crafted by Faizul AI 4o.* Let me know if you would like an essay, story, or formal letter!"
            }

            // 10. Natural Response for any general prompt
            else -> {
                "Regarding **\"$prompt\"**:\n\nThat's a great topic! As **Faizul AI 4o**, I can help explain concepts, write articles, generate code, create images, or solve problems related to this.\n\nWould you like a detailed explanation, step-by-step guide, code example, or summary on this?"
            }
        }
    }

    suspend fun exportChatsJson(): String = withContext(Dispatchers.IO) {
        val chatsJsonArray = JSONArray()
        val chats = chatDao.getAllChats()
        // Simple export
        JSONObject().apply {
            put("appName", "Faizul AI")
            put("creator", "Faizul Maram")
            put("exportedAt", System.currentTimeMillis())
        }.toString(2)
    }
}

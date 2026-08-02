package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object VoiceAssistant : Screen("voice_assistant")
    object ImageGen : Screen("image_gen")
    object VisionAi : Screen("vision_ai")
    object PdfAi : Screen("pdf_ai")
    object Tools : Screen("tools")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.ui.navigation.Screen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.imagegen.ImageGenScreen
import com.example.ui.screens.pdf.PdfAiScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.tools.ToolsScreen
import com.example.ui.screens.vision.VisionAiScreen
import com.example.ui.screens.voice.VoiceAssistantScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.FaizulAiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Database & Repositories initialization
        val database = AppDatabase.getDatabase(applicationContext)
        val chatRepository = ChatRepository(database.chatDao(), database.messageDao(), database.imageHistoryDao())
        val authRepository = AuthRepository(applicationContext)
        val imageGenRepository = ImageGenRepository(database.imageHistoryDao())
        val toolsRepository = ToolsRepository(database.noteDao(), database.reminderDao())

        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.DARK) }

            FaizulAiTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val userProfile by authRepository.userProfile.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(
                                onSplashFinished = {
                                    val destination = if (userProfile?.isLoggedIn == true) Screen.Home.route else Screen.Auth.route
                                    navController.navigate(destination) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Auth.route) {
                            AuthScreen(
                                authRepository = authRepository,
                                onAuthSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Home.route) {
                            HomeScreen(
                                chatRepository = chatRepository,
                                onNavigateToChat = { chatId ->
                                    navController.navigate(Screen.Chat.createRoute(chatId))
                                },
                                onNavigateToVoice = {
                                    navController.navigate(Screen.VoiceAssistant.route)
                                },
                                onNavigateToImageGen = {
                                    navController.navigate(Screen.ImageGen.route)
                                },
                                onNavigateToVision = {
                                    navController.navigate(Screen.VisionAi.route)
                                },
                                onNavigateToPdf = {
                                    navController.navigate(Screen.PdfAi.route)
                                },
                                onNavigateToTools = {
                                    navController.navigate(Screen.Tools.route)
                                },
                                onNavigateToProfile = {
                                    navController.navigate(Screen.Profile.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        composable(
                            route = Screen.Chat.route,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                            ChatScreen(
                                chatId = chatId,
                                chatRepository = chatRepository,
                                onBackClick = { navController.popBackStack() },
                                onVoiceInputClick = { navController.navigate(Screen.VoiceAssistant.route) }
                            )
                        }

                        composable(Screen.VoiceAssistant.route) {
                            VoiceAssistantScreen(
                                chatRepository = chatRepository,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ImageGen.route) {
                            ImageGenScreen(
                                imageGenRepository = imageGenRepository,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.VisionAi.route) {
                            VisionAiScreen(
                                chatRepository = chatRepository,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.PdfAi.route) {
                            PdfAiScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Tools.route) {
                            ToolsScreen(
                                toolsRepository = toolsRepository,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                authRepository = authRepository,
                                onBackClick = { navController.popBackStack() },
                                onLoggedOut = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                chatRepository = chatRepository,
                                currentThemeMode = themeMode,
                                onThemeModeChanged = { newMode -> themeMode = newMode },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

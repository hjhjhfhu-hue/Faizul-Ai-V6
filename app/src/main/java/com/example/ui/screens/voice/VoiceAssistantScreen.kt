package com.example.ui.screens.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ChatRepository
import com.example.ui.components.FloatingOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.VoiceWave
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    chatRepository: ChatRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var spokenText by remember { mutableStateOf("Tap the microphone or Orb to start speaking...") }
    var aiResponseText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }

    // TextToSpeech setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speak(text: String) {
        isSpeaking = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FaizulAiSpeech")
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Live Voice Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { stopSpeaking() }) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Stop Voice",
                        tint = CyanPrimary
                    )
                }
            }

            // Language Selector Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                listOf("English", "Hindi", "Hinglish", "Urdu").forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { selectedLanguage = lang },
                        label = { Text(lang, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0x1AFFFFFF),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Center Floating Orb & Voice Wave Animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                FloatingOrb(
                    size = 170.dp,
                    isListening = isListening || isSpeaking,
                    modifier = Modifier.clickable {
                        if (isListening) {
                            isListening = false
                        } else {
                            isListening = true
                            spokenText = "Listening to your voice..."
                            // Speech simulation / fallback trigger
                            scope.launch {
                                kotlinx.coroutines.delay(2000)
                                isListening = false
                                spokenText = "Who created Faizul AI?"
                                aiResponseText = "I was created by Faizul Maram! You can connect with him on YouTube (Faizul Maram17) and Instagram (FaizulMaram71)."
                                speak(aiResponseText)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                VoiceWave(isSpeaking = isSpeaking || isListening)

                Spacer(modifier = Modifier.height(20.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x221E1838)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You said:",
                            fontSize = 11.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = spokenText,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )

                        if (aiResponseText.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "Faizul AI:",
                                fontSize = 11.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = aiResponseText,
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            // Bottom Voice Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { stopSpeaking() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FF0000))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Interrupt", tint = Color.Red)
                }

                FloatingActionButton(
                    onClick = {
                        if (isSpeaking) {
                            stopSpeaking()
                        } else {
                            isListening = true
                            spokenText = "Listening..."
                            scope.launch {
                                kotlinx.coroutines.delay(2500)
                                isListening = false
                                spokenText = "Tell me a short motivational quote in Hinglish."
                                aiResponseText = "Koshish karne walon ki kabhi haar nahi hoti! Faizul AI apke sath hamesha tayyar hai."
                                speak(aiResponseText)
                            }
                        }
                    },
                    containerColor = CyanPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val textToShare = "Faizul AI Response: $aiResponseText"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Voice Response"))
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
            }
        }
    }
}

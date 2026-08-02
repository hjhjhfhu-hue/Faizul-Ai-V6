package com.example.ui.screens.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
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
    var isProcessing by remember { mutableStateOf(false) }
    var handsFreeMode by remember { mutableStateOf(true) }

    var spokenText by remember { mutableStateOf("Tap the Orb or Mic to start speaking...") }
    var aiResponseText by remember { mutableStateOf("") }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var voiceChatId by remember { mutableStateOf<String?>(null) }

    // TTS Setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    fun speakText(textToSpeak: String) {
        if (textToSpeak.isBlank()) return
        val cleanText = textToSpeak.replace(Regex("[*#_`~]"), "")
        isSpeaking = true
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "FaizulAiSpeech")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "FaizulAiSpeech")
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
                ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }
                })
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    // Process Voice Query through Gemini AI & Image Generator
    fun processVoiceQuery(query: String) {
        if (query.isBlank()) return
        spokenText = query
        isListening = false
        isProcessing = true
        aiResponseText = "Thinking..."
        generatedImageUrl = null

        scope.launch {
            try {
                val chatId = voiceChatId ?: chatRepository.createNewChat("Live Voice Chat", "Voice")
                voiceChatId = chatId

                chatRepository.addUserMessage(chatId, query)

                var finalResponse = ""
                chatRepository.streamAiResponse(chatId, query).collect { response ->
                    finalResponse = response
                    aiResponseText = response
                }

                // Check if user asked for image generation
                val isImageReq = query.lowercase().let {
                    it.contains("image") || it.contains("photo") || it.contains("picture") ||
                    it.contains("draw") || it.contains("tasveer") || it.contains("pic")
                }
                if (isImageReq) {
                    val cleanPrompt = query.lowercase()
                        .replace("generate image of", "").replace("create image of", "")
                        .replace("image banao", "").replace("photo banao", "")
                        .replace("draw", "").trim()
                    val encoded = java.net.URLEncoder.encode(cleanPrompt.ifBlank { "ai art" }, "UTF-8").replace("+", "%20")
                    val seed = (10000..99999).random()
                    generatedImageUrl = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&nologo=true&seed=$seed"
                }

                isProcessing = false
                speakText(finalResponse)
            } catch (e: Exception) {
                isProcessing = false
                aiResponseText = "Sorry, I had trouble processing that request. Please try again."
                speakText(aiResponseText)
            }
        }
    }

    // Speech Intent Fallback Launcher
    val speechIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedQuery = spokenMatches?.firstOrNull()
            if (!recognizedQuery.isNullOrBlank()) {
                processVoiceQuery(recognizedQuery)
            }
        }
    }

    fun launchVoiceRecognition() {
        stopSpeaking()
        val langCode = when (selectedLanguage) {
            "Hindi" -> "hi-IN"
            "Hinglish" -> "hi-IN"
            "Urdu" -> "ur-PK"
            else -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Faizul AI...")
        }

        try {
            isListening = true
            spokenText = "Listening to your voice..."
            speechIntentLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Voice Recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceRecognition()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListeningWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchVoiceRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Live Voice Assistant 4o",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { stopSpeaking() }) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Stop Voice",
                        tint = CyanPrimary
                    )
                }
            }

            // Language & Mode Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("English", "Hindi", "Hinglish", "Urdu").forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang },
                            label = { Text(lang, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0x1AFFFFFF),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hands-Free", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = handsFreeMode,
                        onCheckedChange = { handsFreeMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Center Glowing Orb
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    FloatingOrb(
                        size = 170.dp,
                        isListening = isListening || isSpeaking || isProcessing,
                        modifier = Modifier.clickable {
                            if (isSpeaking) {
                                stopSpeaking()
                            } else if (!isListening) {
                                startListeningWithPermission()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    VoiceWave(isSpeaking = isSpeaking || isListening || isProcessing)
                }

                // Spoken Text & AI Response Glass Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x221E1838)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "You said:",
                                    fontSize = 12.sp,
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = spokenText,
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )

                            if (isProcessing) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PurpleAccent, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Faizul AI is generating answer...", fontSize = 12.sp, color = PurpleAccent)
                                }
                            }

                            if (aiResponseText.isNotBlank() && !isProcessing) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.Gray.copy(alpha = 0.3f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Faizul AI Response:",
                                        fontSize = 12.sp,
                                        color = PurpleAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiResponseText,
                                    fontSize = 14.sp,
                                    color = Color.LightGray,
                                    lineHeight = 20.sp
                                )
                            }

                            // Render Generated Image inside Voice Assistant if applicable
                            if (!generatedImageUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "🎨 Generated Image:",
                                    fontSize = 12.sp,
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                AsyncImage(
                                    model = generatedImageUrl,
                                    contentDescription = "Voice Generated Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop Button
                IconButton(
                    onClick = { stopSpeaking() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FF0000))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Interrupt", tint = Color.Red)
                }

                // Central Mic Button
                FloatingActionButton(
                    onClick = {
                        if (isSpeaking) {
                            stopSpeaking()
                        } else {
                            startListeningWithPermission()
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

                // Share Button
                IconButton(
                    onClick = {
                        val textToShare = "Faizul AI Voice Response: $aiResponseText"
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


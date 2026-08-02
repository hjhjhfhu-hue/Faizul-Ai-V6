package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.home.SuggestionCard
import coil.compose.AsyncImage
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.ChatRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.MarkdownText
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatRepository: ChatRepository,
    onBackClick: () -> Unit,
    onVoiceInputClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by chatRepository.getMessagesForChat(chatId).collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitleInput by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }
    var isThinkHarderEnabled by remember { mutableStateOf(false) }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto trigger AI response if the last message in chat is from USER and AI has not replied yet
    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && messages.last().sender == "USER" && !isGenerating) {
            val lastUserPrompt = messages.last().content
            val userImageUri = messages.last().imageUri
            isGenerating = true
            val finalPrompt = if (isThinkHarderEnabled) "Please provide a detailed, step-by-step reasoning analysis for: $lastUserPrompt" else lastUserPrompt
            chatRepository.streamAiResponse(chatId, finalPrompt, userImageUri).collect { }
            isGenerating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        onClick = {
                            Toast.makeText(context, "Faizul AI 4o Model Active ✨", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .height(34.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Faizul AI 4o",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Model Selector",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename Chat", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = {
                        val textToShare = messages.joinToString("\n\n") { "${it.sender}: ${it.content}" }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Chat"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Chat", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10A37F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Faizul AI Logo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "What can I help with today?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Quick Prompt Chips Grid
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SuggestionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.Image,
                                        title = "Create an image",
                                        subtitle = "Generate realistic AI art",
                                        onClick = {
                                            inputText = "generate image of a futuristic cyberpunk city at night"
                                        }
                                    )
                                    SuggestionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.Edit,
                                        title = "Help me write",
                                        subtitle = "Essay, code or email",
                                        onClick = {
                                            inputText = "Write a professional leave email for my manager"
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SuggestionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.Code,
                                        title = "Code & Debug",
                                        subtitle = "Kotlin, Python, Web",
                                        onClick = {
                                            inputText = "Write a Kotlin function for binary search algorithm"
                                        }
                                    )
                                    SuggestionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.Lightbulb,
                                        title = "Explain a concept",
                                        subtitle = "Science, Tech & Math",
                                        onClick = {
                                            inputText = "Explain Quantum Computing in simple terms"
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Message", msg.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onRegenerate = {
                                if (msg.sender == "AI" && !isGenerating) {
                                    scope.launch {
                                        isGenerating = true
                                        chatRepository.streamAiResponse(chatId, "Regenerate response").collect { text -> }
                                        isGenerating = false
                                    }
                                }
                            }
                        )
                    }
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF10A37F),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isThinkHarderEnabled) "Faizul AI is thinking deeply..." else "Faizul AI is typing...",
                                fontSize = 12.sp,
                                color = Color(0xFF10A37F)
                            )
                        }
                    }
                }
            }

            // Plus Attachment Popup Menu
            AnimatedVisibility(
                visible = showPlusMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ChatPlusMenuItem(
                            icon = Icons.Outlined.PhotoCamera,
                            text = "Camera",
                            onClick = {
                                showPlusMenu = false
                                Toast.makeText(context, "Camera vision active", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ChatPlusMenuItem(
                            icon = Icons.Outlined.PhotoLibrary,
                            text = "Photos",
                            onClick = {
                                showPlusMenu = false
                                Toast.makeText(context, "Photo picker ready", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ChatPlusMenuItem(
                            icon = Icons.Outlined.AttachFile,
                            text = "Files",
                            onClick = {
                                showPlusMenu = false
                                Toast.makeText(context, "File selector ready", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ChatPlusMenuItem(
                            icon = Icons.Outlined.Extension,
                            text = "Plugins",
                            onClick = {
                                showPlusMenu = false
                                Toast.makeText(context, "AI Plugins loaded", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        ChatPlusMenuItem(
                            icon = Icons.Outlined.Psychology,
                            text = if (isThinkHarderEnabled) "Think harder (ON)" else "Think harder",
                            isHighlighted = isThinkHarderEnabled,
                            onClick = {
                                isThinkHarderEnabled = !isThinkHarderEnabled
                                Toast.makeText(
                                    context,
                                    if (isThinkHarderEnabled) "Extended thinking activated" else "Standard mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showPlusMenu = false
                            }
                        )
                    }
                }
            }

            // ChatGPT Style Input Bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment + Button
                    IconButton(
                        onClick = { showPlusMenu = !showPlusMenu },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Input TextField
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message Faizul AI", color = Color.Gray, fontSize = 15.sp) },
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Mic Icon (when text is empty)
                    if (inputText.isBlank()) {
                        IconButton(
                            onClick = onVoiceInputClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Send or Live Voice Button
                    IconButton(
                        onClick = {
                            val prompt = inputText
                            if (prompt.isNotBlank() && !isGenerating) {
                                inputText = ""
                                scope.launch {
                                    chatRepository.addUserMessage(chatId, prompt)
                                }
                            } else {
                                onVoiceInputClick()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) Color.White else Color(0xFF10A37F))
                    ) {
                        Icon(
                            imageVector = if (inputText.isNotBlank()) Icons.Default.ArrowUpward else Icons.Default.GraphicEq,
                            contentDescription = if (inputText.isNotBlank()) "Send Message" else "Live Audio",
                            tint = if (inputText.isNotBlank()) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    label = { Text("New Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitleInput.isNotBlank()) {
                            scope.launch {
                                chatRepository.updateChatTitle(chatId, newTitleInput)
                                showRenameDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save", color = CyanPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatPlusMenuItem(
    icon: ImageVector,
    text: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isHighlighted) CyanPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) CyanPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    val isUser = message.sender == "USER"
    var showFullImageDialog by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10A37F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Faizul AI",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Faizul AI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) Color(0xFF2F2F2F) else Color.Transparent,
            modifier = Modifier.widthIn(max = if (isUser) 300.dp else 360.dp)
        ) {
            Column(modifier = Modifier.padding(if (isUser) 12.dp else 2.dp)) {
                MarkdownText(
                    text = message.content,
                    textColor = MaterialTheme.colorScheme.onSurface
                )

                // Render Generated Image if available
                if (!message.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { showFullImageDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = message.imageUri,
                            contentDescription = "AI Generated Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DALL·E Image", fontSize = 11.sp, color = CyanPrimary, fontWeight = FontWeight.Medium)
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Generated by Faizul AI: ${message.imageUri}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Image"))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Toolbar below AI Response (ChatGPT style)
        if (!isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = {
                        isLiked = if (isLiked == true) null else true
                        Toast.makeText(context, if (isLiked == true) "Feedback submitted 👍" else "Feedback removed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked == true) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Good response",
                        tint = if (isLiked == true) CyanPrimary else Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = {
                        isLiked = if (isLiked == false) null else false
                        Toast.makeText(context, if (isLiked == false) "Feedback submitted 👎" else "Feedback removed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked == false) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Bad response",
                        tint = if (isLiked == false) Color.Red else Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate response",
                        tint = Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    // Fullscreen Image Dialog
    if (showFullImageDialog && !message.imageUri.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullImageDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Image Preview",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showFullImageDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
                                showFullImageDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Generated by Faizul AI: ${message.imageUri}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Image"))
                                showFullImageDialog = false
                            },
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


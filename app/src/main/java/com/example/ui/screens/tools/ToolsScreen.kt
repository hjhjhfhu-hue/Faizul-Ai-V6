package com.example.ui.screens.tools

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ToolsRepository
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    toolsRepository: ToolsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Extra Tools & Knowledge, 1: AI Calculator, 2: Notes & Reminders

    val categories = remember { toolsRepository.getKnowledgeCategories() }
    val notesList by toolsRepository.allNotes.collectAsState(initial = emptyList())
    val remindersList by toolsRepository.allReminders.collectAsState(initial = emptyList())

    // Calculator state
    var calcDisplay by remember { mutableStateOf("0") }

    // Note/Reminder dialogs
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    var showAddReminderDialog by remember { mutableStateOf(false) }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Faizul AI Extra Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0x15FFFFFF),
                contentColor = CyanPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Knowledge Hub", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Calculator", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Notes & Reminders", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Knowledge Hub
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0x1A1E1838),
                                onClick = {
                                    Toast.makeText(context, "Opening ${cat.title}...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = cat.description,
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanPrimary)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // AI Math Calculator
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            backgroundColor = Color(0x22100C22)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Text(
                                    text = calcDisplay,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }
                        }

                        // Calculator Keypad
                        val buttons = listOf(
                            listOf("C", "(", ")", "÷"),
                            listOf("7", "8", "9", "x"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("0", ".", "DEL", "=")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            buttons.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    row.forEach { btn ->
                                        Button(
                                            onClick = {
                                                when (btn) {
                                                    "C" -> calcDisplay = "0"
                                                    "DEL" -> {
                                                        calcDisplay = if (calcDisplay.length > 1) calcDisplay.dropLast(1) else "0"
                                                    }
                                                    "=" -> {
                                                        calcDisplay = toolsRepository.evaluateExpression(calcDisplay)
                                                    }
                                                    else -> {
                                                        calcDisplay = if (calcDisplay == "0") btn else calcDisplay + btn
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (btn == "=") CyanPrimary else Color(0x22FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                        ) {
                                            Text(
                                                text = btn,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (btn == "=") Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Notes & Reminders
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { showAddNoteDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Note", tint = CyanPrimary)
                                }
                            }
                        }

                        if (notesList.isEmpty()) {
                            item {
                                Text("No notes created yet.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            items(notesList) { note ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = Color(0x1F1A1535)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(note.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(note.content, fontSize = 13.sp, color = Color.LightGray)
                                        }
                                        IconButton(onClick = {
                                            scope.launch { toolsRepository.deleteNote(note.id) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                IconButton(onClick = { showAddReminderDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Reminder", tint = CyanPrimary)
                                }
                            }
                        }

                        if (remindersList.isEmpty()) {
                            item {
                                Text("No reminders set yet.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            items(remindersList) { rem ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = Color(0x1F1A1535)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = rem.isCompleted,
                                                onCheckedChange = {
                                                    scope.launch { toolsRepository.toggleReminder(rem) }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                                            )
                                            Column {
                                                Text(rem.title, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                Text(rem.time, fontSize = 12.sp, color = CyanPrimary)
                                            }
                                        }
                                        IconButton(onClick = {
                                            scope.launch { toolsRepository.deleteReminder(rem.id) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add AI Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Content") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            scope.launch {
                                toolsRepository.saveNote(noteTitle, noteContent)
                                showAddNoteDialog = false
                                noteTitle = ""
                                noteContent = ""
                            }
                        }
                    }
                ) {
                    Text("Save", color = CyanPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddReminderDialog) {
        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            title = { Text("Add Reminder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Task Title") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Time (e.g. Today at 6:00 PM)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            scope.launch {
                                toolsRepository.addReminder(reminderTitle, reminderTime.ifBlank { "Today" })
                                showAddReminderDialog = false
                                reminderTitle = ""
                                reminderTime = ""
                            }
                        }
                    }
                ) {
                    Text("Save", color = CyanPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

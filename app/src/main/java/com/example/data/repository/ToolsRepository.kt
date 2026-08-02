package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ToolsRepository(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao
) {
    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun saveNote(title: String, content: String, category: String = "General") {
        noteDao.insertNote(NoteEntity(title = title, content = content, category = category))
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun addReminder(title: String, time: String) {
        reminderDao.insertReminder(ReminderEntity(title = title, time = time))
    }

    suspend fun toggleReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
    }

    suspend fun deleteReminder(id: String) {
        reminderDao.deleteReminderById(id)
    }

    // Calculator helper
    fun evaluateExpression(expr: String): String {
        return try {
            val clean = expr.replace("x", "*").replace("÷", "/")
            val result = calculateSimpleMath(clean)
            if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun calculateSimpleMath(expr: String): Double {
        // Quick evaluator for standard expressions
        var result = 0.0
        try {
            val tokens = expr.split("+")
            if (tokens.size > 1) {
                return tokens.sumOf { calculateSimpleMath(it) }
            }
            val minusTokens = expr.split("-")
            if (minusTokens.size > 1) {
                var res = calculateSimpleMath(minusTokens[0])
                for (i in 1 until minusTokens.size) {
                    res -= calculateSimpleMath(minusTokens[i])
                }
                return res
            }
            val multTokens = expr.split("*")
            if (multTokens.size > 1) {
                var res = 1.0
                for (tok in multTokens) {
                    res *= calculateSimpleMath(tok)
                }
                return res
            }
            val divTokens = expr.split("/")
            if (divTokens.size > 1) {
                var res = calculateSimpleMath(divTokens[0])
                for (i in 1 until divTokens.size) {
                    res /= calculateSimpleMath(divTokens[i])
                }
                return res
            }
            return expr.trim().toDouble()
        } catch (e: Exception) {
            return 0.0
        }
    }

    // Knowledge & Assistant Presets
    fun getKnowledgeCategories(): List<KnowledgeCategory> = listOf(
        KnowledgeCategory("Cricket Knowledge", "🏏", "IPL stats, World Cup history, records, player profiles & rules."),
        KnowledgeCategory("Coding Assistant", "💻", "Syntax guides, bug finder, algorithms in Kotlin, Python, JS, C++."),
        KnowledgeCategory("Study Assistant", "📚", "Summary notes, flashcards, concept explainers & quiz generator."),
        KnowledgeCategory("Science & Space", "🔬", "Physics, Chemistry, Biology, Astronomy & Space discoveries."),
        KnowledgeCategory("Math Solver", "🧮", "Algebra, Calculus, Geometry & step-by-step problem solver."),
        KnowledgeCategory("History & World", "🏛️", "World History, Ancient civilizations, Indian History & timelines."),
        KnowledgeCategory("Geography & Nature", "🌍", "Map facts, capitals, climate science, oceans & mountains."),
        KnowledgeCategory("General Knowledge", "💡", "Current trivia, inventions, famous personalities & world records.")
    )
}

data class KnowledgeCategory(
    val title: String,
    val emoji: String,
    val description: String
)

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val blocks = parseMarkdownBlocks(text)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(block.code, block.language, context)
                }
                is MarkdownBlock.TextBlock -> {
                    val annotatedString = buildAnnotatedString {
                        var currentIndex = 0
                        val rawText = block.text

                        // Handle simple **bold** syntax
                        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                        var lastIndex = 0

                        boldRegex.findAll(rawText).forEach { matchResult ->
                            val start = matchResult.range.first
                            val end = matchResult.range.last + 1
                            val boldContent = matchResult.groupValues[1]

                            if (start > lastIndex) {
                                append(rawText.substring(lastIndex, start))
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                                append(boldContent)
                            }
                            lastIndex = end
                        }
                        if (lastIndex < rawText.length) {
                            append(rawText.substring(lastIndex))
                        }
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(code: String, language: String, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2F2F2F))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB4B4B4),
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Code", code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFFB4B4B4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Copy code",
                    fontSize = 11.sp,
                    color = Color(0xFFB4B4B4)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFECECEC),
                lineHeight = 19.sp
            )
        }
    }
}

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val list = mutableListOf<MarkdownBlock>()
    val codeRegex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")

    var lastIndex = 0
    codeRegex.findAll(text).forEach { match ->
        val start = match.range.first
        val end = match.range.last + 1
        val language = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()

        if (start > lastIndex) {
            val textSegment = text.substring(lastIndex, start).trim()
            if (textSegment.isNotEmpty()) {
                list.add(MarkdownBlock.TextBlock(textSegment))
            }
        }
        list.add(MarkdownBlock.CodeBlock(code, language))
        lastIndex = end
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            list.add(MarkdownBlock.TextBlock(remaining))
        }
    }

    if (list.isEmpty() && text.isNotEmpty()) {
        list.add(MarkdownBlock.TextBlock(text))
    }

    return list
}

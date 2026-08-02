package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanVariant
import com.example.ui.theme.PurpleAccent

@Composable
fun VoiceWave(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceWaveTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 800 else 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val barCount = 36
        val barWidth = width / (barCount * 1.8f)

        for (i in 0 until barCount) {
            val x = i * (width / barCount) + barWidth / 2
            val normalizedX = i.toFloat() / barCount
            val multiplier = if (isSpeaking) 1.0f else 0.3f

            val waveHeight = (kotlin.math.sin((normalizedX * 4 * Math.PI + phase).toDouble()) * 0.5 + 0.5) * height * 0.7f * multiplier
            val barHeight = kotlin.math.max(12.dp.toPx(), waveHeight.toFloat())

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(CyanPrimary, PurpleAccent, CyanVariant)
                ),
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth
            )
        }
    }
}

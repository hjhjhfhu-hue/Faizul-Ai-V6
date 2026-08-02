package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanVariant
import com.example.ui.theme.PurpleAccent

@Composable
fun FloatingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isListening: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * pulseScale)) {
            val center = this.center
            val radius = this.size.minDimension / 2

            // Glowing Outer Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(CyanPrimary, PurpleAccent, CyanVariant, CyanPrimary)
                ),
                radius = radius * 0.95f,
                style = Stroke(width = 6.dp.toPx())
            )

            // Inner Pulsing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanPrimary.copy(alpha = 0.9f),
                        PurpleAccent.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.75f
                ),
                radius = radius * 0.75f
            )

            // Accent Particles
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 4.dp.toPx(),
                center = center.copy(
                    x = center.x + (radius * 0.4f * kotlin.math.cos(Math.toRadians(rotationAngle.toDouble()))).toFloat(),
                    y = center.y + (radius * 0.4f * kotlin.math.sin(Math.toRadians(rotationAngle.toDouble()))).toFloat()
                )
            )
        }
    }
}

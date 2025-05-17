package com.thiago.chatjump.ui.realtime.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitingLines(
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbiting")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 2000 else 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 500 else 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width.coerceAtMost(size.height) * 0.3f * pulse

        rotate(rotation) {
            // Draw 4 orbiting lines
            repeat(4) { index ->
                val angle = (index * 90f + rotation) * (Math.PI / 180f)
                val startX = centerX + (radius * cos(angle)).toFloat()
                val startY = centerY + (radius * sin(angle)).toFloat()
                val endX = centerX + (radius * cos(angle + Math.PI)).toFloat()
                val endY = centerY + (radius * sin(angle + Math.PI)).toFloat()

                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    waveform: List<Float>,
    isSpeaking: Boolean
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (waveform.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val centerY = height / 2
        val pointCount = waveform.size
        val pointSpacing = width / (pointCount - 1)

        val path = Path()
        path.moveTo(0f, centerY)

        waveform.forEachIndexed { index, amplitude ->
            val x = index * pointSpacing
            val y = centerY + (amplitude * height * 0.4f)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round
            )
        )
    }
} 
package com.thiago.chatjump.ui.voicechat.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated "ball of yarn" built from curved lines. The lines rotate around the
 * center and their curvature subtly pulses, giving a loose–thread look. When
 * [isRecording] is true the ball spins faster and pulses more aggressively.
 */
@Composable
fun YarnBallVisualizer(
    isRecording: Boolean,
    amplitude: Float = 0f,
    // 0f = full yarn ball, 1f = totally flattened into a single line
    morphToLineProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Simple infinite rotation based on time
    val infiniteTransition = rememberInfiniteTransition(label = "yarnBallRotation")

    // Base duration of one full revolution and a dynamic version that speeds up
    // proportionally with the current voice amplitude. The higher the amplitude,
    // the quicker the spin (up to ~70 % faster).
    val baseDuration = if (isRecording) 2000 else 6000
    val rotationDuration = (baseDuration * (1f - 0.7f * amplitude)).toInt().coerceAtLeast(500)

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val baseStroke = if (isRecording) 6f else 4f
    // Freeze the hue shift as we morph so that the colour transition also calms down.
    val hueBase = ((rotation * (1f - morphToLineProgress) + amplitude * 360f) * 0.4f + if (isRecording) 160f else 220f) % 360f
    val baseColor = Color.hsv(hueBase, 0.7f, 1f)

    // Pulse scale controls ball breathing
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Additional small oscillation that bends the threads while rotating
    val curvaturePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 1600 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "curvaturePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = this.center
        val baseRadius = size.minDimension * 0.25f
        val radius = baseRadius * (if (isRecording) pulse else 1f)

        // Draw subtle radial gradient behind for 3D shading
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = 0.15f), baseColor.copy(alpha = 0.05f), Color.Transparent),
                center = center,
                radius = radius * 1.25f
            ),
            radius = radius * 1.25f,
            center = center
        )

        val lines = 6 // more threads for a fuller ball
        for (i in 0 until lines) {
            // As morph progresses we bring all the angles towards 0° (horizontal)
            val angleDeg = (rotation * (1f - morphToLineProgress)) + i * 360f / lines * (1f - morphToLineProgress)
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            // Direction unit vector
            val dx = cos(angleRad)
            val dy = sin(angleRad)

            val start = Offset(center.x - dx * radius, center.y - dy * radius)
            val end = Offset(center.x + dx * radius, center.y + dy * radius)

            // Perpendicular vector for control point
            val perp = Offset(-dy, dx)
            // Gradually remove curvature so threads become straight
            val curvatureMag = radius * 0.35f * (0.6f + 0.4f * sin(curvaturePhase + i) + amplitude * 0.5f) * (1f - morphToLineProgress)
            val control = Offset(center.x + perp.x * curvatureMag, center.y + perp.y * curvatureMag)

            val depthRaw = (cos(angleRad) + 1f) / 2f
            val depth = depthRaw * (1f - morphToLineProgress) + morphToLineProgress // smoothly approach 1f

            val strokeWidth = baseStroke * (0.6f + 0.8f * depth) * (1f + amplitude * 0.4f)
            val color = baseColor.copy(alpha = 0.3f + 0.7f * depth)

            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticBezierTo(control.x, control.y, end.x, end.y)
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0f), color, color.copy(alpha = 0f)),
                    start = start,
                    end = end
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/** Horizontal waveform visualizer that stretches across the screen. */
@Composable
fun WaveformVisualizer(
    amplitude: Float, // 0f..1f
    modifier: Modifier = Modifier,
) {
    // Animate phase shift for traveling wave and subtle amplitude breathing
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    val breath by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breath"
    )

    // Derive a hue similar to the YarnBall formula so both visualizers stay in sync.
    val hueBase = (((phase * 180f / PI.toFloat()) + amplitude * 360f) * 0.4f + 220f) % 360f
    val baseColor = Color.hsv(hueBase, 0.7f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerY = size.height / 2f
        val maxAmp = size.height / 2.5f // max peak
        val path = Path()
        val points = 120
        for (i in 0..points) {
            val x = size.width * i / points.toFloat()
            val progress = i / points.toFloat()
            // Composite waveform: fundamental + 2nd harmonic for richer look
            val fundamental = sin((progress * 2f * PI + phase).toFloat())
            val harmonic = 0.5f * sin((progress * 4f * PI + phase * 1.3f).toFloat())
            val env = (1f - (progress - 0.5f).let { it * it * 4f }.coerceIn(0f, 1f)) // subtle tapering near edges

            val y = centerY + (fundamental + harmonic) * maxAmp * amplitude * breath * env
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(baseColor.copy(alpha = 0.1f), baseColor, baseColor.copy(alpha = 0.1f))
            ),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
} 
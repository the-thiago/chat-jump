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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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
    modifier: Modifier = Modifier,
    targetFlatLineStrokeDp: Dp = 3.dp // New parameter for final flat line stroke
) {
    // Simple infinite rotation based on time
    val infiniteTransition = rememberInfiniteTransition(label = "yarnBallRotation")

    val density = LocalDensity.current

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

    // Base stroke for the yarn ball form (when not morphing)
    val yarnBallBaseStrokePx = with(density) { (if (isRecording) 4.dp else 3.dp).toPx() }

    // Target stroke for the final flat line
    val finalFlatLineStrokePx = with(density) { targetFlatLineStrokeDp.toPx() }

    // Freeze the hue shift as we morph so that the colour transition also calms down.
    val hueBase = ((rotation * (1f - morphToLineProgress) + amplitude * 360f) * 0.4f + if (isRecording) 160f else 220f) % 360f
    val baseColor = Color.hsv(hueBase, 0.7f, 1f)

    // Additional small oscillation that bends the threads while rotating
    val curvaturePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 1600 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "curvaturePhase"
    )

    // New animation for per-line radius modulation to create an orbiting effect
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 5000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "orbitPhase"
    )

    // Animation to drive individual, out-of-sync line pulses
    // Speed of individual pulses now also reacts to amplitude
    val individualPulseDuration = ((if (isRecording) 800 else 1500) * (1f - amplitude * 0.5f)).toInt().coerceAtLeast(300)
    val individualPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(), // Cycle through a full sine wave
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = individualPulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart // Restart for continuous cycling
        ), label = "individualPulsePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = this.center
        val baseRadius = size.minDimension * 0.25f
        // globalRadius is now the baseline before individual pulsing
        // val globalRadius = baseRadius * (if (isRecording) pulse else 1f) // Old global pulse removed

        // Draw subtle radial gradient behind for 3D shading (using baseRadius for stability)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(baseColor.copy(alpha = 0.15f), baseColor.copy(alpha = 0.05f), Color.Transparent),
                center = center,
                radius = baseRadius * 1.25f // Gradient based on stable baseRadius
            ),
            radius = baseRadius * 1.25f,
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

            // 1. Apply global orbiting modulation to baseRadius
            val orbitModulationFactor = 0.08f // Gentle global orbit +/- 8%
            val orbitAdjustedRadius = baseRadius * (1f + orbitModulationFactor * sin(orbitPhase + i * PI.toFloat() * 0.5f /* Stagger orbit phase per line */))

            // 2. Apply individual line pulse to the orbitAdjustedRadius
            // When recording: very tiny base pulse, with amplitude contributing more significantly.
            // Max pulse when recording: 0.005f (base) + 0.145f (from full amplitude) = 0.15f
            // When idle (not recording): a gentle constant pulse.
            val basePulseMagnitude = if (isRecording) 0.005f else 0.05f
            val amplitudeScaledPulse = if (isRecording) (amplitude.coerceIn(0f, 1f) * 0.145f) else 0f
            val pulseMagnitude = basePulseMagnitude + amplitudeScaledPulse

            val lineSpecificPulseOffset = (i * PI.toFloat() * 0.75f) // Stagger pulses
            val linePulseScale = 1f + pulseMagnitude * sin(individualPulsePhase + lineSpecificPulseOffset)

            val lineRadius = orbitAdjustedRadius * linePulseScale

            val start = Offset(center.x - dx * lineRadius, center.y - dy * lineRadius)
            val end = Offset(center.x + dx * lineRadius, center.y + dy * lineRadius)

            // Perpendicular vector for control point
            val perp = Offset(-dy, dx)
            // Gradually remove curvature so threads become straight
            val curvatureMag = lineRadius * 0.35f * (0.6f + 0.4f * sin(curvaturePhase + i) + amplitude * 0.5f) * (1f - morphToLineProgress)
            val control = Offset(center.x + perp.x * curvatureMag, center.y + perp.y * curvatureMag)

            val depthRaw = (cos(angleRad) + 1f) / 2f
            val depth = depthRaw * (1f - morphToLineProgress) + morphToLineProgress // smoothly approach 1f

            // Calculate effective stroke scale. When morphProgress is 0, scale is 1.
            // When morphProgress is 1, scale is adjusted so the final line width matches targetFlatLineStrokeDp.
            // The reference stroke for a flat line (amplitude=0, depth=1) without this special scaling would be yarnBallBaseStrokePx * 1.4f.
            val baseForFlatLine = yarnBallBaseStrokePx * 1.4f
            val strokeScaleAtMorphEnd = if (baseForFlatLine > 0.001f) finalFlatLineStrokePx / baseForFlatLine else 1f

            val strokeScale = lerp(1f, strokeScaleAtMorphEnd, morphToLineProgress)

            val strokeWidth = yarnBallBaseStrokePx * (0.6f + 0.8f * depth) * (1f + amplitude * 0.4f) * strokeScale
            val color = baseColor.copy(alpha = (0.3f + 0.7f * depth) * strokeScale)

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
    initialAmplitudeFactor: Float = 1f, // 0f = flat line, 1f = full amplitude
    strokeWidthDp: Dp = 3.dp // New parameter for stroke width
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

            val waveHeight = (fundamental + harmonic) * maxAmp * amplitude * breath * env
            val y = centerY + waveHeight * initialAmplitudeFactor // Apply the factor here
            val yPinned = if (i == 0 || i == points) centerY else y
            if (i == 0) {
                path.moveTo(x, yPinned)
            } else {
                path.lineTo(x, yPinned)
            }
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(baseColor.copy(alpha = 0.1f), baseColor, baseColor.copy(alpha = 0.1f))
            ),
            style = Stroke(width = strokeWidthDp.toPx(), cap = StrokeCap.Round)
        )
    }
} 
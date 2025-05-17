package com.thiago.chatjump.ui.realtime

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Real Time") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            OrbitingLinesAnimation()
        }
    }
}

@Composable
private fun OrbitingLinesAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    
    // Create multiple orbits with different speeds and radii
    val orbits = listOf(
        Orbit(radius = 150f, speed = 0.5f, color = MaterialTheme.colorScheme.primary),
        Orbit(radius = 200f, speed = 0.3f, color = MaterialTheme.colorScheme.secondary),
        Orbit(radius = 100f, speed = 0.7f, color = MaterialTheme.colorScheme.tertiary)
    )

    // Create animations for each orbit
    val orbitAngles = orbits.map { orbit ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1000 / orbit.speed).toInt(),
                    easing = LinearEasing
                )
            ),
            label = "orbit_angle_${orbit.radius}"
        )
    }

    Canvas(
        modifier = Modifier
            .size(400.dp)
            .padding(16.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        orbits.forEachIndexed { index, orbit ->
            val angle = orbitAngles[index].value
            val x = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * orbit.radius
            val y = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * orbit.radius

            // Draw the orbit path
            drawCircle(
                color = orbit.color.copy(alpha = 0.1f),
                radius = orbit.radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )

            // Draw the moving point
            drawCircle(
                color = orbit.color,
                radius = 8f,
                center = Offset(x, y)
            )

            // Draw lines connecting points
            orbits.forEachIndexed { otherIndex, otherOrbit ->
                if (index != otherIndex) {
                    val otherAngle = orbitAngles[otherIndex].value
                    val otherX = centerX + cos(Math.toRadians(otherAngle.toDouble())).toFloat() * otherOrbit.radius
                    val otherY = centerY + sin(Math.toRadians(otherAngle.toDouble())).toFloat() * otherOrbit.radius

                    drawLine(
                        color = orbit.color.copy(alpha = 0.3f),
                        start = Offset(x, y),
                        end = Offset(otherX, otherY),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private data class Orbit(
    val radius: Float,
    val speed: Float,
    val color: Color
) 
package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.repository.ActiveDownloadState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThreeDimensionalDownloadCircle(
    activeState: ActiveDownloadState,
    hasClipboardLink: Boolean,
    detectedPlatform: PlatformType,
    onCircleClick: () -> Unit,
    onCancelClick: () -> Unit,
    onOpenCompleted: () -> Unit,
    onResetToReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    // Press animation for realistic 3D tactile depression
    val pressProgress = remember { Animatable(0f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            pressProgress.animateTo(1f, tween(80, easing = FastOutSlowInEasing))
        } else {
            pressProgress.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    // Infinite breathing/pulsing animation for idle state when link detected
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Rotation angle for analyzing state
    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalRotation"
    )

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }

    // Dynamic accent color based on detected platform or download state
    val coreSphereColor = when {
        hasClipboardLink && !activeState.isRunning -> detectedPlatform.primaryColor
        activeState.status == DownloadStatus.COMPLETED -> NeonGreen
        activeState.isRunning -> NeonCyan
        else -> NeonCyan
    }

    val sphereSizeDp = 230.dp

    Box(
        modifier = modifier
            .size(280.dp)
            .testTag("three_dimensional_download_circle"),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Outer Glow Canvas (Radiates when link detected or downloading)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (hasClipboardLink || activeState.isRunning) (0.6f * pulseGlow).coerceIn(0f, 1f) else 0.2f
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreSphereColor.copy(alpha = 0.45f),
                        coreSphereColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )
        }

        // The 3D Sphere Interactive Container
        Box(
            modifier = Modifier
                .size(sphereSizeDp)
                .graphicsLayer {
                    // 3D displacement on press: shifts down and slightly compresses
                    val depthShift = pressProgress.value * 12f
                    translationY = depthShift
                    scaleX = 1f - (pressProgress.value * 0.05f)
                    scaleY = 1f - (pressProgress.value * 0.05f)
                }
                .pointerInput(activeState.status, activeState.isRunning, hasClipboardLink) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            triggerHapticFeedback()
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            when {
                                activeState.isRunning -> { /* Already downloading */ }
                                activeState.status == DownloadStatus.COMPLETED -> {
                                    if (hasClipboardLink) {
                                        // User has a new link ready! Tap starts downloading new link immediately
                                        onCircleClick()
                                    } else {
                                        // Open completed video and reset circle back to ready
                                        onOpenCompleted()
                                        onResetToReady()
                                    }
                                }
                                else -> onCircleClick()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 3D Custom Canvas Drawing: Shadow, Chamfer, Gradient Sphere, Highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // 1. Drop Shadow underneath (shrunk on press)
                val shadowOffsetY = 16f - (pressProgress.value * 10f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = center + Offset(0f, shadowOffsetY),
                        radius = radius * 1.08f
                    ),
                    radius = radius * 1.05f,
                    center = center + Offset(0f, shadowOffsetY)
                )

                // 2. Beveled Metallic Outer Rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF384358),
                            Color(0xFF1A2230),
                            Color(0xFF0F1520),
                            Color(0xFF2E394A)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    radius = radius,
                    center = center
                )

                // 3. Inner Bevel Ring
                val innerRimRadius = radius * 0.94f
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D121B),
                            Color(0xFF182232)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height)
                    ),
                    radius = innerRimRadius,
                    center = center
                )

                // 4. 3D Spherical Body with Realistic Specular Highlight (Light source at top-left)
                val sphereCoreRadius = radius * 0.88f
                val lightSourceCenter = center + Offset(-sphereCoreRadius * 0.35f, -sphereCoreRadius * 0.35f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),            // Sharp specular hotspot
                            coreSphereColor.copy(alpha = 0.95f),        // Vibrant midtone
                            coreSphereColor.copy(alpha = 0.75f),        // Body color
                            Color(0xFF0A1D33),                          // Deep shadow falloff
                            Color(0xFF040A12)                           // Dark rim
                        ),
                        center = lightSourceCenter,
                        radius = sphereCoreRadius * 1.3f
                    ),
                    radius = sphereCoreRadius,
                    center = center
                )

                // 5. Subtle Ambient Rim Reflection (Bottom-Right bounce light)
                val bounceLightCenter = center + Offset(sphereCoreRadius * 0.45f, sphereCoreRadius * 0.45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreSphereColor.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = bounceLightCenter,
                        radius = sphereCoreRadius * 0.6f
                    ),
                    radius = sphereCoreRadius,
                    center = center
                )

                // 6. Active State Canvas Animations:
                if (activeState.status == DownloadStatus.ANALYZING) {
                    // Rotating glowing orbital arc
                    val sweepAngle = 90f
                    val strokeW = 6.dp.toPx()
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, NeonCyan, Color.White),
                            center = center
                        ),
                        startAngle = orbitalRotation,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius * 0.93f, center.y - radius * 0.93f),
                        size = Size(radius * 1.86f, radius * 1.86f)
                    )
                } else if (activeState.status == DownloadStatus.DOWNLOADING) {
                    // Live download progress ring track
                    val trackRadius = radius * 0.93f
                    val strokeW = 7.dp.toPx()

                    // Background track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = trackRadius,
                        center = center,
                        style = Stroke(width = strokeW)
                    )

                    // Active progress sweep
                    val sweepAngle = (activeState.progress * 360f).coerceIn(0f, 360f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, NeonCyanLight, Color.White, NeonCyan),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - trackRadius, center.y - trackRadius),
                        size = Size(trackRadius * 2f, trackRadius * 2f)
                    )
                }
            }

            // Central Content Inside 3D Sphere (Icon, Text, Progress)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-pressProgress.value * 2f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    activeState.status == DownloadStatus.COMPLETED -> {
                        if (hasClipboardLink) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Download starten",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "HERUNTERLADEN",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Neuer Link bereit",
                                color = NeonCyanLight,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Fertig",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "FERTIG",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Tippen zum Ansehen",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    activeState.status == DownloadStatus.DOWNLOADING -> {
                        val percentInt = (activeState.progress * 100).toInt()
                        Text(
                            text = "$percentInt%",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 38.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (activeState.speed.isNotBlank()) {
                            Text(
                                text = activeState.speed,
                                color = NeonCyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Download läuft...",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    activeState.status == DownloadStatus.ANALYZING -> {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Analysieren",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    translationY = sin(Math.toRadians(orbitalRotation.toDouble())).toFloat() * 6f
                                }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "PRÜFE LINK...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    else -> {
                        // Idle state
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Download starten",
                            tint = Color.White,
                            modifier = Modifier
                                .size(62.dp)
                                .graphicsLayer {
                                    // Subtle 3D embossed look with light shadow
                                    shadowElevation = 8f
                                }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasClipboardLink) "HERUNTERLADEN" else "STARTEN",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (hasClipboardLink) "Link erkannt" else "Link einfügen",
                            color = if (hasClipboardLink) NeonCyanLight else Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Cancel Button Overlay when active download is running
        if (activeState.isRunning) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
            ) {
                androidx.compose.material3.FilledIconButton(
                    onClick = onCancelClick,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF1E2638),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Download abbrechen",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Speed options for the circular speed wheel.
 * Arranged clockwise from top: 4× → 2× → 1.25× → 1× → 1.5× → 3×
 */
data class SpeedOption(val speed: Float, val label: String, val sectorAngleDeg: Float)

val SPEED_OPTIONS = listOf(
    SpeedOption(4.0f,  "4×",    270f),  // top
    SpeedOption(2.0f,  "2×",    330f),  // top-right
    SpeedOption(1.25f, "1.25×", 30f),   // bottom-right
    SpeedOption(1.0f,  "1×",    90f),   // bottom
    SpeedOption(1.5f,  "1.5×",  150f),  // bottom-left
    SpeedOption(3.0f,  "3×",    210f),  // top-left
)

private const val SECTOR_HALF = 30f // each sector spans ±30°

/**
 * Premium circular speed-selector wheel.
 *
 * Appears around the long-press finger position. The user slides their finger
 * in any direction; the nearest speed chip highlights with a glow.
 *
 *         ───  4×  ───
 *      3×              2×
 *           \   ●   /
 *      1.5×          1.25×
 *         ───  1×  ───
 *
 * Release applies the highlighted speed (then resets to 1× on release
 * per temporary-boost behavior).
 *
 * @param isVisible       Whether the wheel is shown
 * @param centerX         Press-down X position in pixels
 * @param centerY         Press-down Y position in pixels
 * @param fingerX         Current finger X in pixels (for angle calculation)
 * @param fingerY         Current finger Y in pixels
 * @param selectedIndex   Precomputed index into SPEED_OPTIONS (-1 = none)
 * @param currentSpeed    The baseline playback speed
 */
@Composable
fun CircularSpeedWheel(
    isVisible: Boolean,
    centerX: Float,
    centerY: Float,
    fingerX: Float,
    fingerY: Float,
    selectedIndex: Int,
    currentSpeed: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val wheelRadiusDp = 90.dp
    val wheelRadiusPx = with(density) { wheelRadiusDp.toPx() }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) +
                scaleIn(tween(350, delayMillis = 50), initialScale = 0.5f),
        exit  = fadeOut(tween(150)) +
                scaleOut(tween(200), targetScale = 0.5f),
        modifier = modifier
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Radial scrim ────────────────────────────────────────────────────
            Canvas(
                modifier = Modifier
                    .offset { IntOffset((centerX - wheelRadiusPx * 1.6f).toInt(),
                                         (centerY - wheelRadiusPx * 1.6f).toInt()) }
                    .size(wheelRadiusDp * 3.2f)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Radial gradient scrim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = wheelRadiusPx * 1.6f
                    ),
                    radius = wheelRadiusPx * 1.6f
                )
                // Outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = wheelRadiusPx * 1.15f,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                // Sector lines
                SPEED_OPTIONS.forEach { opt ->
                    val angleRad = Math.toRadians(opt.sectorAngleDeg.toDouble()).toFloat()
                    val ex = cx + wheelRadiusPx * 1.15f * cos(angleRad)
                    val ey = cy + wheelRadiusPx * 1.15f * sin(angleRad)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(cx + wheelRadiusPx * 0.25f * cos(angleRad),
                                       cy + wheelRadiusPx * 0.25f * sin(angleRad)),
                        end = Offset(ex, ey),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                // Connection line from center to finger
                if (selectedIndex >= 0) {
                    val fingerAngle = atan2(fingerY - centerY, fingerX - centerX)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.4f),
                                primaryColor.copy(alpha = 0.0f)
                            )
                        ),
                        start = Offset(cx, cy),
                        end = Offset(cx + wheelRadiusPx * 0.9f * cos(fingerAngle),
                                     cy + wheelRadiusPx * 0.9f * sin(fingerAngle)),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── Speed label chips positioned around the circle ──────────────────
            SPEED_OPTIONS.forEachIndexed { index, option ->
                val isHighlighted = index == selectedIndex
                val isCurrentActive = option.speed == currentSpeed && !isHighlighted

                val angleRad = Math.toRadians(option.sectorAngleDeg.toDouble()).toFloat()
                val labelX = centerX + wheelRadiusPx * cos(angleRad)
                val labelY = centerY + wheelRadiusPx * sin(angleRad)

                val labelScale by animateFloatAsState(
                    targetValue = if (isHighlighted) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "wheelLabelScale_$index"
                )

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset((labelX / density.density - 28).dp.roundToPx(),
                                      (labelY / density.density - 16).dp.roundToPx())
                        }
                        .size(56.dp, 32.dp)
                        .scale(labelScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isHighlighted -> MaterialTheme.colorScheme.primary
                                    isCurrentActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            )
                            .then(
                                if (isHighlighted) Modifier.shadow(12.dp, RoundedCornerShape(16.dp), clip = false)
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            fontSize = 14.sp,
                            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = when {
                                isHighlighted -> MaterialTheme.colorScheme.onPrimary
                                isCurrentActive -> MaterialTheme.colorScheme.primary
                                else -> Color.White.copy(alpha = 0.85f)
                            },
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            // ── Center dot ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((centerX / density.density - 5).dp.roundToPx(),
                                  (centerY / density.density - 5).dp.roundToPx())
                    }
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
        }
    }
}

/**
 * Resolves which speed chip the finger is pointing at based on angular position.
 *
 * @param fingerX   Current finger X in pixels
 * @param fingerY   Current finger Y in pixels
 * @param centerX   Wheel center X in pixels
 * @param centerY   Wheel center Y in pixels
 * @return Index into SPEED_OPTIONS, or -1 if finger is too close to center
 */
fun resolveSpeedIndex(
    fingerX: Float,
    fingerY: Float,
    centerX: Float,
    centerY: Float
): Int {
    val dx = fingerX - centerX
    val dy = fingerY - centerY
    val distance = dx * dx + dy * dy

    // Dead zone: if finger hasn't moved enough from center, don't select yet
    val minRadiusPx = 180f // ~20dp at 160dpi — small dead zone
    if (distance < minRadiusPx * minRadiusPx) return -1

    // atan2: 0=right, PI/2=down, PI/-PI=left, -PI/2=up
    val angleRad = atan2(dy, dx)
    var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
    if (angleDeg < 0f) angleDeg += 360f

    // Find nearest sector
    var bestIndex = -1
    var bestDiff = Float.MAX_VALUE
    SPEED_OPTIONS.forEachIndexed { index, option ->
        var diff = kotlin.math.abs(angleDeg - option.sectorAngleDeg)
        if (diff > 180f) diff = 360f - diff
        if (diff <= SECTOR_HALF && diff < bestDiff) {
            bestDiff = diff
            bestIndex = index
        }
    }
    return bestIndex
}
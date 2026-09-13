/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================================
 * HOA PALETTE DESIGN SYSTEM — REUSABLE UI PRIMITIVES
 * ============================================================================
 *
 * These composables encode the "per-tool signature color" design language used
 * across the player's More panel, sheets, and toolbars:
 *
 *   • Inactive elements show their signature color at reduced alpha.
 *   • ACTIVE / emphasized elements light up to full color with a soft radial
 *     glow halo and, where applicable, an indicator.
 *   • Page chrome (headers, buttons, badges) is tinted with one accent color
 *     so every screen reads at a glance.
 *
 * Every component takes a [Color] accent (pass `ToolIconPalette.<Tool>`).
 * Apply `Modifier.frostedGlass()` (Glassmorphism.kt) for the translucent
 * card surfaces these headers/buttons sit on.
 *
 * --- Quick usage ------------------------------------------------------------
 * AccentGlowIcon(                      // glowing icon — the visual signature
 *     icon = Icons.Default.Audio,
 *     accent = ToolIconPalette.Audio
 * )
 *
 * PaletteIconButton(                   // animated toolbar/tool button
 *     onClick = { ... },
 *     icon = Icons.Default.Lock,
 *     contentDescription = "Lock",
 *     color = ToolIconPalette.Lock,
 *     isActive = isLockActive
 * )
 *
 * PaletteSheetHeader(                  // standard sheet header
 *     title = "Smart Scenes",
 *     subtitle = "AI-Detected Scene Boundaries",
 *     accent = ToolIconPalette.Bookmarks,
 *     leadingIcon = Icons.Default.AutoAwesome,   // OR onBack = { ... }
 *     trailing = {
 *         PaletteButton(onClick = { runDetect() }, accent = ToolIconPalette.Bookmarks) {
 *             Icon(Icons.Default.AutoAwesome, contentDescription = null)
 *             Spacer(Modifier.width(6.dp))
 *             Text("Detect Scenes")
 *         }
 *     }
 * )
 *
 * PaletteBadge(text = "LIVE STREAMING", accent = ToolIconPalette.Network)
 * ============================================================================
 */

/**
 * A soft radial glow halo in the signature accent color — the underline of the
 * palette system. Wrap an icon or control with it to mark it as primary /
 * "on". Prefer [AccentGlowIcon] for icon-sized use, or apply to a whole
 * surface by sizing its [modifier].
 */
@Composable
fun GlowHalo(
    accent: Color,
    size: Dp = 44.dp,
    glowAlpha: Float = 0.30f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent)
                )
            )
    )
}

/**
 * An icon seated in a radial glow halo of its signature color. This is THE
 * visual signature of the HOA palette system — used for per-tool header
 * icons, active toolbar buttons, and emphasized controls.
 *
 * @param icon            The [ImageVector] to draw.
 * @param accent          Signature color of the tool.
 * @param contentDescription Accessibility label; null means decorative.
 * @param iconSize        Diameter of the icon.
 * @param haloSize        Diameter of the glow halo.
 * @param glowAlpha       Halo opacity (0..1); higher = stronger glow.
 */
@Composable
fun AccentGlowIcon(
    icon: ImageVector,
    accent: Color,
    contentDescription: String? = null,
    iconSize: Dp = 26.dp,
    haloSize: Dp = 44.dp,
    glowAlpha: Float = 0.30f,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        GlowHalo(accent = accent, size = haloSize, glowAlpha = glowAlpha)
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * The animated toolbar / tool icon button — press-to-scale feedback and an
 * active state that lights the icon up to full color with a [GlowHalo] glow
 * plus a small indicator dot. Promoted here from the player toolbar so any
 * toolbar / tool drawer can reuse it.
 *
 * @param onClick           Button action.
 * @param icon              The [ImageVector] to draw.
 * @param contentDescription Accessibility label.
 * @param color             Signature color of the tool. Shown at 75% alpha
 *                          when inactive; full + glowing when [isActive].
 * @param isActive          When true: icon lights to full color, gains a glow
 *                          halo, an indicator dot, and a slight scale pop.
 */
@Composable
fun PaletteIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    color: Color = Color.White,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isActive) 1.15f else 1f,
        animationSpec = tween(150),
        label = "iconScale"
    )

    val actualTint = if (isActive) color else color.copy(alpha = 0.75f)

    Box(contentAlignment = Alignment.Center) {
        if (isActive) {
            GlowHalo(accent = color)
        }
        IconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier.scale(scale)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = actualTint)
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(5.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

/**
 * The standard HOA sheet / sub-page header: an accent-glow back-button OR a
 * leading accent icon, a bold title (with optional subtitle), and optional
 * trailing content (buttons, badges, counts) anchored to the right edge.
 *
 * Mirrors the headers on the Queue, Watch Party, Video Adjustments,
 * Diagnostics, Quality Report and Smart Scenes surfaces — use this instead of
 * hand-copying the halo + title block.
 *
 * @param title        Header title (rendered white, bold, titleLarge).
 * @param accent       Signature color of the page/tool.
 * @param modifier     Applied to the outer row (e.g. padding).
 * @param onBack       If provided, renders a tinted back button in a glow halo
 *                     at the leading edge (preferred over [leadingIcon]).
 * @param leadingIcon  If [onBack] is null and this is set, renders the icon in
 *                     a glow halo at the leading edge.
 * @param subtitle     Optional secondary line under the title.
 * @param iconSize     Leading icon diameter.
 * @param trailing     Optional [RowScope] content anchored to the trailing edge.
 */
@Composable
fun PaletteSheetHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    subtitle: String? = null,
    iconSize: Dp = 26.dp,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        when {
            onBack != null -> {
                Box(contentAlignment = Alignment.Center) {
                    GlowHalo(accent = accent)
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = accent
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            leadingIcon != null -> {
                AccentGlowIcon(
                    icon = leadingIcon,
                    accent = accent,
                    iconSize = iconSize
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        trailing?.let { it() }
    }
}

/**
 * A filled button tinted entirely with the tool's signature color. Use for
 * the primary action on any palette page (e.g. "Detect Scenes", "Show Room
 * QR Code", "Close"). Content is passed via [content] so you can compose an
 * icon + label.
 *
 * @param onClick     Button action.
 * @param accent      Signature color; fills the button container.
 * @param modifier    Applied to the button.
 * @param enabled     Standard enabled gate.
 * @param contentColor Foreground color (defaults to black for contrast on the
 *                     bright signature colors; override for dark accents).
 * @param content     [RowScope] content — icon, label, progress, etc.
 */
@Composable
fun PaletteButton(
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Black,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = contentColor
        ),
        content = content
    )
}

/**
 * A compact, accent-tinted pill badge — e.g. "LIVE STREAMING", "HQ", queue
 * counts, status chips. Semi-transparent accent fill with a matching border
 * and bold accent text (the signed "inactive, but emphasized" state).
 */
@Composable
fun PaletteBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
    surfaceAlpha: Float = 0.15f,
    borderAlpha: Float = 0.5f
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = accent.copy(alpha = surfaceAlpha),
        border = BorderStroke(0.5.dp, accent.copy(alpha = borderAlpha))
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

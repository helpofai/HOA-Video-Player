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
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.theme

import androidx.compose.ui.graphics.Color

/**
 * PROFESSIONAL COLOR ICON SYSTEM — one signature color per function.
 *
 * Every tool, tab, file type and action in the app maps to a single
 * signature color so the whole UI reads at a glance. Screens render the
 * color at reduced alpha when the feature is idle and at full alpha
 * (with a glow) when it is active.
 */
object ToolIconPalette {
    // --- Player toolbar ---
    val Audio = Color(0xFF4CAF50)        // green — sound
    val Subtitles = Color(0xFFFFC107)    // amber — captions
    val VideoEnhancer = Color(0xFFAB47BC) // purple — magic wand
    val AutoAI = Color(0xFF40C4FF)       // electric blue — intelligence
    val HQ = Color(0xFFFFD740)           // gold — premium quality
    val Adjustments = Color(0xFFF06292)  // pink — transforms
    val Lock = Color(0xFFFFA726)         // orange — security
    val Speed = Color(0xFFFF5252)        // red — velocity
    val Equalizer = Color(0xFF536DFE)    // indigo — sound shaping
    val Loop = Color(0xFF00BCD4)         // teal — repeat
    val Screenshot = Color(0xFF64B5F6)   // light blue — capture
    val ABRepeat = Color(0xFFFF7043)     // deep orange — markers
    val Rotate = Color(0xFF448AFF)       // blue — orientation
    val PiP = Color(0xFF4DD0E1)          // cyan — mini player
    val Info = Color(0xFF90A4AE)         // blue grey — details

    // --- More slider / tools page ---
    val Queue = Color(0xFF00E5FF)        // bright cyan — playlist
    val WatchParty = Color(0xFFFF6D00)   // deep orange — cast party
    val Bookmarks = Color(0xFFFFB300)    // amber gold — saved marks
    val Display = Color(0xFF448AFF)      // blue — display settings
    val QualityAnalyzer = Color(0xFFFFD740) // gold — analysis
    val Favorite = Color(0xFFE91E63)     // pink red — hearts
    val PlaylistAdd = Color(0xFF7C4DFF)  // deep purple — add
    val Share = Color(0xFF00C853)        // green — send
    val Network = Color(0xFF26C6DA)      // light cyan — stream

    // --- Library / home / navigation ---
    val Home = Color(0xFF00CEC9)         // signature teal — home
    val Folders = Color(0xFFFFB300)      // amber gold — folders
    val Files = Color(0xFF5C6BC0)        // indigo — files & documents
    val Tools = Color(0xFF7C4DFF)        // deep purple — tools
    val Search = Color(0xFF40C4FF)       // electric blue — search
    val Settings = Color(0xFF90A4AE)     // blue grey — settings
    val Trash = Color(0xFFFF5252)        // red — delete / trash
    val Insights = Color(0xFF26C6DA)     // cyan — stats & habits
    val Refresh = Color(0xFF00E676)      // green — refresh / scan
    val Back = Color(0xFFFFFFFF)         // white — navigation
    val More = Color(0xFFFFFFFF)         // white — overflow
}

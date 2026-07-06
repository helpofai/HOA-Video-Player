import re

file_path = r"C:\Users\rajib\Desktop\vidplay\app\src\main\java\com\helpofai\videoplayer\feature\player\PlayerScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove boolean state declarations and insert activeDialog
state_declarations_pattern = re.compile(r"    // Subtitles/Audio State.*?var showSubtitlesSheet by remember \{ mutableStateOf\(false\) \}", re.DOTALL)
replacement_state = """    // Dialog State
    var activeDialog by remember { androidx.compose.runtime.mutableStateOf<com.helpofai.videoplayer.feature.player.components.PlayerDialogType?>(null) }
    
    // Subtitles/Audio State
    var trackSelectorInitialTab by remember { mutableIntStateOf(0) }

    // Decoder State
    var decoderMode by remember { mutableStateOf("HW") }
    
    val bookmarks by viewModel.bookmarks.collectAsState()"""

content = state_declarations_pattern.sub(replacement_state, content)

# 2. Replace setter usages
replacements = {
    "showTrackSelector = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.TRACK_SELECTOR_AUDIO",
    "showSubtitlesSheet = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.TRACK_SELECTOR_SUBTITLE",
    "showDecoderSelector = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.DECODER_SELECTOR",
    "showInfoDialog = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.VIDEO_INFO",
    "showEqualizer = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.EQUALIZER",
    "showSpeedDial = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.SPEED_DIAL",
    "showVideoAdjustments = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.VIDEO_ADJUSTMENTS",
    "showQualitySheet = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.QUALITY_SHEET",
    "showMorePopup = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.MORE_POPUP",
    "showAdPopup = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.AD_POPUP",
    "showBookmarksSheet = true": "activeDialog = com.helpofai.videoplayer.feature.player.components.PlayerDialogType.BOOKMARKS_SHEET"
}

for old, new in replacements.items():
    content = content.replace(old, new)

# 3. Remove all the if blocks for dialogs and replace with PlayerDialogManager
# The dialogs start after DynamicTopBar or PlayerBottomController
# Let's find the start: "if (showTrackSelector) {"
dialogs_pattern = re.compile(r"        if \(showTrackSelector\) \{.*        if \(showSubtitlesSheet\) \{.*?        \}", re.DOTALL)

manager_code = """        com.helpofai.videoplayer.feature.player.components.PlayerDialogManager(
            activeDialog = activeDialog,
            onDismissRequest = { activeDialog = null },
            viewModel = viewModel,
            decoderMode = decoderMode,
            onDecoderModeSelect = {
                decoderMode = it
                activeDialog = null
            },
            videoMetadata = videoMetadata,
            currentVideo = playlist.find { it.path == viewModel.currentVideoPath },
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationZ = rotationZ,
            onUpdateScale = { scale = it },
            onUpdateOffsetX = { offsetX = it },
            onUpdateOffsetY = { offsetY = it },
            onUpdateRotation = { rotationZ = it; feedbackEvent = FeedbackEvent(FeedbackType.INFO, androidx.compose.material.icons.Icons.AutoMirrored.Filled.RotateRight, "${(it % 360).toInt()}°") },
            qualityReport = qualityReport,
            isAnalyzingQuality = isAnalyzingQuality,
            playlist = playlist,
            currentVideoPath = viewModel.currentVideoPath,
            onVideoSelect = { path ->
                viewModel.playVideo(path)
                activeDialog = null
            },
            onReorderPlaylist = { from, to -> viewModel.reorderPlaylist(from, to) },
            onShowDialog = { activeDialog = it },
            isPlaying = isPlaying,
            bookmarks = bookmarks,
            onSeekTo = { pos -> 
                viewModel.videoPlayer.player.seekTo(pos)
                currentPosition = pos
                activeDialog = null
            },
            isGeneratingChapters = isGeneratingChapters,
            onGenerateAutoChapters = { onStart, onComplete ->
                viewModel.generateAutoChapters(
                    onStart = {
                        isGeneratingChapters = true
                        onStart()
                    },
                    onComplete = { success ->
                        isGeneratingChapters = false
                        onComplete(success)
                    }
                )
            },
            onLoadExternalSubtitle = { subtitlePickerLauncher.launch("*/*") },
            onFeedbackEvent = { feedbackEvent = it }
        )"""

content = dialogs_pattern.sub(manager_code, content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("PlayerScreen refactored with PlayerDialogManager.")

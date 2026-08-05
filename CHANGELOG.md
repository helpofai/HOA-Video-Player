# Changelog

## [4.1.0] - 2026-08-05

### Added
- **Signature Color Icon System:** Every tab and file type now has its own signature color (teal Home, amber Folders, indigo Files, orange Watch Party, purple Tools) via a shared `ToolIconPalette`, with a soft glow halo behind the selected bottom-nav icon.
- **Auto AI Enhancement Engine:** Per-video enhancement analyzer that samples actual frames (luminance, contrast, colorfulness) when you tap "Auto AI" for the current video, producing a tailored enhancement config persisted per video.
- **Media Scan Progress Dialog:** Live progress feedback when refreshing the library, plus pull-to-refresh on the Home page.

### Changed
- Extended the signature-color system to FileManager (folder, video, audio, subtitle, image, archive, code, PDF, document icons) and Watch Party screens.
- Edge-to-edge UI for the Tools screen.

### Fixed
- Fixed video buffering bug by restoring the original Media3 effects pipeline.
- Fixed Watch Party UI overlap by using interior Spacers instead of external padding.
- Fixed Top App Bar scroll clipping and bounds so the glass background fully syncs while scrolling.
- Fixed Explorer scroll behavior during advanced media refresh.

## [3.3.0] - 2026-07-28

### Added
- Implemented "Watch Party" and "Explorer" pages.
- Standardized UI with ultra-premium frosted glass (glassmorphism) aesthetics.

### Changed
- Increased frosted glass contrast on Top App Bar and Bottom Nav Bar (from 20% to 40% surface alpha).
- Updated top and bottom bar content to pure white for enhanced visibility.
- Refactored `HomeScreen` and Bottom Navigation to be space-efficient and full-screen.
- Automatically hide unselected labels in Bottom Navigation Bar to accommodate more items gracefully.
- Improved Top App Bar animation to fully sync glass background while scrolling out of view.
- Removed unused properties (`currentRoot`, `activeSafCurrentUri`) and updated UI styling.

### Fixed
- Fixed landscape orientation bug that occurred after returning from the video player.
- Resolved text overflow in "Watch Party" bottom navigation label.
- Fixed deprecated Material3 `AutoMirrored` icons in `FileManagerScreen`.

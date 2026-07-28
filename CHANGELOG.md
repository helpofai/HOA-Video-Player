# Changelog

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

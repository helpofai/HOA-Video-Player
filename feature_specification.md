i try to build android video player similer to like mx player, vlc player, etc. but not a simple video player, i try
to build most advance and profasonal featured video player, so now read exiting project codes and
C:\Users\rajib\Desktop\vidplay\feature_specification.md file

# Advanced Offline Android Video Player - Feature Specification

This document describes a fully offline professional Android video player. No cloud services, backend servers, user accounts, or external APIs are required.

!!! IMPORTANT FOR AI: FOLLOW THIS ARCHITECTURE RIGOROUSLY FOR EVERY FILE AND FOLDER CREATED !!!

**advanced Architecture folder and file name will features wise. files will be lightwate and reuseble**

_MANDATORY ARCHITECTURE & NAMING CONVENTIONS (CRITICAL)_

To maintain a professional, industry-grade,   and do not havy any files lightweight codebase, this project uses a **Modular Domain-Driven Architecture**. Every file must be self-descriptive and include its feature/folder name in the filename.

**1. Explicit Feature Naming Mandate**
Every filename MUST include the name of the feature (folder) it belongs to. This prevents confusion during search and prevents class name collisions.

## Core Playback Engine

- [x] Media3
- [x] Hardware decoding
- [x] Software decoding
- [x] Automatic decoder switching
- [x] Audio focus
- [x] Background playback
- [x] Playlists
- [x] Media session
- [x] PiP

## Supported Video Formats (ExoPlayer Native)

- [x] MP4
- [x] MKV
- [x] AVI
- [x] MOV
- [x] FLV
- [x] WMV
- [x] WebM
- [x] 3GP
- [x] MPEG
- [x] MPG
- [x] TS
- [x] M2TS
- [x] M4V
- [x] OGV
- [x] VOB
- [x] ASF
- [x] RMVB

## Supported Audio Formats (ExoPlayer Native)

- [x] MP3
- [x] AAC
- [x] FLAC
- [x] WAV
- [x] OGG
- [x] OPUS
- [x] M4A
- [x] AC3
- [x] EAC3
- [x] DTS

## Subtitle Support

- [x] SRT
- [x] ASS
- [x] SSA
- [x] VTT
- [x] SUB
- [x] Embedded & external subtitles
- [ ] Delay adjustment
- [ ] Font/size/color/background
- [ ] Outline/shadow
- [ ] Position
- [ ] Encoding

## Audio Controls

- [x] Audio track switching
- [x] Equalizer
- [x] Bass boost
- [x] Virtualizer
- [ ] Loudness enhancer
- [ ] Stereo balance
- [ ] Mono mode
- [ ] Audio delay

## Playback Controls

- [x] Play/Pause
- [x] Next/Previous
- [x] Fast forward/Rewind
- [ ] Frame-by-frame
- [x] 0.1x-4x speed (Full UI)
- [x] AB Repeat
- [x] Loop
- [x] Resume playback
- [x] Bookmarks
- [x] Chapter navigation

## Gesture Controls

- [x] Brightness swipe
- [x] Volume swipe
- [x] Seek swipe
- [x] Double-tap seek
- [x] Long press speed boost
- [x] Pinch zoom
- [x] Control lock

## Video Adjustments

- [ ] Brightness
- [ ] Contrast
- [ ] Saturation
- [ ] Hue
- [ ] Gamma
- [ ] Sharpness
- [x] Rotate
- [x] Mirror
- [x] Flip
- [x] Crop
- [x] Zoom
- [x] Aspect ratio

## File Manager

- [x] Browse
- [x] Rename
- [x] Delete
- [ ] Copy
- [ ] Move
- [x] Share
- [x] Folders
- [ ] Sort
- [ ] Filter
- [ ] Multi-select
- [ ] Search

## Library

- [ ] Recently played
- [x] Recently added
- [x] Favorites
- [ ] Hidden videos
- [ ] Collections
- [x] Continue watching
- [ ] Most watched

## FFmpeg Tools

- [x] Trim
- [x] Merge
- [x] Extract audio
- [x] Compress
- [x] Convert
- [x] Rotate
- [x] Reverse
- [x] Resolution change
- [x] Extract frames
- [x] GIF maker
- [x] Screenshots

## Privacy

- [x] App lock
- [x] PIN
- [ ] Fingerprint
- [ ] Private folders
- [ ] Incognito
- [x] Clear history

## UI

- [x] Material 3
- [x] Light/Dark/AMOLED
- [x] Dynamic colors
- [x] Tablet/Foldable
- [ ] Mini player
- [x] Smooth animations

## Performance

- [x] Hardware acceleration
- [x] Caching
- [x] Lazy loading
- [x] Background scanning
- [x] Optimized indexing

## Offline Smart Features

- [x] Duplicate detection
- [x] Smart categorization
- [x] Continue watching
- [x] Thumbnail selection (Coil Video Frames)
- [x] Corrupted file detection
- [x] Storage cleanup suggestions

## Architecture

- [x] Presentation (Compose UI)
- [x] Media Controller (Background Service)
- [x] Media3 Playback
- [x] FFmpeg Module
- [x] Media Scanner
- [x] Room DB
- [x] Thumbnail Cache
- [x] MediaStore
- [ ] Local Storage (File manager fallback)

**Advanced Offline AI Features for a Professional Android Video Player**
_This document describes advanced on-device intelligence features that require no backend, cloud service, user account, or external API. Features rely on local metadata analysis, heuristics, Media3, FFmpeg, Android system capabilities, and optional lightweight on-device machine learning models._

1. Smart Scene Detection
   Analyze frame changes, color histograms, motion, and transitions to detect scene boundaries. Generate scene markers, previews, and allow instant navigation between scenes.
2. Automatic Chapter Generator
   Create chapters automatically by using FFmpeg scene detection heuristics. Users can view and navigate chapters via the Bookmarks sheet. (Implemented)
3. Smart Resume Engine
   Store playback position, playback speed, subtitle, audio track, zoom level, last watched time, and estimated remaining time. Display Continue Watching cards.
4. Intelligent Thumbnail Engine (Implemented)
   Extract many candidate frames and score them by sharpness, brightness, face visibility, blur, and composition to choose the best thumbnail.
5. Video Quality Analyzer
   Calculate a quality score using resolution, bitrate, codec, FPS, HDR support, and audio quality. Present a detailed health report.
6. Video Health Report
   Detect damaged containers, unreadable frames, missing audio, subtitle issues, unsupported codecs, and playback failures.
7. Smart Content Classification
   Categorize videos into Movies, TV Shows, Anime, Tutorials, Lectures, Screen Recordings, Camera Videos, Gameplay, Music Videos, Family Videos, etc., using local metadata and filename analysis. (Implemented)
8. Smart Playlist Generator
   Automatically build playlists such as Recently Added, Continue Watching, Favorites, 4K, HDR, Kids, Travel, Music, and Short Videos.
9. Local Recommendation Engine
   Recommend content using only watch history, folder relationships, duration, playback habits, and favorites. No internet required.
10. Watching Habit Analysis
    Analyze preferred viewing times, average session length, genres, playback speed, subtitle usage, and frequently visited folders.
11. Storage Prediction
    Estimate storage consumption trends and predict when storage may become full. Suggest cleanup opportunities.
12. Advanced Duplicate Detection
    Combine file hashes, metadata, duration, bitrate, visual fingerprints, and optional audio fingerprints to identify exact and near-duplicate videos.
13. Offline Face Grouping (Optional)
    With user permission, use an on-device vision model to cluster recurring faces in personal videos. Data never leaves the device.
14. Emotion & Scene Intensity Analysis
    Estimate calm, action, dark, or energetic sections from local visual and audio characteristics for timeline visualization.
15. Audio Analyzer
    Measure speech ratio, music ratio, silence percentage, dynamic range, clipping, and channel information.
16. Subtitle Intelligence
    Detect subtitle language, fix timing offsets, remove duplicates, and improve synchronization locally.
17. Natural Metadata Search
    Support searches such as '4K videos', 'videos longer than 2 hours', 'camera videos', or 'downloads' using indexed metadata.
18. Motion Analysis
    Calculate motion intensity, camera movement, stabilization level, and action score for each video.
19. Scene Heatmap
    Display an activity heatmap across the timeline to help users jump to high-action sections.
20. Video Fingerprinting
    Generate visual, audio, and metadata fingerprints for reliable identification even if filenames change.
21. Folder Health Dashboard
    Report folder size, duplicates, corrupted files, storage usage, and health score.
22. Intelligent Timeline
    Overlay chapters, bookmarks, subtitle markers, scene changes, screenshots, and playback history on the seek bar.
23. Storage Dashboard
    Provide storage analytics by folder, category, resolution, duplicates, and unused content. (Implemented)
24. Adaptive Interface
    Reorder home sections and shortcuts based on local usage patterns while keeping all data on-device.
25. Smart Decoder Selection
    Automatically choose the best decoder based on codec support, battery usage, device capability, and playback stability.
26. Offline Learning Engine
    Learn user preferences such as subtitle language, playback speed, brightness, volume, aspect ratio, decoder preference, and sleep timer entirely on-device.

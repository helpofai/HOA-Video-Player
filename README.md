<div align="center">
  <img src="app/src/main/assets/hoa_logo_animated.svg" alt="HOA Video Player Animated Logo" width="250" />
  
  # HOA Video Player
  
  **The Most Advanced, Professional, and Feature-Rich Offline Video Player for Android.**
  
  <p align="center">
    <a href="https://android.com"><img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=for-the-badge&logo=android" alt="Platform" /></a>
    <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=for-the-badge&logo=kotlin" alt="Kotlin" /></a>
    <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?style=for-the-badge&logo=android" alt="Compose" /></a>
    <a href="#"><img src="https://img.shields.io/badge/Architecture-Clean%20MVVM-blueviolet.svg?style=for-the-badge" alt="Architecture" /></a>
    <a href="#"><img src="https://img.shields.io/badge/ExoPlayer-Media3-red.svg?style=for-the-badge" alt="Media3" /></a>
  </p>
</div>

---

**HOA Video Player** is a highly advanced, ultra-lightweight Android Video Player built entirely with Kotlin, Jetpack Compose, and Media3 ExoPlayer. Designed with a focus on immersive user experiences, it features stunning **Material 3 aesthetics**, seamless **glassmorphism overlays**, an incredibly intuitive **single-row compact control layout**, and **on-device smart features**.

This application operates 100% entirely offline. No cloud APIs, no backend, no data harvesting. Your privacy is absolutely guaranteed.

---

## ✨ Standout Features

### 🧠 Advanced AI & Smart Features
*   **Intelligent Thumbnail Engine:** Extracts multiple candidate frames across the video and uses luminance variance heuristics to automatically select the most visually engaging thumbnail.
*   **Smart Folder Library:** Features a beautiful masonry 2x2 grid displaying dynamic folder previews.
*   **Intelligent Storage Dashboard:** Real-time analysis of storage capacity, categorizing sizes natively.

### 🎬 Core Playback Experience
*   **Media3 ExoPlayer Integration:** High-performance, low-latency video playback for almost all major formats.
*   **Immersive Edge-to-Edge Display:** 90% of the screen is dedicated to the video. The System UI intelligently auto-hides.
*   **Picture-in-Picture (PiP):** Seamlessly transition to PiP mode when leaving the app.
*   **Smart Folder Navigation:** Browse and play local videos effortlessly with an integrated folder playlist system. 
*   **Auto Play Next:** Playlists are auto-queued; jump seamlessly to the next or previous video in the directory.

### 🎛 Advanced Player Capabilities
*   **A-B Repeat:** Continuously loop a specific segment of the video (Point A to Point B) for studying or detailed viewing.
*   **Persistent Bookmarks & Chapters:** Save timestamps directly to the local database. Bookmarks are retained even after the app is closed.
*   **Subtitles Support:** Load and switch between subtitle tracks effortlessly.
*   **Video Transformations:**
    *   Dynamic Resize Modes (Fit, Fill, Zoom)
    *   Video Mirroring & Flipping
    *   360-degree arbitrary Rotation (Z-axis)
*   **Gesture Controls:**
    *   **Swipe Vertical (Left):** Adjust Screen Brightness.
    *   **Swipe Vertical (Right):** Adjust Audio Volume.
    *   **Swipe Horizontal:** Seek forward and backward with precise visual feedback.
*   **Security Controls:** One-tap UI Lock button to freeze all gestures and prevent accidental touches.

### 🎨 UI & Aesthetics
*   **Compact Single-Row Controller:** `[Prev] [Play] [Next] [Time] [Slider] [Duration] [Fullscreen]` logic that saves massive amounts of screen real estate.
*   **Glassmorphism Effects:** Beautiful 80% transparent blur effects utilized throughout the UI (More Menus, Bottom Sheets, Settings, etc.).
*   **Material 3 Dynamic Theming:** Adapts to modern Android design standards with premium animations.

### 🛡 Stability & Reporting
*   **Intelligent Crash Handler:** If the app ever crashes, a beautiful custom recovery screen is launched.
*   **Rapid Bug Reporting:** Export crash logs to the clipboard, instantly send them via WhatsApp, or auto-generate a GitHub issue ticket.

---

## 🛠 Tech Stack & Architecture
This project rigorously adheres to a **Modular Domain-Driven Architecture**. Every file is self-descriptive and incredibly lightweight. 

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Edge-to-Edge Enabled)
*   **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
*   **Dependency Injection:** Hilt / Dagger
*   **Video Engine:** AndroidX Media3 (ExoPlayer) with `MediaSessionService` support
*   **Local Storage:** Room Database (for Watch History, Bookmarks, and Chapters)
*   **Image Loading:** Coil (with VideoFrameDecoder hardware support)

---

## 📦 Building from Source
1. Clone the repository.
2. Open the project in Android Studio (Jellyfish or newer recommended).
3. Let Gradle sync and resolve dependencies.
4. Run the app on an emulator or physical device.

```bash
./gradlew clean assembleDebug
```

---

## 🔒 Privacy Policy
At HOA Video Player, we respect your privacy. This application operates entirely offline and does not collect, store, or transmit any of your personal data or video metadata to any external servers. All video indexing, bookmarks, and chapters are stored securely on your local device.

## 📝 License
This project is licensed under the MIT License - see the LICENSE file for details.

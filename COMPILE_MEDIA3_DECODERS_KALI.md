# Compiling Media3 Video Decoders (AV1 & VP9) on Kali Linux

This guide provides step-by-step instructions for compiling the AndroidX Media3 software video decoders (`libgav1` and `libvpx`) natively on Kali Linux. 

These compiled `.aar` files will allow your Android app (VidPlay) to decode 4K AV1 and VP9 videos on devices where the hardware decoder fails.

---

## Step 1: Install Build Dependencies
Kali is Debian-based, so you can use `apt` to install all necessary C++ build tools.
Open your Kali terminal and run:
```bash
sudo apt update
sudo apt install -y build-essential cmake ninja-build git python3 python3-pip dos2unix wget unzip
```

## Step 2: Download the Linux NDK
Do **not** use the Windows NDK or the NDK installed via Windows Android Studio. You must download the Linux binary.
```bash
cd ~
# Download NDK r26b (Recommended for Media3)
wget https://dl.google.com/android/repository/android-ndk-r26b-linux.zip

# Unzip it
unzip android-ndk-r26b-linux.zip -d ~/android-ndk

# Export the NDK path (Add this to your ~/.zshrc or ~/.bashrc to make it permanent)
export NDK_PATH="$HOME/android-ndk/android-ndk-r26b"
```

## Step 3: Clone the Media3 Source Code
Do not use the repository cloned in Windows, as it may contain Windows line endings (`CRLF`) which break Linux bash scripts. Clone a fresh copy inside Kali:
```bash
cd ~
git clone https://github.com/androidx/media.git
cd media
```

---

## Step 4: Compiling the AV1 Decoder (libgav1)

1. Navigate to the AV1 decoder directory:
   ```bash
   cd ~/media/libraries/decoder_av1
   ```
2. Clone the `libgav1` source code into the `jni` folder:
   ```bash
   git clone https://chromium.googlesource.com/codecs/libgav1 jni/libgav1
   ```
3. Execute the build script:
   ```bash
   ./build_gav1.sh
   ```
4. **Result:** Once finished, your compiled AV1 `.aar` file will be located at:
   `~/media/libraries/decoder_av1/build/outputs/aar/`

---

## Step 5: Compiling the VP9 Decoder (libvpx)

1. Navigate to the VP9 decoder directory:
   ```bash
   cd ~/media/libraries/decoder_vp9
   ```
2. Clone the `libvpx` source code into the `jni` folder:
   ```bash
   git clone https://chromium.googlesource.com/webm/libvpx jni/libvpx
   ```
3. Execute the build script:
   ```bash
   ./build_vpx.sh
   ```
4. **Result:** Once finished, your compiled VP9 `.aar` file will be located at:
   `~/media/libraries/decoder_vp9/build/outputs/aar/`

---

## Step 6: Importing the Decoders into VidPlay

1. Copy the `.aar` files from your Kali machine into your VidPlay project folder. 
   *(If Kali is running via WSL, you can copy them directly to your Windows C: drive like this:)*
   ```bash
   cp ~/media/libraries/decoder_av1/build/outputs/aar/*.aar /mnt/c/Users/rajib/Desktop/vidplay/app/libs/
   cp ~/media/libraries/decoder_vp9/build/outputs/aar/*.aar /mnt/c/Users/rajib/Desktop/vidplay/app/libs/
   ```
   *(If Kali is on a separate machine, use a USB drive, SCP, or a shared folder to move the `.aar` files to `C:\Users\rajib\Desktop\vidplay\app\libs\` on your Windows PC).*

2. Open `build.gradle.kts` in your VidPlay Android Studio project and uncomment/add the decoders:
   ```kotlin
   implementation("androidx.media3:media3-decoder-av1:1.10.1")
   implementation("androidx.media3:media3-decoder-vp9:1.10.1")
   ```
3. Click **Sync Now** in Android Studio.

---

## Troubleshooting Guide

### Error: `\r: command not found` or `syntax error near unexpected token`
**Cause:** You are trying to run a bash script that has Windows (`CRLF`) line endings.
**Fix:** Convert it to Unix format using `dos2unix`.
```bash
dos2unix build_gav1.sh
dos2unix build_vpx.sh
```

### Error: `NDK_PATH not set`
**Cause:** The build script cannot find your NDK.
**Fix:** Verify you exported the path correctly. Run `echo $NDK_PATH`. If it is empty, re-run:
```bash
export NDK_PATH="$HOME/android-ndk/android-ndk-r26b"
```

### Error: `Out of memory` (Process killed)
**Cause:** Compiling `libvpx` for 4 different CPU architectures simultaneously uses massive amounts of RAM.
**Fix:** Ensure Kali has at least 8GB of RAM allocated. Close background applications, or modify `build_vpx.sh` to compile with fewer parallel threads.

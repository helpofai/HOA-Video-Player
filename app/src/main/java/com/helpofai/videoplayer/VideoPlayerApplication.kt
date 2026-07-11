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
package com.helpofai.videoplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoPlayerApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        com.helpofai.videoplayer.core.ads.AdManager.init(this)
        com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance().init(this)
        
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            val stackTrace = android.util.Log.getStackTraceString(exception)
            val pm = packageManager
            val pInfo = pm.getPackageInfo(packageName, 0)
            val appVersion = "${pInfo.versionName} (${if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode})"
            val deviceInfo = """
                Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
                App Version: $appVersion
            """.trimIndent()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val crashTime = dateFormat.format(java.util.Date())

            val intent = android.content.Intent(this, com.helpofai.videoplayer.feature.crash.CrashReportActivity::class.java).apply {
                putExtra("CRASH_LOG", stackTrace)
                putExtra("DEVICE_INFO", deviceInfo)
                putExtra("CRASH_TIME", crashTime)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            // Brief delay so the crash activity can start before the process dies
            try { Thread.sleep(300) } catch (_: InterruptedException) {}
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("advanced_image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB High-Performance Cache
                    .build()
            }
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .dispatcher(kotlinx.coroutines.Dispatchers.IO.limitedParallelism(2))
            .crossfade(true)
            .build()
    }
}
package com.helpofai.videoplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoPlayerApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
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
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

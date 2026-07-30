package com.helpofai.videoplayer.core.media

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Emits a Unit every time the MediaStore reports a change to video files.
     * Uses conflate() to debounce rapid consecutive events (e.g. during a bulk download).
     */
    fun observeMediaChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                trySend(Unit)
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true, // Notify for descendants
            observer
        )

        // Initial emission to trigger the first load
        trySend(Unit)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()
}

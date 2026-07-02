package com.helpofai.videoplayer.core.playback.di

import com.helpofai.videoplayer.core.playback.ExoPlayerImpl
import com.helpofai.videoplayer.core.playback.VideoPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {

    @Binds
    @Singleton
    abstract fun bindVideoPlayer(
        exoPlayerImpl: ExoPlayerImpl
    ): VideoPlayer
}

package com.mundus.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.mundus.core.model.PlayerSettings

/**
 * Builds an [ExoPlayer] wired to the user's [PlayerSettings]. This is where the
 * classic IPTV "buffer" / timeout / user-agent knobs actually take effect.
 */
@OptIn(UnstableApi::class)
object PlayerFactory {

    fun create(context: Context, raw: PlayerSettings): ExoPlayer {
        val settings = PlayerSettings.sanitize(raw)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                settings.minBufferMs,
                settings.maxBufferMs,
                settings.bufferForPlaybackMs,
                settings.bufferForPlaybackAfterRebufferMs,
            )
            .build()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(settings.userAgent)
            .setConnectTimeoutMs(settings.connectTimeoutMs)
            .setReadTimeoutMs(settings.readTimeoutMs)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }
}

package com.mundus.core.model

import kotlinx.serialization.Serializable

/**
 * User-tunable playback / buffering settings, mirroring the classic IPTV player
 * options (TiViMate "Playback settings"). These map onto a Media3 `LoadControl`
 * and `ExoPlayer` configuration in [com.mundus.player.PlayerFactory].
 */
@Serializable
data class PlayerSettings(
    /** Minimum buffer to keep, in milliseconds, before playback can start/continue. */
    val minBufferMs: Int = 15_000,

    /** Maximum buffer to hold in memory, in milliseconds. Higher = smoother, more RAM. */
    val maxBufferMs: Int = 50_000,

    /** Buffer required after a user-initiated seek/zap, in milliseconds. */
    val bufferForPlaybackMs: Int = 2_500,

    /** Buffer required to resume after a rebuffer, in milliseconds. */
    val bufferForPlaybackAfterRebufferMs: Int = 5_000,

    /** Connection timeout for the HTTP data source, in milliseconds. */
    val connectTimeoutMs: Int = 8_000,

    /** Read timeout for the HTTP data source, in milliseconds. */
    val readTimeoutMs: Int = 8_000,

    /** Preferred user-agent sent to stream servers (some panels require a specific one). */
    val userAgent: String = "Mundus/0.1 (Android)",

    /** Automatically reconnect the stream on error. */
    val autoReconnect: Boolean = true,

    /** Hardware decoding preference; false forces software decoders for tricky streams. */
    val preferHardwareDecoding: Boolean = true,
) {
    companion object {
        val Default = PlayerSettings()

        /** Guard rails so a bad value can't wedge the LoadControl. */
        fun sanitize(s: PlayerSettings): PlayerSettings {
            val minB = s.minBufferMs.coerceIn(2_000, 120_000)
            val maxB = s.maxBufferMs.coerceIn(minB, 600_000)
            return s.copy(
                minBufferMs = minB,
                maxBufferMs = maxB,
                bufferForPlaybackMs = s.bufferForPlaybackMs.coerceIn(500, minB),
                bufferForPlaybackAfterRebufferMs =
                    s.bufferForPlaybackAfterRebufferMs.coerceIn(1_000, maxB),
                connectTimeoutMs = s.connectTimeoutMs.coerceIn(2_000, 60_000),
                readTimeoutMs = s.readTimeoutMs.coerceIn(2_000, 60_000),
            )
        }
    }
}

package com.mundus.core.model

import kotlinx.serialization.Serializable

/**
 * User-tunable playback / buffering settings.
 *
 * Buffer knobs are exposed as simple **1→10 levels** (easier to dial in than raw
 * milliseconds). The concrete millisecond values fed to Media3's `LoadControl` are
 * derived from those levels — see the computed properties below.
 *
 *  - [minBufferLevel]  : how much to keep buffered before playback continues.
 *  - [maxBufferLevel]  : how much to hold in memory (higher = smoother, more RAM).
 *  - [latencyLevel]    : how much to pre-buffer before starting / after a stall.
 *                        Higher = smoother but more start delay; lower = snappier
 *                        zapping but more rebuffer risk. Tune per source (Vavoo/Huhu/
 *                        Kool often need a higher level to feel like "real" IPTV).
 */
@Serializable
data class PlayerSettings(
    val minBufferLevel: Int = 4,
    val maxBufferLevel: Int = 6,
    val latencyLevel: Int = 4,

    val connectTimeoutMs: Int = 8_000,
    val readTimeoutMs: Int = 8_000,
    val userAgent: String = "Mundus/0.1 (Android)",
    val autoReconnect: Boolean = true,
    val preferHardwareDecoding: Boolean = true,
) {
    val minBufferMs: Int get() = lerp(minBufferLevel, 2_000, 60_000)

    /** Always kept at least a bit above the minimum. */
    val maxBufferMs: Int get() = maxOf(lerp(maxBufferLevel, 15_000, 300_000), minBufferMs + 5_000)

    val bufferForPlaybackMs: Int get() = lerp(latencyLevel, 700, 8_000)

    val bufferForPlaybackAfterRebufferMs: Int get() = lerp(latencyLevel, 1_500, 15_000)

    companion object {
        val Default = PlayerSettings()

        /** Linear map of a 1..10 [level] onto [min]..[max]. */
        fun lerp(level: Int, min: Int, max: Int): Int {
            val l = level.coerceIn(1, 10)
            return min + (max - min) * (l - 1) / 9
        }

        fun sanitize(s: PlayerSettings): PlayerSettings = s.copy(
            minBufferLevel = s.minBufferLevel.coerceIn(1, 10),
            maxBufferLevel = s.maxBufferLevel.coerceIn(1, 10),
            latencyLevel = s.latencyLevel.coerceIn(1, 10),
            connectTimeoutMs = s.connectTimeoutMs.coerceIn(2_000, 60_000),
            readTimeoutMs = s.readTimeoutMs.coerceIn(2_000, 60_000),
        )
    }
}

package com.mundus.core.model

/**
 * A single EPG programme (one cell in the TiViMate-style timeline grid).
 * Times are epoch milliseconds (UTC).
 */
data class Programme(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startMs: Long,
    val stopMs: Long,
    val category: String? = null,
) {
    val durationMs: Long get() = (stopMs - startMs).coerceAtLeast(0)

    fun isLiveAt(nowMs: Long): Boolean = nowMs in startMs until stopMs

    /** 0f..1f progress of this programme at [nowMs]. */
    fun progressAt(nowMs: Long): Float {
        if (durationMs == 0L) return 0f
        return ((nowMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
    }
}

/** All programmes for a single EPG channel id, sorted by start time. */
data class ChannelGuide(
    val epgChannelId: String,
    val programmes: List<Programme>,
) {
    fun nowPlaying(nowMs: Long): Programme? = programmes.firstOrNull { it.isLiveAt(nowMs) }
}

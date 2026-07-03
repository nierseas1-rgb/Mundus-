package com.mundus.data.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal subset of the Xtream Codes `player_api.php` responses that we consume. */

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
)

@Serializable
data class XtreamLiveStream(
    @SerialName("num") val num: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("stream_id") val streamId: Long = 0,
    @SerialName("stream_icon") val streamIcon: String = "",
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String = "",
)

@Serializable
data class XtreamVodStream(
    @SerialName("num") val num: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("stream_id") val streamId: Long = 0,
    @SerialName("stream_icon") val streamIcon: String = "",
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("container_extension") val containerExtension: String = "mp4",
)

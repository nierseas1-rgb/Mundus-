package com.mundus.core.model

/**
 * A group of channels within a [Section] (e.g. "Sport", "Cinéma FR").
 * Categories from multiple sources sharing the same name are merged.
 */
data class Category(
    val id: String,
    val name: String,
    val section: Section,
    val channelCount: Int = 0,
)

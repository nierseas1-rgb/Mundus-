package com.mundus.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.mundus.core.model.Section

/**
 * Per-section visual identity. Each [Section] carries its own accent + colour scheme
 * so the whole app re-skins when you move between TV, Films, Séries and Animés —
 * the "personnalisation de thème en fonction de la section" requirement.
 *
 * All schemes are dark-first (the norm for a 10-foot / TV UI) but differ in hue,
 * accent and mood.
 */
data class SectionTheme(
    val accent: Color,
    val accentSoft: Color,
    val colorScheme: ColorScheme,
    val gradient: List<Color>,
)

object SectionThemes {

    private fun scheme(
        primary: Color,
        secondary: Color,
        background: Color,
        surface: Color,
    ): ColorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF08090C),
        secondary = secondary,
        onSecondary = Color(0xFF08090C),
        background = background,
        onBackground = Color(0xFFECEFF4),
        surface = surface,
        onSurface = Color(0xFFECEFF4),
        surfaceVariant = surface.copy(alpha = 0.7f),
        onSurfaceVariant = Color(0xFFB9C0CC),
        outline = Color(0xFF3A4150),
    )

    val liveTv = SectionTheme(
        accent = Color(0xFF35C4F0),
        accentSoft = Color(0xFF1C6C86),
        gradient = listOf(Color(0xFF0A1622), Color(0xFF0C2233)),
        colorScheme = scheme(
            primary = Color(0xFF35C4F0),
            secondary = Color(0xFF4DE1C1),
            background = Color(0xFF0A1017),
            surface = Color(0xFF121B26),
        ),
    )

    val movies = SectionTheme(
        accent = Color(0xFFE7B44C),
        accentSoft = Color(0xFF7A5A1E),
        gradient = listOf(Color(0xFF1A0E0E), Color(0xFF2A1412)),
        colorScheme = scheme(
            primary = Color(0xFFE7B44C),
            secondary = Color(0xFFD9534F),
            background = Color(0xFF130C0C),
            surface = Color(0xFF201414),
        ),
    )

    val series = SectionTheme(
        accent = Color(0xFFB08CF0),
        accentSoft = Color(0xFF5B4590),
        gradient = listOf(Color(0xFF140E1F), Color(0xFF1E1330)),
        colorScheme = scheme(
            primary = Color(0xFFB08CF0),
            secondary = Color(0xFF7C6BF0),
            background = Color(0xFF100C17),
            surface = Color(0xFF1A1426),
        ),
    )

    val anime = SectionTheme(
        accent = Color(0xFFFF6FB5),
        accentSoft = Color(0xFF8E3564),
        gradient = listOf(Color(0xFF1B0E17), Color(0xFF2A1224)),
        colorScheme = scheme(
            primary = Color(0xFFFF6FB5),
            secondary = Color(0xFF6FE0FF),
            background = Color(0xFF140A11),
            surface = Color(0xFF21121B),
        ),
    )

    fun of(section: Section): SectionTheme = when (section) {
        Section.LIVE_TV -> liveTv
        Section.MOVIES -> movies
        Section.SERIES -> series
        Section.ANIME -> anime
    }
}

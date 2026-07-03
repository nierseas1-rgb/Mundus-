package com.mundus.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mundus.core.model.Section

/** Access the current [SectionTheme] anywhere in the composition. */
val LocalSectionTheme = staticCompositionLocalOf { SectionThemes.liveTv }

/**
 * Applies the theme for [section]. Key colours are animated so switching sections
 * feels like the whole app smoothly changes identity rather than hard-cutting.
 */
@Composable
fun MundusTheme(
    section: Section,
    content: @Composable () -> Unit,
) {
    val target = SectionThemes.of(section)
    val base = target.colorScheme

    val animSpec = tween<Color>(durationMillis = 450)
    val primary by animateColorAsState(base.primary, animSpec, label = "primary")
    val background by animateColorAsState(base.background, animSpec, label = "background")
    val surface by animateColorAsState(base.surface, animSpec, label = "surface")

    val animated = base.copy(primary = primary, background = background, surface = surface)

    CompositionLocalProvider(LocalSectionTheme provides target) {
        MaterialTheme(
            colorScheme = animated,
            typography = MundusTypography,
            content = content,
        )
    }
}

private val MundusTypography = Typography()

package com.liam.appstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WerkstattLightScheme = lightColorScheme(
    primary = WerkstattColors.Terracotta,
    onPrimary = WerkstattColors.CardCream,
    secondary = WerkstattColors.Sage,
    onSecondary = WerkstattColors.TextDark,
    background = WerkstattColors.Cream,
    onBackground = WerkstattColors.TextDark,
    surface = WerkstattColors.CardCream,
    onSurface = WerkstattColors.TextDark,
    surfaceVariant = WerkstattColors.CardNeutral,
    onSurfaceVariant = WerkstattColors.TextMuted,
    outline = WerkstattColors.Divider,
    error = WerkstattColors.TerracottaDark
)

// Der Prototyp definiert nur ein warmes, helles Stimmungsbild. Dunkelmodus
// nutzt dasselbe Farbgerüst, nur abgedunkelt statt einer eigenen Palette.
private val WerkstattDarkScheme = darkColorScheme(
    primary = WerkstattColors.Terracotta,
    onPrimary = WerkstattColors.CardCream,
    secondary = WerkstattColors.Sage,
    onSecondary = WerkstattColors.CardCream,
    background = androidx.compose.ui.graphics.Color(0xFF211D18),
    onBackground = WerkstattColors.Cream,
    surface = androidx.compose.ui.graphics.Color(0xFF2C2620),
    onSurface = WerkstattColors.Cream,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF362F27),
    onSurfaceVariant = WerkstattColors.PlaceholderLight,
    outline = androidx.compose.ui.graphics.Color(0xFF4A4136)
)

@Composable
fun LiamsAppstoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WerkstattDarkScheme else WerkstattLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WerkstattTypography,
        content = content
    )
}

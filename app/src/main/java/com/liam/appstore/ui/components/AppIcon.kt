package com.liam.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.liam.appstore.ui.theme.WerkstattColors

/**
 * Farbiges Icon-Quadrat mit Anfangsbuchstaben, wie im Prototyp (G/K/P/T/W/R).
 * Lädt iconUrl, sobald vorhanden — sonst bleibt der Platzhalter mit Buchstabe.
 */
@Composable
fun AppIconAvatar(
    name: String,
    iconUrl: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val bg = avatarColorFor(name)
    Box(
        modifier = modifier
            .size(size)
            .background(bg, RoundedCornerShape((size.value * 0.32f).dp))
    ) {
        if (iconUrl.isNotBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = WerkstattColors.TextDark,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun avatarColorFor(name: String): androidx.compose.ui.graphics.Color {
    val palette = listOf(WerkstattColors.AvatarSage, WerkstattColors.AvatarPeach, WerkstattColors.SageBgStrong, WerkstattColors.PeachBadge)
    val index = (name.hashCode().let { if (it < 0) -it else it }) % palette.size
    return palette[index]
}

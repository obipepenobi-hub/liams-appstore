package com.liam.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liam.appstore.ui.theme.WerkstattColors

/** Gefüllter Pill-Button — Terrakotta, für die wichtigste Aktion (Installieren/Update). */
@Composable
fun FilledPillButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (enabled) WerkstattColors.Terracotta else WerkstattColors.PlaceholderLight
    Text(
        text = text,
        color = WerkstattColors.CardCream,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(bg, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp)
    )
}

/** Umrandeter Pill-Button — für sekundäre Aktionen (Ansehen, Abbrechen, Öffnen). */
@Composable
fun OutlinePillButton(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = WerkstattColors.Divider,
    textColor: Color = WerkstattColors.TextDark,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp)
    )
}

/** Kleine Pill-Variante für Listenzeilen (Installieren/Öffnen/Update in App-Reihen). */
@Composable
fun SmallStatusPill(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (filled) {
        Text(
            text = text,
            color = WerkstattColors.CardCream,
            style = MaterialTheme.typography.labelMedium,
            modifier = modifier
                .background(WerkstattColors.Terracotta, CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        )
    } else {
        Text(
            text = text,
            color = WerkstattColors.TextDark,
            style = MaterialTheme.typography.labelMedium,
            modifier = modifier
                .border(1.dp, WerkstattColors.Divider, CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

package com.liam.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.ui.theme.WerkstattColors
import java.util.Locale

fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return String.format(Locale.GERMANY, "%.1f MB", mb)
}

@Composable
fun AppListRow(
    entry: AppEntry,
    state: AppState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        AppIconAvatar(name = entry.name, iconUrl = entry.iconUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                color = WerkstattColors.TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "von ${entry.author} · ${entry.version}",
                style = MaterialTheme.typography.bodyMedium,
                color = WerkstattColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val label = when (state) {
            AppState.NOT_INSTALLED -> "Installieren"
            AppState.UPDATE_AVAILABLE -> "Update"
            AppState.UP_TO_DATE -> "Öffnen"
        }
        val filled = state != AppState.UP_TO_DATE
        SmallStatusPill(text = label, filled = filled, onClick = onAction, modifier = Modifier.width(104.dp))
    }
}

@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) WerkstattColors.Terracotta else WerkstattColors.CardCream
    val fg = if (selected) WerkstattColors.CardCream else WerkstattColors.TextDark
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

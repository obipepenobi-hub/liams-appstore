package com.liam.appstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.ui.components.Badge
import com.liam.appstore.ui.components.FilledPillButton
import com.liam.appstore.ui.components.formatSize
import com.liam.appstore.ui.theme.WerkstattColors

@Composable
fun AppDetailScreen(
    entry: AppEntry,
    state: AppState,
    onBack: () -> Unit,
    onAction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(WerkstattColors.Cream)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = WerkstattColors.TextDark)
                    }
                    Badge(text = "TEASER · 0:24 · VON ${entry.author.uppercase()}")
                    Spacer(Modifier.height(8.dp))
                    Text(entry.name, style = MaterialTheme.typography.headlineLarge, color = WerkstattColors.TextDark)
                }
            }

            if (entry.screenshots.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entry.screenshots.forEach { label ->
                            ScreenshotThumb(label)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val actionLabel = when (state) {
                        AppState.NOT_INSTALLED -> "Installieren · ${formatSize(entry.sizeBytes)}"
                        AppState.UPDATE_AVAILABLE -> "Update · ${formatSize(entry.sizeBytes)}"
                        AppState.UP_TO_DATE -> "Öffnen"
                    }
                    FilledPillButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Signatur geprüft · ${entry.version} · APK aus deiner Werkstatt",
                        style = MaterialTheme.typography.bodySmall,
                        color = WerkstattColors.TextMuted
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(value = formatSize(entry.sizeBytes), label = "PAKET", modifier = Modifier.weight(1f))
                        StatTile(value = "API ${entry.minSdk}", label = "ZIEL", modifier = Modifier.weight(1f))
                        StatTile(value = "Geprüft", label = "SIGNATUR", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))

                    if (entry.description.isNotBlank()) {
                        SectionHeading("Worum es geht")
                        Text(entry.description, style = MaterialTheme.typography.bodyLarge, color = WerkstattColors.TextDark)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            entry.tags.forEach { tag -> Badge(text = tag, background = WerkstattColors.CardNeutral) }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    if (entry.permissions.isNotEmpty()) {
                        SectionHeading("Zugriffsrechte")
                        entry.permissions.forEach { permission ->
                            Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .width(6.dp)
                                        .height(6.dp)
                                        .background(WerkstattColors.Terracotta, CircleShape)
                                )
                                Text(permission.label, style = MaterialTheme.typography.bodyLarge, color = WerkstattColors.TextDark)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    entry.changelog.firstOrNull()?.let { change ->
                        SectionHeading("Neu in ${change.version}")
                        Text(change.date, style = MaterialTheme.typography.bodySmall, color = WerkstattColors.TextMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(change.notes, style = MaterialTheme.typography.bodyLarge, color = WerkstattColors.TextDark)
                        Spacer(Modifier.height(24.dp))
                    }

                    if (entry.reviews.isNotEmpty()) {
                        SectionHeading("Was die Runde sagt")
                        entry.reviews.forEach { review ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WerkstattColors.SageBg, RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(28.dp)
                                            .background(WerkstattColors.SageBgStrong, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(review.initials, style = MaterialTheme.typography.labelSmall, color = WerkstattColors.TextDark)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(review.author, style = MaterialTheme.typography.titleSmall, color = WerkstattColors.TextDark)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(review.quote, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextDark)
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenshotThumb(label: String) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .background(WerkstattColors.CardNeutral, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(WerkstattColors.PlaceholderLight, RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = WerkstattColors.TextMuted)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(WerkstattColors.CardCream, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = WerkstattColors.TextDark)
        Text(label, style = MaterialTheme.typography.labelSmall, color = WerkstattColors.TextMuted)
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        color = WerkstattColors.TextDark,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

package com.liam.appstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.data.StoreManifest
import com.liam.appstore.ui.components.AppIconAvatar
import com.liam.appstore.ui.components.FilledPillButton
import com.liam.appstore.ui.components.SmallStatusPill
import com.liam.appstore.ui.components.formatSize
import com.liam.appstore.ui.theme.WerkstattColors

@Composable
fun ShelfScreen(
    manifest: StoreManifest?,
    appStates: Map<String, AppState>,
    onOpenApp: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onUpdateAll: () -> Unit
) {
    val apps = manifest?.apps.orEmpty()
    val installed = apps.filter { (appStates[it.id] ?: AppState.NOT_INSTALLED) != AppState.NOT_INSTALLED }
    val needingUpdate = installed.filter { appStates[it.id] == AppState.UPDATE_AVAILABLE }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WerkstattColors.Cream),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 24.dp)
    ) {
        item {
            Text("Meine Apps", style = MaterialTheme.typography.headlineLarge, color = WerkstattColors.TextDark)
            Text(
                "${installed.size} installiert · ${needingUpdate.size} Updates offen",
                style = MaterialTheme.typography.bodyMedium,
                color = WerkstattColors.TextMuted
            )
            Spacer(Modifier.height(16.dp))
        }

        if (needingUpdate.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WerkstattColors.PeachBadge, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Updates bereit", style = MaterialTheme.typography.titleLarge, color = WerkstattColors.TextDark)
                        FilledPillButton(text = "Alle laden", onClick = onUpdateAll)
                    }
                    Spacer(Modifier.height(12.dp))
                    needingUpdate.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            AppIconAvatar(name = entry.name, iconUrl = entry.iconUrl, size = 36.dp)
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall, color = WerkstattColors.TextDark)
                                Text("Auf ${entry.version} · ${formatSize(entry.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = WerkstattColors.TextMuted)
                            }
                            SmallStatusPill(text = "Update", filled = false, onClick = { onAction(entry) })
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        items(installed, key = { it.id }) { entry ->
            val state = appStates[entry.id] ?: AppState.NOT_INSTALLED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApp(entry) }
                    .padding(vertical = 10.dp)
            ) {
                AppIconAvatar(name = entry.name, iconUrl = entry.iconUrl)
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium, color = WerkstattColors.TextDark)
                    val subtitle = if (state == AppState.UPDATE_AVAILABLE) "Update auf ${entry.version} bereit" else "Aktuell · ${entry.version}"
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
                }
                val label = if (state == AppState.UPDATE_AVAILABLE) "Update" else "Öffnen"
                SmallStatusPill(text = label, filled = false, onClick = { onAction(entry) })
            }
        }

        if (installed.isEmpty()) {
            item {
                Text(
                    "Noch nichts installiert. Schau in der Werkstatt vorbei.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WerkstattColors.TextMuted
                )
            }
        }
    }
}

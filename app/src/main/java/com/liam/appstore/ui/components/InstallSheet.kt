package com.liam.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.DownloadProgress
import com.liam.appstore.ui.theme.WerkstattColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallConfirmSheet(
    entry: AppEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = WerkstattColors.CardCream) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Badge(text = "UNBEKANNTE QUELLE · APK")
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${entry.name} installieren?",
                style = MaterialTheme.typography.headlineSmall,
                color = WerkstattColors.TextDark
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Liams Appstore installiert das Paket direkt. Die App bekommt diese Rechte:",
                style = MaterialTheme.typography.bodyLarge,
                color = WerkstattColors.TextMuted
            )
            Spacer(Modifier.height(16.dp))
            entry.permissions.forEach { permission ->
                PermissionLine(permission.label)
            }
            if (entry.permissions.isEmpty()) {
                PermissionLine("Keine besonderen Rechte")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${formatSize(entry.sizeBytes)} · ${entry.version} · Signatur ungeprüft (aus deiner Werkstatt)",
                style = MaterialTheme.typography.bodySmall,
                color = WerkstattColors.TextMuted
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinePillButton(text = "Abbrechen", modifier = Modifier.weight(1f), onClick = onDismiss)
                FilledPillButton(text = "Installieren", modifier = Modifier.weight(1f), onClick = onConfirm)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DownloadProgressSheet(entry: AppEntry, progress: DownloadProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WerkstattColors.CardCream, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(24.dp)
    ) {
        Text(
            text = "${entry.name} wird geladen …",
            style = MaterialTheme.typography.titleLarge,
            color = WerkstattColors.TextDark
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress.fraction },
            color = WerkstattColors.Terracotta,
            trackColor = WerkstattColors.Divider,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${formatSize(progress.bytesDone)} von ${formatSize(progress.bytesTotal)}",
            style = MaterialTheme.typography.bodyMedium,
            color = WerkstattColors.TextMuted
        )
    }
}

@Composable
private fun PermissionLine(label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .padding(end = 10.dp)
                .width(6.dp)
                .height(6.dp)
                .background(WerkstattColors.Terracotta, CircleShape)
        ) {}
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = WerkstattColors.TextDark)
    }
}

@Composable
fun Badge(text: String, background: androidx.compose.ui.graphics.Color = WerkstattColors.PeachBadge) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = WerkstattColors.TerracottaDark,
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun InstalledToast(message: String) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .background(WerkstattColors.TextDark, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text = "●  $message", color = WerkstattColors.Cream, style = MaterialTheme.typography.labelLarge)
    }
}

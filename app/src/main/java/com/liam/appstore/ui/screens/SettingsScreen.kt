package com.liam.appstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.liam.appstore.BuildConfig
import com.liam.appstore.ui.components.FilledPillButton
import com.liam.appstore.ui.theme.WerkstattColors

data class SettingsState(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
    val wifiOnly: Boolean,
    val autoLoadUpdates: Boolean,
    val showTestBuilds: Boolean,
    val selfUpdateEnabled: Boolean,
    val friends: List<String>
)

@Composable
fun SettingsScreen(
    state: SettingsState,
    onSaveSource: (owner: String, repo: String, branch: String) -> Unit,
    onSaveToken: (String) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onToggleAutoLoad: (Boolean) -> Unit,
    onToggleTestBuilds: (Boolean) -> Unit,
    onToggleSelfUpdate: (Boolean) -> Unit,
    onCheckSelfUpdate: () -> Unit
) {
    var owner by remember(state.owner) { mutableStateOf(state.owner) }
    var repo by remember(state.repo) { mutableStateOf(state.repo) }
    var branch by remember(state.branch) { mutableStateOf(state.branch) }
    var token by remember(state.token) { mutableStateOf(state.token) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WerkstattColors.Cream),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 32.dp)
    ) {
        item {
            Text("Deine Werkstatt", style = MaterialTheme.typography.headlineLarge, color = WerkstattColors.TextDark)
            Text("$owner/$repo · ${state.friends.size} Freunde", style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
            Spacer(Modifier.height(20.dp))
        }

        item {
            SettingsCard(title = "Store-Quelle (GitHub)") {
                LabeledField("Owner", owner) { owner = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("Repo", repo) { repo = it }
                Spacer(Modifier.height(10.dp))
                LabeledField("Branch", branch) { branch = it }
                Spacer(Modifier.height(14.dp))
                FilledPillButton(text = "Speichern", onClick = { onSaveSource(owner, repo, branch) })
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            SettingsCard(title = "GitHub-Token (nur für private Repos)") {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    placeholder = { Text("ghp_...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                FilledPillButton(text = "Token speichern", onClick = { onSaveToken(token) })
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            ToggleRow(
                title = "Nur über WLAN",
                subtitle = "Große Pakete warten aufs Netz",
                checked = state.wifiOnly,
                onCheckedChange = onToggleWifiOnly
            )
            ToggleRow(
                title = "Updates automatisch laden",
                subtitle = "Sonst fragt Liams Appstore vorher",
                checked = state.autoLoadUpdates,
                onCheckedChange = onToggleAutoLoad
            )
            ToggleRow(
                title = "Test-Builds anzeigen",
                subtitle = "Halb fertige Sachen der Freunde",
                checked = state.showTestBuilds,
                onCheckedChange = onToggleTestBuilds
            )
            ToggleRow(
                title = "Store-Selbstupdate",
                subtitle = "Liams Appstore aktualisiert sich selbst über GitHub Releases",
                checked = state.selfUpdateEnabled,
                onCheckedChange = onToggleSelfUpdate
            )
            Spacer(Modifier.height(6.dp))
            FilledPillButton(text = "Jetzt nach Store-Update suchen", onClick = onCheckSelfUpdate)
            Spacer(Modifier.height(20.dp))
        }

        if (state.friends.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WerkstattColors.SageBg, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Text("Freundeskreis", style = MaterialTheme.typography.headlineSmall, color = WerkstattColors.TextDark)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.friends.forEach { friend ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(WerkstattColors.CardCream, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(friend.take(2).uppercase(), style = MaterialTheme.typography.labelSmall, color = WerkstattColors.Sage)
                                Spacer(Modifier.width(6.dp))
                                Text(friend, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextDark)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        item {
            Text(
                "Liams Appstore ${BuildConfig.VERSION_NAME} · Installationen laufen über die System-Abfrage „Unbekannte Quelle“. Nichts wird an Dritte gesendet.",
                style = MaterialTheme.typography.bodySmall,
                color = WerkstattColors.TextMuted
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WerkstattColors.CardCream, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = WerkstattColors.TextDark)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = WerkstattColors.CardNeutral,
    unfocusedContainerColor = WerkstattColors.CardNeutral,
    focusedBorderColor = WerkstattColors.Divider,
    unfocusedBorderColor = WerkstattColors.Divider
)

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(WerkstattColors.CardCream, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = WerkstattColors.TextDark)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = WerkstattColors.Terracotta,
                checkedThumbColor = WerkstattColors.CardCream
            )
        )
    }
    Spacer(Modifier.height(10.dp))
}

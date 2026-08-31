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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.data.StoreManifest
import com.liam.appstore.ui.components.AppListRow
import com.liam.appstore.ui.theme.WerkstattColors

@Composable
fun SearchScreen(
    manifest: StoreManifest?,
    appStates: Map<String, AppState>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenApp: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val apps = manifest?.apps.orEmpty()
    val filtered = if (query.isBlank()) apps else apps.filter {
        it.name.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WerkstattColors.Cream),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Builds und Freunde suchen") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = WerkstattColors.TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WerkstattColors.CardCream,
                    unfocusedContainerColor = WerkstattColors.CardCream,
                    focusedBorderColor = WerkstattColors.Divider,
                    unfocusedBorderColor = WerkstattColors.Divider
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Alle ${apps.size} Builds", style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
            Spacer(Modifier.height(6.dp))
        }

        items(filtered, key = { it.id }) { entry ->
            AppListRow(
                entry = entry,
                state = appStates[entry.id] ?: AppState.NOT_INSTALLED,
                onClick = { onOpenApp(entry) },
                onAction = { onAction(entry) }
            )
        }

        if (manifest != null && manifest.categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Kategorien", style = MaterialTheme.typography.headlineSmall, color = WerkstattColors.TextDark)
                Spacer(Modifier.height(10.dp))
            }
            items(manifest.categories) { category ->
                val count = apps.count { it.category == category.id }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCategory(category.id) }
                        .background(WerkstattColors.CardCream, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(WerkstattColors.PeachBadge, CircleShape)
                            .padding(10.dp)
                    ) {
                        Text(String.format("%02d", count), style = MaterialTheme.typography.labelMedium, color = WerkstattColors.TerracottaDark)
                    }
                    Spacer(Modifier.height(0.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium, color = WerkstattColors.TextDark)
                        if (category.subtitle.isNotBlank()) {
                            Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = WerkstattColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

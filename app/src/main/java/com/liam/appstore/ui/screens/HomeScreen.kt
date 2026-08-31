package com.liam.appstore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.data.StoreManifest
import com.liam.appstore.ui.CatalogUiState
import com.liam.appstore.ui.components.AppListRow
import com.liam.appstore.ui.components.Badge
import com.liam.appstore.ui.components.CategoryChip
import com.liam.appstore.ui.components.FilledPillButton
import com.liam.appstore.ui.components.OutlinePillButton
import com.liam.appstore.ui.components.formatSize
import com.liam.appstore.ui.theme.WerkstattColors

@Composable
fun HomeScreen(
    catalogState: CatalogUiState,
    appStates: Map<String, AppState>,
    selectedCategory: String?,
    friendsCount: Int,
    onSelectCategory: (String?) -> Unit,
    onOpenApp: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(WerkstattColors.Cream)) {
        when (catalogState) {
            is CatalogUiState.Loading -> CircularProgressIndicator(
                color = WerkstattColors.Terracotta,
                modifier = Modifier.align(Alignment.Center)
            )
            is CatalogUiState.Failed -> CatalogError(catalogState.message, onRetry)
            is CatalogUiState.Loaded -> HomeContent(
                manifest = catalogState.manifest,
                appStates = appStates,
                selectedCategory = selectedCategory,
                friendsCount = friendsCount,
                onSelectCategory = onSelectCategory,
                onOpenApp = onOpenApp,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun CatalogError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Katalog konnte nicht geladen werden", style = MaterialTheme.typography.titleLarge, color = WerkstattColors.TextDark)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
        Spacer(Modifier.height(20.dp))
        FilledPillButton(text = "Erneut versuchen", onClick = onRetry)
    }
}

@Composable
private fun HomeContent(
    manifest: StoreManifest,
    appStates: Map<String, AppState>,
    selectedCategory: String?,
    friendsCount: Int,
    onSelectCategory: (String?) -> Unit,
    onOpenApp: (AppEntry) -> Unit,
    onAction: (AppEntry) -> Unit
) {
    val filtered = if (selectedCategory == null) manifest.apps
    else manifest.apps.filter { it.category == selectedCategory }

    val featured = manifest.apps.maxByOrNull { it.versionCode }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(manifest.storeName, style = MaterialTheme.typography.headlineLarge, color = WerkstattColors.TextDark)
                    Text("$friendsCount Freunde", style = MaterialTheme.typography.bodyMedium, color = WerkstattColors.TextMuted)
                }
            }
        }

        featured?.let { entry ->
            item {
                FeaturedCard(
                    entry = entry,
                    onInstall = { onAction(entry) },
                    onOpenDetail = { onOpenApp(entry) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryChip(text = "Alle", selected = selectedCategory == null, onClick = { onSelectCategory(null) })
                manifest.categories.forEach { category ->
                    CategoryChip(
                        text = category.name,
                        selected = selectedCategory == category.id,
                        onClick = { onSelectCategory(category.id) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Text(
                "Zuletzt hochgeladen",
                style = MaterialTheme.typography.headlineSmall,
                color = WerkstattColors.TextDark,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        items(filtered, key = { it.id }) { entry ->
            AppListRow(
                entry = entry,
                state = appStates[entry.id] ?: AppState.NOT_INSTALLED,
                onClick = { onOpenApp(entry) },
                onAction = { onAction(entry) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun FeaturedCard(
    entry: AppEntry,
    onInstall: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WerkstattColors.CardCream, RoundedCornerShape(28.dp))
            .clickable(onClick = onOpenDetail)
            .padding(20.dp)
    ) {
        Badge(text = "NEUER BUILD · VON ${entry.author.uppercase()}")
        Spacer(Modifier.height(10.dp))
        Text(entry.name, style = MaterialTheme.typography.headlineMedium, color = WerkstattColors.TextDark)
        Spacer(Modifier.height(6.dp))
        Text(entry.teaser, style = MaterialTheme.typography.bodyLarge, color = WerkstattColors.TextMuted)
        Spacer(Modifier.height(14.dp))
        if (entry.featuredImageUrl.isNotBlank()) {
            AsyncImage(
                model = entry.featuredImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(WerkstattColors.PlaceholderLight, RoundedCornerShape(20.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(WerkstattColors.PlaceholderLight, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = WerkstattColors.CardCream)
                Text(
                    "PLATZHALTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = WerkstattColors.PlaceholderDark,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledPillButton(text = "Installieren", onClick = onInstall)
            OutlinePillButton(text = "Ansehen", onClick = onOpenDetail)
        }
    }
}

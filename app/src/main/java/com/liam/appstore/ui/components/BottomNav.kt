package com.liam.appstore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.appstore.ui.theme.WerkstattColors

enum class StoreTab(val label: String) {
    STORE("Werkstatt"),
    SEARCH("Suche"),
    SHELF("Regal"),
    SETTINGS("Mehr")
}

@Composable
fun WerkstattBottomNav(
    current: StoreTab,
    modifier: Modifier = Modifier,
    onSelect: (StoreTab) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WerkstattColors.CardCream)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StoreTab.entries.forEach { tab ->
            val selected = tab == current
            val icon = when (tab) {
                StoreTab.STORE -> Icons.Filled.AutoAwesome
                StoreTab.SEARCH -> Icons.Filled.Search
                StoreTab.SHELF -> Icons.AutoMirrored.Filled.ViewList
                StoreTab.SETTINGS -> Icons.Filled.Settings
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .background(
                        if (selected) WerkstattColors.PeachBadge else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = if (selected) WerkstattColors.Terracotta else WerkstattColors.TextMuted
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) WerkstattColors.Terracotta else WerkstattColors.TextMuted
                )
            }
        }
    }
}

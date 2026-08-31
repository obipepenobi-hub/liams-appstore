package com.liam.appstore.data

import kotlinx.coroutines.flow.first

sealed class CatalogResult {
    data class Success(val manifest: StoreManifest) : CatalogResult()
    data class Error(val message: String) : CatalogResult()
}

class AppRepository(
    private val config: StoreConfig,
    private val github: GithubClient,
    private val installedApps: InstalledApps
) {

    suspend fun loadCatalog(): CatalogResult {
        return try {
            val url = config.manifestUrl()
            val token = config.githubToken.first()
            val manifest = github.fetchManifest(url, token)
            val showTestBuilds = config.showTestBuilds.first()
            val filtered = if (showTestBuilds) manifest else manifest.copy(
                apps = manifest.apps.filterNot { it.tags.any { tag -> tag.equals("Test-Build", ignoreCase = true) } }
            )
            CatalogResult.Success(filtered)
        } catch (e: Exception) {
            CatalogResult.Error(e.message ?: "Unbekannter Fehler beim Laden des Katalogs")
        }
    }

    fun stateFor(entry: AppEntry): AppState = installedApps.stateFor(entry)

    fun installedVersionLabel(entry: AppEntry): String? {
        val code = installedApps.installedVersionCode(entry.packageName) ?: return null
        return "v$code"
    }
}

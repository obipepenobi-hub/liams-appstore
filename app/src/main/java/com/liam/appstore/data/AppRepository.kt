package com.liam.appstore.data

import com.liam.appstore.BuildConfig
import com.liam.appstore.security.TrustedUrls
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
            val owner = config.contentOwner.first()
            val manifest = sanitize(github.fetchManifest(url, token), owner)
            val showTestBuilds = config.showTestBuilds.first()
            val filtered = if (showTestBuilds) manifest else manifest.copy(
                apps = manifest.apps.filterNot { it.tags.any { tag -> tag.equals("Test-Build", ignoreCase = true) } }
            )
            CatalogResult.Success(filtered)
        } catch (e: Exception) {
            CatalogResult.Error(e.message ?: "Unbekannter Fehler beim Laden des Katalogs")
        }
    }

    /**
     * Der Katalog ist fernveraenderbare Eingabe: Eintraege mit unzulaessiger id/packageName
     * oder Download-URL ausserhalb des eigenen GitHub-Kontos werden verworfen, Medien-URLs
     * ausserhalb des Kontos geleert. Ob ein Eintrag installiert werden darf, entscheidet
     * zusaetzlich das Signatur-Pinning (TrustedSigners / ApkVerifier).
     */
    private fun sanitize(manifest: StoreManifest, owner: String): StoreManifest {
        val seen = HashSet<String>()
        val apps = manifest.apps.mapNotNull { entry ->
            if (!TrustedUrls.isValidId(entry.id) || !seen.add(entry.id)) return@mapNotNull null
            if (!TrustedUrls.isValidPackageName(entry.packageName)) return@mapNotNull null
            if (entry.packageName == BuildConfig.APPLICATION_ID) return@mapNotNull null
            if (!TrustedUrls.isOwnedUrl(entry.apkUrl, owner)) return@mapNotNull null
            fun media(url: String) = if (url.isBlank() || TrustedUrls.isOwnedUrl(url, owner)) url else ""
            entry.copy(
                iconUrl = media(entry.iconUrl),
                teaserVideoUrl = media(entry.teaserVideoUrl),
                featuredImageUrl = media(entry.featuredImageUrl),
                // Screenshots duerfen auch reine Text-Platzhalter sein; nur echte URLs werden geprueft.
                screenshots = entry.screenshots.filter {
                    !(it.startsWith("http://") || it.startsWith("https://")) || TrustedUrls.isOwnedUrl(it, owner)
                }
            )
        }
        return manifest.copy(apps = apps)
    }

    fun stateFor(entry: AppEntry): AppState = installedApps.stateFor(entry)

    fun installedVersionLabel(entry: AppEntry): String? {
        val code = installedApps.installedVersionCode(entry.packageName) ?: return null
        return "v$code"
    }

    /** Echte installierte Versionsanzeige (versionName), falls vorhanden - sonst null. */
    fun installedVersionName(entry: AppEntry): String? = installedApps.installedVersionName(entry.packageName)
}

package com.liam.appstore.update

import android.content.Context
import android.content.pm.PackageManager
import com.liam.appstore.BuildConfig
import com.liam.appstore.data.GithubClient
import com.liam.appstore.data.LatestRelease
import com.liam.appstore.data.StoreConfig
import com.liam.appstore.security.TrustedUrls
import kotlinx.coroutines.flow.first

sealed class SelfUpdateCheck {
    data object UpToDate : SelfUpdateCheck()
    data class UpdateAvailable(val release: LatestRelease) : SelfUpdateCheck()
    data class Failed(val message: String) : SelfUpdateCheck()
}

/**
 * Sucht in den GitHub-Releases des eigenen Repos das neueste Store-Release ("v15" ->
 * versionCode 15, Anhang liams-appstore-v15.apk). Releases anderer Apps im selben Repo
 * werden bewusst ignoriert (siehe GithubClient.fetchLatestStoreRelease). Die heruntergeladene
 * APK wird vor der Installation gegen die fest eingebaute Signatur geprueft (ApkVerifier).
 */
class SelfUpdateManager(
    private val context: Context,
    private val config: StoreConfig,
    private val github: GithubClient
) {

    private fun currentVersionCode(): Long {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    suspend fun check(): SelfUpdateCheck {
        if (!config.selfUpdateEnabled.first()) return SelfUpdateCheck.UpToDate
        return try {
            val url = config.releasesApiUrl()
            val token = config.githubToken.first()
            val release = github.fetchLatestStoreRelease(url, token)
                ?: return SelfUpdateCheck.Failed("Kein Store-Release gefunden")
            if (!TrustedUrls.isOwnedUrl(release.apkDownloadUrl, config.contentOwner.first())) {
                return SelfUpdateCheck.Failed("Release-Anhang stammt nicht vom erwarteten GitHub-Konto")
            }
            if (release.versionCode > currentVersionCode()) {
                SelfUpdateCheck.UpdateAvailable(release)
            } else {
                SelfUpdateCheck.UpToDate
            }
        } catch (e: Exception) {
            SelfUpdateCheck.Failed(e.message ?: "Update-Check fehlgeschlagen")
        }
    }
}

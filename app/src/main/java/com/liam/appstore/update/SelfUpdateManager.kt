package com.liam.appstore.update

import android.content.Context
import android.content.pm.PackageManager
import com.liam.appstore.BuildConfig
import com.liam.appstore.data.GithubClient
import com.liam.appstore.data.LatestRelease
import com.liam.appstore.data.StoreConfig
import kotlinx.coroutines.flow.first

sealed class SelfUpdateCheck {
    data object UpToDate : SelfUpdateCheck()
    data class UpdateAvailable(val release: LatestRelease) : SelfUpdateCheck()
    data class Failed(val message: String) : SelfUpdateCheck()
}

/**
 * Checks GitHub Releases of THIS app's own repo for a newer build than the
 * one currently installed, using the versionCode tucked into the release tag
 * (e.g. tag "v7" -> versionCode 7). See .github/workflows/release.yml, which
 * cuts these releases automatically whenever a `vX` tag is pushed.
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
            val release = github.fetchLatestRelease(url, token)
            if (release.apkDownloadUrl.isBlank()) {
                return SelfUpdateCheck.Failed("Release ohne APK-Anhang gefunden")
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

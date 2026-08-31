package com.liam.appstore.data

import android.content.Context
import android.content.pm.PackageManager

/** Thin wrapper around PackageManager so we can tell "Installieren" apart from "Öffnen"/"Update". */
class InstalledApps(private val context: Context) {

    fun installedVersionCode(packageName: String): Long? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun isInstalled(packageName: String): Boolean = installedVersionCode(packageName) != null

    fun stateFor(entry: AppEntry): AppState {
        val installed = installedVersionCode(entry.packageName) ?: return AppState.NOT_INSTALLED
        return if (entry.versionCode > installed) AppState.UPDATE_AVAILABLE else AppState.UP_TO_DATE
    }
}

package com.liam.appstore.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.liam.appstore.data.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Downloads an APK from a GitHub Release asset URL and hands it to the
 * system package installer. We never install silently — Android requires
 * user confirmation through the system UI unless this app is a device/profile
 * owner, which a sideloaded personal store is not.
 */
class ApkInstaller(private val context: Context, private val http: OkHttpClient) {

    private val downloadDir: File
        get() = File(context.cacheDir, "apk").apply { mkdirs() }

    fun downloadFlow(appId: String, url: String, expectedSha256: String): Flow<DownloadProgress> = callbackFlow {
        val target = File(downloadDir, "$appId.apk")
        try {
            val request = Request.Builder().url(url).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    trySend(DownloadProgress(appId, 0, 0, done = true, error = "HTTP ${response.code}"))
                    close()
                    return@callbackFlow
                }
                val body = response.body ?: run {
                    trySend(DownloadProgress(appId, 0, 0, done = true, error = "Leere Antwort"))
                    close()
                    return@callbackFlow
                }
                val total = body.contentLength()
                var done = 0L
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            done += read
                            trySend(DownloadProgress(appId, done, total))
                        }
                    }
                }
            }

            if (expectedSha256.isNotBlank()) {
                val actual = sha256(target)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    target.delete()
                    trySend(DownloadProgress(appId, 0, 0, done = true, error = "Prüfsumme stimmt nicht überein"))
                    close()
                    return@callbackFlow
                }
            }

            trySend(DownloadProgress(appId, target.length(), target.length(), done = true))
        } catch (e: Exception) {
            trySend(DownloadProgress(appId, 0, 0, done = true, error = e.message ?: e.javaClass.simpleName))
        }
        close()
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    fun apkFileFor(appId: String): File = File(downloadDir, "$appId.apk")

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun unknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun buildInstallIntent(apkFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Kein FLAG_ACTIVITY_NEW_TASK: wir starten aus einer Activity heraus, daher
            // bleibt der System-Installer im selben Task und "Fertig" kehrt zu uns zurück
            // (onResume aktualisiert dann den Installiert/Öffnen-Zustand). Mit dem Flag
            // landete man nach dem Installieren teils auf dem Homescreen statt in der App.
        }
    }

    fun buildUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
    }
}

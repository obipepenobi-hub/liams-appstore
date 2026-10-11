package com.liam.appstore.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.liam.appstore.data.DownloadProgress
import com.liam.appstore.security.ApkVerifier
import com.liam.appstore.security.TrustedUrls
import com.liam.appstore.security.VerifyPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Laedt eine APK von einer GitHub-Release-URL und uebergibt sie dem System-Installer.
 * Es wird nie still installiert - Android verlangt die Bestaetigung ueber die System-UI.
 * Vor der Uebergabe wird die Datei geprueft (Pruefsumme, Paketname, Versionscode,
 * Signatur-Pinning, siehe [ApkVerifier]); nur eine fertig verifizierte Datei bekommt
 * ihren endgueltigen Namen und kann ueberhaupt installiert werden.
 */
class ApkInstaller(
    private val context: Context,
    private val http: OkHttpClient,
    private val verifier: ApkVerifier
) {

    private val downloadDir: File
        get() = File(context.cacheDir, "apk").apply { mkdirs() }

    private fun safeName(appId: String): String =
        appId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(64).ifBlank { "app" }

    fun apkFileFor(appId: String): File = File(downloadDir, "${safeName(appId)}.apk")

    /** Alte Downloads (auch abgebrochene .part-Dateien) nicht dauerhaft herumliegen lassen. */
    fun cleanupStale() {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        downloadDir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    fun downloadFlow(appId: String, url: String, policy: VerifyPolicy): Flow<DownloadProgress> = flow {
        fun failure(message: String) = DownloadProgress(appId, 0, 0, done = true, error = message)

        val parsed = url.toHttpUrlOrNull()
        if (parsed == null || !TrustedUrls.isAllowedRequestUrl(parsed)) {
            emit(failure("Download-Adresse nicht erlaubt"))
            return@flow
        }

        val finalFile = apkFileFor(appId)
        val partFile = File(downloadDir, finalFile.name + ".part")
        finalFile.delete()
        partFile.delete()

        try {
            http.newCall(Request.Builder().url(parsed).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(failure("HTTP ${response.code}"))
                    return@flow
                }
                val body = response.body
                if (body == null) {
                    emit(failure("Leere Antwort"))
                    return@flow
                }
                val total = body.contentLength()
                if (total > MAX_APK_BYTES) {
                    emit(failure("Datei ist unerwartet groß"))
                    return@flow
                }
                var done = 0L
                var lastEmit = 0L
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            done += read
                            if (done > MAX_APK_BYTES) {
                                partFile.delete()
                                emit(failure("Datei ist unerwartet groß"))
                                return@flow
                            }
                            output.write(buffer, 0, read)
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 100) {
                                lastEmit = now
                                emit(DownloadProgress(appId, done, total))
                            }
                        }
                    }
                }
            }

            val problem = verifier.verify(partFile, policy)
            if (problem != null) {
                partFile.delete()
                emit(failure(problem))
                return@flow
            }
            if (!partFile.renameTo(finalFile)) {
                partFile.delete()
                emit(failure("Datei konnte nicht abgelegt werden"))
                return@flow
            }
            emit(DownloadProgress(appId, finalFile.length(), finalFile.length(), done = true))
        } catch (e: CancellationException) {
            partFile.delete()
            throw e
        } catch (e: Exception) {
            partFile.delete()
            emit(failure(e.message ?: e.javaClass.simpleName))
        }
    }.flowOn(Dispatchers.IO)

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
            // bleibt der System-Installer im selben Task und "Fertig" kehrt zu uns zurück.
        }
    }

    fun buildUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
    }

    companion object {
        private const val MAX_APK_BYTES = 400L * 1024 * 1024
    }
}

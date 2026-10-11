package com.liam.appstore.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Was eine heruntergeladene APK erfuellen muss, bevor sie dem System-Installer uebergeben wird. */
data class VerifyPolicy(
    val expectedPackage: String,
    val expectedVersionCode: Long,
    val allowedSigners: Set<String>,
    val requireSha256: Boolean,
    val expectedSha256: String
)

class ApkVerifier(private val context: Context) {

    /** @return null wenn alles passt, sonst eine verstaendliche Fehlermeldung. */
    fun verify(file: File, policy: VerifyPolicy): String? {
        if (policy.requireSha256 && !TrustedUrls.isValidSha256(policy.expectedSha256)) {
            return "Prüfsumme fehlt im Katalog"
        }
        if (policy.expectedSha256.isNotBlank()) {
            if (!TrustedUrls.isValidSha256(policy.expectedSha256)) return "Prüfsumme im Katalog ungültig"
            if (!sha256(file).equals(policy.expectedSha256, ignoreCase = true)) return "Prüfsumme stimmt nicht überein"
        }
        if (policy.allowedSigners.isEmpty()) {
            return "Signatur dieser App ist im Store nicht hinterlegt"
        }

        val info = archiveInfo(file) ?: return "APK ist beschädigt oder nicht korrekt signiert"
        if (info.packageName != policy.expectedPackage) return "Paketname der APK stimmt nicht überein"
        if (versionCode(info) != policy.expectedVersionCode) return "Versionscode der APK stimmt nicht überein"

        val signers = signerHashes(info)
        if (signers.isEmpty() || !policy.allowedSigners.containsAll(signers)) {
            return "Signatur der APK ist nicht vertrauenswürdig"
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(file: File): PackageInfo? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return try {
            pm.getPackageArchiveInfo(file.path, flags)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signerHashes(info: PackageInfo): Set<String> {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        return signatures.orEmpty().map { sha256Hex(it.toByteArray()) }.toSet()
    }

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

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

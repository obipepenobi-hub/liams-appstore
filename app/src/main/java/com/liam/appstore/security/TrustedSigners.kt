package com.liam.appstore.security

import com.liam.appstore.BuildConfig

/**
 * Vertrauensanker liegen bewusst IM Programmcode und nicht in apps.json: Wer das
 * GitHub-Repo veraendern kann, soll damit keine fremde App einschleusen koennen.
 * Eine neue App im Katalog braucht daher eine neue Store-Version (Eintrag hier +
 * <queries>-freie Sichtbarkeit ist via QUERY_ALL_PACKAGES bereits abgedeckt).
 *
 * Werte: SHA-256 des Signaturzertifikats (Hex, kleingeschrieben) - zu ermitteln mit
 * tools/apk-info.sh <apk>.
 */
object TrustedSigners {

    private val catalogApps: Map<String, Set<String>> = mapOf(
        "com.liamkkm.cadence" to setOf(
            "0676f4b55f3084fce80f7e63dec08cffd62df9eab9a736eed20dcd80156d52d7"
        ),
        "com.liam.kaptalismusaufhalter" to setOf(
            // Bisheriger Schluessel (liegt oeffentlich im Impulskauf-Repo, daher faelschbar -
            // nach der geplanten Rotation hier entfernen).
            "bda870ace1b90382049581b0901b8fec0225914093d2b19b92784b3d4536d0a0",
            // Kuenftiger privater Release-Schluessel (Rotation per Lineage).
            "bb9dc73af5f956742ffbef7ebbe68ad56fb1ce9318de41cd8a382244ce6906b3"
        )
    )

    fun forPackage(packageName: String): Set<String> = catalogApps[packageName].orEmpty()

    /** Erlaubte Signaturen fuer Updates des Stores selbst (kommagetrennt in BuildConfig). */
    fun forSelf(): Set<String> =
        BuildConfig.SELF_SIGNER_SHA256.split(',').map { it.trim().lowercase() }.filter { it.length == 64 }.toSet()
}

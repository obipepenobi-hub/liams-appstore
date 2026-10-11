package com.liam.appstore.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Validierung aller Werte, die aus dem (fernveraenderbaren) Katalog stammen. */
object TrustedUrls {

    private val ID_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    private val SHA256_REGEX = Regex("^[0-9a-fA-F]{64}$")
    private val NAME_REGEX = Regex("^[A-Za-z0-9_.-]{1,100}$")

    fun isValidId(id: String) = ID_REGEX.matches(id)
    fun isValidPackageName(name: String) = name.length <= 200 && PACKAGE_REGEX.matches(name)
    fun isValidSha256(hash: String) = SHA256_REGEX.matches(hash)

    /** Owner/Repo/Branch aus den Einstellungen - nur harmlose Zeichen, keine Pfadtricks. */
    fun isValidSourcePart(value: String) = NAME_REGEX.matches(value) && value != "." && value != ".."

    fun isAllowedHost(host: String): Boolean =
        host == "github.com" || host == "api.github.com" || host.endsWith(".githubusercontent.com")

    /** Jeder einzelne Netzwerk-Hop (inkl. Redirects) muss HTTPS zu GitHub sein. */
    fun isAllowedRequestUrl(url: HttpUrl): Boolean = url.isHttps && isAllowedHost(url.host)

    /** Hosts, an die das GitHub-Token gesendet werden darf. */
    fun mayReceiveToken(url: HttpUrl): Boolean =
        url.isHttps && (url.host == "api.github.com" || url.host == "raw.githubusercontent.com")

    /** Die URL muss auf github.com/<owner>/... bzw. raw.githubusercontent.com/<owner>/... zeigen. */
    fun isOwnedUrl(url: String, owner: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        if (!parsed.isHttps) return false
        if (parsed.host != "github.com" && parsed.host != "raw.githubusercontent.com") return false
        return parsed.pathSegments.firstOrNull().equals(owner, ignoreCase = true)
    }
}

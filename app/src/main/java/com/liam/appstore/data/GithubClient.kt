package com.liam.appstore.data

import com.liam.appstore.security.TrustedUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Result of a GitHub release lookup, used for self-update. */
data class LatestRelease(
    val tagName: String,
    val versionCode: Long,
    val apkDownloadUrl: String,
    val htmlUrl: String,
    val sizeBytes: Long
)

class GithubClient {

    // callTimeout ist die harte Obergrenze: ein haengender Request wird nach spaetestens
    // 5 Minuten abgebrochen. Der Network-Interceptor prueft JEDEN Hop (auch Redirects):
    // nur HTTPS zu GitHub-Hosts, sonst bricht die Anfrage ab.
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .followSslRedirects(false)
        .addNetworkInterceptor(Interceptor { chain ->
            val url = chain.request().url
            if (!TrustedUrls.isAllowedRequestUrl(url)) {
                throw IOException("Blockierte Adresse: ${url.host}")
            }
            chain.proceed(chain.request())
        })
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ETag-Cache: der Store fragt im Vordergrund jede Minute nach; eine unveraenderte
    // Antwort (304) kostet kein Rate-Limit und keinen Datenverkehr.
    private data class Cached(val etag: String, val body: String)
    private val cache = ConcurrentHashMap<String, Cached>()

    suspend fun fetchManifest(url: String, token: String): StoreManifest = withContext(Dispatchers.IO) {
        val body = getText(url, token)
        json.decodeFromString(StoreManifest.serializer(), body)
    }

    /**
     * Sucht in der Release-Liste das neueste "vN"-Release des Stores. Andere Releases im
     * selben Repo (Katalog-Apps wie cadence-v1.1.16) und Prereleases/Entwuerfe werden
     * ignoriert - sonst koennte ein App-Release als "Store-Update" angeboten werden.
     */
    suspend fun fetchLatestStoreRelease(apiUrl: String, token: String): LatestRelease? = withContext(Dispatchers.IO) {
        val body = getText(apiUrl, token, isApi = true)
        val releases = json.parseToJsonElement(body) as? JsonArray ?: return@withContext null
        var best: LatestRelease? = null
        for (element in releases) {
            val obj = element as? JsonObject ?: continue
            if (obj.bool("draft") || obj.bool("prerelease")) continue
            val tag = obj.str("tag_name")
            val match = STORE_TAG.matchEntire(tag) ?: continue
            val versionCode = match.groupValues[1].toLongOrNull() ?: continue
            if (best != null && versionCode <= best.versionCode) continue
            val assets = obj["assets"] as? JsonArray ?: continue
            val apk = assets.mapNotNull { it as? JsonObject }
                .firstOrNull { STORE_ASSET.matches(it.str("name")) } ?: continue
            best = LatestRelease(
                tagName = tag,
                versionCode = versionCode,
                apkDownloadUrl = apk.str("browser_download_url"),
                htmlUrl = obj.str("html_url"),
                sizeBytes = (apk["size"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L
            )
        }
        best
    }

    private fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.contentOrNull == "true"

    private fun getText(url: String, token: String, isApi: Boolean = false): String {
        val parsed = url.toHttpUrl()
        val builder = Request.Builder().url(parsed)
        // Das Token darf ausschliesslich zu api.github.com / raw.githubusercontent.com.
        if (token.isNotBlank() && TrustedUrls.mayReceiveToken(parsed)) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        if (isApi) {
            builder.addHeader("Accept", "application/vnd.github+json")
        }
        cache[url]?.let { builder.addHeader("If-None-Match", it.etag) }

        http.newCall(builder.build()).execute().use { response ->
            if (response.code == 304) {
                return cache[url]?.body ?: throw IOException("Leere Antwort von GitHub")
            }
            if (!response.isSuccessful) {
                throw IOException("GitHub request failed: ${response.code} ${response.message}")
            }
            val source = (response.body ?: throw IOException("Leere Antwort von GitHub")).source()
            source.request(MAX_TEXT_BYTES + 1)
            if (source.buffer.size > MAX_TEXT_BYTES) throw IOException("Antwort von GitHub zu groß")
            val text = source.readUtf8()
            response.header("ETag")?.let { cache[url] = Cached(it, text) }
            return text
        }
    }

    fun rawClient(): OkHttpClient = http

    companion object {
        private const val MAX_TEXT_BYTES = 2L * 1024 * 1024
        private val STORE_TAG = Regex("^v(\\d{1,6})$")
        private val STORE_ASSET = Regex("^liams-appstore-v\\d{1,6}\\.apk$")
    }
}

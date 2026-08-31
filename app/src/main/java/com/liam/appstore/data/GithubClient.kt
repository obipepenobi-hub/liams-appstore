package com.liam.appstore.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of a GitHub "latest release" lookup, used for self-update. */
data class LatestRelease(
    val tagName: String,
    val versionCode: Long,
    val apkDownloadUrl: String,
    val htmlUrl: String,
    val sizeBytes: Long
)

class GithubClient {

    // callTimeout ist die entscheidende Absicherung: OkHttp bricht damit einen
    // haengenden Request nach spaetestens 5 Minuten hart ab. 5 Minuten reichen
    // auch der groessten APK im Katalog (Cadence, ~63 MB) auf einer langsamen
    // Verbindung noch bequem - ohne diese harte Grenze konnte ein Download
    // lautlos ewig haengen bleiben, ohne dass die App das je als Fehler bemerkt.
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchManifest(url: String, token: String): StoreManifest = withContext(Dispatchers.IO) {
        val body = getText(url, token)
        json.decodeFromString(StoreManifest.serializer(), body)
    }

    suspend fun fetchLatestRelease(apiUrl: String, token: String): LatestRelease = withContext(Dispatchers.IO) {
        val body = getText(apiUrl, token, isApi = true)
        val obj = json.parseToJsonElement(body).let { it as kotlinx.serialization.json.JsonObject }
        val tag = obj["tag_name"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content } ?: ""
        val versionCode = tag.filter { it.isDigit() }.ifBlank { "0" }.let {
            // tag convention: v12 or store-v12 -> take trailing digits as versionCode
            it.takeLast(6).toLongOrNull() ?: 0L
        }
        val assets = obj["assets"]?.let { it as kotlinx.serialization.json.JsonArray } ?: kotlinx.serialization.json.JsonArray(emptyList())
        val apkAsset = assets.map { it as kotlinx.serialization.json.JsonObject }
            .firstOrNull { (it["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.endsWith(".apk") == true }
        val downloadUrl = apkAsset?.get("browser_download_url")?.let { (it as kotlinx.serialization.json.JsonPrimitive).content } ?: ""
        val size = apkAsset?.get("size")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } ?: 0L
        val htmlUrl = obj["html_url"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content } ?: ""
        LatestRelease(tagName = tag, versionCode = versionCode, apkDownloadUrl = downloadUrl, htmlUrl = htmlUrl, sizeBytes = size)
    }

    private fun getText(url: String, token: String, isApi: Boolean = false): String {
        val builder = Request.Builder().url(url)
        if (token.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        if (isApi) {
            builder.addHeader("Accept", "application/vnd.github+json")
        }
        http.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub request failed: ${response.code} ${response.message}")
            }
            return response.body?.string() ?: throw IOException("Leere Antwort von GitHub")
        }
    }

    fun rawClient(): OkHttpClient = http
}

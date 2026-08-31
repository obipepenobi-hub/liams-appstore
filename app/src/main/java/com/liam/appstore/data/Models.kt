package com.liam.appstore.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val subtitle: String = ""
)

@Serializable
data class ChangelogEntry(
    val version: String,
    val date: String,
    val notes: String
)

@Serializable
data class Review(
    val author: String,
    val initials: String,
    val quote: String
)

@Serializable
data class AppPermission(
    val label: String,
    val manifestName: String = ""
)

@Serializable
data class AppEntry(
    val id: String,
    val packageName: String,
    val name: String,
    val author: String,
    val category: String,
    val version: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val teaser: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val permissions: List<AppPermission> = emptyList(),
    val changelog: List<ChangelogEntry> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val apkUrl: String,
    val apkSha256: String = "",
    val minSdk: Int = 26,
    val installs: Int = 0
)

@Serializable
data class StoreManifest(
    val storeName: String = "Liams Appstore",
    val friends: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val apps: List<AppEntry> = emptyList()
)

enum class AppState {
    NOT_INSTALLED,
    UPDATE_AVAILABLE,
    UP_TO_DATE
}

data class DownloadProgress(
    val appId: String,
    val bytesDone: Long,
    val bytesTotal: Long,
    val done: Boolean = false,
    val error: String? = null
) {
    val fraction: Float
        get() = if (bytesTotal <= 0L) 0f else (bytesDone.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
}

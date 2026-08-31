package com.liam.appstore.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "store_config")

/**
 * Everything the app needs to know where to fetch its catalog and its own
 * updates from. Defaults point at DEFAULT_OWNER/DEFAULT_REPO, changeable in
 * Mehr -> Store-Quelle.
 */
class StoreConfig(private val context: Context) {

    private object Keys {
        val CONTENT_OWNER = stringPreferencesKey("content_owner")
        val CONTENT_REPO = stringPreferencesKey("content_repo")
        val CONTENT_BRANCH = stringPreferencesKey("content_branch")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AUTO_LOAD_UPDATES = booleanPreferencesKey("auto_load_updates")
        val SHOW_TEST_BUILDS = booleanPreferencesKey("show_test_builds")
        val SELF_UPDATE_ENABLED = booleanPreferencesKey("self_update_enabled")
        val LOCAL_FRIENDS = stringSetPreferencesKey("local_friends")
    }

    val contentOwner: Flow<String> = context.dataStore.data.map { it[Keys.CONTENT_OWNER] ?: DEFAULT_OWNER }
    val contentRepo: Flow<String> = context.dataStore.data.map { it[Keys.CONTENT_REPO] ?: DEFAULT_REPO }
    val contentBranch: Flow<String> = context.dataStore.data.map { it[Keys.CONTENT_BRANCH] ?: DEFAULT_BRANCH }
    val githubToken: Flow<String> = context.dataStore.data.map { it[Keys.GITHUB_TOKEN] ?: "" }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: true }
    val autoLoadUpdates: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_LOAD_UPDATES] ?: false }
    val showTestBuilds: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TEST_BUILDS] ?: true }
    val selfUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SELF_UPDATE_ENABLED] ?: true }
    val localFriends: Flow<Set<String>> = context.dataStore.data.map { it[Keys.LOCAL_FRIENDS] ?: emptySet() }

    suspend fun manifestUrl(): String {
        val owner = context.dataStore.data.first()[Keys.CONTENT_OWNER] ?: DEFAULT_OWNER
        val repo = context.dataStore.data.first()[Keys.CONTENT_REPO] ?: DEFAULT_REPO
        val branch = context.dataStore.data.first()[Keys.CONTENT_BRANCH] ?: DEFAULT_BRANCH
        return "https://raw.githubusercontent.com/$owner/$repo/$branch/apps.json"
    }

    suspend fun releasesApiUrl(): String {
        val owner = context.dataStore.data.first()[Keys.CONTENT_OWNER] ?: DEFAULT_OWNER
        val repo = context.dataStore.data.first()[Keys.CONTENT_REPO] ?: DEFAULT_REPO
        return "https://api.github.com/repos/$owner/$repo/releases/latest"
    }

    suspend fun setContentSource(owner: String, repo: String, branch: String) {
        context.dataStore.edit {
            it[Keys.CONTENT_OWNER] = owner.trim()
            it[Keys.CONTENT_REPO] = repo.trim()
            it[Keys.CONTENT_BRANCH] = branch.trim().ifBlank { DEFAULT_BRANCH }
        }
    }

    suspend fun setGithubToken(token: String) {
        context.dataStore.edit { it[Keys.GITHUB_TOKEN] = token }
    }

    suspend fun setWifiOnly(value: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = value }
    }

    suspend fun setAutoLoadUpdates(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LOAD_UPDATES] = value }
    }

    suspend fun setShowTestBuilds(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TEST_BUILDS] = value }
    }

    suspend fun setSelfUpdateEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.SELF_UPDATE_ENABLED] = value }
    }

    suspend fun addLocalFriend(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit {
            val current = it[Keys.LOCAL_FRIENDS] ?: emptySet()
            it[Keys.LOCAL_FRIENDS] = current + trimmed
        }
    }

    suspend fun removeLocalFriend(name: String) {
        context.dataStore.edit {
            val current = it[Keys.LOCAL_FRIENDS] ?: emptySet()
            it[Keys.LOCAL_FRIENDS] = current - name
        }
    }

    companion object {
        const val DEFAULT_OWNER = "obipepenobi-hub"
        const val DEFAULT_REPO = "liams-appstore"
        const val DEFAULT_BRANCH = "main"
    }
}

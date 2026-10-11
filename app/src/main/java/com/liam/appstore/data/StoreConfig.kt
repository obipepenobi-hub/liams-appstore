package com.liam.appstore.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.liam.appstore.security.SecureStorage
import com.liam.appstore.security.TrustedUrls
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

    // Nur harmlose Zeichen (keine "/", "?", ".." usw.) - sonst Rueckfall auf den Standard.
    private fun safePart(value: String?, default: String): String =
        value?.takeIf { TrustedUrls.isValidSourcePart(it) } ?: default

    val contentOwner: Flow<String> = context.dataStore.data.map { safePart(it[Keys.CONTENT_OWNER], DEFAULT_OWNER) }
    val contentRepo: Flow<String> = context.dataStore.data.map { safePart(it[Keys.CONTENT_REPO], DEFAULT_REPO) }
    val contentBranch: Flow<String> = context.dataStore.data.map { safePart(it[Keys.CONTENT_BRANCH], DEFAULT_BRANCH) }
    // Das Token liegt nur verschluesselt (Android Keystore, AES-GCM) im DataStore.
    val githubToken: Flow<String> = context.dataStore.data.map { SecureStorage.decrypt(it[Keys.GITHUB_TOKEN] ?: "") }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: true }
    val autoLoadUpdates: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_LOAD_UPDATES] ?: false }
    val showTestBuilds: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TEST_BUILDS] ?: true }
    val selfUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SELF_UPDATE_ENABLED] ?: true }
    val localFriends: Flow<Set<String>> = context.dataStore.data.map { it[Keys.LOCAL_FRIENDS] ?: emptySet() }

    suspend fun manifestUrl(): String {
        val owner = contentOwner.first()
        val repo = contentRepo.first()
        val branch = contentBranch.first()
        return "https://raw.githubusercontent.com/$owner/$repo/$branch/apps.json"
    }

    suspend fun releasesApiUrl(): String {
        val owner = contentOwner.first()
        val repo = contentRepo.first()
        return "https://api.github.com/repos/$owner/$repo/releases?per_page=30"
    }

    suspend fun setContentSource(owner: String, repo: String, branch: String) {
        context.dataStore.edit {
            it[Keys.CONTENT_OWNER] = safePart(owner.trim(), DEFAULT_OWNER)
            it[Keys.CONTENT_REPO] = safePart(repo.trim(), DEFAULT_REPO)
            it[Keys.CONTENT_BRANCH] = safePart(branch.trim(), DEFAULT_BRANCH)
        }
    }

    suspend fun setGithubToken(token: String) {
        context.dataStore.edit { it[Keys.GITHUB_TOKEN] = SecureStorage.encrypt(token.trim()) }
    }

    /** Aeltere Versionen haben das Token im Klartext gespeichert - einmalig verschluesseln. */
    suspend fun migrateLegacyToken() {
        context.dataStore.edit {
            val stored = it[Keys.GITHUB_TOKEN].orEmpty()
            if (stored.isNotEmpty() && !SecureStorage.isEncrypted(stored)) {
                it[Keys.GITHUB_TOKEN] = SecureStorage.encrypt(stored)
            }
        }
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

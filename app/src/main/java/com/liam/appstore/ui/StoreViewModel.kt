package com.liam.appstore.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liam.appstore.AppstoreApplication
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.data.CatalogResult
import com.liam.appstore.data.DownloadProgress
import com.liam.appstore.data.StoreManifest
import com.liam.appstore.update.SelfUpdateCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    data object Loading : CatalogUiState()
    data class Loaded(val manifest: StoreManifest) : CatalogUiState()
    data class Failed(val message: String) : CatalogUiState()
}

sealed class InstallStep {
    data object Idle : InstallStep()
    data class Confirming(val entry: AppEntry) : InstallStep()
    data class Downloading(val entry: AppEntry, val progress: DownloadProgress) : InstallStep()
    data class ReadyToInstall(val entry: AppEntry) : InstallStep()
    data class NeedsPermission(val entry: AppEntry) : InstallStep()
    data class Error(val entry: AppEntry, val message: String) : InstallStep()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppstoreApplication

    private val _catalog = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val catalog: StateFlow<CatalogUiState> = _catalog.asStateFlow()

    private val _appStates = MutableStateFlow<Map<String, AppState>>(emptyMap())
    val appStates: StateFlow<Map<String, AppState>> = _appStates.asStateFlow()

    private val _installStep = MutableStateFlow<InstallStep>(InstallStep.Idle)
    val installStep: StateFlow<InstallStep> = _installStep.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _selfUpdate = MutableStateFlow<SelfUpdateCheck>(SelfUpdateCheck.UpToDate)
    val selfUpdate: StateFlow<SelfUpdateCheck> = _selfUpdate.asStateFlow()

    init {
        refresh()
        checkSelfUpdate()
    }

    fun refresh() {
        viewModelScope.launch {
            _catalog.value = CatalogUiState.Loading
            when (val result = app.repository.loadCatalog()) {
                is CatalogResult.Success -> {
                    _catalog.value = CatalogUiState.Loaded(result.manifest)
                    recomputeStates(result.manifest)
                }
                is CatalogResult.Error -> _catalog.value = CatalogUiState.Failed(result.message)
            }
        }
    }

    fun recomputeInstalledStates() {
        (catalog.value as? CatalogUiState.Loaded)?.let { recomputeStates(it.manifest) }
    }

    private fun recomputeStates(manifest: StoreManifest) {
        _appStates.value = manifest.apps.associate { it.id to app.repository.stateFor(it) }
    }

    fun checkSelfUpdate(announceResult: Boolean = false) {
        viewModelScope.launch {
            val result = app.selfUpdateManager.check()
            _selfUpdate.value = result
            when (result) {
                is SelfUpdateCheck.UpdateAvailable -> beginInstall(selfUpdateEntry(result.release))
                SelfUpdateCheck.UpToDate -> if (announceResult) showToast("Liams Appstore ist aktuell")
                is SelfUpdateCheck.Failed -> if (announceResult) showToast("Update-Check fehlgeschlagen: ${result.message}")
            }
        }
    }

    private fun selfUpdateEntry(release: com.liam.appstore.data.LatestRelease): AppEntry = AppEntry(
        id = SELF_UPDATE_ID,
        packageName = app.packageName,
        name = "Liams Appstore",
        author = "dir",
        category = "",
        version = release.tagName,
        versionCode = release.versionCode,
        sizeBytes = release.sizeBytes,
        apkUrl = release.apkDownloadUrl
    )

    fun beginInstall(entry: AppEntry) {
        if (!app.apkInstaller.canInstallUnknownApps()) {
            _installStep.value = InstallStep.NeedsPermission(entry)
        } else {
            _installStep.value = InstallStep.Confirming(entry)
        }
    }

    fun confirmInstall(entry: AppEntry) {
        viewModelScope.launch {
            app.apkInstaller.downloadFlow(entry.id, entry.apkUrl, entry.apkSha256).collect { progress ->
                if (progress.error != null) {
                    _installStep.value = InstallStep.Error(entry, progress.error)
                } else if (progress.done) {
                    _installStep.value = InstallStep.ReadyToInstall(entry)
                } else {
                    _installStep.value = InstallStep.Downloading(entry, progress)
                }
            }
        }
    }

    fun launchInstall(entry: AppEntry): android.content.Intent {
        val file = app.apkInstaller.apkFileFor(entry.id)
        return app.apkInstaller.buildInstallIntent(file)
    }

    fun unknownSourcesIntent(): android.content.Intent = app.apkInstaller.unknownSourcesSettingsIntent()

    fun dismissInstallSheet() {
        _installStep.value = InstallStep.Idle
    }

    fun onInstallHandedOff() {
        _installStep.value = InstallStep.Idle
        recomputeInstalledStates()
    }

    fun showToast(message: String) {
        _toast.value = message
    }

    fun clearToast() {
        _toast.value = null
    }

    companion object {
        const val SELF_UPDATE_ID = "__self_update__"
    }
}

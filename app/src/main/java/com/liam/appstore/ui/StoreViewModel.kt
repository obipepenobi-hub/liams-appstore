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
import com.liam.appstore.security.TrustedSigners
import com.liam.appstore.security.VerifyPolicy
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

    // Echte installierte Versionsnamen vom Geraet, nicht die (ggf. veraltete)
    // apps.json-Angabe - siehe AppRepository.installedVersionName.
    private val _installedVersions = MutableStateFlow<Map<String, String>>(emptyMap())
    val installedVersions: StateFlow<Map<String, String>> = _installedVersions.asStateFlow()

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

    // silent=true wird bei jedem App-Oeffnen (onResume) verwendet, damit neue
    // apps.json-Eintraege sofort sichtbar sind, OHNE dass Liams Appstore selbst
    // ein Update braucht - dabei aber ohne den Lade-Screen aufblitzen zu lassen
    // und ohne einen bereits geladenen Katalog wegen eines kurzen Netzwerk-
    // Ausrutschers durch eine Fehleransicht zu ersetzen.
    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _catalog.value = CatalogUiState.Loading
            when (val result = app.repository.loadCatalog()) {
                is CatalogResult.Success -> {
                    _catalog.value = CatalogUiState.Loaded(result.manifest)
                    recomputeStates(result.manifest)
                }
                is CatalogResult.Error -> {
                    if (!silent || catalog.value !is CatalogUiState.Loaded) {
                        _catalog.value = CatalogUiState.Failed(result.message)
                    }
                }
            }
        }
    }

    fun recomputeInstalledStates() {
        (catalog.value as? CatalogUiState.Loaded)?.let { recomputeStates(it.manifest) }
    }

    private fun recomputeStates(manifest: StoreManifest) {
        _appStates.value = manifest.apps.associate { it.id to app.repository.stateFor(it) }
        _installedVersions.value = manifest.apps.mapNotNull { entry ->
            app.repository.installedVersionName(entry)?.let { entry.id to it }
        }.toMap()
    }

    // Pro Prozess hoechstens ein Fehler-Toast des stillen Auto-Checks (sonst Toast-Flut im
    // Minutentakt, z.B. bei Offline/Rate-Limit); der manuelle Check zeigt immer alles.
    private var autoFailureShown = false

    // Das Update-Popup erscheint pro Version nur einmal je Prozess - wer es wegtippt,
    // soll nicht jede Minute erneut davon unterbrochen werden. Der manuelle Check
    // ("Jetzt nach Store-Update suchen") zeigt es immer wieder an.
    private var promptedVersionCode = 0L

    fun checkSelfUpdate(announceResult: Boolean = false) {
        viewModelScope.launch {
            val result = app.selfUpdateManager.check()
            _selfUpdate.value = result
            when (result) {
                is SelfUpdateCheck.UpdateAvailable -> {
                    val idle = _installStep.value is InstallStep.Idle
                    if (idle && (announceResult || result.release.versionCode != promptedVersionCode)) {
                        promptedVersionCode = result.release.versionCode
                        beginInstall(selfUpdateEntry(result.release))
                    }
                }
                SelfUpdateCheck.UpToDate -> if (announceResult) showToast("Liams Appstore ist aktuell")
                is SelfUpdateCheck.Failed -> if (announceResult || !autoFailureShown) {
                    autoFailureShown = true
                    showToast("Update-Check fehlgeschlagen: ${result.message}")
                }
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
            app.apkInstaller.downloadFlow(entry.id, entry.apkUrl, policyFor(entry)).collect { progress ->
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

    // Store-Updates gelten nur mit der fest eingebauten Signatur; Katalog-Apps brauchen
    // zusaetzlich eine Pruefsumme und ein im Code hinterlegtes Signaturzertifikat.
    private fun policyFor(entry: AppEntry): VerifyPolicy {
        val isSelf = entry.id == SELF_UPDATE_ID
        return VerifyPolicy(
            expectedPackage = entry.packageName,
            expectedVersionCode = entry.versionCode,
            allowedSigners = if (isSelf) TrustedSigners.forSelf() else TrustedSigners.forPackage(entry.packageName),
            requireSha256 = !isSelf,
            expectedSha256 = entry.apkSha256
        )
    }

    fun launchInstall(entry: AppEntry): android.content.Intent {
        val file = app.apkInstaller.apkFileFor(entry.id)
        return app.apkInstaller.buildInstallIntent(file)
    }

    fun unknownSourcesIntent(): android.content.Intent = app.apkInstaller.unknownSourcesSettingsIntent()

    fun uninstallIntent(packageName: String): android.content.Intent = app.apkInstaller.buildUninstallIntent(packageName)

    fun dismissInstallSheet() {
        _installStep.value = InstallStep.Idle
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

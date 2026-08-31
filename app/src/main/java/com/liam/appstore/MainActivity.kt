package com.liam.appstore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.liam.appstore.data.AppEntry
import com.liam.appstore.data.AppState
import com.liam.appstore.ui.CatalogUiState
import com.liam.appstore.ui.InstallStep
import com.liam.appstore.ui.StoreViewModel
import com.liam.appstore.ui.components.DownloadProgressSheet
import com.liam.appstore.ui.components.InstallConfirmSheet
import com.liam.appstore.ui.components.InstalledToast
import com.liam.appstore.ui.components.StoreTab
import com.liam.appstore.ui.components.WerkstattBottomNav
import com.liam.appstore.ui.screens.AppDetailScreen
import com.liam.appstore.ui.screens.HomeScreen
import com.liam.appstore.ui.screens.SearchScreen
import com.liam.appstore.ui.screens.SettingsScreen
import com.liam.appstore.ui.screens.SettingsState
import com.liam.appstore.ui.screens.ShelfScreen
import com.liam.appstore.ui.theme.LiamsAppstoreTheme
import com.liam.appstore.ui.theme.WerkstattColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class Route {
    data class Tab(val tab: StoreTab) : Route()
    data class Detail(val appId: String) : Route()
}

class MainActivity : ComponentActivity() {

    private val viewModel: StoreViewModel by viewModels()

    private val unknownSourcesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Zurück aus den Systemeinstellungen — der Nutzer tippt "Installieren" erneut,
        // um den Vorgang mit der frisch erteilten Erlaubnis fortzusetzen.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiamsAppstoreTheme {
                AppRoot(viewModel, onOpenUnknownSourcesSettings = { intent -> unknownSourcesLauncher.launch(intent) })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.recomputeInstalledStates()
    }
}

@Composable
private fun AppRoot(
    viewModel: StoreViewModel,
    onOpenUnknownSourcesSettings: (Intent) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AppstoreApplication
    val scope = rememberCoroutineScope()

    var route by remember { mutableStateOf<Route>(Route.Tab(StoreTab.STORE)) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val catalogState by viewModel.catalog.collectAsState()
    val appStates by viewModel.appStates.collectAsState()
    val installStep by viewModel.installStep.collectAsState()
    val toast by viewModel.toast.collectAsState()

    val owner by app.config.contentOwner.collectAsState(initial = "")
    val repo by app.config.contentRepo.collectAsState(initial = "")
    val branch by app.config.contentBranch.collectAsState(initial = "")
    val token by app.config.githubToken.collectAsState(initial = "")
    val wifiOnly by app.config.wifiOnly.collectAsState(initial = true)
    val autoLoad by app.config.autoLoadUpdates.collectAsState(initial = false)
    val showTestBuilds by app.config.showTestBuilds.collectAsState(initial = true)
    val selfUpdateEnabled by app.config.selfUpdateEnabled.collectAsState(initial = true)
    val localFriends by app.config.localFriends.collectAsState(initial = emptySet())

    val manifest = (catalogState as? CatalogUiState.Loaded)?.manifest
    val combinedFriends = (manifest?.friends.orEmpty() + localFriends).distinct()

    fun performAction(entry: AppEntry) {
        val state = appStates[entry.id] ?: AppState.NOT_INSTALLED
        if (state == AppState.UP_TO_DATE) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(entry.packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                viewModel.showToast("${entry.name} lässt sich nicht öffnen")
            }
        } else {
            viewModel.beginInstall(entry)
        }
    }

    fun performUninstall(entry: AppEntry) {
        context.startActivity(viewModel.uninstallIntent(entry.packageName))
    }

    Scaffold(
        containerColor = WerkstattColors.Cream,
        bottomBar = {
            if (route is Route.Tab) {
                WerkstattBottomNav(current = (route as Route.Tab).tab) { tab ->
                    route = Route.Tab(tab)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val r = route) {
                is Route.Tab -> when (r.tab) {
                    StoreTab.STORE -> HomeScreen(
                        catalogState = catalogState,
                        appStates = appStates,
                        selectedCategory = selectedCategory,
                        friendsCount = combinedFriends.size,
                        onSelectCategory = { selectedCategory = it },
                        onOpenApp = { route = Route.Detail(it.id) },
                        onAction = { performAction(it) },
                        onRetry = { viewModel.refresh() }
                    )
                    StoreTab.SEARCH -> SearchScreen(
                        manifest = manifest,
                        appStates = appStates,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onOpenApp = { route = Route.Detail(it.id) },
                        onAction = { performAction(it) },
                        onSelectCategory = {
                            selectedCategory = it
                            route = Route.Tab(StoreTab.STORE)
                        }
                    )
                    StoreTab.SHELF -> ShelfScreen(
                        manifest = manifest,
                        appStates = appStates,
                        onOpenApp = { route = Route.Detail(it.id) },
                        onAction = { performAction(it) },
                        onUpdateAll = {
                            manifest?.apps?.filter { appStates[it.id] == AppState.UPDATE_AVAILABLE }
                                ?.forEach { performAction(it) }
                        },
                        onUninstall = { performUninstall(it) }
                    )
                    StoreTab.SETTINGS -> SettingsScreen(
                        state = SettingsState(
                            owner = owner, repo = repo, branch = branch, token = token,
                            wifiOnly = wifiOnly, autoLoadUpdates = autoLoad,
                            showTestBuilds = showTestBuilds, selfUpdateEnabled = selfUpdateEnabled,
                            friends = combinedFriends
                        ),
                        onSaveSource = { o, rp, br ->
                            scope.launch { app.config.setContentSource(o, rp, br); viewModel.refresh() }
                        },
                        onSaveToken = { t -> scope.launch { app.config.setGithubToken(t); viewModel.refresh() } },
                        onToggleWifiOnly = { v -> scope.launch { app.config.setWifiOnly(v) } },
                        onToggleAutoLoad = { v -> scope.launch { app.config.setAutoLoadUpdates(v) } },
                        onToggleTestBuilds = { v -> scope.launch { app.config.setShowTestBuilds(v); viewModel.refresh() } },
                        onToggleSelfUpdate = { v -> scope.launch { app.config.setSelfUpdateEnabled(v) } },
                        onCheckSelfUpdate = { viewModel.checkSelfUpdate(announceResult = true) },
                        onAddFriend = { name -> scope.launch { app.config.addLocalFriend(name) } },
                        onRemoveFriend = { name -> scope.launch { app.config.removeLocalFriend(name) } },
                        onShareInvite = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Schau dir meinen privaten App-Store an: https://github.com/$owner/$repo")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Einladungslink teilen"))
                        }
                    )
                }
                is Route.Detail -> {
                    val entry = manifest?.apps?.firstOrNull { it.id == r.appId }
                    if (entry != null) {
                        AppDetailScreen(
                            entry = entry,
                            state = appStates[entry.id] ?: AppState.NOT_INSTALLED,
                            onBack = { route = Route.Tab(StoreTab.STORE) },
                            onAction = { performAction(entry) },
                            onUninstall = { performUninstall(entry) }
                        )
                    }
                }
            }

            toast?.let { message ->
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                    InstalledToast(message)
                }
                LaunchedEffect(message) {
                    delay(2200)
                    viewModel.clearToast()
                }
            }
        }
    }

    InstallFlowHost(
        installStep = installStep,
        onDismiss = { viewModel.dismissInstallSheet() },
        onConfirm = { entry -> viewModel.confirmInstall(entry) },
        onOpenUnknownSourcesSettings = { onOpenUnknownSourcesSettings(app.apkInstaller.unknownSourcesSettingsIntent()) },
        onLaunchInstall = { entry ->
            context.startActivity(viewModel.launchInstall(entry))
            viewModel.dismissInstallSheet()
            viewModel.showToast("Installation von ${entry.name} gestartet")
        },
        onError = { message ->
            viewModel.showToast(message)
            viewModel.dismissInstallSheet()
        }
    )
}

@Composable
private fun InstallFlowHost(
    installStep: InstallStep,
    onDismiss: () -> Unit,
    onConfirm: (AppEntry) -> Unit,
    onOpenUnknownSourcesSettings: () -> Unit,
    onLaunchInstall: (AppEntry) -> Unit,
    onError: (String) -> Unit
) {
    when (val step = installStep) {
        is InstallStep.NeedsPermission -> InstallConfirmSheet(
            entry = step.entry,
            onDismiss = onDismiss,
            onConfirm = onOpenUnknownSourcesSettings
        )
        is InstallStep.Confirming -> InstallConfirmSheet(
            entry = step.entry,
            onDismiss = onDismiss,
            onConfirm = { onConfirm(step.entry) }
        )
        is InstallStep.Downloading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            DownloadProgressSheet(entry = step.entry, progress = step.progress)
        }
        is InstallStep.ReadyToInstall -> LaunchedEffect(step) {
            onLaunchInstall(step.entry)
        }
        is InstallStep.Error -> LaunchedEffect(step) {
            onError("${step.entry.name}: ${step.message}")
        }
        InstallStep.Idle -> {}
    }
}

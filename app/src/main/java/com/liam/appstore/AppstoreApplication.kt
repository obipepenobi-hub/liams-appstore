package com.liam.appstore

import android.app.Application
import com.liam.appstore.data.AppRepository
import com.liam.appstore.data.GithubClient
import com.liam.appstore.data.InstalledApps
import com.liam.appstore.data.StoreConfig
import com.liam.appstore.install.ApkInstaller
import com.liam.appstore.security.ApkVerifier
import com.liam.appstore.update.SelfUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppstoreApplication : Application() {

    lateinit var config: StoreConfig
        private set
    lateinit var githubClient: GithubClient
        private set
    lateinit var installedApps: InstalledApps
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var apkInstaller: ApkInstaller
        private set
    lateinit var selfUpdateManager: SelfUpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        config = StoreConfig(this)
        githubClient = GithubClient()
        installedApps = InstalledApps(this)
        repository = AppRepository(config, githubClient, installedApps)
        apkInstaller = ApkInstaller(this, githubClient.rawClient(), ApkVerifier(this))
        selfUpdateManager = SelfUpdateManager(this, config, githubClient)

        CoroutineScope(Dispatchers.IO).launch {
            apkInstaller.cleanupStale()
            runCatching { config.migrateLegacyToken() }
        }
    }
}

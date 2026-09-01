plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.liam.appstore"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.liam.appstore"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.10"
    }

    signingConfigs {
        // Fester Debug-Keystore statt des von AGP automatisch generierten -
        // auf GitHub-Actions-Runnern gibt es keinen persistenten
        // ~/.android/debug.keystore, jeder CI-Lauf bekommt sonst einen
        // NEUEN zufälligen Signierschlüssel. Damit signiert jede Version
        // anders, Android verweigert das Update-Install ueber die
        // bestehende App mit "App nicht installiert" (Signaturkonflikt).
        // Mit diesem eingecheckten Keystore signieren alle Builds - lokal
        // wie in der CI - identisch.
        getByName("debug") {
            storeFile = file("liams-appstore-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.activity:activity-ktx:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

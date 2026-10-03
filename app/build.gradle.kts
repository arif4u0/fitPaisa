import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Secrets: read from android/local.properties (git-ignored) or CI env vars.
// Defaults are placeholders — the app will not connect until real values exist.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun configValue(propName: String, envName: String, default: String): String =
    localProperties.getProperty(propName) ?: System.getenv(envName) ?: default

android {
    namespace = "com.fitpaisa.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitpaisa.app"
        minSdk = 28 // Health Connect requirement (M1)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"${configValue("supabase.url", "SUPABASE_URL", "YOUR_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${configValue("supabase.anon.key", "SUPABASE_ANON_KEY", "YOUR_SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${configValue("google.web.client.id", "GOOGLE_WEB_CLIENT_ID", "YOUR_GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            // Dev variant installs alongside prod: com.fitpaisa.app.dev
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ENV", "\"dev\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "ENV", "\"prod\"")
            // Release signing is configured locally per README (keytool).
            // Do NOT commit keystore files or passwords.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.workmanager)

    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)

    implementation(libs.credentials)
    implementation(libs.credentials.play.auth)
    implementation(libs.googleid)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}

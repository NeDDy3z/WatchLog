import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.neddy.watchlog"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    val localProperties = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
    }
    val tvdbApiKey = localProperties.getProperty("TVDB_API_KEY") ?: ""

    defaultConfig {
        applicationId = "com.neddy.watchlog"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TVDB_API_KEY", "\"$tvdbApiKey\"")
    }

    // Release signing is opt-in. Each RELEASE_* value is read from local.properties, a Gradle
    // property (-P or ~/.gradle/gradle.properties) or an environment variable, in that order.
    fun signingValue(name: String): String? =
        (localProperties.getProperty(name) ?: providers.gradleProperty(name).orNull ?: System.getenv(name))
            ?.takeIf { it.isNotBlank() }

    val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
        ?.replace(Regex("^~"), System.getProperty("user.home"))
        ?.let { path -> File(path).takeIf(File::exists) ?: rootProject.file(path).takeIf(File::exists) }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS") ?: "releasekey"
                keyPassword = signingValue("RELEASE_KEY_PASSWORD") ?: signingValue("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

base.archivesName.set("watchlog-v${android.defaultConfig.versionName}")

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

fun signingValue(
    propertyKey: String,
    envKey: String,
): String? = keystoreProperties.getProperty(propertyKey) ?: System.getenv(envKey)

val localPropertiesFile = rootProject.file("local.properties")
val localProperties =
    Properties().apply {
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }

val debugApiBaseUrl: String? =
    localProperties.getProperty("rosaparks.debugApiBaseUrl")
        ?: System.getenv("ROSAPARKS_DEBUG_API_BASE_URL")

/**
 * Payer details the Blikk probe screen prefills, standing in for the
 * "forskráðar upplýsingar" a real signup flow would have on file. Read from
 * local.properties (gitignored) so a kennitala and a bank account number never
 * enter the repository. Absent → the fields start empty and the probe still
 * runs in anonymous mode.
 */
fun localOrEmpty(key: String): String = localProperties.getProperty(key) ?: ""

android {
    namespace = "is.rosaparks"
    compileSdk = 36

    defaultConfig {
        applicationId = "is.rosaparks"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("ROSAPARKS_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("ROSAPARKS_VERSION_NAME") ?: "1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://rp.xj.is\"")

        // Empty in every build except debug, where the block below fills them
        // from local.properties. A release APK therefore carries no kennitala
        // and no account number, whatever is configured locally.
        buildConfigField("String", "BLIKK_TEST_KENNITALA", "\"\"")
        buildConfigField("String", "BLIKK_TEST_BBAN", "\"\"")
        buildConfigField("String", "BLIKK_TEST_NAME", "\"\"")

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        // Explicit debug signing config bound to a project-relative keystore
        // path. Without this, AGP regenerates ~/.android/debug.keystore every
        // time it considers the auto-managed one "invalid", which makes the
        // SHA-256 fingerprint drift across CI runs and breaks Android App
        // Links verification (assetlinks.json stops matching). Mirrors the
        // ContactDialer pattern (ContactDialer#44 / #45).
        //
        // Local developer machines fall back to ~/.android/debug.keystore so
        // `./gradlew assembleDebug` keeps working out of the box. When App
        // Links land, create a `rosaparks-debug-keystore` GCP secret and
        // restore it to .signing/debug.keystore in the build workflow.
        getByName("debug") {
            val pinnedKeystore = rootProject.file(".signing/debug.keystore")
            val localKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
            val resolved =
                when {
                    pinnedKeystore.exists() -> pinnedKeystore
                    localKeystore.exists() -> localKeystore
                    else -> null
                }
            if (resolved != null) {
                storeFile = resolved
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            // If neither file exists (fresh CI runner without the
            // rosaparks-debug-keystore secret restored yet), leave AGP's
            // default behaviour intact — it will auto-generate
            // ~/.android/debug.keystore on first build. The SHA-pin benefit
            // only kicks in once the secret + the CI restore step land.
        }
        create("release") {
            val storeFilePath = signingValue("storeFile", "ROSAPARKS_UPLOAD_KEYSTORE_PATH")
            val storePwd = signingValue("storePassword", "ROSAPARKS_UPLOAD_KEYSTORE_PASSWORD")
            val alias = signingValue("keyAlias", "ROSAPARKS_UPLOAD_KEY_ALIAS")
            val keyPwd = signingValue("keyPassword", "ROSAPARKS_UPLOAD_KEY_PASSWORD")

            if (storeFilePath != null && storePwd != null && alias != null && keyPwd != null) {
                storeFile = file(storeFilePath)
                storePassword = storePwd
                keyAlias = alias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Point a debug install at a local `wrangler dev` instead of the
            // deployed worker. Set `rosaparks.debugApiBaseUrl` in
            // local.properties (gitignored) or ROSAPARKS_DEBUG_API_BASE_URL in
            // the environment, e.g. http://192.168.1.28:8888 — the phone must
            // be on the same network. Needed for the Blikk rail probe, whose
            // sales-channel key lives only on the developer machine. Falls
            // back to the production URL, so an unconfigured debug build keeps
            // behaving exactly as before.
            debugApiBaseUrl?.let { buildConfigField("String", "API_BASE_URL", "\"$it\"") }
            buildConfigField("String", "BLIKK_TEST_KENNITALA", "\"${localOrEmpty("rosaparks.blikkTestKennitala")}\"")
            buildConfigField("String", "BLIKK_TEST_BBAN", "\"${localOrEmpty("rosaparks.blikkTestBban")}\"")
            buildConfigField("String", "BLIKK_TEST_NAME", "\"${localOrEmpty("rosaparks.blikkTestName")}\"")
        }
        release {
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig =
                if (releaseSigning.storeFile?.exists() == true) releaseSigning else null
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // No-op for the current dependency set — the three bundled .so files
            // (MapLibre, AndroidX graphics-path, DataStore shared-counter) ship
            // pre-stripped, so extractReleaseNativeSymbolTables emits nothing and
            // BUNDLE-METADATA/com.android.tools.build.nativedebug/ is absent.
            // Retained as future-proofing for any first-party native code. See #111.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.maplibre.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.androidx.ui.tooling)
}

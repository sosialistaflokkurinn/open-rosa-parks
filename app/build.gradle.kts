import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
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

android {
    namespace = "is.rosaparks"
    compileSdk = 36

    defaultConfig {
        applicationId = "is.rosaparks"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("ROSAPARKS_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("ROSAPARKS_VERSION_NAME") ?: "1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://rosa-parks.gudrodur.workers.dev\"")

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
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

    lint {
        // AGP 9.2 + Kotlin 2.3 — NonNullableMutableLiveDataDetector throws
        // IncompatibleClassChangeError during lintVitalAnalyzeRelease. The app
        // uses StateFlow exclusively, no LiveData, so the check has nothing to
        // do here regardless.
        disable += "NullSafeMutableLiveData"
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
    debugImplementation(libs.androidx.ui.tooling)
}

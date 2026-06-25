import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingProperties = Properties()
val signingPropertiesFile = listOf(
    rootProject.file("signing.properties"),
    rootProject.file("keystore.properties"),
).firstOrNull { it.isFile }

if (signingPropertiesFile != null) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

fun signingProperty(name: String): String? =
    signingProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

val releaseStoreFile = signingProperty("storeFile")?.let { configuredPath ->
    val candidate = file(configuredPath)
    if (candidate.isAbsolute) candidate else rootProject.file(configuredPath)
}

val hasCompleteReleaseSigning = listOf(
    signingProperty("storePassword"),
    signingProperty("keyAlias"),
    signingProperty("keyPassword"),
).all { !it.isNullOrBlank() } && releaseStoreFile?.isFile == true

android {
    namespace = "com.powerbridge.app"
    compileSdk = 34

    signingConfigs {
        if (hasCompleteReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingProperty("storePassword")
                keyAlias = signingProperty("keyAlias")
                keyPassword = signingProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.powerbridge.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "0.7.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            if (hasCompleteReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

if (signingPropertiesFile != null && !hasCompleteReleaseSigning) {
    logger.warn(
        "PowerBridge Android signing properties were found at ${signingPropertiesFile.path}, " +
            "but release signing is incomplete or the keystore file is missing. " +
            "assembleRelease will continue without local signing."
    )
} else if (hasCompleteReleaseSigning && signingPropertiesFile != null) {
    logger.lifecycle("PowerBridge Android release signing is enabled from ${signingPropertiesFile.path}.")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}

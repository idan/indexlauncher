plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciVersionCode = providers.gradleProperty("ciVersionCode").orNull?.let { value ->
    requireNotNull(value.toIntOrNull()?.takeIf { it in 1..2_100_000_000 }) {
        "ciVersionCode must be an integer between 1 and 2100000000"
    }
} ?: 1
val ciVersionNameSuffix = providers.gradleProperty("ciVersionNameSuffix").getOrElse("")

// Only distribution builds provide these. Local builds keep Android's default debug key.
val devSigning = listOf(
    "DEV_KEYSTORE_PATH",
    "DEV_KEYSTORE_PASSWORD",
    "DEV_KEY_ALIAS",
    "DEV_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull }
val useDevSigning = devSigning.values.any { it != null }
require(!useDevSigning || devSigning.values.all { !it.isNullOrBlank() }) {
    "Development signing requires DEV_KEYSTORE_PATH, DEV_KEYSTORE_PASSWORD, DEV_KEY_ALIAS, and DEV_KEY_PASSWORD"
}

android {
    namespace = "app.indexlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.indexlauncher"
        minSdk = 35 // Android 15+
        targetSdk = 37 // Android 17
        versionCode = ciVersionCode
        versionName = "0.1.0$ciVersionNameSuffix"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (useDevSigning) {
            create("developmentDistribution") {
                storeFile = file(devSigning.getValue("DEV_KEYSTORE_PATH")!!)
                storeType = "PKCS12"
                storePassword = devSigning.getValue("DEV_KEYSTORE_PASSWORD")
                keyAlias = devSigning.getValue("DEV_KEY_ALIAS")
                keyPassword = devSigning.getValue("DEV_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Lets development builds coexist with a store-installed release.
            applicationIdSuffix = ".debug"
            if (useDevSigning) {
                signingConfig = signingConfigs.getByName("developmentDistribution")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.window:window:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

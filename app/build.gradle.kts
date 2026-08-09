plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    baseline = file("$projectDir/detekt-baseline.xml")
}

tasks.named("check") {
    dependsOn("detekt")
}

// Room writes each version's schema JSON here so migrations are diffable in review and testable.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Expose the exported schemas to instrumented tests so MigrationTestHelper can replay them.
android.sourceSets.getByName("androidTest") {
    assets.srcDir("$projectDir/schemas")
}

// Debug signing settings come from the environment first (CI exports them after decoding the
// keystore secret) and fall back to a Gradle property, so a local override can live in
// ~/.gradle/gradle.properties instead of anywhere inside the repo.
fun debugSigningSetting(envName: String, propertyName: String): String? =
    providers.environmentVariable(envName)
        .orElse(providers.gradleProperty(propertyName))
        .orNull
        ?.takeIf { it.isNotBlank() }

val debugKeystorePath = debugSigningSetting("MIXTAPE_DEBUG_KEYSTORE", "mixtape.debug.keystore")
val debugKeystore = debugKeystorePath?.let(::file)

if (debugKeystore != null && !debugKeystore.isFile) {
    // Falling back here would silently reintroduce the mismatched-signature problem this
    // config exists to prevent, so a configured-but-missing keystore is a hard error.
    error(
        "Debug keystore not found at ${debugKeystore.absolutePath}. " +
            "Fix or unset MIXTAPE_DEBUG_KEYSTORE / mixtape.debug.keystore. " +
            "See README.md > Debug signing."
    )
}

android {
    namespace = "pe.net.libre.mixtapehaven"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pe.net.libre.mixtapehaven"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Without this, AGP signs debug builds with a keystore it generates per machine — and
        // CI runners are a fresh machine every run — so an APK from a PR build cannot be
        // installed over a locally built one (INSTALL_FAILED_UPDATE_INCOMPATIBLE), nor over an
        // APK from the previous CI run. Pointing every build at one shared keystore keeps the
        // signature stable. The keystore itself stays out of the repo; see README.md.
        getByName("debug") {
            if (debugKeystore != null) {
                storeFile = debugKeystore
                storePassword =
                    debugSigningSetting(
                        "MIXTAPE_DEBUG_KEYSTORE_PASSWORD",
                        "mixtape.debug.keystore.password"
                    ) ?: "android"
                keyAlias =
                    debugSigningSetting(
                        "MIXTAPE_DEBUG_KEY_ALIAS",
                        "mixtape.debug.key.alias"
                    ) ?: "androiddebugkey"
                keyPassword =
                    debugSigningSetting(
                        "MIXTAPE_DEBUG_KEY_PASSWORD",
                        "mixtape.debug.key.password"
                    ) ?: "android"
            } else {
                logger.lifecycle(
                    "No shared debug keystore configured; using the auto-generated one. " +
                        "APKs from this build may not install over APKs built elsewhere. " +
                        "See README.md > Debug signing."
                )
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            // Robolectric needs the merged manifest and resources on the unit-test classpath.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.slf4j.api)
    implementation(libs.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.coil.compose)
    detektPlugins(libs.detekt.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
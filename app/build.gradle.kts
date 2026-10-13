import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

// Release signing comes from keystore.properties (local, gitignored) or environment variables (CI).
// The keystore file itself lives outside the repo.
val signingProps =
    Properties().apply {
        rootProject
            .file("keystore.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun signingValue(name: String): String? = signingProps.getProperty(name) ?: System.getenv(name)

val hasReleaseKey = signingValue("RELEASE_STORE_FILE") != null

// The release workflow passes -PversionName=1.2.3 from the git tag; versionCode is derived from it.
val appVersionName = providers.gradleProperty("versionName").getOrElse("0.1.0")
val appVersionCode =
    (appVersionName.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 } + listOf(0, 0)).let { (major, minor, patch) ->
        major * 10_000 + minor * 100 + patch
    }

android {
    namespace = "io.winstonhoyle.practicespeaking"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.winstonhoyle.practicespeaking"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = file(signingValue("RELEASE_STORE_FILE")!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Without a release key, local release builds use the debug key so they can be installed and tested.
            // The release workflow checks for the real key before building.
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/DEPENDENCIES",
                    "/META-INF/LICENSE*",
                    "/META-INF/NOTICE*",
                    "/META-INF/versions/9/previous-compilation-data.bin",
                )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // android.util.Log returns defaults in plain JVM tests (the provider classes log token usage).
            isReturnDefaultValues = true
            all { test ->
                // Live provider tests run only with -Plive; they read keys from the repo-root .env.
                test.systemProperty("live", project.hasProperty("live").toString())
                test.systemProperty("envFile", rootProject.file(".env").absolutePath)
                // -Pscreenshots renders the component gallery to docs/images for docs/DESIGN.md.
                test.systemProperty("screenshots", project.hasProperty("screenshots").toString())
                test.systemProperty("screenshotDir", rootProject.file("docs/images").absolutePath)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.anthropic.java)
    implementation(libs.openai.java)
    // Already pulled in by the SDKs; declared so the Gemini REST client can use them directly.
    implementation(libs.okhttp)
    implementation(libs.jackson.databind)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}

// Formatting: `./gradlew spotlessApply` fixes, `spotlessCheck` (in CI) verifies. Rules live in .editorconfig.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion).customRuleSets(
            listOf(
                libs.compose.rules.ktlint
                    .get()
                    .toString(),
            ),
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
}

// Static analysis on top of detekt's defaults; project tweaks in config/detekt/detekt.yml.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/debug/java")
}

// detekt 1.23 runs on its own Kotlin compiler; keep the project's newer Kotlin off its classpath.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
    }
}

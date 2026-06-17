import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.safeargs)
    alias(libs.plugins.hilt.gradle.plugin)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.apptest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.apptest"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.apptest.CustomTestRunner"

        buildConfigField(
            "String",
            "TMDB_TOKEN",
            "\"${localProperties["TMDB_TOKEN"]}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // testShared se agrega como carpeta fuente adicional
        // tanto para unit tests como para tests instrumentados
        getByName("test") {
            java.srcDirs("src/testShared/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/testShared/java")
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {

    // ── UI Components ─────────────────────────────────────────────────────────
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.fragment.ktx)

// ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

// ── Navigation ────────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

// ── Red ───────────────────────────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

// ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

// ── Images ──────────────────────────────────────────────────────────────
    implementation(libs.glide)

// ── Material ──────────────────────────────────────────────────────────────
    implementation(libs.material)
// ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    //Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    //Test-robolectric
    testImplementation(libs.robolectric)

    // Paging 3
    implementation(libs.androidx.paging.runtime.ktx)

    // Testing instrumentado (androidTest/)

    // Hilt en androidTest — permite @HiltAndroidTest y @BindValue en tests instrumentados
    androidTestImplementation(libs.hilt.android.testing)
    // KSP para Hilt en androidTest — genera el código de inyección para los tests
    kspAndroidTest(libs.hilt.compiler)
    // Navigation Testing — provee TestNavHostController para verificar navegación
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
}
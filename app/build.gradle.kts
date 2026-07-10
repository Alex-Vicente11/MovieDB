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
    namespace = "com.alexvicente.moviedb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alexvicente.moviedb"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.alexvicente.moviedb.CustomTestRunner"

        buildConfigField(
            "String",
            "TMDB_TOKEN",
            "\"${localProperties.getProperty("TMDB_TOKEN", "")}\""
        )
    }

    buildTypes {
        debug {
            // Habilita la instrumentación de bytecode para medir cobertura
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
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
            // Necesario para que JaCoCo pueda instrumentar clases con lambdas
            // que el compilador Kotlin genera sin información de línea (ej. coroutines)
            all {
                it.extensions.configure(JacocoTaskExtension::class.java) {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
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

kotlin {
    jvmToolchain(21)
}

dependencies {

    // ── Core Android ──────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)

    // ── UI ────────────────────────────────────────────────────────────────────
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.material3)
    implementation(libs.glide)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Paging 3 ──────────────────────────────────────────────────────────────
    implementation(libs.androidx.paging.runtime.ktx)

    // ── Unit tests (test/) ────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.paging.testing)

    // ── Robolectric — unit tests con contexto Android sin emulador ────────────
    testImplementation(libs.robolectric)

    // ── Instrumented tests (androidTest/) ─────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.paging.testing)
    // Hilt — permite @HiltAndroidTest y @BindValue en tests instrumentados
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    // Navigation — provee TestNavHostController para verificar navegación
    androidTestImplementation(libs.androidx.navigation.testing)
}

// ── JaCoCo report task ────────────────────────────────────────────────────────
// Combina los .exec de unit tests con las clases compiladas de Kotlin/Java
// para generar un reporte HTML en build/reports/jacoco/jacocoTestReport/html/

tasks.register("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    doLast {
        val exclusiones = listOf(
            "**/R.class",
            "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            // Hilt / Dagger
            "**/*_HiltModules*",
            "**/Hilt_*.class",
            "**/*_Factory*",
            "**/*_Provide*Factory*",
            "**/*Module_*",
            "**/*MembersInjector*",
            "**/dagger/**",
            "**/hilt_aggregated_deps/**",
            "**/dagger/hilt/internal/**",
            // Room
            "**/*_Impl*.class",
            "**/*Dao_Impl*",
            // Navigation SafeArgs
            "**/*Args*",
            "**/*Directions*",
            // DataBinding — código generado por el compilador de ViewBinding/DataBinding
            "**/databinding/**",
            "**/*Binding.class",
            "**/*BindingImpl*.class",
            // Android internals
            "android/**/*.*"
        )

        val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(exclusiones)
        }
        val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug") {
            exclude(exclusiones)
        }

        val executionData = fileTree(layout.buildDirectory.get()) {
            include(
                "outputs/unit_test_code_coverage/**/*.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        }

        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "jacocoReport",
                "classname" to "org.jacoco.ant.ReportTask",
                "classpath" to configurations.detachedConfiguration(
                    dependencies.create("org.jacoco:org.jacoco.ant:0.8.12")
                ).asPath
            )
            "jacocoReport" {
                "executiondata" {
                    executionData.forEach { file ->
                        "file"("file" to file)
                    }
                }
                "structure"("name" to "AppTest Coverage") {
                    "classfiles" {
                        kotlinClasses.forEach { "fileset"("file" to it) }
                        javaClasses.forEach { "fileset"("file" to it) }
                    }
                    "sourcefiles" {
                        "fileset"("dir" to "src/main/java")
                    }
                }
                "html"("destdir" to "${layout.buildDirectory.get()}/reports/jacoco/html")
                "xml"("destfile" to "${layout.buildDirectory.get()}/reports/jacoco/jacoco.xml")
            }
        }
    }
}
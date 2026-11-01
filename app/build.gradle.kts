import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Модель изменяемая, экраны перерисовываются по счётчику ревизий в AppViewModel,
// поэтому дочерние composable с тем же vm не должны пропускаться (strong skipping).
composeCompiler {
    @Suppress("DEPRECATION")
    enableStrongSkippingMode = false
}

// Ключ подписи лежит вне git: keystore.properties + .jks в корне проекта (см. README).
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Как в исходном APK: versionCode = минуты с 1970 года на момент сборки.
val buildVersionCode = (System.currentTimeMillis() / 60000L).toInt()

// Версия для проверки обновлений сравнивается с тегом последнего релиза GitHub (v2.1.0 → 2.1.0).
val appVersion = (project.findProperty("appVersion") as String?) ?: "2.0.0"
val updateRepo = (project.findProperty("updateRepo") as String?).orEmpty()

android {
    namespace = "com.example.pulseworkout"
    compileSdk = 35

    defaultConfig {
        // Отдельный applicationId: нативная версия ставится рядом со старой WebView-версией
        // и не требует её удалять (а значит, не стирает её данные).
        applicationId = "com.example.pulseworkout.compose"
        minSdk = 23
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = appVersion
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // UI-тесты на JVM (Robolectric) со снимками экранов (Roborazzi)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

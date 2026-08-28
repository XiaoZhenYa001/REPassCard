plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.passcard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.passcard"
        minSdk = 28
        targetSdk = 35
        versionCode = 81
        versionName = "0.81"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        if (providers.gradleProperty("enableComposeCompilerReports").orNull == "true") {
            val reportsDir = layout.buildDirectory.dir("compose_compiler").get().asFile.absolutePath
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$reportsDir",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$reportsDir"
            )
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Core KTX
    implementation(libs.core.ktx)
    
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    
    // Activity Compose
    implementation(libs.activity.compose)
    
    // Navigation
    implementation(libs.navigation.compose)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    
    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Persistent, constrained automatic cloud backups.
    implementation(libs.work.runtime.ktx)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    implementation(libs.sqlcipher.android)
    ksp(libs.room.compiler)
    
    // Material (for legacy views if needed)
    implementation(libs.material)

    // Biometric authentication
    implementation(libs.biometric)

    // SVG icon import support
    implementation(libs.androidsvg)

    // Installs generated Baseline Profiles for sideloaded builds.
    implementation(libs.profileinstaller)
    baselineProfile(project(":benchmark"))

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

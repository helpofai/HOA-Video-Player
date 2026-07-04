import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val admobAppId = localProperties.getProperty("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
val admobBannerId = localProperties.getProperty("ADMOB_BANNER_ID") ?: "ca-app-pub-3940256099942544/6300978111"
val admobInterstitialId = localProperties.getProperty("ADMOB_INTERSTITIAL_ID") ?: "ca-app-pub-3940256099942544/1033173712"
val admobRewardedId = localProperties.getProperty("ADMOB_REWARDED_ID") ?: "ca-app-pub-3940256099942544/5354046379"
val admobNativeId = localProperties.getProperty("ADMOB_NATIVE_ID") ?: "ca-app-pub-3940256099942544/2247696110"

android {
    namespace = "com.helpofai.videoplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.helpofai.videoplayer"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$admobNativeId\"")
            
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5354046379\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.animation:animation-graphics:1.6.0")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.reorderable)
    
    // FFmpegKit (video processing) — local AAR + JARs in app/libs/
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    
    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.1.0")
}
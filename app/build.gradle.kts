plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.project"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.project"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase (existing entries, allowing BOM to manage versions where possible)
    // It's generally best to put platform(libs.firebase.bom) at the very top of Firebase dependencies.
    implementation(platform(libs.firebase.bom)) // Keep this line as is, ensuring it's loaded first for Firebase.
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.inappmessaging.ktx)
    implementation(libs.firebase.analytics) // From libs.toml

    // Specific Firebase versions you listed (BOM should override these if they conflict, but keeping them as you requested)
    // Note: If issues persist, consider removing these explicit versions if their libs.ktx equivalents are present.
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    implementation("com.google.firebase:firebase-database") // This one is not .ktx

    // Kotlin Coroutines and Lifecycle KTX (ESSENTIAL ADDITIONS for suspend functions and lifecycleScope)
    // Use the same version as kotlinx-coroutines-play-services if possible for consistency.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Or your desired latest stable version

    // Firebase Coroutines extensions (already present, ensuring it's consistent with above coroutine versions)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")


    implementation(libs.car.ui.lib)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(libs.firebase.firestore) // This seems redundant with firestore.ktx and the explicit version above

    //for FCM
    //implementation("com.google.firebase:firebase-messaging") // Your commented out line
    implementation("com.google.firebase:firebase-messaging:24.1.1") // Your active FCM line
    //implementation 'com.google.firebase:firebase-messaging-ktx:23.0.0' // Your commented out ktx line

    //those two above are for conversation with backend
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")


    implementation("com.google.android.material:material:1.11.0") // Your explicit material version
    implementation("androidx.appcompat:appcompat:1.6.1") // Your explicit appcompat version


    //for bottom menu
    implementation(libs.material.v110) // Your material from libs.toml
    implementation(libs.glide)
}
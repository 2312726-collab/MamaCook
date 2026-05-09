plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.mamacook"
    compileSdk = 36 

    defaultConfig {
        applicationId = "com.example.mamacook"
        minSdk = 26
        targetSdk = 36
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

        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase & Play Services
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Location (Dùng bản 21.0.1 để đảm bảo độ ổn định cao nhất cho demo)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Facebook Login
    implementation(libs.facebook.login)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide & Other
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")

    // Thêm AI Gemini vào đây nè:
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("com.google.guava:guava:31.1-android")

    // Thư viện hỗ trợ Java 8
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Luban - Nén ảnh siêu tốc độ
    implementation("com.github.Curzibn:Luban:1.1.8")
}

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.mamacook"
    compileSdk = 36

    // Hiếu: Định nghĩa signingConfigs TRƯỚC KHI sử dụng trong buildTypes
    signingConfigs {
        create("sharedDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.example.mamacook"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc API Key từ local.properties (Phần này giữ nguyên của người khác)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        getByName("debug") {
            // Sửa lỗi: Sử dụng toán tử [] để truy cập signingConfig một cách chắc chắn hơn
            signingConfig = signingConfigs["sharedDebug"]
        }
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
    implementation(libs.camera.camera2.pipe)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Khai báo rõ ràng để fix triệt để lỗi NonNull
    implementation("androidx.annotation:annotation:1.9.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-functions")
    implementation(libs.play.services.auth)

    // Thêm App Check ở đây:
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    implementation("com.google.firebase:firebase-storage")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Facebook Login
    implementation(libs.facebook.login)

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Thư viện hỗ trợ Java 8+ (Desugaring)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Thư viện cắt ảnh chuyên nghiệp
    implementation("io.github.canhub:android-image-cropper:4.5.0")

    // THƯ VIỆN CẦN THIẾT CHO AI VÀ KIỂM DUYỆT
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("com.google.guava:guava:33.0.0-android")

    // Luban - Nén ảnh siêu tốc độ
    implementation("com.github.Curzibn:Luban:1.1.8")


    // ZXing for QR Code
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

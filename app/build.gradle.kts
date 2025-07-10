plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sakurapainting.mediaprogramandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sakurapainting.mediaprogramandroid"
        minSdk = 19  // Android 4.4 (API level 19)
        targetSdk = 22  // 兼容Android 4.4，避免权限问题
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Lint配置 - 针对Android 4.4兼容性项目
    lint {
        disable += "ExpiredTargetSdkVersion"  // 忽略targetSdk过低警告，这是为了Android 4.4兼容性
        abortOnError = false  // 不因为警告终止构建
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
}

dependencies {
    // AndroidX兼容库 - 向后兼容Android 4.4
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.6.1")
    
    // MQTT客户端库 - Paho Android
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // JSON处理 - 使用Android系统自带的org.json
    // implementation("org.json:json:20210307") // 注释掉，使用系统自带
    
    // 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    
    // 权限处理
    implementation("androidx.core:core:1.8.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
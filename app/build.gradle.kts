import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = "com.wannabeinseoul.seoulpublicservice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wannabeinseoul.seoulpublicservice"
        minSdk = 28
        targetSdk = 34
        versionCode = 7
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SEOUL_KEY", properties.getProperty("SEOUL_KEY"))
        buildConfigField("String", "WEATHER_SHORT_KEY", properties.getProperty("WEATHER_SHORT_KEY"))
        buildConfigField("String", "KMA_API_KEY", properties.getProperty("KMA_API_KEY"))
        buildConfigField("String", "TEMP_API_KEY", properties.getProperty("TEMP_API_KEY"))
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    //retrofit
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("io.coil-kt:coil:2.3.0")

    implementation("androidx.room:room-runtime:2.6.1") // Room 라이브러리
    ksp("androidx.room:room-compiler:2.6.1") // Room의 애노테이션 프로세서
    implementation("androidx.room:room-ktx:2.6.1")

    // 지도 페이지 관련 라이브러리
    implementation("com.naver.maps:map-sdk:3.17.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.firebase:firebase-firestore-ktx:24.10.3")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //새로고침 관련 라이브러리
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}

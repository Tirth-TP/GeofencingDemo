import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {

    lateinit var apiKey: String

    namespace = "com.example.geofencingdemo"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.geofencingdemo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        project.rootProject.file("local.properties").inputStream().use { input ->
            properties.load(input)
        }

        //return empty key in case something goes wrong
        apiKey = properties.getProperty("MAP_API_KEY") ?: ""
        manifestPlaceholders["GOOGLE_MAP_KEY"] = apiKey
    }

    buildTypes {
        debug {
            resValue("string", "google_maps_key", apiKey)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-database:20.3.1")
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //Google MAP
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    //Google Places
    implementation("com.google.android.libraries.places:places:3.3.0")
    //Google Location (Provide Geofencing)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation("org.greenrobot:eventbus:3.3.1")
}
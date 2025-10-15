plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.dev.mymusicapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dev.mymusicapp"
        minSdk = 24
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
    }

    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Firebase - Para AppDistribuition
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    // ExoPlayer - Para o leitor de música
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")

    // Room - Para o banco de dados de playlists
    val roomVersion = "2.8.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // RecyclerView e CardView - Para as listas
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // Material Design - Para interfaces e alertas
    implementation("com.google.android.material:material:1.13.0")

    // Glide - Para carregar as capas dos álbuns
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))
}
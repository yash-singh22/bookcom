import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load supabase.properties (separate from local.properties to avoid Android Studio overwriting)
val supabaseProperties = Properties()
val supabasePropertiesFile = rootProject.file("supabase.properties")
if (supabasePropertiesFile.exists()) {
    FileInputStream(supabasePropertiesFile).use { stream ->
        supabaseProperties.load(stream)
    }
}

val supabaseUrl: String = supabaseProperties.getProperty("SUPABASE_URL") ?: ""
val supabaseAnonKey: String = supabaseProperties.getProperty("SUPABASE_ANON_KEY") ?: ""

android {
    namespace = "com.example.bookcom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bookcom"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Supabase BuildConfig fields
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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
    
    buildFeatures {
        buildConfig = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.drawerlayout)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
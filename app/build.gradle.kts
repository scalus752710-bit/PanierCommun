plugins { id("com.android.application") }

android {
    namespace = "fr.paniercommun.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "fr.paniercommun.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 29
        versionName = "1.28"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}

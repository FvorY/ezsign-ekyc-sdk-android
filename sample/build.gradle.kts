plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.fvory.ekyc.sample"
    compileSdk = 34

    defaultConfig {
        // Megvii liveness only works for package names registered on the ezSign license.
        // Override with: ./gradlew :sample:assembleDebug -PekycApplicationId=<registered.package>
        applicationId = providers.gradleProperty("ekycApplicationId")
            .getOrElse("com.sign.livenessdemo")
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":ekyc-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}

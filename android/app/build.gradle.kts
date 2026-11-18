plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mangareader.local"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mangareader.local"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2"
    }

    // Kotlin + Compose app lives in src/main/kotlin, JVM unit tests in src/test/kotlin.
    sourceSets.getByName("main").java.setSrcDirs(listOf("src/main/kotlin"))
    // The server's API contract (contract/*.json) is a test resource: the parser is checked against it.
    sourceSets.getByName("test").resources.directories.add("../../contract")

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    val camerax = "1.6.2"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    testImplementation("junit:junit:4.13.2")
    // android.jar only has stubs of org.json; unit tests on the JVM need the real implementation
    testImplementation("org.json:json:20240303")
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.takusemba.spotlight"

    compileSdk = 32
    defaultConfig {
        minSdk = 14
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "io.shortcut.library"
            artifactId = "spotlight"
            version = "1.0.0-alpha01"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.22")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.transition:transition-ktx:1.4.1")
}

// apply { from("../publish.gradle") }

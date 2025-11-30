import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.protobuf)
    id("kotlin-parcelize")
}

android {
    namespace = "dev.ujhhgtg.pandorasbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.ujhhgtg.pandorasbox"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
//            isMinifyEnabled = true
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
//        mlModelBinding = true
    }
}

dependencies {
    // Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.foundation)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.google.android.material)
//    implementation(libs.miuix)

    // YukiHookAPI
//    implementation(libs.yukihookapi.api)
//    implementation(libs.kavaref.core)
//    implementation(libs.kavaref.extension)
//    compileOnly(libs.xposed.api)
//    ksp(libs.yukihookapi.ksp.xposed)

    // ML Kit
//    implementation(libs.pose.detection.accurate)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // LiteRT
//    implementation(libs.litert)
//    implementation(libs.litert.metadata)
//    implementation(libs.litert.support)
//    implementation(libs.litert.gpu)

    // Kotlin Reflection
    implementation(libs.kotlin.reflect)

    // WebSocket
    implementation(libs.java.websocket)

    // UPnP & DLNA
//    implementation(libs.cling.core)
//    implementation(libs.cling.support)
    implementation(libs.jupnp)
    implementation(libs.jupnp.android)
    implementation(libs.nanoHttpd)
    implementation(libs.javax.servlet.api)
    implementation(libs.jetty.server)
    implementation(libs.jetty.client)
    implementation(libs.jetty.servlet)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.android)

    // ProtoBuf
    implementation(libs.androidx.datastore.core)
    implementation(libs.protobuf.javalite)

    // ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)

    // Rhino
    implementation(libs.rhino)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}

//protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:3.14.0"
//    }
//
//    generateProtoTasks {
//        all().each { task ->
//            task.builtins {
//                java {
//                    option 'lite'
//                }
//            }
//        }
//    }
//}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.14.0"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
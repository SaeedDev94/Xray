plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools.ksp)
    id("kotlin-parcelize")
}

val abiId: String by project
val abiTarget: String by project

fun calcVersionCode(): Int {
    val versionCodeFile = file("versionCode.txt")
    val versionCode = versionCodeFile.readText().trim().toInt()
    return versionCode + abiId.toInt()
}

android {
    namespace = "io.github.saeeddev94.xray"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.saeeddev94.xray"
        minSdk = 26
        targetSdk = 36
        versionCode = calcVersionCode()
        versionName = "11.8.7"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    externalNativeBuild {
        ndkVersion = "28.2.13676358"
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = false
            reset()
            //noinspection ChromeOsAbiSupport
            include(*abiTarget.split(",").toTypedArray())
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("/tmp/xray.jks")
            storePassword = System.getenv("KS_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.blacksquircle.ui.editorkit)
    implementation(libs.blacksquircle.ui.language.json)
    implementation(libs.google.material)
    implementation(libs.topjohnwu.libsu.core)
    implementation(libs.yuriy.budiyev.code.scanner)
}

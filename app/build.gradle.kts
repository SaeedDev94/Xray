plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
}

val abiId: String = project.property("abiId").toString()
val abiTarget: String = project.property("abiTarget").toString()

fun calcVersionCode(): Int = file("versionCode.txt").readText().trim().let { versionCode ->
    versionCode.toInt() + abiId.toInt()
}

android {
    namespace = "io.github.saeeddev94.xray"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.saeeddev94.xray"
        minSdk = 26
        targetSdk = 37
        versionCode = calcVersionCode()
        versionName = "12.7.0"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        ndkVersion = "29.0.14206865"
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

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.topjohnwu.libsu.core)
    implementation(libs.yuriy.budiyev.code.scanner)
}

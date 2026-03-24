import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("ServerDatabase") {
            packageName.set("org.cuak.sshapp")
            dialect(libs.sqldelight.dialect)
        }
    }
}

kotlin {

    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }


    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(compose.materialIconsExtended)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.okio)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.koin)
                implementation(libs.voyager.transitions)
                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)
                implementation(libs.multiplatform.settings.noarg)
            }
        }

        // --- SOURCE SET COMPARTIDO (Android + JVM) ---
        val sharedJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.hierynomus:sshj:${libs.versions.sshj.get()}") {
                    exclude(group = "org.bouncycastle")
                }

                implementation(libs.bouncycastle.prov)
                implementation(libs.bouncycastle.pkix)
                implementation(libs.slf4j.nop)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")
            }
        }

        val androidMain by getting {
            dependsOn(sharedJvmMain)

            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.koin.android)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayer.rtsp) // Vital para cámaras
                implementation(libs.androidx.media3.ui)

            }
        }

        val jvmMain by getting {
            dependsOn(sharedJvmMain)

            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.javacv)
                implementation(libs.ffmpeg)
                val osName = System.getProperty("os.name").lowercase()
                val osArch = System.getProperty("os.arch").lowercase()
                val nativeClassifier = when {
                    osName.contains("linux") && (osArch == "amd64" || osArch == "x86_64") -> "linux-x86_64"
                    osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64") -> "linux-arm64" // Para Raspberry Pi o VMs ARM
                    osName.contains("windows") && (osArch == "amd64" || osArch == "x86_64") -> "windows-x86_64"
                    osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> "macosx-arm64" // Mac M1/M2/M3
                    osName.contains("mac") -> "macosx-x86_64" // Mac Intel antiguo
                    else -> throw IllegalStateException("Sistema no soportado automáticamente: $osName $osArch")
                }
                val ffmpegVersion = libs.versions.ffmpeg.get()
                implementation("org.bytedeco:ffmpeg:$ffmpegVersion:$nativeClassifier")
            }
        }

    }
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
android {
    namespace = "org.cuak.sshapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["KEY_ALIAS"] as String? ?: ""
            keyPassword = keystoreProperties["KEY_PASSWORD"] as String? ?: ""
            storeFile = keystoreProperties["KEYSTORE_FILE"]?.let { file(it as String) }
            storePassword = keystoreProperties["KEYSTORE_PASSWORD"] as String? ?: ""
        }
    }
    defaultConfig {
        applicationId = "org.cuak.sshapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.cuak.sshapp.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "org.cuak.sshapp"
            packageVersion = "1.0.1"
            modules("java.sql")
        }
    }
}
tasks.findByName("jvmRun")?.configure<JavaExec> {
    mainClass.set("org.cuak.sshapp.MainKt")
}
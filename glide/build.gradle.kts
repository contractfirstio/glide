import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.components.resources)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.pdfbox)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "GlideKt"

        jvmArgs("-Dapple.awt.application.name=Glide")

        nativeDistributions {
            // Bundled JRE is built with jlink and omits modules not listed here.
            // jdk.httpserver is required for Google OAuth loopback (HttpServer on port 8765).
            modules("java.instrument", "java.management", "jdk.httpserver", "jdk.unsupported")

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Glide"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "glide.generated.resources"
}

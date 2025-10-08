import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    // jetbrainsCompose
    id("org.jetbrains.compose")
    // compose-compiler
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.movcontext"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RemoveConfig"
            packageVersion = "1.0.0"
            windows{
//                console = true
                dirChooser = true
                menuGroup = "幕境"
                iconFile.set(project.file("src/main/resources/remove.ico"))
            }
        }
    }
}

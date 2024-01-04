
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.0"
}

group = "com.movcontext"
version = "2.2.13"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "18"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("org.jetbrains.compose.material3:material3:1.0.1")
                implementation ("org.jetbrains.compose.material:material-icons-extended:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                implementation("io.github.microutils:kotlin-logging:2.1.21")
                implementation("uk.co.caprica:vlcj:4.8.2")
//               如果不添加这个库，字幕描述会出现乱码，native-streams 3.0.0 有问题，暂时使用 2.0.0
                implementation("uk.co.caprica:native-streams:2.0.0")
                implementation("com.formdev:flatlaf:3.1")
                implementation("com.formdev:flatlaf-extras:2.6")
                implementation("org.apache.opennlp:opennlp-tools:1.9.4")
                implementation("org.apache.pdfbox:pdfbox:2.0.24")
                implementation("com.squareup.okhttp3:okhttp:4.10.0")
                implementation(files("lib/ebml-reader-0.1.1.jar"))
                implementation(files("lib/subtitleConvert-1.0.2.jar"))
                implementation(files("lib/jacob-1.20.jar"))
                implementation("org.apache.maven:maven-artifact:3.8.6")
                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
                implementation("com.github.albfernandez:juniversalchardet:2.4.0")
                implementation("junit:junit:4.13.2")
                implementation("org.junit.vintage:junit-vintage-engine:5.9.0")
                implementation("com.darkrockstudios:mpfilepicker:2.0.2")
                implementation("net.bramp.ffmpeg:ffmpeg:0.7.0")
                implementation("org.apache.poi:poi:5.2.5")
                implementation("org.apache.poi:poi-ooxml:5.2.5")
                implementation("org.xerial:sqlite-jdbc:3.44.1.0")
            }
        }
//        val jvmTest by getting {
//            dependencies {
//                implementation(compose.desktop.uiTestJUnit4)
//                implementation(compose.desktop.currentOs)
//            }
//        }
    }
}


tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}




/**
 *  `src/main/resources` 文件夹里的文件会被打包到 MovContext.jar 里面，然后通过 getResource 访问，
 *   只读文件可以放在 `src/main/resources` 文件夹里面，需要修改的文件不能放在这个文件夹里面
 */
compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf("-client")
        jvmArgs += listOf("-Dfile.encoding=UTF-8")
        jvmArgs += listOf("-Dapple.awt.application.appearance=system")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "幕境"
            packageVersion = version.toString()
            modules("java.compiler","java.instrument","java.prefs", "java.sql", "jdk.unsupported","jdk.accessibility")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            copyright = "Copyright 2023 Shimin Tang. All rights reserved."
            vendor = "深圳市龙华区幕境网络工作室"
            licenseFile.set(project.file("LICENSE"))
            windows{
//                console = true
                dirChooser = true
                menuGroup = "幕境"
                iconFile.set(project.file("src/jvmMain/resources/logo/logo.ico"))
            }
            macOS{
                iconFile.set(project.file("src/jvmMain/resources/logo/logo.icns"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/logo/logo.png"))
            }
        }
    }
}



import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.0"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "com.movcontext"
version = "2.3.1"

buildConfig {
    buildConfigField("APP_NAME", provider { "幕境" })
    buildConfigField("APP_VERSION", provider { "v${project.version}" })
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.compose.material3:material3:1.0.1")
    implementation ("org.jetbrains.compose.material:material-icons-extended:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("uk.co.caprica:vlcj:4.8.2")
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
    implementation ("io.ktor:ktor-client-core:1.6.7")
    implementation ("io.ktor:ktor-client-cio:1.6.7")
    implementation ("net.java.dev.jna:jna:5.14.0")
    implementation ("net.java.dev.jna:jna-platform:5.14.0")
}


tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
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
        jvmArgs += listOf("-Dstdout.encoding=UTF-8")
        jvmArgs += listOf("-Dstderr.encoding=UTF-8")
        jvmArgs += listOf("-Dsun.stdout.encoding=UTF-8")
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
                iconFile.set(project.file("src/main/resources/logo/logo.ico"))
            }
            macOS{
                iconFile.set(project.file("src/main/resources/logo/logo.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/logo/logo.png"))
            }
        }
    }
}

val createVlcCacheDistributable by tasks.registering{
    group = "compose desktop"
    description = "Run vlc-cache-gen to generate VLC Plugins cache"
    val createDistributable = tasks.named<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>("createDistributable")
    dependsOn(createDistributable)
    doLast{
        println("Running custom task to generate VLC Plugins cache")
        val cacheGen =  project.layout.projectDirectory.dir("build/compose/binaries/main/app/幕境/app/resources/VLC/vlc-cache-gen.exe").getAsFile().absolutePath
        val plugins =  project.layout.projectDirectory.dir("build/compose/binaries/main/app/幕境/app/resources/VLC/plugins").getAsFile().absolutePath

        val command = listOf(cacheGen, plugins)
        try {
            val process = ProcessBuilder(command).start()
            process.waitFor()
        } catch (e: Exception) {
            println("Error running vlc-cache-gen: ${e.message}")
        }
    }
}

tasks.withType(JavaCompile::class.java) {
    options.encoding = "UTF-8"
}

project.afterEvaluate {
    val os = System.getProperty("os.name", "generic").lowercase()
    // 如果是 windows 系统，需要在打包之前运行 vlc-cache-gen 生成 VLC 插件缓存
    if(os.indexOf("windows") >= 0){
        tasks.named("prepareAppResources") {
            doLast {
                println("Running custom task after prepareAppResources")
                val plugins =  project.layout.projectDirectory.dir("build/compose/tmp/prepareAppResources/VLC/plugins").getAsFile().absolutePath
                val cacheGen =  project.layout.projectDirectory.dir("build/compose/tmp/prepareAppResources/VLC/vlc-cache-gen.exe").getAsFile().absolutePath
                val command = listOf(cacheGen, plugins)
                try {
                    val process = ProcessBuilder(command).start()
                    process.waitFor()
                } catch (e: Exception) {
                    println("Error running vlc-cache-gen: ${e.message}")
                }

            }
        }
    }else if(os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0){
     // github 的下载地址不支持中文名称，所以需要在打包之后修改安装包的名称
        tasks.register("renameDmg") {
            doLast {
                val dmgFile = fileTree("${buildDir}/compose/binaries/main/dmg") {
                    include("*.dmg")
                }.singleFile
                val arch = System.getProperty("os.arch").lowercase()
                val newDmgFile = file("${dmgFile.parentFile}/MuJing-${project.version}-${arch}.dmg")
                if (newDmgFile.exists()) {
                    newDmgFile.delete()
                }

                dmgFile.renameTo(newDmgFile)
            }
        }
        tasks.named("packageDmg") {
            finalizedBy("renameDmg")
        }
    }



}

apply(from = "wix.gradle.kts")


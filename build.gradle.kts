
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.nio.file.Files

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.0"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "com.movcontext"
version = "2.6.0"

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
    implementation(files("lib/ebml-reader-0.1.1.jar"))
    implementation(files("lib/subtitleConvert-1.0.3.jar"))
    implementation(files("lib/jacob-1.20.jar"))
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")
    implementation("com.darkrockstudios:mpfilepicker:2.0.2")
    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation ("io.ktor:ktor-client-core:2.3.11")
    implementation ("io.ktor:ktor-client-cio:2.3.11")
    implementation ("net.java.dev.jna:jna:5.14.0")
    implementation ("net.java.dev.jna:jna-platform:5.14.0")
    implementation ("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")

    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation("org.junit.vintage:junit-vintage-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.commons:commons-compress:1.26.0")
        classpath("org.tukaani:xz:1.10")
    }
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
            modules("java.compiler","java.instrument","java.management","java.prefs", "java.security.jgss","java.sql", "jdk.unsupported","java.xml.crypto","jdk.accessibility", "java.naming" )
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

tasks.withType(JavaCompile::class.java) {
    options.encoding = "UTF-8"
}

// 第一次编译之前要解压缩词典文件
tasks.named("compileKotlin") {
    doFirst{
        val dictFile = layout.projectDirectory.dir("resources/common/dictionary/ecdict.db").asFile
        if (!dictFile.exists()) {
            println("解压缩词典文件")
            val input = layout.projectDirectory.dir("dict/ecdict.7z").asFile
            val destination = layout.projectDirectory.dir("resources/common/dictionary").asFile
            decompressDict(input, destination)
        }
    }
}

tasks.named<Test>("test") {
    description = "Runs unit tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    exclude("**/ui/**")
}

tasks.register<Test>("uiTest") {
    description = "Runs UI tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/ui/**")
}

project.afterEvaluate {
    val os = System.getProperty("os.name", "generic").lowercase()
    if(os.indexOf("windows") >= 0){
        tasks.named("prepareAppResources") {
            doFirst {
                val pluginsCache =  layout.projectDirectory.dir("resources/windows/VLC/plugins/plugins.dat").asFile
                if(pluginsCache.exists()){
                    println("Delete the VLC plugin cache produced after launching the program from Main.kt")
                    pluginsCache.delete()
                }
            }
        }

        tasks.named("runDistributable") {
            doFirst {
                println("update VLC plugins cache")
                val plugins = project.layout.projectDirectory.dir("build/compose/binaries/main/app/幕境/app/resources/VLC/plugins").asFile.absolutePath
                val cacheGen = project.layout.projectDirectory.dir("build/compose/binaries/main/app/幕境/app/resources/VLC/vlc-cache-gen.exe").asFile.absolutePath
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
                val dmgFile = fileTree( project.layout.projectDirectory.dir("build/compose/binaries/main/dmg")) {
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


/**
 * 解压缩 7z 文件
 */
@Throws(IOException::class)
fun decompressDict(input: File, destination: File) {
    SevenZFile.builder()
        .setSeekableByteChannel(Files.newByteChannel(input.toPath()))
        .get().use { sevenZFile ->

        val entry = sevenZFile.nextEntry
        if (entry != null && !entry.isDirectory) {
            val outFile = File(destination, entry.name)
            outFile.parentFile.mkdirs()

            FileOutputStream(outFile).use { out ->
                val content = ByteArray(entry.size.toInt())
                sevenZFile.read(content, 0, content.size)
                out.write(content)
            }
        }
    }
}
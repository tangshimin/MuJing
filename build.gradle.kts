import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.nio.file.Files
import org.gradle.api.tasks.Exec
import java.io.File
import java.net.URL
import java.io.InputStream

plugins {
    // 版本设置在 settings.gradle.kts 的 plugins 块中
    // kotlin
    kotlin("jvm")
    // jetbrainsCompose
    id("org.jetbrains.compose")
    // compose-compiler
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "com.movcontext"
version = "2.9.1"

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
    implementation ("org.jetbrains.compose.material:material-icons-extended:1.0.1")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("uk.co.caprica:vlcj:4.11.0")
    implementation("com.formdev:flatlaf:3.6.1")
    implementation("com.formdev:flatlaf-extras:2.6")
    implementation("org.apache.opennlp:opennlp-tools:1.9.4")
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    implementation(files("lib/ebml-reader-0.1.1.jar"))
    implementation(files("lib/subtitleConvert-1.0.3.jar"))
    implementation(files("lib/jacob-1.20.jar"))
    implementation("org.apache.maven:maven-artifact:3.9.11")
    implementation("sh.calvin.reorderable:reorderable:3.0.0")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")
    implementation("com.darkrockstudios:mpfilepicker:2.0.2")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation ("io.ktor:ktor-client-core:2.3.13")
    implementation ("io.ktor:ktor-client-cio:2.3.11")
    implementation ("net.java.dev.jna:jna:5.14.0")
    implementation ("net.java.dev.jna:jna-platform:5.14.0")
    implementation ("ch.qos.logback:logback-classic:1.5.13")
    implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")
    implementation("io.github.vinceglb:filekit-dialogs:0.10.0")
    implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0")


    // 如果需要 Compose UI 测试，保留这个
    testImplementation(compose.desktop.uiTestJUnit4)
    // 测试依赖 - 使用完整的 JUnit 5 配置
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
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
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
}


// 构建 Rust JNI 库 ---
val buildRustZstdJni by tasks.registering(Exec::class) {
    group = "build"
    description = "Build rust-zstd-jni native library via cargo"
    workingDir = file("rust-zstd-jni")

    val cargoPath = resolveCargoPath()
    doFirst {
        println("Using cargo: $cargoPath")
        // 确保 PATH 包含常见 cargo 安装目录
        val home = System.getProperty("user.home") ?: System.getenv("HOME") ?: ""
        val extra = sequenceOf(
            if (home.isNotEmpty()) "$home/.cargo/bin" else "",
            "/opt/homebrew/bin",
            "/usr/local/bin"
        ).filter { it.isNotEmpty() }.joinToString(File.pathSeparator)
        val currentPath = System.getenv("PATH") ?: ""
        environment("PATH", listOf(currentPath, extra).filter { it.isNotEmpty() }.joinToString(File.pathSeparator))
    }

    // 显式指定可执行文件与参数
    executable = cargoPath
    args("build", "--release")

    // 若 cargo 存在但构建失败，应当让任务失败以暴露错误
    isIgnoreExitValue = false

    // 仅当子项目存在时执行
    onlyIf { file("rust-zstd-jni/Cargo.toml").exists() }
}


/**
 *  `src/main/resources` 文件夹里的文件会被打包到 MovContext.jar 里面，然后通过 getResource 访问，
 *   只读文件可以放在 `src/main/resources` 文件夹里面，需要修改的文件不能放在这个文件夹里面
 */
compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-server", //server 模式
            "-XX:+UnlockExperimentalVMOptions",// 解锁实验性 JVM 选项

            "-Xms64m",  //初始堆大小
            "-Xmx1g", //最大堆大小
            "-XX:NewRatio=1", // 年轻代与老年代比例 1:1
            "-XX:SurvivorRatio=8", // Eden 与 Survivor 比例

            // ZGC 优化参数
            "-XX:+UseZGC", // ZGC 垃圾回收器
            "-XX:+ZGenerational", // 启用分代 ZGC（JDK 21+）
            "-XX:+ZUncommit", // ZGC 垃圾回收器的未使用内存归还功能
            "-XX:ZUncommitDelay=5", // 缩短内存归还延迟
            "-XX:ZCollectionInterval=2", // 更频繁的 GC

            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dapple.awt.application.appearance=system",
            "-Dcompose.swing.render.on.graphics=true", // 直接在 Swing 组件上渲染 Compose
            "-Dcompose.interop.blending=true" // 让 Compose 能显示在 Swing 组件上面
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "幕境"
            packageVersion = version.toString()
            modules("java.compiler","java.instrument","java.management","java.prefs", "java.security.jgss","jdk.security.auth","java.sql", "jdk.unsupported","java.xml.crypto","jdk.accessibility", "java.naming" )
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
val decompressDictionary by tasks.registering {
    group = "build"
    description = "Decompress ecdict database if missing"
    doLast {
        val dictFile = layout.projectDirectory.dir("resources/common/dictionary/ecdict.db").asFile
        if (!dictFile.exists()) {
            println("解压缩词典文件")
            val input = layout.projectDirectory.dir("dict/ecdict.7z").asFile
            val destination = layout.projectDirectory.dir("resources/common/dictionary").asFile
            decompressDict(input, destination)
        } else {
            println("词典已存在，跳过解压缩")
        }
    }
}

tasks.named("compileKotlin") {
    // 移除模型下载依赖，只保留 ffmpeg 准备
    dependsOn("prepareFfmpeg")
}

tasks.register("prepareFfmpeg") {
    group = "verification"
    description = "Fix permissions, remove quarantine"
    // 依赖字典解压任务，确保顺序：先解压 -> 再准备 ffmpeg -> 再编译
    dependsOn(decompressDictionary)
    doLast {
        if (!org.gradle.internal.os.OperatingSystem.current().isMacOsX) return@doLast
        val arch = System.getProperty("os.arch").lowercase()
        val ffmpegPath = if (arch == "arm" || arch == "aarch64")
            "resources/macos-arm64/ffmpeg/ffmpeg"
        else
            "resources/macos-x64/ffmpeg/ffmpeg"
        val f = file(ffmpegPath)
        if (f.exists()) {
            f.setExecutable(true)
            fun run(vararg cmd: String) {
                try { project.exec { commandLine(*cmd) } } catch (_: Exception) {}
            }
            run("xattr", "-dr", "com.apple.quarantine", f.absolutePath)
            println("Prepared ffmpeg: $ffmpegPath")
        } else {
            println("ffmpeg not found at $ffmpegPath")
        }
    }
}

// 解压缩 7z 文件
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

// 下载 Whisper 模型文件到测试资源目录
val downloadWhisperModels by tasks.registering {
    group = "verification"
    description = "Download Whisper model files for testing if missing"

    doLast {
        // 修改为测试资源目录下的 whisper 文件夹
        val modelsDir = layout.projectDirectory.dir("src/test/resources/whisper").asFile
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        // 需要下载的模型列表
        val models = listOf("base.en")

        for (model in models) {
            val modelFile = File(modelsDir, "ggml-$model.bin")
            if (!modelFile.exists()) {
                println("下载 Whisper 模型到测试资源目录: $model")
                downloadWhisperModel(model, modelsDir)
            } else {
                println("测试用 Whisper 模型 $model 已存在，跳过下载")
            }
        }
    }
}

// 确保测试前已构建 Rust JNI 库和下载测试模型
tasks.named<Test>("test") {
    dependsOn(buildRustZstdJni)
    dependsOn("downloadWhisperModels")  // 添加模型下载依赖
    // 排除 JNI 端到端测试，避免在无 cargo 环境失败
    exclude("**/fsrs/zstd/**")
    description = "Runs unit tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    exclude("**/ui/**")

    // 将需要最后执行的测试类从默认 test 任务中排除
    exclude("**/util/TestRemoveConfigDependencies*")
    exclude("**/util/TestRuntimeModules*")

    // 启用 JUnit 5（包含 Vintage 引擎时也可运行 JUnit 4）
    useJUnitPlatform()
    
    // 配置测试执行顺序（Jupiter）
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation")

    // 测试日志配置
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// 新增一个只运行“最后执行”的测试类的任务，并让其在 test 任务之后执行
val testLast by tasks.registering(Test::class) {
    description = "Runs tests that must execute last."
    group = "verification"

    // 复用测试编译产物与依赖
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    // 仅包含需要最后执行的测试类
    include("**/util/TestRemoveConfigDependencies*")
    include("**/util/TestRuntimeModules*")

    // 使用 JUnit Platform（同时覆盖 JUnit 5 与 Vintage）
    useJUnitPlatform()

    // 确保类级别顺序按 @Order 注解执行
    systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation")

    // 与默认 test 任务保持一致的日志配置
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// 当执行 `gradlew test` 时，确保上述两个测试在最后执行
tasks.named("test") {
    finalizedBy(testLast)
}

// 让 `check` 任务覆盖到最后执行的测试（通过 finalizedBy 已涵盖，这里显式依赖更清晰）
tasks.named("check") {
    dependsOn(testLast)
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

// 解析 cargo 路径（优先级：-PcargoPath > CARGO 环境变量 > 常见安装路径列表 > PATH 中的 cargo）
fun resolveCargoPath(): String {
    // 允许通过 -PcargoPath 显式指定
    val propPath = (project.findProperty("cargoPath") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    if (propPath != null && File(propPath).canExecute()) return propPath

    // 允许通过环境变量 CARGO 指定
    val envCargo = System.getenv("CARGO")?.trim()?.takeIf { it.isNotEmpty() }
    if (envCargo != null && File(envCargo).canExecute()) return envCargo

    val home = System.getProperty("user.home") ?: System.getenv("HOME") ?: ""
    val candidates = buildList {
        if (home.isNotEmpty()) add("$home/.cargo/bin/cargo")
        // macOS Homebrew
        add("/opt/homebrew/bin/cargo")
        // 常见 Linux/macOS 路径
        add("/usr/local/bin/cargo")
        add("/usr/bin/cargo")
    }
    val hit = candidates.firstOrNull { File(it).canExecute() }
    if (hit != null) return hit

    // 回退到 PATH 中的 cargo（若存在）
    return "cargo"
}

/**
 * 下载 Whisper 模型文件
 * 参考 whisper.cpp 的下载脚本
 */
fun downloadWhisperModel(model: String, modelsDir: File) {
    val src = "https://huggingface.co/ggerganov/whisper.cpp"
    val pfx = "resolve/main/ggml"
    val url = "$src/$pfx-$model.bin"
    val outputFile = File(modelsDir, "ggml-$model.bin")

    println("正在从 '$src' 下载 ggml 模型 $model ...")

    try {
        // 使用 Java 的 URL 类进行下载
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Gradle)")

        connection.getInputStream().use { input: InputStream ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                val contentLength = connection.contentLengthLong
                var lastProgress = -1

                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    // 显示下载进度 - 单行更新
                    if (contentLength > 0) {
                        val progress = (totalBytes * 100L / contentLength).toInt()
                        if (progress != lastProgress && progress % 5 == 0) {
                            val mbTotal = contentLength / 1024L / 1024L
                            val mbDownloaded = totalBytes / 1024L / 1024L
                            print("\r下载进度: ${mbDownloaded}MB / ${mbTotal}MB ($progress%)")
                            System.out.flush()
                            lastProgress = progress
                        }
                    }
                    bytesRead = input.read(buffer)
                }
            }
        }

        // 下载完成后换行
        println("\n完成！模型 '$model' 已保存到 '${outputFile.absolutePath}'")

    } catch (e: Exception) {
        // 出错时也换行，确保错误信息显示正确
        println("\n下载 ggml 模型 $model 失败")
        println("错误信息: ${e.message}")
        println("请稍后重试或手动下载原始 Whisper 模型文件并转换。")

        // 删除可能不完整的文件
        if (outputFile.exists()) {
            outputFile.delete()
        }

        throw e
    }
}

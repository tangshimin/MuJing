package util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import player.isMacOS
import player.isWindows
import java.io.File

/**
 * RemoveConfig 依赖一致性测试
 *
 * ## 测试目的
 * 验证 RemoveConfig 模块的运行时依赖是否与 MuJing 主程序的依赖一致。
 *
 * ## 为什么需要这个测试？
 *
 * ### 问题背景
 * RemoveConfig 是一个独立的 Compose Desktop 应用程序，用于清理 MuJing 的配置文件。
 * 在打包时，RemoveConfig.exe、RemoveConfig.jar 和 RemoveConfig.cfg 会被复制到 MuJing 的安装包中，
 * 与 MuJing 共享同一个运行时环境（包括 JVM 和所有依赖库）。
 *
 * ### 发现的 Bug
 * 在 2025 年 10 月，发现了一个严重的 bug：RemoveConfig.exe 无法启动。
 * 原因是：
 * - MuJing 和 RemoveConfig 使用了不同版本的 `kotlinx-coroutines-core`
 * - MuJing: 1.10.2
 * - RemoveConfig: 1.8.1
 * - 当 RemoveConfig.exe 在 MuJing 的运行时环境中启动时，找到的是 MuJing 的依赖版本
 * - 版本不匹配导致 RemoveConfig 无法正常启动
 *
 * ### 根本原因
 * RemoveConfig 作为一个独立的 Gradle 模块，其依赖可能会随着时间推移与 MuJing 主程序产生差异。
 * 因为它们不共享 `build.gradle.kts` 的依赖声明，所以很容易出现版本不一致的情况。
 *
 * ## 测试策略
 *
 * ### 测试内容
 * 1. 自动执行两个模块的 `createDistributable` 任务，生成最新的构建产物
 * 2. 提取并解析两个模块的所有 jar 依赖（库名 + 版本号）
 * 3. 验证 RemoveConfig 的每个依赖都存在于 MuJing 中，且版本完全一致
 *
 * ### 为什么是子集关系？
 * RemoveConfig 的依赖必须是 MuJing 依赖的**子集**，因为：
 * - RemoveConfig 使用 MuJing 的运行时环境
 * - MuJing 可以有额外的依赖（不影响 RemoveConfig）
 * - 但 RemoveConfig 不能有 MuJing 中不存在的依赖
 * - 所有共同依赖的版本必须完全一致
 *
 * ## 预防措施
 *
 * ### 如何避免类似问题？
 * 1. **定期运行此测试**：建议在 CI/CD 流程中自动运行
 * 2. **依赖更新时运行**：每次更新依赖后都应该运行此测试
 * 3. **发布前验证**：每次发布前必须运行此测试
 *
 * ### 修复依赖不一致的方法
 * 如果测试失败，需要在 `RemoveConfig/build.gradle.kts` 中显式声明依赖版本：
 * ```kotlin
 * dependencies {
 *     implementation(compose.desktop.currentOs)
 *     // 强制使用与 MuJing 一致的版本
 *     implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
 * }
 * ```
 *
 * ## 测试执行
 *
 * ### 运行测试
 * ```bash
 * ./gradlew test --tests util.TestRemoveConfigDependencies
 * ```
 *
 * ### 测试流程
 * 1. 自动执行 `createDistributable` 构建两个模块（不检查缓存，确保最新）
 * 2. 扫描 `build/compose/binaries/main/app` 目录中的所有 jar 文件
 * 3. 解析 jar 文件名，提取库名和版本号（处理各种格式，如 `1.0`、`1.10.2`、`0.9.22.2` 等）
 * 4. 比较两个模块的依赖列表
 * 5. 如果发现不一致，输出详细的差异信息
 *
 */
class TestRemoveConfigDependencies {

    @Test
    fun testRemoveConfigDependenciesMatchMuJing() {
        // 先执行 createDistributable 任务
        ensureDistributablesBuilt()

        // MuJing 的 app 目录
        val mujingAppDir = File("build/compose/binaries/main/app")
        // RemoveConfig 的 app 目录
        val removeConfigAppDir = File("RemoveConfig/build/compose/binaries/main/app")

        // 检查目录是否存在，如果不存在给出提示
        require(mujingAppDir.exists()) { "MuJing app 目录不存在，createDistributable 任务执行失败" }
        require(removeConfigAppDir.exists()) { "RemoveConfig app 目录不存在，createDistributable 任务执行失败" }

        // 获取 MuJing 的 app 子目录（在 macOS 上是 .app，其他系统可能不同）
        val mujingRealAppDir: File = requireNotNull(findAppDirectory(mujingAppDir)) {
            "找不到 MuJing 的实际 app 目录"
        }

        val removeConfigRealAppDir: File = requireNotNull(findAppDirectory(removeConfigAppDir)) {
            "找不到 RemoveConfig 的实际 app 目录"
        }

        // 获取两个模块的 jar 文件列表（只取文件名，去掉哈希部分）
        val mujingJars = getJarDependencies(mujingRealAppDir)
        val removeConfigJars = getJarDependencies(removeConfigRealAppDir)
            .filter { !it.nameWithoutHash.startsWith("RemoveConfig") } // 排除 RemoveConfig 相关的 jar

        println("MuJing 依赖数量: ${mujingJars.size}")
        println("RemoveConfig 依赖数量: ${removeConfigJars.size} (已排除 RemoveConfig jar)")

        // 找出 RemoveConfig 中有但 MuJing 中没有的依赖
        val extraInRemoveConfig = removeConfigJars.filter { jar ->
            !mujingJars.any { it.nameWithoutHash == jar.nameWithoutHash }
        }

        // 找出版本不一致的依赖
        val versionMismatches = mutableListOf<String>()
        removeConfigJars.forEach { removeJar ->
            val mujingJar = mujingJars.find { it.nameWithoutHash == removeJar.nameWithoutHash }
            if (mujingJar != null && mujingJar.version != removeJar.version) {
                versionMismatches.add(
                    "${removeJar.nameWithoutHash}: MuJing=${mujingJar.version}, RemoveConfig=${removeJar.version}"
                )
            }
        }

        // 输出差异信息
        if (extraInRemoveConfig.isNotEmpty()) {
            println("\n❌ RemoveConfig 中存在但 MuJing 中不存在的依赖:")
            extraInRemoveConfig.forEach { jar ->
                println("  - ${jar.nameWithoutHash} (${jar.version})")
            }
        }

        if (versionMismatches.isNotEmpty()) {
            println("\n❌ 版本不一致的依赖:")
            versionMismatches.forEach { println("  - $it") }
        }

        // 断言：RemoveConfig 的所有依赖必须在 MuJing 中存在且版本一致
        // 因为 RemoveConfig.exe 会被复制到 MuJing 的安装包中，使用 MuJing 的运行时环境
        val hasError = extraInRemoveConfig.isNotEmpty() || versionMismatches.isNotEmpty()
        assertTrue(
            !hasError,
            "RemoveConfig 的依赖必须是 MuJing 依赖的子集，且版本必须一致！\n" +
                    "额外依赖: ${extraInRemoveConfig.size} 个\n" +
                    "版本不一致: ${versionMismatches.size} 个"
        )

        println("\n✅ 测试通过: RemoveConfig 的依赖是 MuJing 依赖的子集，且版本一致")
    }

    /**
     * 每次都执行 createDistributable，确保使用最新的构建产物
     */
    private fun ensureDistributablesBuilt() {
        val workDir = File("").absolutePath

        // 每次都执行 createDistributable，不检查缓存
        println("正在执行 MuJing createDistributable...")
        executeGradleTask("createDistributable", workDir)

        println("正在执行 RemoveConfig createDistributable...")
        executeGradleTask("createDistributable", File(workDir, "RemoveConfig").absolutePath)
    }

    /**
     * 执行 Gradle 任务
     */
    private fun executeGradleTask(task: String, workingDir: String) {
        val gradlewFile = File(workingDir, if (isWindows()) "gradlew.bat" else "gradlew")

        // 如果没有执行权限，设置执行权限
        if (!gradlewFile.canExecute()) {
            gradlewFile.setExecutable(true)
        }

        val gradlewCommand = if (isMacOS()) "./${gradlewFile.name}" else gradlewFile.name
        val command = mutableListOf<String>()

        if (isWindows()) {
            command.add("cmd")
            command.add("/c")
        }
        if (isMacOS()) {
            command.add("/bin/bash")
            command.add("-c")
        }
        command.add("$gradlewCommand $task")

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(File(workingDir))
        val process = processBuilder.start()

        // 读取输出
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("Gradle 任务执行失败:")
            println("Output: $output")
            println("Error: $error")
            throw RuntimeException("执行 Gradle 任务 '$task' 失败，退出码: $exitCode")
        } else {
            println("✓ Gradle 任务 '$task' 执行成功")
        }
    }

    /**
     * 查找实际的 app 目录
     * macOS: 幕境.app/Contents/app 或 RemoveConfig.app/Contents/app
     * Windows/Linux: 幕境/app 或 RemoveConfig/app
     */
    private fun findAppDirectory(baseDir: File): File? {
        // 先查找 .app 目录（macOS）
        val appBundle = baseDir.listFiles()?.find { it.name.endsWith(".app") }
        if (appBundle != null) {
            val contentsApp = File(appBundle, "Contents/app")
            if (contentsApp.exists()) {
                return contentsApp
            }
        }

        // 查找普通目录（Windows/Linux）
        val normalDir = baseDir.listFiles()?.find { it.isDirectory && !it.name.endsWith(".app") }
        if (normalDir != null) {
            val appDir = File(normalDir, "app")
            if (appDir.exists()) {
                return appDir
            }
        }

        return null
    }

    /**
     * 获取 jar 依赖列表，解析出库名和版本号
     */
    private fun getJarDependencies(appDir: File): List<JarInfo> {
        return appDir.listFiles { file -> file.extension == "jar" }
            ?.map { file ->
                parseJarName(file.name)
            }
            ?.filterNotNull()
            ?: emptyList()
    }

    /**
     * 解析 jar 文件名，提取库名和版本号
     * 例如: kotlinx-coroutines-core-1.10.2-abc123.jar
     * -> nameWithoutHash: kotlinx-coroutines-core, version: 1.10.2
     * 例如: skiko-awt-runtime-macos-arm64-0.9.22.2-abc123.jar
     * -> nameWithoutHash: skiko-awt-runtime-macos-arm64, version: 0.9.22.2
     */
    private fun parseJarName(fileName: String): JarInfo? {
        if (!fileName.endsWith(".jar")) return null

        // 移除 .jar 后缀
        val nameWithoutExtension = fileName.removeSuffix(".jar")

        // 使用正则表达式匹配版本号模式
        // 版本号通常是数字和点号的组合，如 1.0, 1.10.2, 0.9.22.2
        val versionPattern = Regex("""(\d+\.[\d.]+)""")
        val versionMatch = versionPattern.findAll(nameWithoutExtension).lastOrNull()

        if (versionMatch != null) {
            val version = versionMatch.value
            val versionStart = versionMatch.range.first

            // 找到版本号前最后一个 '-' 的位置
            val nameEnd = nameWithoutExtension.lastIndexOf('-', versionStart - 1)
            if (nameEnd > 0) {
                val nameWithoutHash = nameWithoutExtension.substring(0, nameEnd)
                return JarInfo(nameWithoutHash, version, fileName)
            }
        }

        // 如果无法识别版本号，使用原来的逻辑
        val parts = nameWithoutExtension.split("-")
        if (parts.size < 2) return JarInfo(nameWithoutExtension, "", fileName)

        val versionIndex = parts.indexOfLast { part ->
            part.isNotEmpty() && (part[0].isDigit() || part.contains('.'))
        }

        if (versionIndex == -1) {
            return JarInfo(nameWithoutExtension, "", fileName)
        }

        val nameWithoutHash = parts.subList(0, versionIndex).joinToString("-")
        val version = parts[versionIndex]

        return JarInfo(nameWithoutHash, version, fileName)
    }

    data class JarInfo(
        val nameWithoutHash: String,
        val version: String,
        val fullName: String
    )
}

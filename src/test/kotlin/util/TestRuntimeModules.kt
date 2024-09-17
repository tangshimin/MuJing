package util

import org.junit.Test
import org.junit.jupiter.api.condition.EnabledIf
import player.isMacOS
import player.isWindows
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

/**
 * 有时候增加了一些代码，本地运行没有问题，但是打包后运行时会出现一些问题，比如找不到类，这时候可能是因为没有包含某些模块。
 * 这个测试用例会输出需要包含的模块, 如果模块不匹配，就修改 `build.gradle.kts` 中的 `nativeDistributions`
 */
class TestRuntimeModules {

    @Test
    fun `Test Native Distribution Modules`() {
        val workDir = File("").absolutePath
        val gradlewFile = File(workDir, "gradlew")

        // 如果没有执行权限，设置执行权限
        if (!gradlewFile.canExecute()) {
            gradlewFile.setExecutable(true)
        }

        val task = "${if(isMacOS()) "./" else ""}gradlew suggestRuntimeModules"
        val command = mutableListOf<String>()
        if(isWindows()){
            command.add("cmd")
            command.add("/c")
        }
        if(isMacOS()){
            command.add("/bin/bash")
            command.add("-c")
        }
        command.add(task)

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(File(workDir))
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        println("Output:\n$output")
        assert(output.contains("Suggested runtime modules to include:"))
        val expectModules = "modules(\"java.compiler\", \"java.instrument\", \"java.management\", \"java.prefs\", \"java.security.jgss\", \"java.sql\", \"java.xml.crypto\", \"jdk.unsupported\")"
        assert(output.contains(expectModules)) { "Expect $expectModules in output" }
    }

    @Test
    @EnabledIf("isNotGitHubActionsWindows")
    fun `Test Run Distributable`() {
        val workDir = File("").absolutePath
        val gradlewFile = File(workDir, "gradlew")

        // 如果没有执行权限，设置执行权限
        if (!gradlewFile.canExecute()) {
            gradlewFile.setExecutable(true)
        }

        val task = "${if(isMacOS()) "./" else ""}gradlew runDistributable"
        val command = mutableListOf<String>()
        if(isWindows()){
            command.add("cmd")
            command.add("/c")
        }
        if(isMacOS()){
            command.add("/bin/bash")
            command.add("-c")
        }
        command.add(task)

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(File(workDir))
        val process = processBuilder.start()

        val outputReader = process.inputStream.bufferedReader()
        val errorReader = process.errorStream.bufferedReader()

        val output = StringBuilder()
        val error = StringBuilder()

        val outputThread = Thread {
            outputReader.forEachLine { line ->
                output.append(line).append("\n")
            }
        }

        val errorThread = Thread {
            errorReader.forEachLine { line ->
                error.append(line).append("\n")
            }
        }

        outputThread.start()
        errorThread.start()
        // 启动一个线程来定期检测“幕境”进程是否启动
        val executor = Executors.newSingleThreadScheduledExecutor()
        var destroyFailed = false
        executor.scheduleAtFixedRate({
            val newProcess = findProcessByName("幕境")
            if (newProcess != null) {

                // 如果幕境进程已经启动，等待 20 秒后关闭
                // 如第一次关闭失败，不再等待
                if(!destroyFailed){
                    println("'幕境' 进程已启动，等待 20 秒后关闭...")
                    Thread.sleep(20 * 1000)
                }
                val destroyed = newProcess.destroy()
                if(destroyed) {
                    println("'幕境' 进程已关闭")
                } else {
                    destroyFailed = true
                    println("无法关闭 '幕境' 进程，等待 1 分钟后再次尝试关闭...")
                }
                if(destroyed){
                    executor.shutdown()
                }
            }
        }, 0, 1, TimeUnit.SECONDS)

        process.waitFor()
        outputThread.join()
        errorThread.join()

        println("Output:\n$output")
        println("Error:\n$error")
        assert(!error.contains("java.lang.NoClassDefFoundError"))
    }
}

fun findProcessByName(processName: String): ProcessHandle? {
    return ProcessHandle.allProcesses()
        .filter { it.info().command().orElse("").contains(processName) }
        .findFirst().getOrNull()
}

/**
 * 如果不是 GitHub Actions Windows 环境，返回 true
 * 用于在 GitHub Actions Windows 环境下跳过某些测试用例
 */
@Suppress("unused")
fun isNotGitHubActionsWindows(): Boolean {
    val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
    val isGitHubActions = System.getenv("GITHUB_ACTIONS")?.toBoolean() ?: false
    return !(isWindows && isGitHubActions)
}
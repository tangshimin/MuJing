package util

import org.junit.Test
import player.isMacOS
import player.isWindows
import java.io.File

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

        val task = "gradlew suggestRuntimeModules"
        val command = mutableListOf<String>()
        if(isWindows()){
            command.add("cmd")
            command.add("/c")
        }
        if(isMacOS()){
            command.add("bash")
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
}


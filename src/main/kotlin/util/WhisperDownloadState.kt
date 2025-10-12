package util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * 负责持久化当前正在下载的 Whisper 模型。
 * 文件位置：~/.MuJing/Whisper/download_state.properties
 */
object WhisperDownloadState {
    private fun stateFile(): File = File(getWhisperModelsDir(), "download_state.properties")

    data class State(
        val fileName: String,
        val name: String,
        val url: String?
    )

    fun save(option: WhisperModelOption) {
        runCatching {
            val props = Properties()
            props["fileName"] = option.fileName
            props["name"] = option.name
            props["url"] = option.url
            val f = stateFile()
            f.parentFile?.mkdirs()
            FileOutputStream(f).use { out -> props.store(out, "MuJing Whisper download state") }
        }
    }

    fun clear() {
        runCatching { stateFile().delete() }
    }

    fun read(): State? {
        val f = stateFile()
        if (!f.exists()) return null
        return runCatching {
            val props = Properties()
            FileInputStream(f).use { props.load(it) }
            val fileName = props.getProperty("fileName", "")
            val name = props.getProperty("name", "")
            val url = props.getProperty("url", null)
            if (fileName.isBlank() && name.isBlank()) null else State(fileName, name, url)
        }.getOrNull()
    }

    fun resolveOption(state: State): WhisperModelOption? {
        // 优先用 fileName 匹配
        defaultWhisperModels.firstOrNull { it.fileName == state.fileName }?.let { return it }
        // 退化用 name 匹配
        return defaultWhisperModels.firstOrNull { it.name == state.name }
    }
}

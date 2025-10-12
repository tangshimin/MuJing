package util

import state.getSettingsDirectory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * 简单的镜像站点设置持久化：保存在 ~/.MuJing/mirror.properties
 */
object MirrorSettings {
    private fun settingsFile(): File = File(getSettingsDirectory(), "mirror.properties")

    fun isHfMirrorEnabled(): Boolean {
        val f = settingsFile()
        if (!f.exists()) return false
        return runCatching {
            val props = Properties()
            FileInputStream(f).use { props.load(it) }
            props.getProperty("hfMirror", "false").toBooleanStrictOrNull() ?: false
        }.getOrDefault(false)
    }

    fun setHfMirrorEnabled(enabled: Boolean) {
        runCatching {
            val f = settingsFile()
            f.parentFile?.mkdirs()
            val props = Properties()
            if (f.exists()) {
                runCatching { FileInputStream(f).use { props.load(it) } }
            }
            props["hfMirror"] = enabled.toString()
            FileOutputStream(f).use { out -> props.store(out, "MuJing mirror settings") }
        }
    }
}


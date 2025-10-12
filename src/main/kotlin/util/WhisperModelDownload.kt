package util

import state.getSettingsDirectory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// 网络中断或临时性错误（用于 UI 显示“下载中断”）
class DownloadNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Whisper 模型信息 */
data class WhisperModelOption(
    val name: String,           // 例如：base.en
    val url: String,            // 下载地址
    val fileName: String,       // 保存的文件名，例如：ggml-base.en.bin
    val sizeHint: String = "" // 可选的大小说明
)

/** 内置的常用模型选项 */
val defaultWhisperModels = listOf(
    WhisperModelOption(
        name = "tiny",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
        fileName = "ggml-tiny.bin",
        sizeHint = "75 MB",
    ),
    WhisperModelOption(
        name = "tiny.en",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
//        url = "https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
        fileName = "ggml-tiny.en.bin",
        sizeHint = "75 MB",
    ),
    WhisperModelOption(
        name = "base",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        fileName = "ggml-base.bin",
        sizeHint = "142 MB",
    ),
    WhisperModelOption(
        name = "base.en",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
        fileName = "ggml-base.en.bin",
        sizeHint = "142 MB",
    ),
    WhisperModelOption(
        name = "small",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
        fileName = "ggml-small.bin",
        sizeHint = "466 MB",
    ),
    WhisperModelOption(
        name = "small.en",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin",
        fileName = "ggml-small.en.bin",
        sizeHint = "466 MB",
    ),
    WhisperModelOption(
        name = "small.en-tdrz",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en-tdrz.bin",
        fileName = "ggml-small.en-tdrz.bin",
        sizeHint = "465 MB",
    ),
    WhisperModelOption(
        name = "medium",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
        fileName = "ggml-medium.bin",
        sizeHint = "1.5 GiB",
    ),
    WhisperModelOption(
        name = "medium.en",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.en.bin",
        fileName = "ggml-medium.en.bin",
        sizeHint = "1.5 GiB",
    ),
    WhisperModelOption(
        name = "large-v1",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v1.bin",
        fileName = "ggml-large-v1.bin",
        sizeHint = "2.9 GiB",
    ),
    WhisperModelOption(
        name = "large-v2",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2.bin",
        fileName = "ggml-large-v2.bin",
        sizeHint = "2.9 GiB",
    ),
    WhisperModelOption(
        name = "large-v2-q5_0",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2-q5_0.bin",
        fileName = "ggml-large-v2-q5_0.bin",
        sizeHint = "1.1 GiB",
    ),
    WhisperModelOption(
        name = "large-v3",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin",
        fileName = "ggml-large-v3.bin",
        sizeHint = "2.9 GiB",
    ),
    WhisperModelOption(
        name = "large-v3-q5_0",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-q5_0.bin",
        fileName = "ggml-large-v3-q5_0.bin",
        sizeHint = "1.1 GiB",
    ),
    WhisperModelOption(
        name = "large-v3-turbo",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin",
        fileName = "ggml-large-v3-turbo.bin",
        sizeHint = "1.5 GiB",
    ),
    WhisperModelOption(
        name = "large-v3-turbo-q5_0",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
        fileName = "ggml-large-v3-turbo-q5_0.bin",
        sizeHint = "547 MB",
    )
)

/** 获取 Whisper 模型保存目录 ~/.MuJing/Whisper */
fun getWhisperModelsDir(): File {
    val dir = File(getSettingsDirectory(), "Whisper")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun parseTotalFromContentRange(header: String?): Long? {
    if (header.isNullOrBlank()) return null
    // formats like: "bytes 100-199/200" or "bytes */200"
    val slash = header.lastIndexOf('/')
    if (slash <= 0 || slash >= header.length - 1) return null
    return header.substring(slash + 1).toLongOrNull()
}

// 根据镜像设置转换下载 URL
private fun resolveUrlWithMirror(original: String): String {
    if (MirrorSettings.isHfMirrorEnabled()) {
        if (original.contains("hf-mirror.com")) return original
        return original.replace("https://huggingface.co", "https://hf-mirror.com")
            .replace("http://huggingface.co", "https://hf-mirror.com")
    }
    return original
}

/**
 * 下载 Whisper 模型文件（带进度、取消，支持断点续传）
 * @param option 模型选项
 * @param onProgress 进度回调，total 可能为 null（服务器未提供）
 * @param isCancelled 是否已取消下载
 * @return Result<File> 下载完成的文件或错误
 */
fun downloadWhisperModel(
    option: WhisperModelOption,
    onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
    isCancelled: () -> Boolean = { false }
): Result<File> {
    val targetDir = getWhisperModelsDir()
    val targetFile = File(targetDir, option.fileName)
    val tempFile = File(targetDir, option.fileName + ".part")

    try {
        // 如果已经存在完整文件，直接返回
        if (targetFile.exists() && targetFile.length() > 0L) {
            return Result.success(targetFile)
        }

        tempFile.parentFile?.mkdirs()

        var existing = if (tempFile.exists()) tempFile.length() else 0L
        var attemptedResume = existing > 0L

        fun openConnection(from: Long?): HttpURLConnection {
            val effectiveUrl = resolveUrlWithMirror(option.url)
            val url = URL(effectiveUrl)
            return (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MuJing-Whisper-Downloader")
                if (from != null && from > 0L) {
                    setRequestProperty("Range", "bytes=$from-")
                }
            }
        }

        var conn = openConnection(if (attemptedResume) existing else null)
        conn.connect()

        // 处理 416（Range 不可满足）：丢弃已有分片，重新完整下载
        if (conn.responseCode == 416) {
            try { tempFile.delete() } catch (_: Exception) {}
            existing = 0L
            attemptedResume = false
            conn.disconnect()
            conn = openConnection(null)
            conn.connect()
        }

        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            val msg = "HTTP ${code}: ${conn.responseMessage}"
            return if (code >= 500 || code == 408 || code == 429) {
                Result.failure(DownloadNetworkException(msg))
            } else {
                Result.failure(IllegalStateException(msg))
            }
        }

        val isPartial = conn.responseCode == HttpURLConnection.HTTP_PARTIAL
        val contentRangeTotal = parseTotalFromContentRange(conn.getHeaderField("Content-Range"))
        val total: Long? = when {
            contentRangeTotal != null -> contentRangeTotal
            else -> conn.contentLengthLong.takeIf { it > 0 }?.let { if (isPartial) it + existing else it }
        }

        // 如果服务器不支持 Range 或未返回 206，则从头开始，覆盖 .part
        val append = isPartial && attemptedResume
        if (!append && attemptedResume) {
            existing = 0L
            try { FileOutputStream(tempFile, false).use { /* truncate */ } } catch (_: Exception) {}
        }

        val buffer = ByteArray(1024 * 256)
        var downloaded = existing

        BufferedInputStream(conn.inputStream).use { input ->
            FileOutputStream(tempFile, append).use { output ->
                while (true) {
                    if (isCancelled()) {
                        output.flush()
                        return Result.failure(InterruptedException("下载已暂停（已保留进度）"))
                    }
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, total)
                }
                output.flush()
            }
        }

        // 原子替换
        if (targetFile.exists()) targetFile.delete()
        tempFile.renameTo(targetFile)
        return Result.success(targetFile)
    } catch (e: Exception) {
        return when (e) {
            is InterruptedException -> Result.failure(e)
            is IOException -> Result.failure(DownloadNetworkException(e.message ?: "网络异常", e))
            else -> Result.failure(e)
        }
    }
}

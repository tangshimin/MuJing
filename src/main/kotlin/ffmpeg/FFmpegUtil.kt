/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package ffmpeg

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity
import net.bramp.ffmpeg.job.FFmpegJob
import player.isWindows
import state.getResourcesFile
import state.getSettingsDirectory
import ui.dialog.replaceNewLine
import java.io.File
import javax.swing.JOptionPane

fun findFFmpegPath(): String {
    val path: String = if(isWindows()){
        getResourcesFile("ffmpeg/ffmpeg.exe").absolutePath
    }else{
        val ffmpegFile = getResourcesFile("ffmpeg/ffmpeg")
        if(ffmpegFile.exists() && !ffmpegFile.canExecute()){
            ffmpegFile.setExecutable(true)
        }
        getResourcesFile("ffmpeg/ffmpeg").absolutePath
    }
    return path
}

/**
 * 使用 FFmpeg 提取视频里的字幕,并且把字幕转换成 SRT 格式
 */
fun extractSubtitles(
    input: String, subtitleId: Int,
    output: String,
    verbosity: Verbosity = Verbosity.INFO
): String {
    return try {
        val ffmpeg = FFmpeg(findFFmpegPath())
        val builder = FFmpegBuilder()
            .setVerbosity(verbosity)
            .setInput(input)
            .addOutput(output)
            .addExtraArgs("-map", "0:s:$subtitleId") //  -map 0:s:0 表示提取第一个字幕，-map 0:s:1 表示提取第二个字幕。
            .done()
        val executor = FFmpegExecutor(ffmpeg)
        val job = executor.createJob(builder)
        job.run()
        if (job.state == FFmpegJob.State.FINISHED) {
            "finished"
        } else {
            JOptionPane.showMessageDialog(null, "提取字幕失败", "错误", JOptionPane.ERROR_MESSAGE)
            "failed"
        }
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(null, "选择的字幕格式暂时不支持\n ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
        "failed"
    }
}

/**
 * 使用 FFmpeg 把字幕格式转换成 SRT 格式
 */
fun convertToSrt(
    input:String,
    output:String,
    verbosity: Verbosity = Verbosity.INFO
    ):String{
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
        .setVerbosity(verbosity)
        .setInput(input)
        .addOutput(output)
        .done()
    val executor = FFmpegExecutor(ffmpeg)
    val job = executor.createJob(builder)
    job.run()
    if (job.state == FFmpegJob.State.FINISHED) {
        return "finished"
    }
    return "failed"
}



/**
 * 匹配富文本标签的正则表达式
 * 保留换行标签 <br />
 */

const val RICH_TEXT_REGEX = "</?(b|i|u|font|s|ruby|rt|rb|sub|sup)(\\s[^>]*)?>"
/**
 * 移除 SRT 字幕里的富文本标签
 *使用 FFmpeg 提取 mov_text 字幕时，会保留这些富文本标签，但是我们只需要纯文本，所以需要移除这些标签。
 *
 * mov_text 字幕支持以下富文本格式标签：
 * <font>: 字体样式（包括 face、size 和 color 属性）
 * <b>: 粗体文本
 * <i>: 斜体文本
 * <u>: 下划线文本
 * <s>: 删除线文本
 * <ruby>: 用于注音或解释的文本
 * <rt>: 注音文本
 * <rb>: 基本文本（与 <ruby> 一起使用）
 * <sub>: 下标文本
 * <sup>: 上标文本
 */
fun removeRichText(srtFile: File){
    var content = srtFile.readText()
    content = removeRichText(content)
    // 把换行标签 <br /> 替换成 \n
   content =  replaceNewLine(content)
    srtFile.writeText(content)
}

fun removeRichText(content: String): String {
    val richTextRegex = Regex(RICH_TEXT_REGEX)
    return richTextRegex.replace(content, "")
}


fun hasRichText(srtFile: File): Boolean {
    val content = srtFile.readText()
    val richTextRegex = Regex(RICH_TEXT_REGEX)
        return richTextRegex.containsMatchIn(content)
}
fun hasRichText(content: String): Boolean {
    val richTextRegex = Regex(RICH_TEXT_REGEX)
        return richTextRegex.containsMatchIn(content)
}


/**
 * 使用 Whisper 模型生成 SRT 字幕
 * @param input 输入视频/音频文件路径
 * @param output 输出 SRT 文件路径
 * @param modelPath Whisper 模型文件的绝对路径（必需参数）
 * @param language 语言代码 (例如 "en", "zh", "auto")
 * @param queue 队列大小，控制处理延迟和准确性的平衡
 * @return Result<Unit> 成功时返回 Result.success()，失败时返回 Result.failure()
 */
fun generateSrtWithWhisper(
    input: String,
    output: String,
    modelPath: String,
    language: String = "en",
    queue: Int = 3
): Result<Unit> {
    return try {
        val ffmpeg = FFmpeg(findFFmpegPath())

        // 检查模型文件是否存在
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            val error = "Whisper 模型文件不存在: $modelPath\n" +
                    "请运行 './gradlew downloadWhisperModels' 下载模型文件"
            println("错误: $error")
            return Result.failure(IllegalArgumentException(error))
        }

        // 将路径转换为绝对路径并统一为 Unix 风格
        val normalizedModelPath = modelFile.absolutePath.replace("\\", "/")
        val normalizedOutput = File(output).absolutePath.replace("\\", "/")

        val escapedModelPath = escapeFilterPath(normalizedModelPath)
        val escapedOutput = escapeFilterPath(normalizedOutput)

        // 构建 whisper 滤镜参数
        val whisperFilter = "whisper=model=$escapedModelPath:language=$language:queue=$queue:destination=$escapedOutput:format=srt"

        // 确保输出目录存在
        val outputFile = File(output)
        outputFile.parentFile?.mkdirs()

        val builder = FFmpegBuilder()
            .setVerbosity(Verbosity.INFO)
            .setInput(input)
            .addOutput(output)
            .addExtraArgs("-vn") // 禁用视频流
            .addExtraArgs("-af", whisperFilter)
            .addExtraArgs("-f", "null") // 输出格式为 null，因为实际输出由 whisper 滤镜处理
            .done()

        val executor = FFmpegExecutor(ffmpeg)
        val job = executor.createJob(builder)
        job.run()

        if (job.state == FFmpegJob.State.FINISHED) {
            // 检查输出文件是否生成成功
            val outputFile = File(output)
            if (outputFile.exists() && outputFile.length() > 0) {
                Result.success(Unit)
            } else {
                val error = "生成字幕文件失败，文件为空或不存在: $output"
                println("错误: $error")
                Result.failure(RuntimeException(error))
            }
        } else {
            val error = "使用 Whisper 生成字幕失败，FFmpeg 任务状态: ${job.state}"
            println("错误: $error")
            Result.failure(RuntimeException(error))
        }
    } catch (e: Exception) {
        println("使用 Whisper 生成字幕时出现错误:")
        e.printStackTrace()
        Result.failure(e)
    }
}

// 在 FFmpeg 滤镜参数中，需要转义冒号
// Windows 路径中的盘符冒号需要转义为 \\:
fun escapeFilterPath(path: String): String {
    return path.replace(":", "\\\\:")
}

/**
 * 提取选择的字幕到用户目录,字幕浏览器界面使用
 * */
fun writeSubtitleToFile(
    videoPath: String,
    trackId: Int,
): File? {
    val settingsDir = getSettingsDirectory()
    val subtitleFile = File(settingsDir, "subtitles.srt")
    val result = extractSubtitles(videoPath, trackId, subtitleFile.absolutePath)
    if(result == "finished"){
        // 检查字幕文件是否包含富文本标签
        val hasRichText = hasRichText(subtitleFile)
        if(hasRichText){
            removeRichText(subtitleFile)
        }

        return subtitleFile
    }else{
        return null
    }
}
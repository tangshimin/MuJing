package ffmpeg

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity
import net.bramp.ffmpeg.job.FFmpegJob
import player.PlayerCaption
import player.isWindows
import state.getResourcesFile
import state.getSettingsDirectory
import util.parseSubtitles
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
        return "finished"
    }else{
        JOptionPane.showMessageDialog(null, "提取字幕失败", "错误", JOptionPane.ERROR_MESSAGE)
    }
    return "failed"
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
 * 使用 FFmpeg 提取视频里的字幕，并且返回一个 Caption List
 */
fun readCaptionList(
    videoPath: String,
    subtitleId: Int,
    verbosity: Verbosity = Verbosity.INFO
    ): List<PlayerCaption> {
    val captionList = mutableListOf<PlayerCaption>()
    val applicationDir = getSettingsDirectory()
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
        .setVerbosity(verbosity)
        .setInput(videoPath)
        .addOutput("$applicationDir/temp.srt")
        .addExtraArgs("-map", "0:s:$subtitleId") //  -map 0:s:0 表示提取第一个字幕，-map 0:s:1 表示提取第二个字幕。
        .done()
    val executor = FFmpegExecutor(ffmpeg)
    val job = executor.createJob(builder)
    job.run()
    if (job.state == FFmpegJob.State.FINISHED) {
        println("extractSubtitle success")
        captionList.addAll(parseSubtitles("$applicationDir/temp.srt"))
        File("$applicationDir/temp.srt").delete()
    }
    return captionList
}


/**
 * 匹配富文本标签的正则表达式
 */
const val RICH_TEXT_REGEX = "<(b|i|u|font|s|ruby|rt|rb|sub|sup).*?>|</(b|i|u|font|s|ruby|rt|rb|sub|sup)>"


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


/**
 * 提取选择的字幕到用户目录,字幕浏览器界面使用
 * */
fun writeSubtitleToFile(
    videoPath: String,
    trackId: Int,
): File {
    val settingsDir = getSettingsDirectory()
    val subtitleFile = File(settingsDir, "subtitles.srt")
    extractSubtitles(videoPath,trackId,subtitleFile.absolutePath)
    // 检查字幕文件是否包含富文本标签
    val hasRichText = hasRichText(subtitleFile)
    if(hasRichText){
        removeRichText(subtitleFile)
    }

    return subtitleFile
}
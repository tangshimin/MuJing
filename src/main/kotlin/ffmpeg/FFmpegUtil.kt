package ffmpeg

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import player.PlayerCaption
import player.isWindows
import state.getResourcesFile
import state.getSettingsDirectory
import util.parseSubtitles
import java.io.File

fun findFFmpegPath(): String {
    val path: String = if(isWindows()){
        getResourcesFile("ffmpeg/ffmpeg.exe").absolutePath
    }else{
        getResourcesFile("ffmpeg/ffmpeg").absolutePath
    }
    return path
}

/**
 * 使用 FFmpeg 提取视频里的字幕,并且把字幕转换成 SRT 格式
 */
fun extractSubtitles(input: String,subtitleId: Int,output: String): String {
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
        .setInput(input)
        .addOutput(output)
        .addExtraArgs("-map", "0:s:$subtitleId") //  -map 0:s:0 表示提取第一个字幕，-map 0:s:1 表示提取第二个字幕。
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
 * 使用 FFmpeg 把字幕格式转换成 SRT 格式
 */
fun convertToSrt(input:String,output:String):String{
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
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
fun readCaptionList(videoPath: String, subtitleId: Int): List<PlayerCaption> {
    val captionList = mutableListOf<PlayerCaption>()
    val applicationDir = getSettingsDirectory()
    val ffmpeg = FFmpeg(findFFmpegPath())
    val builder = FFmpegBuilder()
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
    val richTextRegex = Regex("<(b|i|u|font|s|ruby|rt|rb|sub|sup).*?>|</(b|i|u|font|s|ruby|rt|rb|sub|sup)>")
    return richTextRegex.replace(content, "")
}



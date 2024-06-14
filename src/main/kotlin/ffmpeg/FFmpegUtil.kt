package ffmpeg

import player.PlayerCaption
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import player.isMacOS
import player.isWindows
import player.parseSubtitles
import state.getResourcesFile
import state.getSettingsDirectory
import java.io.File

fun findFFmpegPath(): String {
    var path = ""
    if(isWindows()){
        path =  getResourcesFile("FFmpeg/ffmpeg.exe").absolutePath
    }else if(isMacOS()){
        path =  getResourcesFile("ffmpeg").absolutePath
        if(!File(path).exists()){
            println("ffmpeg not unzipped")
            val zipFile = getResourcesFile("ffmpeg-6.0.zip")
            // 必须使用系统命令行解压缩，否则会报错
            val process = Runtime.getRuntime().exec("unzip -o ${zipFile.absolutePath} -d ${zipFile.parentFile.absolutePath}")
            process.waitFor()
            if (!process.isAlive){
                println("unzip ffmpeg success")
                val property = "compose.application.resources.dir"
                val dir = System.getProperty(property)
                //打包之后的环境，幕境已经安装到 macOS 了。
                 if (dir != null) {
                     zipFile.delete()
                     println("delete ffmpeg-6.0.zip file")
                }

            }


        }
    }
    return path
}

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
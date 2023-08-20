package ffmpeg

import player.isMacOS
import player.isWindows
import state.getResourcesFile
import java.io.File

fun findFFmpegPath(): String {
    var path = ""
    if(isWindows()){
        path =  getResourcesFile("ffmpeg.exe").absolutePath
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
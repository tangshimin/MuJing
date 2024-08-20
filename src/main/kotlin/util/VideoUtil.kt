package util

import androidx.compose.ui.window.WindowState
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.io.File

/** 计算文件的媒体类型，
 * 如果文件不存在返回默认的媒体类型 video
 */
fun computeMediaType(mediaPath:String):String{
    val file = File(mediaPath)
    if(file.exists()){
        val extension = file.extension
        //  mp3、aac、wav、mp4、mkv，
        return if(extension =="mp3"||extension =="aac"||extension =="wav"){
            "audio"
        }else{
            "video"
        }
    }
    return "video"
}


/**
 * 计算视频播放窗口的位置和大小
 */
fun computeVideoBounds(
    windowState: WindowState,
    openSettings: Boolean,
    density:Float,
): Rectangle {
    var mainX = windowState.position.x.value.toInt()
    var mainY = windowState.position.y.value.toInt()
    mainX = (mainX).div(density).toInt()
    mainY = (mainY).div(density).toInt()

    val mainWidth = windowState.size.width.value.toInt()
    val mainHeight = windowState.size.height.value.toInt()

    val size = if (mainWidth in 801..1079) {
        Dimension(642, 390)
    } else if (mainWidth > 1080) {
        Dimension(1005, 610)
    } else {
        Dimension(540, 304)
    }
    if(density!=1f){
        size.width = size.width.div(density).toInt()
        size.height = size.height.div(density).toInt()
    }
    var x = (mainWidth - size.width).div(2)
    var y = ((mainHeight - size.height).div(2))
    x += mainX
    y += mainY
    if (openSettings) x += 109
    val point = Point(x, y)
    return Rectangle(point, size)
}
package player

import data.Caption
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component


/**
 * 播放音频
 */
fun play(
    setIsPlaying: (Boolean) -> Unit,
    audioPlayerComponent: AudioPlayerComponent,
    volume: Float,
    caption: Caption,
    videoPath:String,
){


    audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    val start = convertTimeToSeconds(caption.start)
    val end = convertTimeToSeconds(caption.end)
    audioPlayerComponent.mediaPlayer().media()
        .play(videoPath,  ":start-time=$start",  ":stop-time=$end")
}

/**
 * @param time 时间格式为 00:00:00,000 或者 00:00:00.000
 * 转换时间为秒
 */
fun convertTimeToSeconds(time:String):Double{
    try{
        if(time.isEmpty()) return 0.0
        val parts = time.split(":")
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        if(parts.size == 2){
            // 如果没有秒，直接返回小时和分钟的总和
            return (hours * 3600 + minutes * 60).toDouble()
        }
        // 如果是 00:00:00,000 需要把 , 替换为 .
        val seconds = if(parts[2].contains(",")) {
            parts[2].replace(",",".").toDouble()
        }else{
            parts[2].toDouble()
        }
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        return totalSeconds
    }catch (exception: Exception){
        exception.printStackTrace()
        return 0.0
    }

}

/**
 * @param time 时间格式为 00:00:00,000 或者 00:00:00.000
 * 转换时间为毫秒
 */
fun convertTimeToMilliseconds(time:String):Long{
    try{
        val parts = time.split(":")
        // 如果时间格式不正确，直接返回 0
        if(parts.size != 3) return 0L

        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()

        // 支持两种格式：逗号分隔 (00:00:00,000) 和点分隔 (00:00:00.000)
        val secondsAndMillis = if (parts[2].contains(",")) {
            parts[2].split(",")
        } else {
            parts[2].split(".")
        }

        if (secondsAndMillis.size != 2){
            // 如果没有秒和毫秒部分，直接返回小时和分钟的总和
            return (hours * 3600 + minutes * 60) * 1000
        }

        val seconds = secondsAndMillis[0].toLong()
        val milliseconds = secondsAndMillis[1].toLong()

        val totalMilliseconds = ((hours * 3600 + minutes * 60 + seconds) * 1000) + milliseconds
        return totalMilliseconds

    }catch (exception: Exception){
        exception.printStackTrace()
        println("时间转换失败: $time, 错误: ${exception.message}")

        return 0L
    }
}


fun Component.videoSurfaceComponent(): Component {
    return when (this) {
        is CallbackMediaPlayerComponent -> videoSurfaceComponent()
        is EmbeddedMediaPlayerComponent -> videoSurfaceComponent()
        else -> throw IllegalArgumentException("You can only call videoSurfaceComponent() on vlcj player component")
    }
}
package player

import data.Caption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import state.getAudioDirectory
import tts.AzureTTS
import tts.MSTTSpeech
import tts.MacTTS
import tts.UbuntuTTS
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import java.net.URL


/**
 * * 用于播放音频的互斥锁，确保同一时间只能有一个音频在播放
 */
private val audioPlaybackMutex = kotlinx.coroutines.sync.Mutex()

fun playAudio(
    word: String,
    audioPath: String,
    pronunciation: String,
    volume: Float,
    audioPlayerComponent: AudioPlayerComponent,
    changePlayerState: (Boolean) -> Unit,
) {
    if (pronunciation == "local TTS" || audioPath.isEmpty()) {
        changePlayerState(true)
        runBlocking {
            launch(Dispatchers.IO) {
                audioPlaybackMutex.withLock {
                    if (isWindows()) {
                        val speech = MSTTSpeech()
                        speech.speak(word)
                    } else if (isMacOS()) {
                        MacTTS().speakAndWait(word)
                    } else {
                        UbuntuTTS().speakAndWait(word)
                    }
                }
                changePlayerState(false)
            }
        }
    } else if (audioPath.isNotEmpty()) {
        changePlayerState(true)

        runBlocking {
            launch(Dispatchers.IO) {
                audioPlaybackMutex.withLock {
                    try {
                        // 先停止当前播放
                        audioPlayerComponent.mediaPlayer().controls().stop()

                        audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                                mediaPlayer.audio().setVolume((volume * 100).toInt())
                            }

                            override fun finished(mediaPlayer: MediaPlayer) {
                                changePlayerState(false)
                                audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
                            }
                        })

                        audioPlayerComponent.mediaPlayer().media().play(audioPath)
                    } catch (e: Exception) {
                        changePlayerState(false)
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}


/** 计算单词的发音地址 */
fun getAudioPath(
    word: String,
    audioSet:Set<String>,
    addToAudioSet:(String) -> Unit,
    pronunciation: String,
    azureTTS: AzureTTS,
): String {
    if(pronunciation == "local TTS") return ""
    if(pronunciation == "Azure TTS"){
        var audioPath = ""
        val audioFileName = word.lowercase() + "_Azure_${azureTTS.displayName}_${azureTTS.pronunciationStyle}.mp3"
        // 先查询本地有没有
        if (audioSet.contains(audioFileName)) {
            audioPath = File(getAudioDirectory(), audioFileName).absolutePath
        }else {
            if(azureTTS.subscriptionKey == "") return ""
            if(azureTTS.region == "") return ""

            // 本地没有就从 Azure 服务器下载
            runBlocking {
                val path = azureTTS.textToSpeech(word)
                // 如果下载成功，就把文件名加入到 audioSet
                if(!path.isNullOrEmpty()){
                    audioPath = path
                    addToAudioSet(audioFileName)
                }else{
                    audioPath = ""
                }
            }
        }

        return audioPath
    }else{
        val audioDir = getAudioDirectory()
        var path = ""
        val type: Any = when (pronunciation) {
            "us" -> "type=2"
            "uk" -> "type=1"
            "jp" -> "le=jap"
            else -> {
                println("未知类型$pronunciation")
                ""
            }
        }
        val fileName = word.lowercase() + "_" + pronunciation + ".mp3"
        // 先查询本地有没有
        if (audioSet.contains(fileName)) {
            path = File(audioDir, fileName).absolutePath
        }
        // 没有就从有道服务器下载
        if (path.isEmpty()) {
            // 如果单词有空格，查询单词发音会失败,所以要把单词的空格替换成短横。
            var mutableWord = word
            if (pronunciation == "us" || pronunciation == "uk") {
                mutableWord = mutableWord.replace(" ", "-")
            }
            val audioURL = "https://dict.youdao.com/dictvoice?audio=${mutableWord}&${type}"
            try {
                val audioBytes = URL(audioURL).readBytes()
                val file = File(audioDir, fileName)
                file.writeBytes(audioBytes)
                path = file.absolutePath
                addToAudioSet(file.name)
            } catch (exception: Exception) {
                exception.printStackTrace()
                val ttsFileName = word.lowercase() + "_Azure_${azureTTS.displayName}_${azureTTS.pronunciationStyle}.mp3"
                var audioPath: String
                // 先查询本地有没有
                if (audioSet.contains(ttsFileName)) {
                    audioPath = File(getAudioDirectory(), ttsFileName).absolutePath
                }else {
                    runBlocking {
                        val  ttsPath =  azureTTS.textToSpeech(word)
                        if(!ttsPath.isNullOrEmpty()){
                            audioPath = ttsPath
                            addToAudioSet(ttsFileName)
                        }else{
                            audioPath = ""
                        }
                    }
                }

                return audioPath
            }
        }

        return path
    }

}



/**
 * 在字幕浏览器使用这个函数播放音频
 */

fun play(
    setIsPlaying: (Boolean) -> Unit,
    audioPlayerComponent: AudioPlayerComponent,
    volume: Float,
    caption: Caption,
    videoPath: String,
) {
    runBlocking {
        launch(Dispatchers.IO) {
            audioPlaybackMutex.withLock {
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
                    .play(videoPath, ":start-time=$start", ":stop-time=$end")
            }
        }
    }
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



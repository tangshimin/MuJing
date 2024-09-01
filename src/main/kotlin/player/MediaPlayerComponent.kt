package player

import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.sun.jna.NativeLibrary
import ffmpeg.extractSubtitles
import ffmpeg.hasRichText
import ffmpeg.removeRichText
import state.getResourcesFile
import state.getSettingsDirectory
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Point
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.event.HyperlinkEvent


/**
 * 初始化视频播放组件
 */
fun createMediaPlayerComponent(): Component {
    System.setProperty("native.encoding", "UTF-8")
    val cacheExists = getResourcesFile("VLC/plugins/plugins.dat").exists()
    // 如果是 Windows、macOS 就使用内置的 VLC 播放器
    if(isWindows()){
        System.setProperty("jna.library.path", getResourcesFile("VLC").absolutePath)
    }else if(isMacOS()){
        System.setProperty("jna.library.path", getResourcesFile("VLC/lib").absolutePath)
    }else{
        NativeDiscovery().discover()
    }

    val args = mutableListOf(
        "--quiet",  // --quiet 是关闭所有的日志。
        "--sub-language=en",// 使用视频播放器播放视频时，自动选择英语字幕
    )
    if(!cacheExists){
        args.add("--reset-plugins-cache")
    }

    return if (isMacOS()) {
        val mediaPlayerFactory = MediaPlayerFactory(args)
        val callbackMediaPlayerComponent = CallbackMediaPlayerComponent(mediaPlayerFactory, null, null, true, null, null, null, null)
        callbackMediaPlayerComponent
    } else if(isWindows()){
        val mediaPlayerFactory = MediaPlayerFactory(args)
        val embeddedMediaPlayerComponent = EmbeddedMediaPlayerComponent(mediaPlayerFactory, null, null, null, null)
        val embeddedMediaPlayer = embeddedMediaPlayerComponent.mediaPlayer()
        embeddedMediaPlayer.input().enableKeyInputHandling(false)
        embeddedMediaPlayer.input().enableMouseInputHandling(false)
        embeddedMediaPlayerComponent
    }else{

        try{
            NativeLibrary.getInstance("vlc")
        }catch ( exception:UnsatisfiedLinkError){
            val message = JEditorPane()
            message.contentType = "text/html"
            message.text = "<p>幕境 需要 <a href='https://www.videolan.org/'>VLC 视频播放器</a> 播放视频和单词发音</p><br>" +
                    "必须使用命令行 sudo apt-get install vlc  安装VLC，不要从 Snap Store 安装VLC."
            message.addHyperlinkListener {
                if(it.eventType == HyperlinkEvent.EventType.ACTIVATED){
                    Desktop.getDesktop().browse(it.url.toURI())
                }
            }
            message.isEditable = false
            JOptionPane.showMessageDialog(null, message)
        }
        EmbeddedMediaPlayerComponent()
    }
}



fun isMacOS(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0
}

fun isWindows(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("windows") >= 0
}

fun isLinux(): Boolean {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
    return os.indexOf("nux") >= 0
}

fun Component.mediaPlayer(): MediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}


/**
 * 解析选择的文件，返回字幕名称列表，用于用户选择具体的字幕。
 * @param mediaPlayerComponent VLC 组件
 * @param playerWindow 播放视频的窗口
 * @param videoPath 视频路径
 * @param setTrackList 解析完成后，用来设置字幕列表的回调。
 */
fun parseTrackList(
    mediaPlayerComponent: Component,
    parentComponent: Component,
    playerWindow: JFrame,
    videoPath: String,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
) {
    val result = checkSubtitles(videoPath,parentComponent)
    if(result){
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                val list = mutableListOf<Pair<Int, String>>()
                mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
                    if (index != 0) {
                        list.add(Pair(index - 1, trackDescription.description()))
                    }
                }
                mediaPlayer.controls().pause()
                playerWindow.isAlwaysOnTop = true
                playerWindow.title = "视频播放窗口"
                playerWindow.isVisible = false
                setTrackList(list)
                mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
            }
        })
        playerWindow.title = "正在读取字幕列表"
        playerWindow.isAlwaysOnTop = false
        playerWindow.toBack()
        playerWindow.size = Dimension(10, 10)
        playerWindow.location = Point(0, 0)
        playerWindow.layout = null
        playerWindow.contentPane.add(mediaPlayerComponent)
        playerWindow.isVisible = true
        mediaPlayerComponent.mediaPlayer().media().play(videoPath,":no-sub-autodetect-file")
    }
}

/**
 * 有些文件，可能文件扩展是mkv,但实际内容并不是 mkv
 */
fun checkSubtitles(
    videoPath: String,
    parentComponent: Component
):Boolean{

    // MP4 文件不检查字幕
    val extension = File(videoPath).extension
    if(extension == "mp4") return true

    var reader: EBMLReader? = null

    try {
        reader = EBMLReader(videoPath)
        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            JOptionPane.showMessageDialog(parentComponent, "这不是一个 mkv 格式的视频")
            return false
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            JOptionPane.showMessageDialog(parentComponent, "这个视频没有字幕")
            return false
        }
    } catch (exception: IOException) {
        JOptionPane.showMessageDialog(parentComponent, "IO 异常")
        exception.printStackTrace()
        return false
    } catch (exception: UnSupportSubtitlesException) {
        val message = if(exception.message != null) exception.message else "字幕格式不支持"
        JOptionPane.showMessageDialog(parentComponent, message)
        exception.printStackTrace()
        return false
    }catch (exception: NullPointerException){
        JOptionPane.showMessageDialog(parentComponent, "空指针异常")
        exception.printStackTrace()
        return false
    } finally {
        try {
            reader?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return true
}





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

package player

import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.sun.jna.NativeLibrary
import state.getResourcesFile
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

 /**  查找 VLC 播放器的位置，先从资源目录中查找，
  * 如果没有找到，就使用从用户本地找。*/
fun embeddedVLCDiscovery() {
    // 如果是 Windows、macOS 就使用内置的 VLC 播放器
    if(isWindows()){
        System.setProperty("jna.library.path", getResourcesFile("VLC").absolutePath)
    }else if(isMacOS()){
        System.setProperty("jna.library.path", getResourcesFile("VLC/lib").absolutePath)
    }else{
        NativeDiscovery().discover()
    }
}

/**
 * 视频播放组件
 */
fun createMediaPlayerComponent2(): CallbackMediaPlayerComponent {

    // 防止字幕描述在 Windows 乱码
    System.setProperty("native.encoding", "UTF-8")
    // 如果是 Windows、macOS 就使用内置的 VLC 播放器
    embeddedVLCDiscovery()

    val args = mutableListOf(
        "--sub-language=en",// 使用视频播放器播放视频时，自动选择英语字幕
        "--avcodec-hw=any",// 使用硬件加速解码
        "--vout=auto",
        "--no-mouse-events",
        "--no-keyboard-events",
        "--no-video-title-show",
    )

    // 设置日志级别 (0=only errors and standard messages, 1=warnings, 2=debug)
    if(isDevelopment()){
        args.addAll(listOf(
            "--verbose", "1"
        ))
    }else{
        args.addAll(listOf(
            "--quiet"
        ))
    }

    val cacheExists = getResourcesFile("VLC/plugins/plugins.dat").exists()
    if(!cacheExists){
        args.add("--reset-plugins-cache")
    }

    return if (isMacOS() || isWindows()) {
        val mediaPlayerFactory = MediaPlayerFactory(args)
        val callbackMediaPlayerComponent = CallbackMediaPlayerComponent(mediaPlayerFactory, null, null, true, null, null, null, null)
        callbackMediaPlayerComponent
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
        CallbackMediaPlayerComponent()
    }
}


fun isDevelopment(): Boolean {
    val property = "compose.application.resources.dir"
    val dir = System.getProperty(property)
    return dir == null
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
 * @param playerWindow 播放视频的窗口
 * @param videoPath 视频路径
 * @param setTrackList 解析完成后，用来设置字幕列表的回调。
 */
fun parseTrackList(
    parentComponent: Component,
    playerWindow: JFrame,
    videoPath: String,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
) {
    val result = checkSubtitles(videoPath,parentComponent)
    if(result){
        val mediaPlayerComponent = createMediaPlayerComponent2()
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                val list = mutableListOf<Pair<Int, String>>()
                mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index, trackDescription ->
                    if (index != 0) {
                        list.add(Pair(index - 1, trackDescription.description()))
                    }
                }
                mediaPlayer.controls().pause()
                playerWindow.isAlwaysOnTop = false
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





package player

import androidx.compose.ui.window.WindowState
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.matthewn4444.ebml.subtitles.SRTSubtitles
import com.matthewn4444.ebml.subtitles.SSASubtitles
import com.sun.jna.NativeLibrary
import data.Caption
import ui.dialog.removeItalicSymbol
import ui.dialog.removeLocationInfo
import ui.dialog.replaceNewLine
import org.mozilla.universalchardet.UniversalDetector
import state.getResourcesFile
import state.getSettingsDirectory
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import uk.co.caprica.vlcj.binding.RuntimeUtil
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.event.HyperlinkEvent


/**
 * 初始化视频播放组件
 */
fun createMediaPlayerComponent(): Component {
    // 如果是 Windows 就使用内置的 VLC 播放器
    if (isWindows()) {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), getResourcesFile("VLC").absolutePath ?: "")
    } else{
        NativeDiscovery().discover()
    }

    // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
    return if (isMacOS()) {
        // macOS 可能没有安装 VLC 播放器
        try{
            NativeLibrary.getInstance("vlc")
        }catch ( exception:UnsatisfiedLinkError){
            val message = JEditorPane()
            message.contentType = "text/html"
            message.text = "幕境 需要 <a href='https://www.videolan.org/'>VLC 视频播放器</a> 朗读单词发音和播放视频<br>" +
                    "<a href='https://get.videolan.org/vlc/3.0.17.3/macosx/vlc-3.0.17.3-intel64.dmg'>下载地址</a><br>"
            message.addHyperlinkListener {
                if(it.eventType == HyperlinkEvent.EventType.ACTIVATED){
                    Desktop.getDesktop().browse(it.url.toURI())
                }
            }
            message.isEditable = false
            JOptionPane.showMessageDialog(null, message)
        }
        CallbackMediaPlayerComponent()
    } else if(isWindows()){
        // --quiet 是控制台的日志参数，quiet 是关闭所有的日志。
        val args = listOf("--quiet")
        val mediaPlayerFactory = MediaPlayerFactory(null,args )
        val embeddedMediaPlayerComponent = EmbeddedMediaPlayerComponent(mediaPlayerFactory, null, null, null, null)
        val embeddedMediaPlayer = embeddedMediaPlayerComponent.mediaPlayer()
        embeddedMediaPlayer.input().enableKeyInputHandling(false)
        embeddedMediaPlayer.input().enableMouseInputHandling(false)
        embeddedMediaPlayerComponent
    }else{
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

/**
 * 解析字幕，返回最大字符数和字幕列表，用于显示。
 * @param subtitlesPath 字幕的路径
 * @param setMaxLength 用于设置字幕的最大字符数的回调函数
 * @param setCaptionList 用于设置字幕列表的回调函数
 * @param resetSubtitlesState 字幕文件删除，或者被修改，导致不能解析，就重置
 */
fun parseSubtitles(
    subtitlesPath: String,
    setMaxLength: (Int) -> Unit,
    setCaptionList: (List<Caption>) -> Unit,
    resetSubtitlesState:() -> Unit,
) {
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    if(file.exists()){
        try {
            val encoding = UniversalDetector.detectCharset(file)
            val charset =  if(encoding != null){
                Charset.forName(encoding)
            }else{
                Charset.defaultCharset()
            }
            val inputStream: InputStream = FileInputStream(file)
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream,charset)
            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList = mutableListOf<Caption>()
            var maxLength = 0
            for (caption in captions.values) {
                var content = removeLocationInfo(caption.content)
                content = removeItalicSymbol(content)
                content = replaceNewLine(content)

                val newCaption = Caption(
                    start = caption.start.getTime("hh:mm:ss.ms"),
                    end = caption.end.getTime("hh:mm:ss.ms"),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

            setMaxLength(maxLength)
            setCaptionList(captionList)
        }catch (exception: IOException){
            exception.printStackTrace()
            resetSubtitlesState()
        }

    }else{
        println("找不到正在抄写的字幕")
        resetSubtitlesState()
    }

}

/**
 * 提取选择的字幕到用户目录
 * */
fun writeToFile(
    videoPath: String,
    trackId: Int,
    parentComponent: Component,
): File? {
    var reader: EBMLReader? = null
    val settingsDir = getSettingsDirectory()
    var subtitlesFile: File? = null

    try {
        reader = EBMLReader(videoPath)
        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return subtitlesFile
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
            return subtitlesFile
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()

        /**
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }

        val subtitles = reader.subtitles[trackId]
        if(subtitles is SSASubtitles){
            JOptionPane.showMessageDialog(parentComponent, "暂时不支持 ASS 格式的字幕")
        }else if(subtitles is SRTSubtitles){
            subtitlesFile = File(settingsDir, "subtitles.srt")
            subtitles.writeFile(subtitlesFile.absolutePath)
        }

    } catch (exception: Exception) {
        exception.printStackTrace()
    } finally {
        try {
            reader?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return subtitlesFile
}

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
    // 232 是单词 + 字幕的高度 ，再加一个文本输入框48 == 280
    // 48 是内容的 bottom padding
    var y = ((mainHeight - 280 - size.height).div(2)) + 280 + 15-48
    x += mainX
    y += mainY
    if (openSettings) x += 109
    val point = Point(x, y)
    return Rectangle(point, size)
}
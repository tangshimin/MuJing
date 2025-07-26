package util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import data.Caption
import org.mozilla.universalchardet.UniversalDetector
import player.PlayerCaption
import player.convertTimeToMilliseconds
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import ui.dialog.removeItalicSymbol
import ui.dialog.removeNewLine
import ui.dialog.replaceNewLine
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import javax.swing.JOptionPane

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

    // 根据不同分辨率设置不同的窗口大小
    val size = when {
        mainWidth > 3840 -> { // 4K分辨率及以上
            // 保持与原始设计相同的比例 (约93%)
            Dimension(3570, 2010) // 约为4K宽度的93%
        }
        mainWidth > 2560 -> { // 2K和4K之间
            Dimension(2380, 1340) // 约为3K宽度的93%
        }
        mainWidth > 1920 -> { // 2K分辨率
            Dimension(1780, 1000) // 约为2K宽度的93%
        }
        mainWidth > 1080 -> { // 普通高分辨率
            Dimension(1005, 610) // 原始尺寸
        }
        mainWidth in 801..1079 -> { // 中等分辨率
            Dimension(642, 390) // 原始尺寸
        }
        else -> { // 低分辨率
            Dimension(540, 304) // 原始尺寸
        }
    }

    // 处理高DPI缩放
//    if(density!=1f){
//        size.width = size.width.div(density).toInt()
//        size.height = size.height.div(density).toInt()
//    }

    // 计算窗口位置
    // 根据分辨率调整窗口的位置偏移
    var offsetX = 0
    var offsetY = 0

    when {
        mainWidth > 3840 -> { // 4K分辨率及以上
            offsetX = 0
            offsetY = -50 // 在4K分辨率上稍微上移窗口
        }
        mainWidth > 2560 -> { // 2K和4K之间
            offsetX = 0
            offsetY = -30
        }
        mainWidth > 1920 -> { // 2K分辨率
            offsetX = 0
            offsetY = -20
        }
    }

    // 居中计算基本位置
    var x = (mainWidth - size.width).div(2) + offsetX
    var y = ((mainHeight - size.height).div(2)) + offsetY

    // 添加主窗口位置偏移
    x += mainX
    y += mainY

    // 设置选项面板打开时的偏移
    if (openSettings) {
        // 根据分辨率调整设置面板打开时的偏移量
        val settingsOffset = when {
            mainWidth > 2560 -> 150 // 在高分��率下增加偏移
            mainWidth > 1920 -> 130
            else -> 109 // 保持原来的偏移
        }
        x += settingsOffset
    }

    val point = Point(x, y)
    return Rectangle(point, size)
}


/**
 * 计算视频播放的大小
 */
fun computeVideoSize(
    windowSize: DpSize,
): DpSize {
    val mainWidth = windowSize.width.value.toInt()

    // 使用标准16:9视频比例的窗口大小设置
    // 对于学习视频，16:9比例是当今最常见的视频格式
    // 基础尺寸：1005.dp × 565.dp (经过 Mac 和 Windows 测试验证)
    val size = when {
        mainWidth > 3840 -> { // 4K分辨率及以上
            // 按比例放大约 2.4 倍 (3840/1920 = 2, 适当增加到 2.4)
            DpSize(2412.dp, 1356.dp) // 16:9 比例
        }
        mainWidth > 2560 -> { // 2K和4K之间
            // 按比例放大约 1.6 倍 (2560/1920 ≈ 1.33, 适当增加到 1.6)
            DpSize(1608.dp, 904.dp) // 16:9 比例
        }
        mainWidth > 1920 -> { // 2K分辨率
            DpSize(1005.dp, 565.dp) // 16:9 比例，经测试验证的最佳尺寸
        }
        mainWidth > 1080 -> { // 普通高分辨率
            DpSize(1005.dp, 565.dp) // 16:9 比例
        }
        mainWidth in 801..1079 -> { // 中等分辨率
            DpSize(642.dp, 361.dp) // 16:9 比例
        }
        else -> { // 低分辨率
            DpSize(540.dp, 304.dp) // 16:9 比例
        }
    }

    // 在 Compose 中使用 DpSize 和 dp 单位时，不需要手动处理 density
    // dp 单位已经会根据系统的缩放设置自动进行适配

    println("最终计算的视频窗口大小: $size")
    return size
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
                content = removeNewLine(content)

                val newCaption = Caption(
                    start = caption.start.getTime("hh:mm:ss,ms"),
                    end = caption.end.getTime("hh:mm:ss,ms"),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

            setMaxLength(maxLength)
            setCaptionList(captionList)
        } catch (exception: Exception) {
            exception.printStackTrace()
            resetSubtitlesState()
            JOptionPane.showMessageDialog(
                null, "字幕文件解析失败:\n${exception.message}"
            )

        }
    } else {
        JOptionPane.showMessageDialog(null, "找不到字幕")
        resetSubtitlesState()
    }

}


fun parseSubtitles(subtitlesPath: String):List<PlayerCaption>{
    val formatSRT = FormatSRT()
    val file = File(subtitlesPath)
    val captionList = mutableListOf<PlayerCaption>()
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

            var maxLength = 0
            for (caption in captions.values) {
                var content = removeLocationInfo(caption.content)
                content = removeItalicSymbol(content)
                content = replaceNewLine(content)
                val newCaption = PlayerCaption(
                    start = convertTimeToMilliseconds(caption.start.getTime("hh:mm:ss,ms")),
                    end = convertTimeToMilliseconds(caption.end.getTime("hh:mm:ss,ms")),
                    content = content
                )
                if (caption.content.length > maxLength) {
                    maxLength = caption.content.length
                }
                captionList.add(newCaption)
            }

        }catch (exception: Exception){
            exception.printStackTrace()
            JOptionPane.showMessageDialog(null, "字幕文件解析失败:\n${exception.message}")
        }

    }else{
        JOptionPane.showMessageDialog(null, "找不到字幕")
    }
    return captionList
}
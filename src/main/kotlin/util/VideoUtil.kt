package util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import data.Caption
import ffmpeg.findFFmpegPath
import ffmpeg.hasRichText
import ffmpeg.removeRichText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity
import net.bramp.ffmpeg.job.FFmpegJob
import org.mozilla.universalchardet.UniversalDetector
import player.PlayerCaption
import player.convertTimeToMilliseconds
import state.getSettingsDirectory
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

//    println("最终计算的视频窗口大小: $size")
    return size
}

/**
 * 解析字幕文件并设置相关状态
 *
 * 该函数用于解析指定路径的字幕文件（支持SRT格式），自动检测文件编码，
 * 解析字幕内容并通过回调函数设置最大字符数和字幕列表。主要用于字幕显示界面。
 *
 * @param subtitlesPath 字幕文件的完整路径（支持SRT格式）
 * @param setMaxLength 用于设置字幕最大字符数的回调函数
 *                     - 参数: Int - 所有字幕条目中最长的字符数
 *                     - 用途: 帮助UI组件确定合适的显示宽度
 * @param setCaptionList 用于设置字幕列表的回调函数
 *                       - 参数: List<Caption> - 解析后的字幕条目列表
 *                       - 每个Caption包含开始时间、结束时间和内容
 * @param resetSubtitlesState 重置字幕状态的回调函数
 *                           - 当字幕文件不存在、被删除或解析失败时调用
 *                           - 用于清理相关的UI状态
 *
 * 功能特性：
 * - 自动检测文件编码（使用UniversalDetector）
 * - 移除字幕中的位置信息标签
 * - 移除斜体符号标记
 * - 处理换行符（移除多余换行）
 * - 计算字幕内容的最大字符数
 * - 统一时间格式为 "hh:mm:ss,ms"
 *
 * 错误处理：
 * - 文件不存在时显示"找不到字幕"提示并调用resetSubtitlesState
 * - 解析失败时显示详细错误信息并调用resetSubtitlesState
 * - 编码检测失败时使用系统默认编码
 *
 * @throws Exception 当文件读取或解析过程中发生错误时
 *
 * @see Caption 字幕条目数据类（用于显示）
 * @see FormatSRT SRT格式解析器
 * @see UniversalDetector 编码检测工具
 *
 * 示例用法：
 * ```kotlin
 * parseSubtitles(
 *     subtitlesPath = "/path/to/subtitle.srt",
 *     setMaxLength = { maxLen -> println("最大字符数: $maxLen") },
 *     setCaptionList = { captions -> displayCaptions(captions) },
 *     resetSubtitlesState = { clearSubtitleUI() }
 * )
 * ```
 *
 * 注意事项：
 * - 仅支持SRT格式的字幕文件
 * - 会自动处理各种编码格式的字幕文件
 * - 解析失败时会弹出错误对话框提示用户
 * - 字幕内容会被清理（移除格式标记和多余换行）
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


/**
 * 使用 FFmpeg 提取视频中的字幕并返回字幕列表
 *
 * 该函数会从指定的视频文件中提取指定轨道的字幕，将其转换为 SRT 格式，
 * 然后解析为 PlayerCaption 对象列表。提取过程中会在应用程序设置目录中
 * 创建临时的 SRT 文件，处理完成后会自动删除。
 *
 * @param videoPath 视频文件的完整路径
 * @param subtitleId 字幕轨道ID，从0开始计数
 *                   - 0: 第一个字幕轨道
 *                   - 1: 第二个字幕轨道
 *                   - 以此类推
 * @param verbosity FFmpeg 的详细输出级别，默认为 INFO
 *                  可选值：QUIET, PANIC, FATAL, ERROR, WARNING, INFO, VERBOSE, DEBUG, TRACE
 *
 * @return List<PlayerCaption> 解析后的字幕列表
 *         - 如果提取成功，返回包含所有字幕条目的列表
 *         - 如果提取失败或视频中没有指定的字幕轨道，返回空列表
 *
 * @throws Exception 当 FFmpeg 执行失败或文件路径无效时可能抛出异常
 *
 * @see PlayerCaption 字幕条目数据类
 * @see parseSubtitles 用于解析 SRT 文件的工具函数
 *
 * 示例用法：
 * ```kotlin
 * val captions = readCaptionList("/path/to/video.mp4", 0)
 * captions.forEach { caption ->
 *     println("${caption.start} -> ${caption.end}: ${caption.content}")
 * }
 * ```
 *
 * 注意事项：
 * - 需要确保视频文件存在且包含字幕轨道
 * - subtitleId 超出范围时会返回空列表
 * - 函数会自动处理临时文件的清理
 * - 支持大多数常见的字幕格式（如 ASS、SSA、VTT 等）
 */
fun readCaptionList(
    videoPath: String,
    subtitleId: Int,
    verbosity: Verbosity = Verbosity.INFO
): List<PlayerCaption> {
    val captionList = mutableListOf<PlayerCaption>()
    val applicationDir = getSettingsDirectory()

    // 确保 VideoPlayer 存在，如果不存在则创建
    "$applicationDir/VideoPlayer".let { dir ->
        File(dir).apply {
            if (!exists()) {
                mkdirs() // 确保目录存在
            }
        }
    }
    val subtitlePath = "$applicationDir/VideoPlayer/subtitle.srt"
    val infoPath = "$applicationDir/VideoPlayer/last_subtitle.json"

    try {
        val ffmpeg = FFmpeg(findFFmpegPath())
        val builder = FFmpegBuilder()
            .setVerbosity(verbosity)
            .setInput(videoPath)
            .addOutput(subtitlePath)
            .addExtraArgs("-map", "0:s:$subtitleId")
            .done()
        val executor = FFmpegExecutor(ffmpeg)
        val job = executor.createJob(builder)
        job.run()
        if (job.state == FFmpegJob.State.FINISHED) {
            println("extractSubtitle success")
            captionList.addAll(parseSubtitles(subtitlePath))
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
            }
            val info = SubtitleInfo(videoPath, subtitleId)
            val jsonString =  json.encodeToString(info)
            // 将字幕信息写入 JSON 文件
            File(infoPath).writeText(jsonString)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // 发生异常时返回空列表
        return emptyList()
    }
    return captionList
}

@Serializable
data class SubtitleInfo(
    val videoPath: String,
    val trackID: Int
)

fun readCachedSubtitle(
    videoPath: String,
    trackID: Int = 0,
): List<PlayerCaption> {
    val applicationDir = getSettingsDirectory()
    val infoPath = "$applicationDir/VideoPlayer/last_subtitle.json"
    val subtitlePath = "$applicationDir/VideoPlayer/subtitle.srt"
    val infoFile = File(infoPath)
    if (!infoFile.exists()) return emptyList()
    return try {
        val jsonString = infoFile.readText()
        val json = Json { ignoreUnknownKeys }
        val info = json.decodeFromString<SubtitleInfo>(jsonString)
        if (info.videoPath == videoPath && info.trackID == trackID) {
            parseSubtitles(subtitlePath)
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun readTrackIdFromLastSubtitle(): Int? {
    val applicationDir = getSettingsDirectory()
    val infoPath = "$applicationDir/VideoPlayer/last_subtitle.json"
    val infoFile = File(infoPath)
    if (!infoFile.exists()) return null
    return try {
        val jsonString = infoFile.readText()
        val info = Json.decodeFromString<SubtitleInfo>(jsonString)
        info.trackID
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 解析字幕文件并返回PlayerCaption列表
 *
 * 该函数直接解析SRT格式的字幕文件，将时间转换为毫秒格式，
 * 并返回适用于视频播放器的PlayerCaption对象列表。与上面的parseSubtitles函数不同，
 * 这个函数不使用回调，而是直接返回解析结果。
 *
 * @param subtitlesPath 字幕文件的完整路径（仅支持SRT格式）
 *
 * @return List<PlayerCaption> 解析后的字幕列表
 *         - 成功时：包含所有字幕条目的列表，时间以毫秒为单位
 *         - 失败时：返回空列表
 *         - 文件不存在时：返回空列表并显示错误提示
 *
 * 功能特性：
 * - 自动检测字幕文件编码（UTF-8、GBK、Big5等）
 * - 移除字幕中的位置信息标签（如{\an8}等）
 * - 移除斜体标记符号
 * - 处理换行符（替换为适合显示的格式）
 * - 将时间格式从"hh:mm:ss,ms"转换为毫秒数值
 * - 静默处理最大字符数计算（虽然不返回此值）
 *
 * 与第一个parseSubtitles函数的区别：
 * - 返回类型：List<PlayerCaption> vs 通过回调设置
 * - 时间格式：毫秒数值 vs 时间字符串
 * - 换行处理：replaceNewLine vs removeNewLine
 * - 用途：视频播放器 vs 字幕显示界面
 *
 * 错误处理：
 * - 文件不存在时显示"找不到字幕"对话框
 * - 解析异常时显示详细错误信息对话框
 * - 编码检测失败时自动使用系统默认编码
 * - 所有错误情况都返回空列表，不会抛出异常
 *
 * @see PlayerCaption 播放器字幕条目数据类（包含毫秒时间戳）
 * @see Caption 显示用字幕条目数据类（包含时间字符串）
 * @see convertTimeToMilliseconds 时间字符串转毫秒的工具函数
 * @see replaceNewLine 换行符替换函数
 *
 * 示例用法：
 * ```kotlin
 * val playerCaptions = parseSubtitles("/path/to/subtitle.srt")
 * if (playerCaptions.isNotEmpty()) {
 *     playerCaptions.forEach { caption ->
 *         println("${caption.start}ms - ${caption.end}ms: ${caption.content}")
 *     }
 * } else {
 *     println("字幕解析失败或文件为空")
 * }
 * ```
 *
 * 注意事项：
 * - 专为视频播放器设计，时间格式为毫秒便于同步
 * - 仅支持SRT格式，其他格式需要先转换
 * - 解析失败时会显示用户友好的错误对话框
 * - 函数执行完成后会自动关闭文件流资源
 * - 内容清理策略与显示用的parseSubtitles略有不同
 */
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
                if(hasRichText(content)){
                    content = removeRichText(content)
                }
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


/**
 * 自动查找与视频文件关联的字幕文件
 *
 * 该函数会在视频文件所在目录及其下的 Subs 文件夹中查找所有 .srt 和 .ass 字幕文件，
 * 并自动提取每个字幕文件的语言标签。语言标签的提取规则见 getSubtitleLangLabel。
 *
 * 查找逻辑：
 * 1. 查找与视频同目录下、以 baseName 开头的 .srt、.ass文件，提取语言标签。
 * 2. 查找同目录下名为 Subs（不区分大小写）的子文件夹中的所有 .srt 和 .ass 文件，提取语言标签。
 *
 * @param videoPath 视频文件的完整路径
 * @return List<Pair<String, File>> 返回 (语言标签, 字幕文件) 的列表
 *
 * @see getSubtitleLangLabel 用于提取语言标签的工具函数
 */
fun findSubtitleFiles(videoPath: String): List<Pair<String,File>> {
    val videoFile = File(videoPath)
    val baseName = videoFile.nameWithoutExtension
    val dir = videoFile.parentFile ?: return emptyList()
    val result = mutableListOf<Pair<String,File>>()

    // 1. 查找同目录下的 .srt 和 .ass 字幕
    dir.listFiles { file ->
        file.isFile &&
                file.name.startsWith(baseName) &&
                (file.name.endsWith(".srt", ignoreCase = true) || file.name.endsWith(".ass", ignoreCase = true))
    }?.forEach { file ->

        val name = file.name
        var lang = name.removePrefix(baseName)
        if (lang.startsWith("_")) lang = lang.removePrefix("_")
        if (lang.startsWith(".")) lang = lang.removePrefix(".")
        if (lang.isBlank()) lang = "subtitle"
        result.add(lang to file)
    }

    // 2. 查找 Subs 子文件夹下的 .srt 和 .ass 字幕
    // 查找同目录下所有名为 Subs/ subs/ SUBS 等的文件夹
    val subsDirs = dir.listFiles { file ->
        file.isDirectory && file.name.equals("Subs", ignoreCase = true)
    } ?: emptyArray()

    for (subsDir in subsDirs) {
        subsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".srt", ignoreCase = true) || file.name.endsWith(".ass", ignoreCase = true))
        }?.forEach { file ->

            val name = file.name
            if(name.startsWith(baseName)){
                var lang = name.removePrefix(baseName)
                if (lang.startsWith("_")) lang = lang.removePrefix("_")
                if (lang.startsWith(".")) lang = lang.removePrefix(".")
                if (lang.isBlank()) lang = "subtitle"
                result.add(lang to file)
            } else {
                var lang = name
                if (lang.startsWith("_")) lang = lang.removePrefix("_")
                if (lang.startsWith(".")) lang = lang.removePrefix(".")
                result.add(lang to file)
            }

        }
    }

    return result

}

/**
 * 提取字幕文件的语言标签
 *
 * 根据字幕文件名和视频基础名，自动提取语言标签部分。
 * - 如果文件名以 baseName 开头，则去除 baseName 和前缀的下划线/点，空则返回 "subtitle"
 * - 如果文件名不以 baseName 开头，则直接去除前缀下划线
 *
 * 适用于自动识别同目录或 Subs 文件夹下的字幕文件语言标识。
 *
 * @param baseName 视频文件的基础名（不含扩展名）
 * @param fileName 字幕文件名（含扩展名）
 * @return 提取出的语言标签字符串
 */
fun getSubtitleLangLabel(baseName: String, fileName: String): String {
    val nameWithoutExt = fileName.removeSuffix(".srt")
    val lang = if (nameWithoutExt.startsWith(baseName)) {
        var l = nameWithoutExt.removePrefix(baseName)
        if (l.startsWith("_")) l = l.removePrefix("_")
        if (l.startsWith(".")) l = l.removePrefix(".")
        if (l.isBlank()) l = "subtitle"
        l
    } else {
        var l = nameWithoutExt
        if (l.startsWith("_")) l = l.removePrefix("_")
        l
    }
    return lang
}
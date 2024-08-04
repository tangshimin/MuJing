package state

import androidx.compose.runtime.*
import com.formdev.flatlaf.FlatLightLaf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.JOptionPane

/** 抄写单词的数据类 */
@ExperimentalSerializationApi
@Serializable
data class DataSubtitlesState(
    val videoPath: String = "",
    val subtitlesPath: String = "",
    val trackID: Int = 0,
    val trackDescription: String = "",
    val trackSize: Int = 0,
    val currentIndex: Int = 0,
    val firstVisibleItemIndex: Int = 0,
    var sentenceMaxLength: Int = 0,
    var transcriptionCaption: Boolean = false,
    var currentCaptionVisible: Boolean = true,
    var notWroteCaptionVisible: Boolean = true,
    var externalSubtitlesVisible: Boolean = true,
)

/** 抄写单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class SubtitlesState(dataSubtitlesState: DataSubtitlesState) {

    /**
     * 抄写字幕时的媒体文件，支持的格式： mp3、aac、wav、mp4、mkv，
     * 因为最开始只支持 mkv,要向后兼容，就没有改数据类的变量名
     */
    var mediaPath by mutableStateOf(dataSubtitlesState.videoPath)

    /** 抄写字幕时的字幕文件的路径 */
    var subtitlesPath by mutableStateOf(dataSubtitlesState.subtitlesPath)

    /** 抄写字幕时的字幕的轨道 ID,
     *  如果等于 -1 表示不使用内置的轨道，
     *  而是使用外部的字幕。
     */
    var trackID by mutableStateOf(dataSubtitlesState.trackID)

    /** 选择的字幕名称  */
    var trackDescription by mutableStateOf(dataSubtitlesState.trackDescription)

    /** 字幕轨道的数量  */
    var trackSize by mutableStateOf(dataSubtitlesState.trackSize)

    /** 抄写字幕的索引  */
    var currentIndex by mutableStateOf(dataSubtitlesState.currentIndex)

    /** 抄写字幕时屏幕顶部的行索引  */
    var firstVisibleItemIndex by mutableStateOf(dataSubtitlesState.firstVisibleItemIndex)

    /** 字幕的最大长度，用来计算字幕的宽度  */
    var sentenceMaxLength by mutableStateOf(dataSubtitlesState.sentenceMaxLength)

    /** 是否抄写字幕 */
    var transcriptionCaption by mutableStateOf(dataSubtitlesState.transcriptionCaption)

    /** 当前字幕的可见性 */
    var currentCaptionVisible by mutableStateOf(dataSubtitlesState.currentCaptionVisible)

    /** 未抄写字幕的可见性 */
    var notWroteCaptionVisible by mutableStateOf(dataSubtitlesState.notWroteCaptionVisible)

    /** 外部字幕的可见性 */
    var externalSubtitlesVisible by mutableStateOf(dataSubtitlesState.externalSubtitlesVisible)

    /** 保存抄写字幕的配置信息 */
    fun saveTypingSubtitlesState() {
        runBlocking {
            launch (Dispatchers.IO){
                val dataSubtitlesState = DataSubtitlesState(
                    mediaPath,
                    subtitlesPath,
                    trackID,
                    trackDescription,
                    trackSize,
                    currentIndex,
                    firstVisibleItemIndex,
                    sentenceMaxLength,
                    transcriptionCaption,
                    currentCaptionVisible,
                    notWroteCaptionVisible,
                    externalSubtitlesVisible,
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(dataSubtitlesState)
                val typingSubtitlesSetting = getSubtitlesSettingsFile()
                typingSubtitlesSetting.writeText(json)
            }
        }
    }

}


@Composable
fun rememberSubtitlesState():SubtitlesState = remember{
    loadSubtitlesState()
}

/** 加载抄写字幕的配置信息 */
@OptIn(ExperimentalSerializationApi::class)
private fun loadSubtitlesState(): SubtitlesState {
    val typingSubtitlesSetting = getSubtitlesSettingsFile()
    return if (typingSubtitlesSetting.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataSubtitlesState = decodeFormat.decodeFromString<DataSubtitlesState>(typingSubtitlesSetting.readText())
            SubtitlesState(dataSubtitlesState)
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingSubtitlesSetting")
            SubtitlesState(DataSubtitlesState())
        }
    } else {
        SubtitlesState(DataSubtitlesState())
    }
}

/** 获取抄写字幕的配置文件 */
private fun getSubtitlesSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingSubtitlesSettings.json")
}
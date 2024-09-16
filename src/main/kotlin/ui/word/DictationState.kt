package ui.word

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
import state.getSettingsDirectory
import java.io.File
import javax.swing.JOptionPane


/**
 * 听写单词的数据类
 */
@ExperimentalSerializationApi
@Serializable
data class DataDictationState(
    val phoneticVisible: Boolean = false,
    val morphologyVisible: Boolean = false,
    val definitionVisible: Boolean = false,
    val translationVisible: Boolean = false,
    val subtitlesVisible: Boolean = false,
    val sentencesVisible: Boolean = false,
)

/**
 * 听写单词时的状态
 */
@OptIn(ExperimentalSerializationApi::class)
class DictationState(dataDictationState: DataDictationState){
    /**
     * 音标组件的可见性
     */
    var phoneticVisible by mutableStateOf(dataDictationState.phoneticVisible)

    /**
     * 词型组件的可见性
     */
    var morphologyVisible by mutableStateOf(dataDictationState.morphologyVisible)

    /**
     * 定义组件的可见性
     */
    var definitionVisible by mutableStateOf(dataDictationState.definitionVisible)

    /**
     * 翻译组件的可见性
     */
    var translationVisible by mutableStateOf(dataDictationState.translationVisible)

    /**
     * 字幕组件的可见性
     */
    var subtitlesVisible by mutableStateOf(dataDictationState.subtitlesVisible)

    /**
     * 例句组件的可见性
     */
    var sentencesVisible by mutableStateOf(dataDictationState.subtitlesVisible)

    /** 保存听写时的配置信息 */
    fun saveDictationState() {
        runBlocking {
            launch (Dispatchers.IO){
                val dataDictationState = DataDictationState(
                    phoneticVisible,
                    morphologyVisible,
                    definitionVisible,
                    translationVisible,
                    subtitlesVisible,
                    sentencesVisible,
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(dataDictationState)
                val dictationSettings = getDictationFile()
                dictationSettings.writeText(json)
            }
        }
    }
}



@Composable
fun rememberDictationState(): DictationState = remember{
    loadDictationState()
}

/** 加载听写单词时界面的设置信息 */
@OptIn(ExperimentalSerializationApi::class)
private fun loadDictationState(): DictationState {
    val dictationSettings = getDictationFile()
    return if (dictationSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataDictationState = decodeFormat.decodeFromString<DataDictationState>(dictationSettings.readText())
            val dictationState = DictationState(dataDictationState)
            dictationState
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$dictationSettings")
            DictationState(DataDictationState())
        }

    } else {
        DictationState(DataDictationState())
    }
}

/** 获取记忆单词的配置文件 */
private fun getDictationFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "DictationSettings.json")
}
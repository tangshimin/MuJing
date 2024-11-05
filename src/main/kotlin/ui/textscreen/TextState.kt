package ui.textscreen

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

/** 抄写文本界面的数据类 */
@ExperimentalSerializationApi
@Serializable
data class DataTextState(
    val textPath:String = "",
    val currentIndex: Int = 0,
    val firstVisibleItemIndex: Int = 0,
)

/** 抄写文本界面的可观察状态类 */
@OptIn(ExperimentalSerializationApi::class)
class TextState(dataTextState: DataTextState){

    /** 文本文件的路径 */
    var textPath by mutableStateOf(dataTextState.textPath)

    /** 正在抄写的行数 */
    var currentIndex by mutableStateOf(dataTextState.currentIndex)

    /** 正在抄写的那一页的第一行行数 */
    var firstVisibleItemIndex by mutableStateOf(dataTextState.firstVisibleItemIndex)

    /** 保持抄写文本的配置信息 */
    fun saveTypingTextState() {

        runBlocking {
            launch (Dispatchers.IO){
                val dataTextState = DataTextState(
                    textPath,
                    currentIndex,
                    firstVisibleItemIndex,
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(dataTextState)
                val typingTextSetting = getTextSettingsFile()
                typingTextSetting.writeText(json)
            }
        }
    }

}

@Composable
fun rememberTextState(): TextState = remember{
    loadTextState()
}

/** 加载抄写文本的配置信息 */
@OptIn(ExperimentalSerializationApi::class)
private fun loadTextState(): TextState {
    val typingTextSetting = getTextSettingsFile()
    return if(typingTextSetting.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataTextState = decodeFormat.decodeFromString<DataTextState>(typingTextSetting.readText())
            TextState(dataTextState)
        }catch (exception:Exception){
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingTextSetting")
            TextState(DataTextState())
        }

    }else{
        TextState(DataTextState())
    }
}

/** 获取抄写文本的配置文件 */
private fun getTextSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingTextSettings.json")
}
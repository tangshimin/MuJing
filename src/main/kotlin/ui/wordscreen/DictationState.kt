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

package ui.wordscreen

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
    val showUnderline: Boolean = false,
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
     * 显示下划线
     */
    var showUnderline by mutableStateOf(true)

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
                    showUnderline,
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
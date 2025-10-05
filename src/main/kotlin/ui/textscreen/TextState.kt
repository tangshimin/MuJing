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
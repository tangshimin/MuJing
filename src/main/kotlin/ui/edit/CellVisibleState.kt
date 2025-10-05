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

package ui.edit

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

@ExperimentalSerializationApi
@Serializable
data class CellVisible(
    var translationVisible: Boolean = true,
    var definitionVisible: Boolean = true,
    var uKPhoneVisible: Boolean = true,
    var usPhoneVisible: Boolean = true,
    var exchangeVisible: Boolean = true,
    var captionsVisible: Boolean = true,
    var sentencesVisible: Boolean = true,
)

@OptIn(ExperimentalSerializationApi::class)
class CellVisibleState(cellVisible: CellVisible){
    var translationVisible by mutableStateOf(cellVisible.translationVisible)
    var definitionVisible by mutableStateOf(cellVisible.definitionVisible)
    var uKPhoneVisible by mutableStateOf(cellVisible.uKPhoneVisible)
    var usPhoneVisible by mutableStateOf(cellVisible.usPhoneVisible)
    var exchangeVisible by mutableStateOf(cellVisible.exchangeVisible)
    var captionsVisible by mutableStateOf(cellVisible.captionsVisible)
    var sentencesVisible by mutableStateOf(cellVisible.sentencesVisible)


    /** 保持列可见性的配置信息 */
    fun saveCellVisibleState() {
        runBlocking {
            launch (Dispatchers.IO){
                val cellVisible = CellVisible(
                    translationVisible,
                    definitionVisible,
                    uKPhoneVisible,
                    usPhoneVisible,
                    exchangeVisible,
                    captionsVisible,
                    sentencesVisible
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(cellVisible)
                val typingTextSetting = getCellVisibleFile()
                typingTextSetting.writeText(json)
            }
        }
    }

}
@OptIn(ExperimentalSerializationApi::class)
class CellVisibleSwingState(cellVisible: CellVisible){
    var translationVisible = cellVisible.translationVisible
    var definitionVisible = cellVisible.definitionVisible
    var uKPhoneVisible = cellVisible.uKPhoneVisible
    var usPhoneVisible = cellVisible.usPhoneVisible
    var exchangeVisible = cellVisible.exchangeVisible
    var captionsVisible = cellVisible.captionsVisible
    var sentencesVisible = cellVisible.sentencesVisible
}


/** 用于显示和隐藏列窗口 */
@Composable
fun rememberCellVisibleState():CellVisibleState = remember{
    loadCellVisibleState()
}
@OptIn(ExperimentalSerializationApi::class)
private fun loadCellVisibleState():CellVisibleState{
    val cellVisibleSetting = getCellVisibleFile()
    return if(cellVisibleSetting.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataTextState = decodeFormat.decodeFromString<CellVisible>(cellVisibleSetting.readText())
            CellVisibleState(dataTextState)
        }catch (exception:Exception){
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$cellVisibleSetting")
            CellVisibleState(CellVisible())
        }

    }else{
        CellVisibleState(CellVisible())
    }


}

/** 加载编辑词库界面的设置信息 */
@OptIn(ExperimentalSerializationApi::class)
fun loadCellVisibleSwingState():CellVisibleSwingState{
    val cellVisibleSetting = getCellVisibleFile()
    return if(cellVisibleSetting.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataTextState = decodeFormat.decodeFromString<CellVisible>(cellVisibleSetting.readText())
            CellVisibleSwingState(dataTextState)
        }catch (exception:Exception){
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$cellVisibleSetting")
            CellVisibleSwingState(CellVisible())
        }

    }else{
        CellVisibleSwingState(CellVisible())
    }
}

/** 获取编辑词库界面的配置文件 */
private fun getCellVisibleFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "CellVisible.json")
}
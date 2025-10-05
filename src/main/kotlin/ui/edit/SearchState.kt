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
data class SearchData(
    val matchCaseIsSelected: Boolean = false,
    val wordsIsSelected: Boolean = false,
    val regexIsSelected: Boolean = false,
    val numberSelected: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
class SearchState (searchState: SearchData){
    var matchCaseIsSelected = searchState.matchCaseIsSelected
    var wordsIsSelected = searchState.wordsIsSelected
    var regexIsSelected = searchState.regexIsSelected
    var numberSelected = searchState.numberSelected

    /**  */
    fun saveSearchState() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        runBlocking {
            launch (Dispatchers.IO){
                val searchState = SearchData(
                    matchCaseIsSelected,
                    wordsIsSelected,
                    regexIsSelected,
                    numberSelected
                )
                val json = encodeBuilder.encodeToString(searchState)
                val searchStateFile = getSearchDataFile()
                searchStateFile.writeText(json)
            }
        }
    }
}

/** 加载编辑词库界面的设置信息 */
@OptIn(ExperimentalSerializationApi::class)
fun loadSearchState():SearchState{
    val cellVisibleSetting = getSearchDataFile()
    return if(cellVisibleSetting.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val searchData = decodeFormat.decodeFromString<SearchData>(cellVisibleSetting.readText())
            SearchState(searchData)
        }catch (exception:Exception){
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$cellVisibleSetting")
            SearchState(SearchData())
        }

    }else{
        SearchState(SearchData())
    }
}

/**   */
private fun getSearchDataFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "SearchState.json")
}
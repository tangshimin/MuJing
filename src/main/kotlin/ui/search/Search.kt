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

package ui.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import tts.rememberAzureTTS
import ui.wordscreen.WordScreenState
import util.rememberMonospace

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
fun Search(
    appState: AppState,
    wordScreenState: WordScreenState,
    vocabulary:  MutableVocabulary,
){
    val scope = rememberCoroutineScope()
    val azureTTS = rememberAzureTTS()
    var searchResult by remember{ mutableStateOf<Word?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    /** 等宽字体*/
    val monospace  = rememberMonospace()
    val onDismissRequest :() -> Unit = {
        appState.searching = false
    }
    val audioPlayer = LocalAudioPlayerComponent.current
    val keyEvent: (KeyEvent) -> Boolean = {
        val isModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
        if (isModifierPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
            onDismissRequest()
            true
        }else if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
            onDismissRequest()
            true
        }else if (isModifierPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) {
            if (!isPlayingAudio && searchResult != null && searchResult!!.value.isNotEmpty()) {
                scope.launch (Dispatchers.IO){
                    val audioPath = getAudioPath(
                        word = searchResult!!.value,
                        audioSet = appState.localAudioSet,
                        addToAudioSet = {audioPath -> appState.localAudioSet.add(audioPath)},
                        pronunciation = wordScreenState.pronunciation,
                        azureTTS = azureTTS,
                    )
                    playAudio(
                        word = searchResult!!.value,
                        audioPath = audioPath,
                        pronunciation =  wordScreenState.pronunciation,
                        volume = appState.global.audioVolume,
                        audioPlayerComponent = audioPlayer,
                        changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                    )
                }
            }
            true
        } else true // 不继续传播事件
    }

    Dialog(
        onDismissRequest = {onDismissRequest()},
    ) {

        val focusRequester = remember { FocusRequester() }
        var input by remember { mutableStateOf("") }

        /** 熟悉词库 */
        val familiarVocabulary = remember{ loadMutableVocabularyByName("FamiliarVocabulary") }

        val search:(String) -> Unit = {
                input = it
                if(searchResult != null) {
                    searchResult!!.value = ""
                }

                val inputWord = Word(value = input.lowercase().trim())
                // 先搜索当前词库
                val index = vocabulary.wordList.indexOf(inputWord)
                if(index != -1){
                    searchResult = vocabulary.wordList.get(index).deepCopy()
                }
                // 如果当前词库没有，或者当前词库的单词没有字幕，再搜索熟悉词库。
                if((searchResult == null) || searchResult!!.value.isEmpty() ||
                    (searchResult!!.captions.isEmpty() && searchResult!!.externalCaptions.isEmpty())){
                    val indexOf = familiarVocabulary.wordList.indexOf(inputWord)
                    if(indexOf != -1){
                        val familiar = familiarVocabulary.wordList.get(indexOf).deepCopy()
                        searchResult = familiar
                    }
                }

                // 如果词库里面没有，就搜索内置词典
                if((searchResult == null) || searchResult!!.value.isEmpty()){
                    val dictWord = Dictionary.query(input.lowercase().trim())
                    if(dictWord != null){
                        searchResult = dictWord.deepCopy()
                    }
                }

        }
        Surface(
            elevation = 5.dp,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
            modifier = Modifier
                .width(600.dp)
                .height(500.dp)
                .onKeyEvent { keyEvent(it) },
        ) {
            Box(Modifier.fillMaxSize()) {
                val stateVertical = rememberScrollState(0)
                Column(Modifier.verticalScroll(stateVertical)) {
                    Row(Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Localized description",
                            tint = if (MaterialTheme.colors.isLight) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                        )

                        BasicTextField(
                            value = input,
                            onValueChange = { search(it) },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = MaterialTheme.typography.h5.copy(
                                color = MaterialTheme.colors.onBackground,
                                fontFamily = monospace
                            ),
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 5.dp, bottom = 5.dp)
                                .focusRequester(focusRequester)
                        )

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                    }
                    Divider()
                    if (searchResult != null && searchResult!!.value.isNotEmpty()) {

                        SearchResultInfo(
                            word = searchResult!!,
                            appState = appState,
                            wordScreenState = wordScreenState,
                            azureTTS = azureTTS,
                        )

                        if (searchResult!!.captions.isNotEmpty()) {
                            searchResult!!.captions.forEachIndexed { index, caption ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${index + 1}. ${caption.content}",
                                        modifier = Modifier.padding(5.dp)
                                    )


                                    val mediaInfo = MediaInfo(
                                        caption = caption,
                                        mediaPath = vocabulary.relateVideoPath,
                                        trackId = vocabulary.subtitlesTrackId
                                    )
                                    PlayerBox(
                                        mediaInfo = mediaInfo,
                                        vocabularyDir =  wordScreenState.getVocabularyDir(),
                                        volume = appState.global.videoVolume
                                    )

                                }
                            }

                        }
                        if (searchResult!!.externalCaptions.isNotEmpty()) {
                            searchResult!!.externalCaptions.forEachIndexed { index, externalCaption ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${index + 1}. ${externalCaption.content}",
                                        modifier = Modifier.padding(5.dp)
                                    )
                                    val caption =
                                        Caption(externalCaption.start, externalCaption.end, externalCaption.content)


                                    val mediaInfo = MediaInfo(
                                        caption = caption,
                                        mediaPath = externalCaption.relateVideoPath,
                                        trackId = externalCaption.subtitlesTrackId
                                    )
                                    PlayerBox(
                                        mediaInfo = mediaInfo,
                                        vocabularyDir =  wordScreenState.getVocabularyDir(),
                                        volume = appState.global.videoVolume
                                    )
                                }
                            }

                        }

                    }else if(input.isNotEmpty()){
                        Text("没有找到相关单词")
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(stateVertical)
                )
            }


        }

    }

}
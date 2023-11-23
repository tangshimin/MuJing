package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import player.isWindows
import state.AppState
import state.DictationState
import state.MemoryStrategy
import state.WordScreenState
import ui.dialog.SelectChapterDialog

/**
 * 侧边菜单
 */
@OptIn(
    ExperimentalSerializationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
)
@Composable
fun WordScreenSidebar(
    state: AppState,
    wordScreenState: WordScreenState,
    dictationState: DictationState,
) {

    if (state.openSettings) {
        val scope = rememberCoroutineScope()
        Box(Modifier.width(216.dp).fillMaxHeight()){
            val stateVertical = rememberScrollState(0)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(stateVertical)
            ) {
                Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 78.dp else 48.dp))
                Divider()
                val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground

                var showDictationDialog by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showDictationDialog = true }.padding(start = 16.dp, end = 8.dp)
                ) {

                    Text("听写复习", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Filled.RateReview,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                    if(showDictationDialog){
                        SelectChapterDialog(
                            close = {showDictationDialog = false},
                            wordScreenState = wordScreenState,
                            isMultiple = true
                        )
                    }
                }
                var showChapterDialog by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showChapterDialog = true }.padding(start = 16.dp, end = 8.dp)
                ) {

                    Text("选择章节", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Filled.Apps,
                        contentDescription = "Localized description",
                        tint = tint,
                            modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                    if(showChapterDialog){
                        SelectChapterDialog(
                            close = {showChapterDialog = false},
                            wordScreenState = wordScreenState,
                            isMultiple = false
                        )
                    }
                }
                Divider()
                val ctrl = LocalCtrl.current
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("显示单词", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+V",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.wordVisible,
                        onCheckedChange = {
                            scope.launch {
                                wordScreenState.wordVisible = it
                                wordScreenState.saveWordScreenState()
                            }
                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text(text = "显示音标", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+P",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.phoneticVisible,
                        onCheckedChange = {
                            scope.launch {
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.Review ){
                                    dictationState.phoneticVisible = it
                                    dictationState.saveDictationState()
                                }
                                wordScreenState.phoneticVisible = it
                                wordScreenState.saveWordScreenState()
                            }
                        },

                        )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("显示词形", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+L",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.morphologyVisible,
                        onCheckedChange = {
                            scope.launch {
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.Review ){
                                    dictationState.morphologyVisible = it
                                    dictationState.saveDictationState()
                                }
                                wordScreenState.morphologyVisible = it
                                wordScreenState.saveWordScreenState()
                            }

                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("英文释义", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+E",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.definitionVisible,
                        onCheckedChange = {
                            scope.launch {
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.Review ){
                                    dictationState.definitionVisible = it
                                    dictationState.saveDictationState()
                                }
                                wordScreenState.definitionVisible = it
                                wordScreenState.saveWordScreenState()
                            }
                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("中文释义", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+K",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.translationVisible,
                        onCheckedChange = {
                            scope.launch {
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.Review ){
                                    dictationState.translationVisible = it
                                    dictationState.saveDictationState()
                                }
                                wordScreenState.translationVisible = it
                                wordScreenState.saveWordScreenState()
                            }

                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Row {
                        Text("显示字幕", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$ctrl+S",
                            color = MaterialTheme.colors.onBackground
                        )
                    }

                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.subtitlesVisible,
                        onCheckedChange = {
                            scope.launch {
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.Review ){
                                    dictationState.subtitlesVisible = it
                                    dictationState.saveDictationState()
                                }
                                wordScreenState.subtitlesVisible = it
                                wordScreenState.saveWordScreenState()
                            }

                        },
                    )
                }
                Divider()
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("击键音效", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = state.global.isPlayKeystrokeSound,
                        onCheckedChange = {
                            scope.launch {
                                state.global.isPlayKeystrokeSound = it
                                state.saveGlobalState()
                            }
                        },
                        )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("提示音效", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.isPlaySoundTips,
                        onCheckedChange = {
                            scope.launch {
                                wordScreenState.isPlaySoundTips = it
                                wordScreenState.saveWordScreenState()
                            }
                        },

                        )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text(text = "自动切换", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    val tooltip = if(wordScreenState.isAuto) "关闭之后使用 Enter 键切换到下一个单词" else "打开之后会自动切换到下一个单词"
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = tooltip, modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.CenterEnd,
                            alignment = Alignment.CenterEnd,
                            offset = DpOffset.Zero
                        )
                    ) {
                        Switch(
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                            checked = wordScreenState.isAuto,
                            onCheckedChange = {
                                scope.launch {
                                    wordScreenState.isAuto = it
                                    wordScreenState.saveWordScreenState()
                                }

                            },

                            )
                    }

                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("外部字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.externalSubtitlesVisible,
                        onCheckedChange = {
                            scope.launch {
                                wordScreenState.externalSubtitlesVisible = it
                                wordScreenState.saveWordScreenState()
                            }
                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("抄写字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = wordScreenState.isWriteSubtitles,
                        onCheckedChange = {
                            scope.launch {
                                wordScreenState.isWriteSubtitles = it
                                wordScreenState.saveWordScreenState()
                            }
                        },
                    )
                }
                var audioExpanded by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable {  audioExpanded = true }.padding(start = 16.dp, end = 8.dp)
                ) {

                    Row {
                        Text("音量控制", color = MaterialTheme.colors.onBackground)
                    }
                    Spacer(Modifier.width(15.dp))
                    CursorDropdownMenu(
                        expanded = audioExpanded,
                        onDismissRequest = { audioExpanded = false },
                    ) {
                        Surface(
                            elevation = 4.dp,
                            shape = RectangleShape,
                        ) {
                            Column(Modifier.width(300.dp).height(180.dp).padding(start = 16.dp, end = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("击键音效")
                                    Slider(value = state.global.keystrokeVolume, onValueChange = {
                                        Thread {
                                            state.global.keystrokeVolume = it
                                            state.saveGlobalState()
                                        }.start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("提示音效")
                                    Slider(value = wordScreenState.soundTipsVolume, onValueChange = {
                                        Thread {
                                            wordScreenState.soundTipsVolume = it
                                            wordScreenState.saveWordScreenState()
                                        }.start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("单词发音")
                                    Slider(value = state.global.audioVolume, onValueChange = {
                                        Thread {
                                            state.global.audioVolume = it
                                            state.saveGlobalState()
                                        }.start()
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("视频播放")
                                    Slider(
                                        value = state.global.videoVolume,
                                        valueRange = 1f..100f,
                                        onValueChange = {
                                        Thread {
                                            state.global.videoVolume = it
                                            state.saveGlobalState()
                                        }.start()
                                    })
                                }

                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }

                var expanded by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable {  expanded = true }
                        .padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("发音设置", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    CursorDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        Surface(
                            elevation = 4.dp,
                            shape = RectangleShape,
                        ) {
                            Row(Modifier.width(240.dp).height(120.dp)){
                                Column {
                                    if (wordScreenState.vocabulary.language == "english") {
                                        DropdownMenuItem(
                                            onClick = {
                                                scope.launch {
                                                    wordScreenState.pronunciation = "uk"
                                                    wordScreenState.saveWordScreenState()
                                                }
                                            },
                                            modifier = Modifier.width(120.dp).height(40.dp)
                                        ) {
                                            Text("英式发音")
                                            if(wordScreenState.pronunciation == "uk"){
                                                RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                            }
                                        }
                                        DropdownMenuItem(
                                            onClick = {
                                                scope.launch {
                                                    wordScreenState.pronunciation = "us"
                                                    wordScreenState.saveWordScreenState()
                                                }
                                            },
                                            modifier = Modifier.width(120.dp).height(40.dp)
                                        ) {
                                            Text("美式发音")
                                            if(wordScreenState.pronunciation == "us"){
                                                RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                            }
                                        }
                                    }

                                    if (wordScreenState.vocabulary.language == "japanese") {
                                        DropdownMenuItem(
                                            onClick = {
                                                scope.launch {
                                                    wordScreenState.pronunciation = "jp"
                                                    wordScreenState.saveWordScreenState()
                                                }
                                            },
                                            modifier = Modifier.width(120.dp).height(40.dp)
                                        ) {
                                            Text("日语")
                                            if(wordScreenState.pronunciation == "jp"){
                                                RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                            }
                                        }
                                    }

                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.pronunciation = "local TTS"
                                                wordScreenState.saveWordScreenState()
                                            }
                                        },
                                        modifier = Modifier.width(120.dp).height(40.dp)
                                    ) {
                                        Text("语音合成")
                                        if(wordScreenState.pronunciation == "local TTS"){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }
                                }
                                Divider(Modifier.width(1.dp).fillMaxHeight())
                                Column {
                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.playTimes = 0
                                                wordScreenState.saveWordScreenState()
                                            }
                                        },
                                        modifier = Modifier.width(120.dp).height(40.dp)
                                    ) {
                                        Text("关闭发音")
                                        if( wordScreenState.playTimes == 0){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.playTimes = 1
                                                wordScreenState.saveWordScreenState()
                                            }
                                        },
                                        modifier = Modifier.width(120.dp).height(40.dp)
                                    ) {
                                        Text("播放一次")
                                        if( wordScreenState.playTimes == 1){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.playTimes = 2
                                                wordScreenState.saveWordScreenState()
                                            }
                                        },
                                        modifier = Modifier.width(120.dp).height(40.dp)
                                    ) {
                                        Text("播放多次")
                                        if( wordScreenState.playTimes == 2){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.InterpreterMode,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }


            }

            VerticalScrollbar(
                style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(stateVertical)
            )
        }
    }
}

fun java.awt.Color.toCompose(): Color {
    return Color(red, green, blue)
}

fun Color.toAwt(): java.awt.Color {
    return java.awt.Color(red, green, blue)
}
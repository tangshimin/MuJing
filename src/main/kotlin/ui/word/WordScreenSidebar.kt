package ui.word

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tts.AzureTTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import state.AppState
import state.DictationState
import ui.dialog.AzureTTSDialog
import ui.dialog.SelectChapterDialog
import java.awt.Rectangle

/**
 * 侧边菜单
 */
@OptIn(
    ExperimentalSerializationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
)
@Composable
fun WordScreenSidebar(
    appState: AppState,
    wordScreenState: WordScreenState,
    dictationState: DictationState,
    azureTTS: AzureTTS,
    wordRequestFocus:() -> Unit,
    resetVideoBounds :() -> Rectangle,
) {

    if (appState.openSettings) {
        val scope = rememberCoroutineScope()
        Box(Modifier.width(216.dp).fillMaxHeight().onKeyEvent { it ->
            if (it.isCtrlPressed && it.key == Key.One && it.type == KeyEventType.KeyUp){
            scope.launch {
                appState.openSettings = !appState.openSettings
                if(!appState.openSettings){
                    wordRequestFocus()
                }
            }
            true
        }else false
        }){
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
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val tooltip =  "听写测试，可以选择多个章节"
                            Text(text = tooltip, modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.CenterEnd,
                        alignment = Alignment.CenterEnd,
                        offset = DpOffset(10.dp, 0.dp)
                    )
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { showDictationDialog = true }.padding(start = 16.dp, end = 8.dp)
                    ) {
                        Text("听写测试", color = MaterialTheme.colors.onBackground)
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
                                wordRequestFocus = wordRequestFocus,
                                wordScreenState = wordScreenState,
                                isMultiple = true
                            )
                        }
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
                            wordRequestFocus = wordRequestFocus,
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
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.DictationTest ){
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
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.DictationTest ){
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
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.DictationTest ){
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
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.DictationTest ){
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
                                if(wordScreenState.memoryStrategy== MemoryStrategy.Dictation || wordScreenState.memoryStrategy== MemoryStrategy.DictationTest ){
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
                        checked = appState.global.isPlayKeystrokeSound,
                        onCheckedChange = {
                            scope.launch {
                                appState.global.isPlayKeystrokeSound = it
                                appState.saveGlobalState()
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
                if(wordScreenState.isAuto){
                    Divider()
                }

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val tooltip = "拼写成功后，自动切换到下一个单词"
                            Text(text = tooltip, modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.CenterEnd,
                        alignment = Alignment.CenterEnd,
                         offset = DpOffset(10.dp, 0.dp)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                    ) {
                        Text(text = "自动切换", color = MaterialTheme.colors.onBackground)
                        Spacer(Modifier.width(15.dp))
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
                if (wordScreenState.isAuto) {
                    var times by remember { mutableStateOf("${wordScreenState.repeatTimes}") }
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                val tooltip = "拼写成功 $times 次后，自动切换到下一个单词"
                                Text(text = tooltip, modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.CenterEnd,
                            alignment = Alignment.CenterEnd,
                             offset = DpOffset(10.dp, 0.dp)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { }.padding(top = 10.dp,start = 16.dp, end = 14.dp,bottom = 10.dp)
                        ) {
                            Text(text = "重复次数", color = MaterialTheme.colors.onBackground)
                            val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                            BasicTextField(
                                value = times,
                                onValueChange = {
                                    scope.launch {
                                        times = it
                                        wordScreenState.repeatTimes = it.toIntOrNull() ?: 1
                                        wordScreenState.saveWordScreenState()
                                    }

                                },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                textStyle = TextStyle(
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colors.onBackground
                                ),
                                modifier = Modifier
                                    .width(40.dp)
                                    .border(border = border)
                                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                            )

                        }
                    }
                    Divider()
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val tooltip =  "播放视频时，自动加载外部字幕"
                            Text(text = tooltip, modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.CenterEnd,
                        alignment = Alignment.CenterEnd,
                        offset = DpOffset(10.dp, 0.dp)
                    )
                ) {
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
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val tooltip =  "播放视频后，光标自动移动到字幕"
                            Text(text = tooltip, modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.CenterEnd,
                        alignment = Alignment.CenterEnd,
                        offset = DpOffset(10.dp, 0.dp)
                    )
                ) {
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
                                    Slider(value = appState.global.keystrokeVolume, onValueChange = {
                                        scope.launch(Dispatchers.IO) {
                                            println("Current Thread Name:"+Thread.currentThread().name)
                                            appState.global.keystrokeVolume = it
                                            appState.saveGlobalState()
                                        }
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("提示音效")
                                    Slider(value = wordScreenState.soundTipsVolume, onValueChange = {
                                        scope.launch(Dispatchers.IO) {
                                            wordScreenState.soundTipsVolume = it
                                            wordScreenState.saveWordScreenState()
                                        }
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("单词发音")
                                    Slider(value = appState.global.audioVolume, onValueChange = {
                                        scope.launch(Dispatchers.IO) {
                                            appState.global.audioVolume = it
                                            appState.saveGlobalState()
                                        }
                                    })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("视频播放")
                                    Slider(
                                        value = appState.global.videoVolume,
                                        valueRange = 1f..100f,
                                        onValueChange = {
                                            scope.launch(Dispatchers.IO) {
                                                appState.global.videoVolume = it
                                                appState.saveGlobalState()
                                            }
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
                            Row(Modifier.width(260.dp).height(200.dp)){
                                Column {
                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.playTimes = 0
                                                wordScreenState.saveWordScreenState()
                                            }
                                        },
                                        modifier = Modifier.width(140.dp).height(40.dp)
                                    ) {
                                        Text("关闭发音")
                                        if( wordScreenState.playTimes == 0){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }
                                    if (wordScreenState.vocabulary.language == "english") {
                                        DropdownMenuItem(
                                            onClick = {
                                                scope.launch {
                                                    wordScreenState.pronunciation = "uk"
                                                    if( wordScreenState.playTimes == 0){
                                                        wordScreenState.playTimes = 2
                                                    }
                                                    wordScreenState.saveWordScreenState()
                                                }
                                            },
                                            modifier = Modifier.width(140.dp).height(40.dp)
                                        ) {
                                            Text("英式发音")
                                            if(wordScreenState.pronunciation == "uk" && wordScreenState.playTimes != 0){
                                                RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                            }
                                        }
                                        DropdownMenuItem(
                                            onClick = {
                                                scope.launch {
                                                    wordScreenState.pronunciation = "us"
                                                    wordScreenState.saveWordScreenState()
                                                    if( wordScreenState.playTimes == 0){
                                                        wordScreenState.playTimes = 2
                                                    }
                                                }
                                            },
                                            modifier = Modifier.width(140.dp).height(40.dp)
                                        ) {
                                            Text("美式发音")
                                            if(wordScreenState.pronunciation == "us" && wordScreenState.playTimes != 0){
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
                                                    if( wordScreenState.playTimes == 0){
                                                        wordScreenState.playTimes = 2
                                                    }
                                                }
                                            },
                                            modifier = Modifier.width(140.dp).height(40.dp)
                                        ) {
                                            Text("日语")
                                            if(wordScreenState.pronunciation == "jp" && wordScreenState.playTimes != 0){
                                                RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                            }
                                        }
                                    }

                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.pronunciation = "Azure TTS"
                                                wordScreenState.saveWordScreenState()
                                                if( wordScreenState.playTimes == 0){
                                                    wordScreenState.playTimes = 2
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(140.dp).height(40.dp)
                                    ) {
                                        Text("Azure TTS")
                                        if(wordScreenState.pronunciation == "Azure TTS" && wordScreenState.playTimes != 0){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }

                                    DropdownMenuItem(
                                        onClick = {
                                            scope.launch {
                                                wordScreenState.pronunciation = "local TTS"
                                                wordScreenState.saveWordScreenState()
                                                if( wordScreenState.playTimes == 0){
                                                    wordScreenState.playTimes = 2
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(140.dp).height(40.dp)
                                    ) {
                                        Text("本地语音合成")
                                        if(wordScreenState.pronunciation == "local TTS"){
                                            RadioButton(selected = true, onClick = {},Modifier.padding(start = 10.dp))
                                        }
                                    }

                                }
                                Divider(Modifier.width(1.dp).fillMaxHeight())
                                Column (Modifier.width(120.dp)){
                                    if(wordScreenState.playTimes != 0){
                                        var settingAzureTTS by remember { mutableStateOf(false) }
                                        if(wordScreenState.pronunciation == "Azure TTS"){
                                            DropdownMenuItem(
                                                onClick = { settingAzureTTS = true},
                                                modifier = Modifier.width(120.dp).height(40.dp)
                                            ) {
                                                Box(Modifier.fillMaxSize()){
                                                    IconButton(
                                                        onClick = {settingAzureTTS = true},
                                                        modifier = Modifier.align(Alignment.Center)
                                                    ){
                                                        Icon(
                                                            Icons.Filled.Tune,
                                                            contentDescription = "Localized description",
                                                            tint =  MaterialTheme.colors.onBackground,
                                                        )
                                                    }

                                                }

                                            }
                                        }
                                        if(settingAzureTTS){
                                            AzureTTSDialog(
                                                azureTTS = azureTTS,
                                                close = {settingAzureTTS = false}
                                            )
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
                    }

                    Icon(
                        imageVector = Icons.Filled.InterpreterMode,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }

                var playExpanded by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .clickable {  playExpanded = true }
                        .padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("播放设置", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))

                    CursorDropdownMenu(
                        expanded = playExpanded,
                        onDismissRequest = { playExpanded = false },
                    ) {
                        Surface(
                            elevation = 4.dp,
                            shape = RectangleShape,
                        ) {
                            Row(Modifier.width(280.dp).height(48.dp)){
                                DropdownMenuItem(
                                    onClick = { resetVideoBounds() },
                                    modifier = Modifier.width(280.dp).height(48.dp)
                                ) {
                                    Text("恢复播放器的默认大小和位置",modifier = Modifier.padding(end = 10.dp))
                                    Icon(
                                        imageVector = Icons.Filled.FlipToBack,
                                        contentDescription = "",
                                        tint = MaterialTheme.colors.onBackground,
                                        modifier = Modifier.size(40.dp, 40.dp)
                                    )
                                }
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )

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


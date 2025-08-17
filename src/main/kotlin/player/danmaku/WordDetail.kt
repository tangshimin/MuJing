package player.danmaku

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import data.Word

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.PlayerState
import ui.wordscreen.CopyButton
import ui.wordscreen.DeleteButton
import ui.wordscreen.FamiliarButton

enum class DisplayMode {
    DICT,  // 词典模式
    DANMAKU, // 弹幕模式
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WordDetail(
    word: Word,
    displayMode: DisplayMode = DisplayMode.DANMAKU,
    playerState: PlayerState,
    height: Dp,
    pointerExit:() -> Unit = {},
    deleteWord: (Word) -> Unit = {},
    addToFamiliar: (Word) -> Unit = {},
    playAudio: (String) -> Unit ={}
) {
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Surface(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp), // 设置圆角半径为 12dp
    ) {

        Column(
            Modifier.width(400.dp).height(height)
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .focusable(true)
                .focusRequester(focusRequester)
                .onPointerEvent(PointerEventType.Exit){
                    pointerExit()
                }
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Delete && keyEvent.isShiftPressed && keyEvent.type == KeyEventType.KeyUp) {
                        deleteWord(word)
                        true
                    } else if (keyEvent.key == Key.Y && keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyUp) {
                        addToFamiliar(word)
                        true
                    } else if (keyEvent.key == Key.C && keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyUp) {
                        clipboardManager.setText(AnnotatedString(word.value))
                        true
                    } else false
                }
        ) {
            var settingsExpanded by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(
                        text = word.value,
                        style = MaterialTheme.typography.h5,
                        color = Color.White,
                    )
                }
                IconButton(
                    onClick = { settingsExpanded = !settingsExpanded },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        if (settingsExpanded) Icons.Filled.Close else Icons.Filled.Settings,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }

            }


            Box {
                var tabState by remember { mutableStateOf(if (playerState.preferredChinese) 0 else 1) }
                Column(Modifier.align(Alignment.Center)) {

                    // 单词移动到 tooltip 的时候 Popup 会消失
                    var hasBug by remember { mutableStateOf(true) }
                    if(!hasBug){
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CopyButton(
                                wordValue = word.value,
                                alignment = Alignment.BottomCenter
                            )
                            FamiliarButton(
                                onClick = {
                                    addToFamiliar(word)
                                },
                                alignment = Alignment.BottomCenter
                            )
                            if(displayMode == DisplayMode.DANMAKU){

                                DeleteButton(
                                    onClick = {
                                        deleteWord(word)
                                    },
                                    alignment = Alignment.BottomCenter
                                )
                            }

                        }

                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("英 ${word.ukphone}  美 ${word.usphone}")
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                playAudio(word.value)
                            }
                        }) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                    Divider()

                    TabRow(
                        selectedTabIndex = tabState,
                        backgroundColor = Color.Transparent
                    ) {
                        Tab(
                            text = { Text("中文") },
                            selected = tabState == 0,
                            onClick = { tabState = 0 }
                        )
                        Tab(
                            text = { Text("英文") },
                            selected = tabState == 1,
                            onClick = { tabState = 1 }
                        )
                    }
                    when (tabState) {
                        0 -> {
                            TextBox(text = word.translation)
                        }

                        1 -> {
                            TextBox(text = word.definition)
                        }
                    }
                }
                if (settingsExpanded) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                        val modifier = Modifier.width(300.dp).padding(start = 90.dp).clickable { }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = modifier
                        ) {
                            Text("自动发音", modifier = Modifier.padding(start = 10.dp))
                            Switch(checked = playerState.autoSpeak, onCheckedChange = {
                                playerState.autoSpeak = it
                                playerState.savePlayerState()
                            })
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = modifier
                        ) {
                            Text("自动复制", modifier = Modifier.padding(start = 10.dp))
                            Switch(checked = playerState.autoCopy, onCheckedChange = {
                                playerState.autoCopy = it
                                playerState.savePlayerState()
                            })
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = modifier
                        ) {
                            Text("优先显示中文", modifier = Modifier.padding(start = 10.dp))
                            Switch(checked = playerState.preferredChinese, onCheckedChange = {
                                playerState.preferredChinese = it
                                if (it && tabState == 1) tabState = 0
                                playerState.savePlayerState()
                            })
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = modifier
                        ) {
                            Text("优先显示英文", modifier = Modifier.padding(start = 10.dp))
                            Switch(checked = !playerState.preferredChinese, onCheckedChange = {
                                playerState.preferredChinese = !it
                                if (it && tabState == 0) tabState = 1
                                playerState.savePlayerState()
                            })
                        }
                    }
                }
            }

        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            if (playerState.autoCopy) {
                clipboardManager.setText(AnnotatedString(word.value))
            }
            if (playerState.autoSpeak) {
                scope.launch(Dispatchers.IO) {
                    playAudio(word.value)
                }
            }
        }
    }
}


@Composable
fun TextBox(text: String) {
    Box(Modifier.fillMaxSize()) {
        val stateVertical = rememberScrollState(0)
        Box(Modifier.verticalScroll(stateVertical)) {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.onBackground,
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
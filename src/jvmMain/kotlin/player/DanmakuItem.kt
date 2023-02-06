package player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import data.Word
import ui.CopyButton
import ui.DeleteButton
import ui.FamiliarButton

class DanmakuItem(
    content: String,
    show: Boolean,
    sequence: Int = 0,
    startTime: Int,
    isPause: Boolean = false,
    position: IntOffset,
    word: Word? = null,
) {
    val content by mutableStateOf(content)
    var show by mutableStateOf(show)
    val startTime by mutableStateOf(startTime)
    var sequence by mutableStateOf(sequence)
    var isPause by mutableStateOf(isPause)
    var position by mutableStateOf(position)
    val word by mutableStateOf(word)
    override fun equals(other: Any?): Boolean {
        val otherItem = other as DanmakuItem
        return (this.content == otherItem.content && this.sequence == otherItem.sequence)
    }

    override fun hashCode(): Int {
        return content.hashCode() + sequence.hashCode()
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Danmaku(
    playerState: PlayerState,
    danmakuItem: DanmakuItem,
    playEvent: () -> Unit,
    playAudio: (String) -> Unit,
    fontFamily: FontFamily,
    windowHeight: Int,
    deleteWord: (DanmakuItem) -> Unit,
    addToFamiliar: (DanmakuItem) -> Unit,
    showingDetail:Boolean,
    showingDetailChanged:(Boolean) -> Unit
) {
    if (danmakuItem.show) {
        val text = if(danmakuItem.isPause){
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.LightGray)) {
                    val sequence = if(playerState.showSequence) danmakuItem.sequence else ""
                    append("$sequence ${danmakuItem.content}")
                }
            }


        }else{
            if (playerState.showSequence) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append("${danmakuItem.sequence} ${danmakuItem.content}")
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Transparent)) {
                        append(danmakuItem.sequence.toString())
                    }
                    withStyle(style = SpanStyle(color = Color.White)) {
                        append(" ${danmakuItem.content}")
                    }
                }
            }
        }

        val focusRequester = remember { FocusRequester() }
        fun enter() {
            if(!showingDetail){
                // 如果已经由⌈快速定位弹幕⌋暂停，就不执行。
                if (!danmakuItem.isPause) {
                    danmakuItem.isPause = true
                    showingDetailChanged(true)
                    playEvent()
                }

            }
        }

        fun exit() {
            danmakuItem.isPause = false
            playEvent()
            showingDetailChanged(false)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.h5,
            fontFamily = fontFamily,
            color = Color.White,
            modifier = Modifier
                .offset { danmakuItem.position }
                .onPointerEvent(PointerEventType.Enter) { enter() }
        )

        var offsetX =
            (danmakuItem.position.x - 200 + ((danmakuItem.sequence.toString().length + danmakuItem.content.length + 1) * 12).div(2)).dp
        if (offsetX < 0.dp) offsetX = 0.dp
        val offsetY = danmakuItem.position.y.dp
        val height = if (danmakuItem.position.y + 360 < windowHeight - 40) {
            350.dp
        } else {
            (windowHeight - 40 - danmakuItem.position.y).dp
        }
        val clipboardManager = LocalClipboardManager.current
        DropdownMenu(
            expanded = danmakuItem.isPause,
            onDismissRequest = {
                exit()
            },
            offset = DpOffset(offsetX, offsetY),
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { enter() }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown) {
                        exit()
                        true
                    } else false
                }
        ) {
            Surface(
                elevation = 4.dp,
                shape = RectangleShape,
            ) {

                Column(Modifier.width(400.dp).height(height)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    .focusable(true)
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Delete && keyEvent.isShiftPressed && keyEvent.type == KeyEventType.KeyUp) {
                            deleteWord(danmakuItem)
                            true
                        } else if (keyEvent.key == Key.Y && keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyUp) {
                            addToFamiliar(danmakuItem)
                            true
                        } else if (keyEvent.key == Key.C && keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyUp) {
                            clipboardManager.setText(AnnotatedString(danmakuItem.content))
                            true
                        } else false
                    }
                ) {
                    var settingsExpanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                        ) {
                            CopyButton(wordValue = danmakuItem.content)
                            FamiliarButton(onClick = { addToFamiliar(danmakuItem) })
                            DeleteButton(onClick = { deleteWord(danmakuItem) })
                        }

                        IconButton(
                            onClick = { settingsExpanded = !settingsExpanded },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                if (settingsExpanded) Icons.Filled.Close else Icons.Filled.Settings,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.primary
                            )
                        }

                    }


                    Box {
                        var tabState by remember { mutableStateOf(if (playerState.preferredChinese) 0 else 1) }
                        Column(Modifier.align(Alignment.Center)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = danmakuItem.content,
                                    style = MaterialTheme.typography.h5,
                                    color = Color.White,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("英 ${danmakuItem.word?.ukphone}  美 ${danmakuItem.word?.usphone}")
                                IconButton(onClick = {
                                    playAudio(danmakuItem.content)
                                }) {
                                    Icon(
                                        Icons.Filled.VolumeUp,
                                        contentDescription = "Localized description",
                                        tint = MaterialTheme.colors.primary,
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
                                    TextBox(text = danmakuItem.word?.translation ?: "")
                                }

                                1 -> {
                                    TextBox(text = danmakuItem.word?.definition ?: "")
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
                        clipboardManager.setText(AnnotatedString(danmakuItem.content))
                    }
                    if(playerState.autoSpeak){
                        playAudio(danmakuItem.content)
                    }
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
            style = LocalScrollbarStyle.current.copy(shape = if (isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}
package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ui.getPlayTripleMap
import player.play
import ui.secondsToString
import data.Caption
import data.Dictionary
import data.VocabularyType
import data.Word
import kotlinx.coroutines.launch
import player.isWindows
import state.AppState
import state.WordState
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 编辑当前单词
 * @param word 当前单词
 * @param state 应用程序的状态
 * @param save 点击保存之后调用的回调
 * @param close 点击取消之后调用的回调
 */
@OptIn(
    kotlinx.serialization.ExperimentalSerializationApi::class
)
@Composable
fun EditWordDialog(
    word: Word,
    state: AppState,
    wordState: WordState,
    save: (Word) -> Unit,
    close: () -> Unit
) {
    Dialog(
        title = "编辑单词",
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(610.dp, 700.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {

            var mutableWord by remember { mutableStateOf(word) }

            /**
             * 用户输入的单词
             */
            var inputWordStr by remember { mutableStateOf(TextFieldValue(word.value)) }
            var translationFieldValue by remember { mutableStateOf(TextFieldValue(word.translation)) }
            var definitionFieldValue by remember { mutableStateOf(TextFieldValue(word.definition)) }

            Box {
                Column(Modifier.fillMaxSize().align(Alignment.Center)) {
                    val textStyle = TextStyle(
                        color = MaterialTheme.colors.onBackground
                    )
                    val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    val modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    Divider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier
                    ) {
                        Text("单词：")
                        Spacer(Modifier.width(20.dp))
                        BasicTextField(
                            value = inputWordStr,
                            onValueChange = { inputWordStr = it },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onBackground
                            ),
                            modifier = Modifier
                                .border(border = border)
                                .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                        )
                        Spacer(Modifier.width(10.dp))

                        var updateFailed by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = {
                            Thread {
                                val newWord = Dictionary.query(inputWordStr.text)
                                if (newWord != null) {
                                    mutableWord = newWord
                                    translationFieldValue = TextFieldValue(newWord.translation)
                                    definitionFieldValue = TextFieldValue(newWord.definition)
                                    updateFailed = false
                                } else {
                                    updateFailed = true
                                }
                            }.start()
                        }) {
                            Text("查询")
                        }
                        if (updateFailed) {
                            Text("本地词典没有找到 ${inputWordStr.text} ", modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                    val boxModifier = Modifier.fillMaxWidth().height(115.dp).border(border = border)
                    val isWindows = isWindows()
                    Column(modifier = modifier) {
                        Text("中文释义：")
                        Box(modifier = boxModifier) {

                            val stateVertical = rememberScrollState(0)
                            BasicTextField(
                                value = translationFieldValue,
                                onValueChange = {
                                    translationFieldValue = it
                                },
                                textStyle = textStyle,
                                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                modifier = Modifier
                                    .verticalScroll(stateVertical)
                                    .fillMaxSize().padding(10.dp)
                            )
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical),
                                style = LocalScrollbarStyle.current.copy(shape = if(isWindows) RectangleShape else RoundedCornerShape(4.dp)),
                            )
                        }

                    }
                    Column(modifier = modifier) {
                        Text("英语释义：")
                        Box(modifier = boxModifier) {

                            val stateVertical = rememberScrollState(0)
                            BasicTextField(
                                value = definitionFieldValue,
                                onValueChange = {
                                    definitionFieldValue = it
                                },
                                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                textStyle = textStyle,
                                modifier = Modifier
                                    .verticalScroll(stateVertical)
                                    .fillMaxWidth().padding(10.dp)
                            )
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical),
                                style = LocalScrollbarStyle.current.copy(shape = if(isWindows) RectangleShape else RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                    var linkSize by remember { mutableStateOf(word.externalCaptions.size) }
                    EditingCaptions(
                        state = state,
                        typingWordState = wordState,
                        setLinkSize = { linkSize = it },
                        word = word
                    )

                    if (wordState.vocabulary.type == VocabularyType.DOCUMENT && linkSize <= 3) {
                        var isLink by remember { mutableStateOf(false) }
                        if (isLink && linkSize <= 3) {
                            LinkCaptionDialog(
                                word = word,
                                state = state,
                                typingWordState = wordState,
                                setLinkSize = { linkSize = it },
                                close = { isLink = false }
                            )
                        }

                        if (!isLink && linkSize < 3) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().height(58.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isLink = true },
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Text("链接字幕", modifier = Modifier.padding(end = 10.dp))
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "",
                                        tint = MaterialTheme.colors.primary,
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                        .padding(bottom = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    OutlinedButton(onClick = {
                        // 单词可以从词典查询到，或者只修改了中文释义或英文释义
                        if (inputWordStr.text == mutableWord.value) {
                            mutableWord.translation = translationFieldValue.text
                            mutableWord.definition = definitionFieldValue.text
                            save(mutableWord)
                        } else {
                            // 词典里没有这个单词，用户手动修改中文释义和英文释义
                            val newWord = Word(
                                value = inputWordStr.text,
                                usphone = "",
                                ukphone = "",
                                definition = definitionFieldValue.text,
                                translation = translationFieldValue.text,
                                pos = "",
                                collins = 0,
                                oxford = false,
                                tag = "",
                                bnc = 0,
                                frq = 0,
                                exchange = "",
                                externalCaptions = mutableListOf(),
                                captions = mutableListOf()
                            )
                            save(newWord)
                        }

                    }) {
                        Text("保存")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("取消")
                    }
                }
            }


        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class
)
@Composable
fun EditingCaptions(
    state: AppState,
    typingWordState: WordState,
    setLinkSize: (Int) -> Unit,
    word: Word
) {
    val scope = rememberCoroutineScope()
    val playTripleMap = getPlayTripleMap(typingWordState, word)
    playTripleMap.forEach { (index, playTriple) ->
        val captionContent = playTriple.first.content
        val relativeVideoPath = playTriple.second

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = captionContent, modifier = Modifier.padding(start = 10.dp))
            Row {
                val playerBounds by remember {
                    mutableStateOf(
                        Rectangle(
                            0,
                            0,
                            540,
                            303
                        )
                    )
                }
                val mousePoint by remember{ mutableStateOf(Point(0,0)) }
                var isVideoBoundsChanged by remember{mutableStateOf(false)}
                val resetVideoBounds:() -> Rectangle = {
                    isVideoBoundsChanged = false
                    Rectangle(mousePoint.x, mousePoint.y, 540, 303)
                }
                var isPlaying by remember { mutableStateOf(false) }
                IconButton(onClick = {},
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                            val location =
                                pointerEvent.awtEventOrNull?.locationOnScreen
                            if (location != null) {
                                if(isVideoBoundsChanged){
                                    mousePoint.x = location.x - 270 + 24
                                    mousePoint.y = location.y - 320
                                }else{
                                    playerBounds.x = location.x - 270 + 24
                                    playerBounds.y = location.y - 320
                                }

                                if (!isPlaying) {
                                    isPlaying = true
                                    val file = File(relativeVideoPath)
                                    if (file.exists()) {
                                        scope.launch {
                                            play(
                                                window = state.videoPlayerWindow,
                                                setIsPlaying = { isPlaying = it },
                                                volume = state.global.videoVolume,
                                                playTriple = playTriple,
                                                videoPlayerComponent = state.videoPlayerComponent,
                                                bounds = playerBounds,
                                                resetVideoBounds = resetVideoBounds,
                                                isVideoBoundsChanged = isVideoBoundsChanged,
                                                setIsVideoBoundsChanged = { isVideoBoundsChanged = it }
                                            )
                                        }
                                    } else {
                                        println("视频地址错误")
                                    }
                                }
                            }
                        }) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }

                var showSettingTimeLineDialog by remember { mutableStateOf(false) }
                if (showSettingTimeLineDialog) {
                    SettingTimeLine(
                        index = index,
                        state = state,
                        playTriple = playTriple,
                        mediaPlayerComponent = state.videoPlayerComponent,
                        confirm = { (index, start, end) ->
                            scope.launch {
                                if (typingWordState.vocabulary.type == VocabularyType.DOCUMENT) {
                                    word.externalCaptions[index].start = secondsToString(start)
                                    word.externalCaptions[index].end = secondsToString(end)
                                } else {
                                    word.captions[index].start = secondsToString(start)
                                    word.captions[index].end = secondsToString(end)
                                }
                                typingWordState.saveCurrentVocabulary()
                            }
                        },
                        close = { showSettingTimeLineDialog = false }
                    )
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "调整时间轴", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        showSettingTimeLineDialog = true
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.SpaceBar,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
                var showConfirmationDialog by remember { mutableStateOf(false) }
                if (showConfirmationDialog) {
                    ConfirmDialog(
                        message = "确定要删除 $captionContent 吗？",
                        confirm = {
                            scope.launch {
                                // 在 EditDialog 界面中点击保存，会保存整个词库
                                if (typingWordState.vocabulary.type == VocabularyType.DOCUMENT) {
                                    word.externalCaptions.removeAt(index)
                                } else {
                                    word.captions.removeAt(index)
                                }
                                playTripleMap.remove(index)
                                setLinkSize(playTripleMap.size)
                                showConfirmationDialog = false
                            }
                        },
                        close = { showConfirmationDialog = false }
                    )
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "删除", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        showConfirmationDialog = true

                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }

        }
    }
}

/**
 * 调整字幕时间轴
 * @param index 字幕的索引
 * @param close 点击取消后调用的回调
 * @param confirm 点击确定后调用的回调
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 */
@OptIn(
    kotlinx.serialization.ExperimentalSerializationApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun SettingTimeLine(
    index: Int,
    state: AppState,
    close: () -> Unit,
    confirm: (Triple<Int, Double, Double>) -> Unit,
    playTriple: Triple<Caption, String, Int>,
    mediaPlayerComponent: Component,
) {
    Dialog(
        title = "调整时间轴",
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(700.dp, 700.dp)
        ),
    ) {
        val playerBounds by remember {
            mutableStateOf(
                Rectangle(
                    0,
                    0,
                    540,
                    303
                )
            )
        }
        val mousePoint by remember{ mutableStateOf(Point(0,0)) }
        var isVideoBoundsChanged by remember{mutableStateOf(false)}
        val resetVideoBounds:() -> Rectangle = {
            isVideoBoundsChanged = false
            Rectangle(mousePoint.x, mousePoint.y, 540, 303)
        }
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    /**
                     * 协程构建器
                     */
                    val scope = rememberCoroutineScope()

                    /**
                     * 字幕内容
                     */
                    val caption = playTriple.first

                    /**
                     * 视频地址
                     */
                    val relativeVideoPath = playTriple.second

                    /**
                     * 当前字幕的开始时间，单位是秒
                     */
                    var start by remember {
                        mutableStateOf(
                            LocalTime.parse(caption.start, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                                .toNanoOfDay().toDouble().div(1000_000_000)
                        )
                    }
                    val oldStart = caption.start

                    /**
                     * 当前字幕的结束时间，单位是秒
                     */
                    var end by remember {
                        mutableStateOf(
                            LocalTime.parse(caption.end, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                                .toNanoOfDay().toDouble().div(1000_000_000)
                        )
                    }
                    val oldEnd = caption.end

                    /**
                     * 调整时间轴的精度
                     */
                    var precise by remember { mutableStateOf("1S") }
                    Divider()
                    Row(Modifier.width(540.dp).height(303.dp).padding(top = 40.dp)
                        .onGloballyPositioned { coordinates ->
                            val rect = coordinates.boundsInWindow()
                            if(isVideoBoundsChanged){
                                mousePoint.x = window.x + rect.left.toInt()
                                mousePoint.y = window.y + rect.top.toInt()
                            }else{
                                playerBounds.x = window.x + rect.left.toInt()
                                playerBounds.y = window.y + rect.top.toInt()
                            }

                        }) { }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(top = 40.dp)
                    ) {

                        Text("开始:")
                        TimeControl(
                            time = start,
                            addTime = { start += it },
                            minusTime = { start -= it },
                            precise = precise,
                        )
                        Spacer(Modifier.width(20.dp))
                        Text("结束:")
                        TimeControl(
                            time = end,
                            addTime = { end += it },
                            minusTime = { end -= it },
                            precise = precise,
                        )
                        Spacer(Modifier.width(20.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Text("精度:")
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(93.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(text = precise)
                                Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                            }
                            val menuItemModifier = Modifier.width(93.dp).height(30.dp)
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(93.dp)
                                    .height(190.dp)
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "1S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("1S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "0.5S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.5S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "0.1S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.1S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "0.05S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.05S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "0.01S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.01S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = "0.001S"
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.001S")
                                }
                            }

                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.width(20.dp))
                        OutlinedButton(onClick = {
                            if (oldStart != secondsToString(start) ||
                                oldEnd != secondsToString(end)
                            ) {
                                confirm(Triple(index, start, end))
                            }
                            close()
                        }) {
                            Text("确定")
                        }
                        var isPlaying by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = {},
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                                    val location =
                                        pointerEvent.awtEventOrNull?.locationOnScreen
                                    if (location != null && !isPlaying) {
                                        isPlaying = true
                                        val file = File(relativeVideoPath)
                                        if (file.exists()) {
                                            scope.launch {
                                                playTriple.first.start = secondsToString(start)
                                                playTriple.first.end = secondsToString(end)
                                                play(
                                                    window = state.videoPlayerWindow,
                                                    setIsPlaying = { isPlaying = it },
                                                    volume = state.global.videoVolume,
                                                    playTriple = playTriple,
                                                    videoPlayerComponent = mediaPlayerComponent,
                                                    bounds = playerBounds,
                                                    resetVideoBounds = resetVideoBounds,
                                                    isVideoBoundsChanged = isVideoBoundsChanged,
                                                    setIsVideoBoundsChanged = { isVideoBoundsChanged = it }
                                                )
                                            }

                                        } else {
                                            println("视频地址错误")
                                        }
                                    }
                                }
                        ) {
                            Text("预览")
                        }


                        OutlinedButton(onClick = { close() }, modifier = Modifier.padding(start = 20.dp)) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 调整时间轴的开始或结束时间
 * @param time 时间
 * @param addTime 点击增加按钮后调用的回调
 * @param minusTime 点击减少按钮后调用的回调
 * @param precise 调整精度
 */
@Composable
fun TimeControl(
    time: Double,
    addTime: (Float) -> Unit,
    minusTime: (Float) -> Unit,
    precise: String,
) {
    Text(text = secondsToString(time))
    Column {
        Icon(Icons.Filled.Add,
            contentDescription = "",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                when (precise) {
                    "1S" -> {
                        addTime(1F)
                    }
                    "0.5S" -> {
                        addTime(0.5F)
                    }
                    "0.1S" -> {
                        addTime(0.1F)
                    }
                    "0.05S" -> {
                        addTime(0.05F)
                    }
                    "0.01S" -> {
                        addTime(0.01F)
                    }
                    "0.001S" -> {
                        addTime(0.001F)
                    }
                }
            })
        Icon(Icons.Filled.Remove,
            contentDescription = "",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                when (precise) {
                    "1S" -> {
                        minusTime(1F)
                    }
                    "0.5S" -> {
                        minusTime(0.5F)
                    }
                    "0.1S" -> {
                        minusTime(0.1F)
                    }
                    "0.05S" -> {
                        minusTime(0.05F)
                    }
                    "0.01S" -> {
                        minusTime(0.01F)
                    }
                    "0.001S" -> {
                        minusTime(0.001F)
                    }
                }
            })
    }
}
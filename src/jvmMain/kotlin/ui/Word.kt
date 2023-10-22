package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import data.Word
import player.AudioButton
import state.GlobalState
import state.getResourcesFile
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.sound.sampled.*
import kotlin.concurrent.schedule

/** 单词组件
 * @param word 单词
 * @param global 全局状态
 * @param wordVisible 单词可见性
 * @param pronunciation 单词发音
 * @param playTimes 单词播放次数
 * @param isPlaying 是否正在播放单词发音
 * @param setIsPlaying 设置单词发音播放状态
 * @param isDictation 是否是听写模式
 * @param correctTime 单词的正确数
 * @param wrongTime 单词的错误数
 * @param textFieldValue 用户输入的字符串
 * @param typingResult 用户输入字符的结果
 * @param checkTyping 检查用户的输入是否正确的回调
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Word(
    word: Word,
    global: GlobalState,
    isDictation:Boolean,
    wordVisible:Boolean,
    pronunciation: String,
    playTimes: Int,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    fontFamily: FontFamily,
    audioPath: String,
    correctTime: Int,
    wrongTime: Int,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    focusRequester: FocusRequester,
    textFieldKeyEvent: (KeyEvent) -> Boolean,
    showMenu: () -> Unit,
) {


    val wordValue = word.value
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 48.dp).height(IntrinsicSize.Max)
        ) {
            var textHeight by remember { mutableStateOf(0.dp) }
            val bottom = computeBottom(
               textStyle =  global.wordTextStyle,
                textHeight = textHeight,
            )
            val smallStyleList = listOf("H5","H6","Subtitle1","Subtitle2","Body1","Body2","Button","Caption","Overline")
            Box(Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(start = 50.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    if (!isDictation) {
                        showMenu()
                    }
                }) {
                val fontSize = global.wordFontSize
                DisableTextMenuAndClipboardProvider{
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            checkTyping(input)
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = TextStyle(
                            color = Color.Transparent,
                            fontSize = fontSize,
                            letterSpacing =  global.letterSpacing,
                            fontFamily =fontFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottom)
                            .align(Alignment.Center)
                            .focusRequester(focusRequester)
                            .onKeyEvent { textFieldKeyEvent(it) }
                    )
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Text(
                    modifier = Modifier
                        .padding(bottom = bottom)
                        .align(Alignment.Center)
                        .onGloballyPositioned { layoutCoordinates ->
                        textHeight = (layoutCoordinates.size.height).dp
                    },
                    text = buildAnnotatedString {
                        typingResult.forEach { (char, correct) ->
                            if (correct) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.primary,
                                        fontSize = fontSize,
                                        letterSpacing = global.letterSpacing,
                                        fontFamily = fontFamily,
                                    )
                                ) {
                                    append(char)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = Color.Red,
                                        fontSize =fontSize,
                                        letterSpacing = global.letterSpacing,
                                        fontFamily =fontFamily,
                                    )
                                ) {
                                    if (char == ' ') {
                                        append("_")
                                    } else {
                                        append(char)
                                    }
                                }
                            }
                        }
                        val remainChars = wordValue.substring(typingResult.size)
                        if (isDictation) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.onBackground,
                                    fontSize = fontSize,
                                    letterSpacing =  global.letterSpacing,
                                    fontFamily = fontFamily,
                                )
                            ) {
                                repeat(remainChars.length) {
                                    append(" ")
                                }

                            }
                        } else {
                            if (wordVisible) {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = fontSize,
                                        letterSpacing =  global.letterSpacing,
                                        fontFamily =fontFamily,
                                    )
                                ) {
                                    append(remainChars)
                                }
                            } else {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colors.onBackground,
                                        fontSize = fontSize,
                                        letterSpacing =  global.letterSpacing,
                                        fontFamily = fontFamily,
                                    )
                                ) {
                                    repeat(remainChars.length) {
                                        append("_")
                                    }
                                }

                            }

                        }
                    }
                )
            }


            Column {
                val top = (textHeight - 36.dp).div(2)
                var numberFontSize = LocalTextStyle.current.fontSize
                if(smallStyleList.contains(global.wordTextStyle)) numberFontSize = MaterialTheme.typography.overline.fontSize
                Spacer(modifier = Modifier.height(top))
                Text(text = "${if (correctTime > 0) correctTime else ""}",
                    color = MaterialTheme.colors.primary,
                    fontSize =  numberFontSize)
                Spacer(modifier = Modifier.height(top))
                Text(text = "${if (wrongTime > 0) wrongTime else ""}",
                    color = Color.Red,
                    fontSize =  numberFontSize
                )
            }
            var paddingTop = textHeight.div(2) - 20.dp
            if(paddingTop<0.dp) paddingTop =  0.dp
            if(global.wordTextStyle == "H1") paddingTop = 23.dp

            AudioButton(
                audioPath = audioPath,
                word = wordValue,
                volume = global.audioVolume,
                isPlaying = isPlaying,
                setIsPlaying = setIsPlaying,
                pronunciation = pronunciation,
                playTimes = playTimes,
                paddingTop = paddingTop,
            )
        }
}

/**
 * 音标组件
 */
@Composable
fun Phonetic(
    word: Word,
    phoneticVisible: Boolean,
) {
    if (phoneticVisible) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (word.usphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "美:${word.usphone}",
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            if (word.ukphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "英:${word.ukphone}", color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }

        }
    }
}

/**
 * 禁用文本菜单和剪贴板
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DisableTextMenuAndClipboardProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            )  {

                val items = {listOf<ContextMenuItem>()}
                ContextMenuArea(items, state, content = content)
            }
        },
        LocalClipboardManager provides object :  ClipboardManager {
            override fun getText(): AnnotatedString {
                return AnnotatedString("")
            }

            override fun setText(text: AnnotatedString) {}
        },
        content = content
    )
}

/**
 * 自定义文本菜单和剪贴板，文本菜单只保留复制
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTextMenuProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            )  {
                val localization = LocalLocalization.current
                val items = {
                    listOfNotNull(
                        textManager.copy?.let {
                            ContextMenuItem(localization.copy, it)
                        }
                    )
                }

                ContextMenuArea(items, state, content = content)
            }
        },
        LocalClipboardManager provides object :  ClipboardManager {
            // paste
            override fun getText(): AnnotatedString {
                return AnnotatedString("")
            }
            // copy
            override fun setText(text: AnnotatedString) {
                 Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text.text), null)
            }
        },
        content = content
    )
}

/**
 * 播放音效
 * @param path 路径
 * @param volume 音量
 */
fun playSound(path: String, volume: Float) {
    try {
        val file = getResourcesFile(path)
        AudioSystem.getAudioInputStream(file).use { audioStream ->
            val format = audioStream.format
            val info: DataLine.Info = DataLine.Info(Clip::class.java, format)
            val clip: Clip = AudioSystem.getLine(info) as Clip
            clip.addLineListener{event ->
                if (event.type == LineEvent.Type.STOP) {
                    Timer("clip close", false).schedule(500) {
                        clip.close()
                    }
                }
            }
            clip.open(audioStream)
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val range = gainControl.maximum - gainControl.minimum
            val value = (range * volume) + gainControl.minimum
            gainControl.value = value
            clip.start()
            clip.drain()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/** 计算单词的底部内边距 */
fun computeBottom(
    textStyle:String,
    textHeight:Dp,
): Dp {
    var bottom = 0.dp
    val smallStyleList = listOf("H5","H6","Subtitle1","Subtitle2","Body1","Body2","Button","Caption","Overline")
    if(smallStyleList.contains(textStyle)) bottom = (36.dp - textHeight).div(2)
    if(bottom<0.dp) bottom = 0.dp
    if(bottom>7.5.dp) bottom = 5.dp
    return bottom
}
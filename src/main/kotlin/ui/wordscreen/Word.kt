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

package ui.wordscreen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import tts.AzureTTS
import data.Word
import player.AudioButton
import state.GlobalState
import state.getResourcesFile
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
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
    showUnderline:Boolean,
    wordVisible:Boolean,
    pronunciation: String,
    azureTTS: AzureTTS,
    playTimes: Int,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    fontFamily: FontFamily,
    audioSet:Set<String>,
    addToAudioSet:(String) -> Unit,
    correctTime: Int,
    wrongTime: Int,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (String) -> Unit,
    focusRequester: FocusRequester,
    updateFocusState: (Boolean) -> Unit,
    textFieldKeyEvent: (KeyEvent) -> Boolean,
    showMenu: (Boolean) -> Unit,
) {

    var hideMenuTask : TimerTask? by remember{ mutableStateOf(null) }

    val wordValue = word.value
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Max)
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
                .onPointerEvent(PointerEventType.Move) {
                    if (!isDictation) {
                        showMenu(true)
                        hideMenuTask?.cancel()
                        hideMenuTask = Timer("hideMenu", false).schedule(5000) {
                            showMenu(false)
                        }
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
                            .onFocusChanged{ focusState ->
                                updateFocusState(focusState.isFocused)
                            }
                            .onKeyEvent { textFieldKeyEvent(it) }
                    )
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Text(
                    modifier = Modifier
                        .testTag("Word")
                        .padding(bottom = bottom)
                        .align(Alignment.Center)
                        .onGloballyPositioned { layoutCoordinates ->
                            // 测量单词的高度
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
                            if(showUnderline){
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
                            }else{
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

            if(global.showInputCount){
                Column (Modifier.height(textHeight),
                    verticalArrangement = Arrangement.Center
                ){
                    var numberFontSize = LocalTextStyle.current.fontSize
                    if(smallStyleList.contains(global.wordTextStyle)) numberFontSize = MaterialTheme.typography.overline.fontSize
                    Text(text = "${if (correctTime > 0) correctTime else ""}",
                        color = MaterialTheme.colors.primary,
                        fontSize =  numberFontSize,)

                    Spacer(modifier = Modifier.height(textHeight.div(4)))
                    Text(text = "${if (wrongTime > 0) wrongTime else ""}",
                        color = Color.Red,
                        fontSize =  numberFontSize,
                    )
                }
            }else{
                Spacer(modifier = Modifier.width(2.dp))
            }

            AudioButton(
                audioSet = audioSet,
                addToAudioSet = addToAudioSet,
                word = wordValue,
                volume = global.audioVolume,
                isPlaying = isPlaying,
                setIsPlaying = setIsPlaying,
                pronunciation = pronunciation,
                azureTTS = azureTTS,
                playTimes = playTimes,
                paddingTop = 12.dp,
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
    fontSize: TextUnit
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
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            if (word.ukphone.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = "英:${word.ukphone}",
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onBackground,
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

        LocalClipboard provides object : Clipboard {
            override val nativeClipboard: NativeClipboard
                get() = NativeClipboard()
            // 禁用粘贴
            override suspend fun getClipEntry(): ClipEntry? {
                return null
            }
            // 禁用复制
            override suspend fun setClipEntry(clipEntry: ClipEntry?) {

            }
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
        LocalClipboard provides object : Clipboard {
            override val nativeClipboard: NativeClipboard
                get() = NativeClipboard()
            // 禁用粘贴
            override suspend fun getClipEntry(): ClipEntry? {
                return null
            }
            // 复制
            override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                val transferable = clipEntry?.nativeClipEntry as? Transferable
                if(transferable !== null){
                    val data = transferable.getTransferData(DataFlavor.stringFlavor) as? String
                    if (data != null) {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(data), null)
                        return
                    }
                }
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
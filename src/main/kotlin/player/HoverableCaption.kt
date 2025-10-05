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

package player

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.Dictionary
import data.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import player.danmaku.DisplayMode
import player.danmaku.WordDetail

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HoverableText(
    text: String,
    color : Color = Color.White,
    isActive: Boolean = true,
    playerState: PlayerState,
    playAudio:(String) -> Unit = {},
    modifier: Modifier = Modifier,
    onPopupHoverChanged: (Boolean) -> Unit = {},
    addWord: (Word) -> Unit = {},
    addToFamiliar: (Word) -> Unit = {},

) {
    var expanded by remember { mutableStateOf(false) }
    var isInPopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()


    Box(modifier = Modifier) {
        val interactionSource = remember { MutableInteractionSource()}
        val hoverJob = remember { mutableStateOf<Job?>(null) }
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .background(
                    if (expanded) Color(0xFF29417F) // 悬停时的背景颜色,使用经典的文本选择颜色
                    else Color.Transparent
                )
                .hoverable(interactionSource)
                .onPointerEvent(PointerEventType.Enter) {
                    if(!isActive) return@onPointerEvent
                    hoverJob.value?.cancel() // 取消之前的 Job
                    hoverJob.value = scope.launch {
                        delay(300) // 设置悬停最少停留时间为 300 毫秒
                        expanded = true
                    }
                }
                .onPointerEvent(PointerEventType.Exit) {
                    if(!isActive) return@onPointerEvent
                    hoverJob.value?.cancel() // 取消悬停的 Job
                    // 添加延时，让 Popup 的 Enter 事件有机会先执行
                    scope.launch {
                        delay(50)
                        if (!isInPopup) {
                            expanded = false
                        }
                    }

                }
        )

        if (expanded && isActive) {
            val dictWord = Dictionary.query(text.lowercase().trim())

            Popup(
                alignment = Alignment.TopCenter,
                offset = with(density) { IntOffset(0, -350.dp.toPx().toInt()) },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Column( modifier = Modifier
                    .size(400.dp, 353.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        isInPopup = true
                        onPopupHoverChanged(true)
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isInPopup = false
                        expanded = false
                        onPopupHoverChanged(false)
                    }){
                    Box(modifier = Modifier.size(400.dp, 350.dp),) {
                        if(dictWord != null){
                            WordDetail(
                                word =dictWord ,
                                displayMode = DisplayMode.DICT,
                                playerState = playerState,
                                pointerExit = {},
                                height = 350.dp,
                                addWord = addWord,
                                addToFamiliar = addToFamiliar,
                                playAudio =playAudio ,
                            )
                        }else{
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "内置词典里面没有 \"$text\"",
                                    style = MaterialTheme.typography.body1,
                                    color = Color.White
                                )
                            }
                        }

                    }
                    Box(Modifier.height(3.dp).width(400.dp).background(Color.Transparent))
                }
            }
        }
    }
}


@Composable
fun HoverableCaption(
    caption: String,
    playAudio: (String) -> Unit,
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onPopupHoverChanged: (Boolean) -> Unit = {},
    addWord: (Word) -> Unit = {},
    addToFamiliar: (Word) -> Unit = {},
    primaryCaptionVisible : Boolean = true,
    secondaryCaptionVisible : Boolean= true,
    isBilingual : Boolean= false,
    swapEnabled: Boolean = false,
) {
    Column(modifier) {
        val lines = caption.split("\n")

        // 处理双语字幕的显示顺序
        if (isBilingual && lines.size >= 2) {
            // 确定主次字幕的显示顺序
            val primaryLine = lines[0]
            val secondaryLine =  lines[1]

            // 根据交换状态和可见性决定显示顺序
            val displayLines = mutableListOf<String>()

            if (swapEnabled) {
                // 交换模式：次字幕在前，主字幕在后
                displayLines.add(secondaryLine)
                displayLines.add(primaryLine)
            } else {
                // 正常模式：主字幕在前，次字幕在后
                displayLines.add(primaryLine)
                displayLines.add(secondaryLine)
            }

            displayLines.forEachIndexed { index, line ->
                val color = when (index) {
                    0 -> {
                        // 主字幕（白色）
                        if (primaryCaptionVisible)Color.White else Color.Transparent
                    }
                    1 -> {
                        // 次字幕（灰色）
                        if (secondaryCaptionVisible) Color(0xFFAAAAAA) else Color.Transparent
                    }
                    else -> {
                        Color.Transparent
                    }
                }


                renderCaptionLine(line, color, playAudio, playerState, onPopupHoverChanged, addWord, addToFamiliar)

            }
        } else {
            // 单语字幕或非双语情况
            lines.forEachIndexed { index, line ->
                val color = if (isBilingual) {
                    when (index) {
                        0 if primaryCaptionVisible -> Color.White
                        1 if secondaryCaptionVisible -> Color(0xFFAAAAAA)
                        else -> Color.Transparent
                    }
                } else {
                    if (primaryCaptionVisible) Color.White else Color.Transparent
                }


                renderCaptionLine(line, color, playAudio, playerState, onPopupHoverChanged, addWord, addToFamiliar)

            }
        }
    }
}

@Composable
private fun renderCaptionLine(
    line: String,
    color: Color,
    playAudio: (String) -> Unit,
    playerState: PlayerState,
    onPopupHoverChanged: (Boolean) -> Unit,
    addWord: (Word) -> Unit,
    addToFamiliar: (Word) -> Unit,
) {
    Row {
        // 改进的分词逻辑
        val words = line.split(Regex("\\s+")) // 按空格分割
        words.forEachIndexed { index, rawWord ->
            if (rawWord.isNotEmpty()) {
                // 提取开头的标点符号
                val leadingPunctuation = rawWord.takeWhile { !(it in 'a'..'z' || it in 'A'..'Z' || it.isDigit()) }
                val remaining = rawWord.drop(leadingPunctuation.length)

                // 从剩余部分提取单词（字母、撇号、连字符）
                val wordPart = remaining.takeWhile { it in 'a'..'z' || it in 'A'..'Z' || it.isDigit() || it == '\'' || it == '-' }
                val trailingPunctuation = remaining.drop(wordPart.length)

                // 渲染开头标点
                if (leadingPunctuation.isNotEmpty()) {
                    Text(
                        leadingPunctuation,
                        color = color,
                        style = MaterialTheme.typography.h4
                    )
                }

                // 渲染单词部分
                if (wordPart.isNotEmpty()) {
                    HoverableText(
                        text = wordPart,
                        color = color,
                        isActive = color != Color.Transparent,
                        playAudio = playAudio,
                        playerState = playerState,
                        modifier = Modifier,
                        onPopupHoverChanged = onPopupHoverChanged,
                        addWord = addWord,
                        addToFamiliar = addToFamiliar,
                    )
                }

                // 渲染结尾标点
                if (trailingPunctuation.isNotEmpty()) {
                    Text(
                        trailingPunctuation,
                        color = color,
                        style = MaterialTheme.typography.h4
                    )
                }

                // 添加空格（除了最后一个词）
                if (index < words.size - 1) {
                    Text(
                        " ",
                        color = color,
                        style = MaterialTheme.typography.h4
                    )
                }
            }
        }
    }
}
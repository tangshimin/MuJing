package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.Word
import state.MemoryStrategy


/**
 * 当前章节完成之后的对话框
 * @param close 点击取消之后调用的回调函数
 * @param correctRate 正确率
 * @param isVocabularyFinished 是否整个词典的单词都已经学习完成
 * @param isReviewWrong 是否在复习错误单词
 * @param dictationWrongWords 听写模式的错误单词
 * @param enterDictation 进入听写模式后调用的回调函数
 * @param learnAgain 选择【重复本章】后调用的回调函数
 * @param reviewWrongWords 选择【复习错误单词】后调用的回调函数
 * @param nextChapter 选择【下一章】后调用的回调函数
 * @param resetIndex 选择【返回到第一章】后调用的回调函数，或者选择【随机重置词库】后调用的函数
 * boolean 为 true 表示要随机重置词库
 */
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
@Composable
fun ChapterFinishedDialog(
    close: () -> Unit,
    correctRate: Float,
    isVocabularyFinished: Boolean,
    memoryStrategy: MemoryStrategy,
    openReviewDialog:() -> Unit,
    isReviewWrong: Boolean,
    dictationWrongWords: Map<Word, Int>,
    enterDictation: () -> Unit,
    learnAgain: () -> Unit,
    reviewWrongWords: () -> Unit,
    nextChapter: () -> Unit,
    resetIndex: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val width = if (isVocabularyFinished) 650.dp else 500.dp
    val height = (280 + ((dictationWrongWords.size * 18) + 10)).dp
    Dialog(
        title = "",
        onCloseRequest = { close() },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(width, height)
        ),
    ) {
        WindowDraggableArea {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .focusable(true)
                    .fillMaxSize()
                    .focusable()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        when {
                            (it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                                // 进入听写模式
                                enterDictation()
                                true
                            }
                            (it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
                                // 进入复习错误单词模式
                                reviewWrongWords()
                                true
                            }
                            (it.key == Key.Enter && it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                learnAgain()
                                true
                            }

                            ((it.key == Key.Enter || it.key == Key.NumPadEnter)
                                    && it.type == KeyEventType.KeyUp) -> {

                                if(memoryStrategy == MemoryStrategy.DictationTest || memoryStrategy == MemoryStrategy.DictationTestReviewWrong){
                                    openReviewDialog()
                                }else if (isVocabularyFinished) resetIndex(false)
                                else nextChapter()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                val textColor = MaterialTheme.colors.primary
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    val title = if (isVocabularyFinished) {
                        "您已完成最后一个章节"
                    } else if (memoryStrategy == MemoryStrategy.Dictation) {
                            "您已听写完本章节"
                    } else if (memoryStrategy == MemoryStrategy.DictationTest) {
                            "您已测试完本章节"
                    } else if (isReviewWrong){
                        "您已复习完错误单词"
                    }else "您已学习完本章节"

                    Text(text = title, color = MaterialTheme.colors.onBackground)
                }

                if (memoryStrategy == MemoryStrategy.Dictation || memoryStrategy == MemoryStrategy.DictationTest) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "正确率 ", color = MaterialTheme.colors.onBackground)
                            Text(text = "$correctRate%", color = MaterialTheme.colors.primary)
                        }

                        Column(Modifier.width(IntrinsicSize.Max).padding(top = 10.dp)) {
                            if (correctRate < 100F) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "单词", color = MaterialTheme.colors.onBackground)
                                    Text(
                                        text = "错误数", textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.onBackground,
                                        modifier = Modifier.width(50.dp)
                                    )
                                }
                            }

                            if (dictationWrongWords.isNotEmpty()) Divider(Modifier.fillMaxWidth())
                            var list = dictationWrongWords.toList()
                            list = list.sortedBy { pair ->
                                pair.second
                            }
                            list.forEach {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = it.first.value, color = MaterialTheme.colors.onBackground)
                                    Spacer(Modifier.width(30.dp))
                                    Text(
                                        text = "${it.second}",
                                        textAlign = TextAlign.Center,
                                        color = Color.Red,
                                        modifier = Modifier.width(50.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(33.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "快捷键 Enter", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        if(memoryStrategy == MemoryStrategy.DictationTest || memoryStrategy == MemoryStrategy.DictationTestReviewWrong){
                            OutlinedButton(onClick = {
                                openReviewDialog()
                            }) {
                                Text("(⏎)选择章节", color = textColor)
                            }
                        }else if (isVocabularyFinished) {
                            OutlinedButton(onClick = {
                                resetIndex(false)
                            }) {
                                Text("(⏎)返回到第一章", color = textColor)
                            }
                        } else {
                            OutlinedButton(onClick = {
                                nextChapter()
                            }) {
                                Text("(⏎)下一章", color = textColor)
                            }
                        }
                    }

                    Spacer(Modifier.width(15.dp))
                    if (memoryStrategy == MemoryStrategy.Normal) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "快捷键 Shift+Enter", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            OutlinedButton(onClick = {
                                learnAgain()
                            }) { Text("(Shift+⏎)重复本章", color = textColor) }
                        }
                    }
                    Spacer(Modifier.width(15.dp))
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "快捷键 V", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        OutlinedButton(
                            onClick = { enterDictation() }
                        ) {
                            val text = if (memoryStrategy == MemoryStrategy.Dictation || memoryStrategy == MemoryStrategy.DictationTest) "(V)再听写一次" else "(V)听写测试"
                            Text(text = text, color = textColor)
                        }
                    }
                    Spacer(Modifier.width(15.dp))
                    if ((memoryStrategy == MemoryStrategy.Dictation || memoryStrategy == MemoryStrategy.DictationTest ||
                                memoryStrategy == MemoryStrategy.NormalReviewWrong ||  memoryStrategy == MemoryStrategy.DictationTestReviewWrong
                            )  &&  correctRate < 100F ) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "快捷键 N", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            val text = if(memoryStrategy == MemoryStrategy.NormalReviewWrong ||  memoryStrategy == MemoryStrategy.DictationTestReviewWrong) "(N)再复习一次" else "(N)复习错误单词"
                            OutlinedButton(onClick = {
                                reviewWrongWords()
                            }) { Text(text, color = textColor) }
                        }
                    }

                    if (isVocabularyFinished) {
                        Spacer(Modifier.width(15.dp))
                        OutlinedButton(onClick = {
                            resetIndex(true)
                        }) {
                            Text("随机重置词库", color = textColor)
                        }
                    }

                }

            }
        }


    }
}
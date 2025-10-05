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

package ui.dialog

import theme.LocalCtrl
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isMacOS
import ui.window.windowBackgroundFlashingOnCloseFixHack

@Composable
fun ShortcutKeyDialog(close: () -> Unit) {
    DialogWindow(
        title = "快捷键",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(940.dp, 700.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(Modifier.fillMaxSize()) {
                val plus = if (isMacOS()) "" else "+"
                val ctrl = LocalCtrl.current
                val shift = if (isMacOS()) "⇧" else "Shift"
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,top = 16.dp,bottom = 10.dp)
                    ) {
                        Text("激活复制", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("如果想复制正在抄写的字幕或文本可以先抄写到要复制的词，然后使用")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $shift $plus ← ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  选择要复制的单词\n或者使用快捷键")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus B ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  激活复制功能，激活后，不用先抄写就可以自由的复制，可用用 $ctrl $plus A 全选。")
                            }

                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
                    ) {
                        Text("切换单词", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("切换到下一个单词用")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  Enter 或 ${if(isMacOS()) "→" else "PgDn"} ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  切换到上一个单词用")
                            }
                            //**左箭头：** ←
                            //**右箭头：** →
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  ${if(isMacOS()) "←" else "PgUp"} ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  在听写模式下，不能切换到上一个单词。")
                            }

                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
                    ) {
                        Text("切换光标", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("记忆单词界面的光标切换\n把光标从字幕切换到单词")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus $shift $plus A ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n向上移动光标")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus $shift $plus I ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n向下移动光标")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus $shift $plus K ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n")
                            }
                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
                    ) {
                        Text("播放字幕", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("在记忆单词界面播放字幕快捷键：\n播放第一条字幕")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus 1   ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n播放第二条字幕")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus 2  ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n播放第三条字幕")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus 3  ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。\n")
                            }
                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
                    ) {
                        Text("搜索      ", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("打开搜索")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus F ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  会优先搜索当前词库，如果当前词库没有查到，再搜索内置词典。")
                            }
                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(start = 16.dp,bottom = 10.dp)
                    ) {
                        Text("播放多行字幕  ", modifier = Modifier.padding(end = 20.dp))
                        val annotatedString = buildAnnotatedString {

                            val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("在字幕浏览器界面，如果要播放多行字幕，点击左边的数字就可以开启，点击 5 和 10 再点击左边的播放按钮，" +
                                        "就会从第5行开始播放，到第10行结束。快捷键 ")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl $plus N ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("。")
                            }

                        }
                        Text(annotatedString)
                    }
                }
                Divider(Modifier.padding(bottom = 10.dp))
            }
        }
    }
}
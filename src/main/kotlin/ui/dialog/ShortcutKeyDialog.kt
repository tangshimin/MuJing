package ui.dialog

import LocalCtrl
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isMacOS

@Composable
fun ShortcutKeyDialog(close: () -> Unit) {
    Dialog(
        title = "快捷键",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(940.dp, 700.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(Modifier.fillMaxSize()) {

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
                                append("  $shift + ← ")
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
                                append("  $ctrl + B ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  激活复制功能，激活后，不用先抄写就可以自由的复制，可用用 $ctrl + A 全选。")
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
                                append("  Enter 或 PgDn ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("  切换到上一个单词用")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  PgUp ")
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
                                append("  $ctrl + $shift + A ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("\n向上移动光标")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl + $shift + I ")
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                                append("\n向下移动光标")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary,
                                    background = background
                                )
                            ) {
                                append("  $ctrl + $shift + K ")
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
                                append("  $ctrl + F ")
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
                                append("  $ctrl + N ")
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
package player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URLEncoder
import java.nio.charset.Charset


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTextMenuProvider(content: @Composable () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    var shouldShowMenu by remember { mutableStateOf(false) }
    var clearSelectionKey by remember { mutableStateOf(0) }
    var savedSelectedText by remember { mutableStateOf("") }

    CompositionLocalProvider(
        LocalContextMenuRepresentation provides DarkDefaultContextMenuRepresentation,
        LocalTextContextMenu provides object : TextContextMenu {
            @Composable
            override fun Area(
                textManager: TextContextMenu.TextManager,
                state: ContextMenuState,
                content: @Composable () -> Unit
            ) {
                val selectedText = textManager.selectedText.text

                // 保存非空的选择文本
                LaunchedEffect(selectedText) {
                    if (selectedText.isNotEmpty()) {
                        savedSelectedText = selectedText
                    }
                }

                // 监听菜单显示条件
                LaunchedEffect(shouldShowMenu) {
                    if (shouldShowMenu ) {
                        kotlinx.coroutines.delay(50)
                        val menuRect = calculateMenuPosition( mousePosition)
                        state.status = ContextMenuState.Status.Open(rect = menuRect)
                    } else {
                        state.status = ContextMenuState.Status.Closed
                    }
                }

                LaunchedEffect(state.status){
                    if(state.status == ContextMenuState.Status.Closed) {
                        // 菜单关闭时清除选择
                        if (savedSelectedText.isNotEmpty()) {
                            clearSelectionKey++
                            shouldShowMenu = false
                            savedSelectedText = ""
                        }
                    }
                }

                ContextMenuArea(
                    items = {
                        if (savedSelectedText.isNotEmpty() && shouldShowMenu) {
                            val encoded = URLEncoder.encode(savedSelectedText, Charset.defaultCharset())
                            buildList {

                                add(ContextMenuItem("复制") {
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(selectedText), null)
                                    state.status = ContextMenuState.Status.Closed
                                })

                                add(ContextMenuItem("用百度翻译") {
                                    val encoded = URLEncoder.encode(savedSelectedText, Charset.defaultCharset()).replace("+", "%20")
                                    val containsChinese = savedSelectedText.any { it in '\u4e00'..'\u9fff' }
                                    val fromLang = if (containsChinese) "zh" else "auto"
                                    val toLang = if (containsChinese) "en" else "zh"
                                    uriHandler.openUri("https://fanyi.baidu.com/#$fromLang/$toLang/$encoded")
                                    state.status = ContextMenuState.Status.Closed
                                })

                                add(ContextMenuItem("用 Google 翻译") {
                                    uriHandler.openUri("https://translate.google.com/?sl=auto&tl=zh&text=$encoded")
                                    state.status = ContextMenuState.Status.Closed
                                })

                                add(ContextMenuItem("用 DeepL 翻译") {
                                    val encoded = URLEncoder.encode(savedSelectedText, Charset.defaultCharset()).replace("+", "%20")
                                    uriHandler.openUri("https://www.deepl.com/zh/translator#en/zh-hans/$encoded")
                                    state.status = ContextMenuState.Status.Closed
                                })

                                if (isMacOS()) {
                                    add(ContextMenuItem("系统词典查询") {
                                        openMacOSDictionary(savedSelectedText)
                                        state.status = ContextMenuState.Status.Closed
                                    })
                                }
                            }
                        } else {
                            emptyList()
                        }
                    },
                    state = state
                ) {
                    Box(
                        modifier = Modifier.pointerInput(clearSelectionKey) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.firstOrNull()?.let { change ->
                                        mousePosition = change.position
                                    }

                                    when {
                                        event.changes.any { it.pressed } -> {
                                            if (state.status is ContextMenuState.Status.Open) {
                                                // 如果菜单已打开，点击时不要立即关闭
                                            } else {
                                                state.status = ContextMenuState.Status.Closed
                                                shouldShowMenu = false
                                            }
                                        }
                                        event.changes.any { !it.pressed && it.previousPressed } -> {
                                            val wasClick = event.changes.any { change ->
                                                val dragDistance = (change.position - change.previousPosition).getDistance()
                                                dragDistance < 5f
                                            }

                                            if (wasClick) {
                                                if (selectedText.isNotEmpty()) {
                                                    // 当前有选择，点击空白区域显示菜单
                                                    shouldShowMenu = true
                                                } else if (savedSelectedText.isNotEmpty()) {
                                                    // 之前有选择但现在没有，显示菜单
                                                    shouldShowMenu = true
                                                } else {
                                                    shouldShowMenu = false
                                                }
                                            } else {
                                                // 拖拽操作，不显示菜单
                                                shouldShowMenu = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        key(clearSelectionKey) {
                            content()
                        }
                    }
                }
            }
        },
    ) {
        content()
    }
}
/**
 * 计算菜单显示位置
 */
@OptIn(ExperimentalFoundationApi::class)
private fun calculateMenuPosition(
    mousePosition: Offset
): Rect {
    // 现在有5个菜单项（macOS）或4个菜单项（其他平台）
    val itemCount = if (isMacOS()) 5 else 4
    val estimatedMenuHeight = itemCount * 52f + 16f // 每项大约52px + 内边距
    val offsetY = -estimatedMenuHeight - 10f

    return Rect(
        offset = Offset(mousePosition.x, mousePosition.y + offsetY),
        size = Size(0f, 0f)
    )
}

/**
 * 在 macOS 上打开系统词典查询指定单词
 * @param word 要查询的单词
 */
private fun openMacOSDictionary(word: String) {
    try {
        val process = ProcessBuilder("open", "dict://$word").start()
        // 可选：等待进程完成并检查结果
        process.waitFor()
    } catch (e: Exception) {
        println("无法打开 macOS 词典: ${e.message}")
    }
}
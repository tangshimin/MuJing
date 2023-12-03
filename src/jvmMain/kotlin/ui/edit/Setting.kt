package ui.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import state.getResourcesFile
import state.rememberAppState
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

/**
 *  显示和隐藏列,从编辑词库界面调用
 *  在 Swing 中使用 Compose 如果是 Dark 主题，会出现白色闪光，
 *  相关 Issue https://github.com/JetBrains/compose-multiplatform/issues/1800
 *  这里这么写是为了解决这个问题。
 */
@OptIn(ExperimentalSerializationApi::class)
fun settingWindow(
    close: () -> Unit,
    displayColumn: (String) -> Unit,
    hideColumn: (String) -> Unit,
) {
    val window = JFrame("设置")
    window.size = Dimension(210, 450)
    val iconFile = getResourcesFile("logo/logo.svg")
    val iconImages = FlatSVGUtils.createWindowIconImages(iconFile.toURI().toURL())
    window.iconImages = iconImages
    window.setLocationRelativeTo(null)
    window.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            close()
            window.dispose()
        }
    })

    val composePanel = ComposePanel()
    composePanel.setContent {
        Setting(
            parentWindow = window,
            close = {
                close()
                window.dispose()
            },
            appState = rememberAppState(),
            displayColumn = displayColumn,
            hideColumn = hideColumn
        )
    }

    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalSerializationApi::class)
@Composable
fun Setting(
    parentWindow: JFrame,
    close: () -> Unit,
    appState: AppState,
    displayColumn: (String) -> Unit,
    hideColumn: (String) -> Unit,
) {
    val windowState = rememberDialogState(
        size = DpSize(250.dp, 450.dp),
        position = WindowPosition(parentWindow.location.x.dp, parentWindow.location.y.dp)
    )

    MaterialTheme(colors = appState.colors) {
        Dialog(
            title = "显示和隐藏列",
            onCloseRequest = close,
            resizable = false,
            state = windowState,
        ) {

            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {

                val cellVisible = rememberCellVisibleState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.background)
                ) {

                    Divider()
                    val stateVertical = rememberScrollState(0)
                    Box(modifier = Modifier.fillMaxHeight()) {
                        Divider(Modifier.align(Alignment.TopCenter))
                        Box(
                            Modifier.fillMaxHeight()
                                .verticalScroll(stateVertical)
                        ) {
                            Column {
                                ListItem(
                                    text = { Text("中文释义", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.translationVisible,
                                            onCheckedChange = {
                                                cellVisible.translationVisible = it
                                                if (it) {
                                                    displayColumn("中文释义")
                                                } else {
                                                    hideColumn("中文释义")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英文释义", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.definitionVisible,
                                            onCheckedChange = {
                                                cellVisible.definitionVisible = it
                                                if (it) {
                                                    displayColumn("英文释义")
                                                } else {
                                                    hideColumn("英文释义")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英国音标", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.uKPhoneVisible,
                                            onCheckedChange = {
                                                cellVisible.uKPhoneVisible = it
                                                if (it) {
                                                    displayColumn("英国音标")
                                                } else {
                                                    hideColumn("英国音标")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("美国音标", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.usPhoneVisible,
                                            onCheckedChange = {
                                                cellVisible.usPhoneVisible = it
                                                if (it) {
                                                    displayColumn("美国音标")
                                                } else {
                                                    hideColumn("美国音标")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("词形变化", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.exchangeVisible,
                                            onCheckedChange = {
                                                cellVisible.exchangeVisible = it
                                                if (it) {
                                                    displayColumn("词形变化")
                                                } else {
                                                    hideColumn("词形变化")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("字幕", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.captionsVisible,
                                            onCheckedChange = {
                                                cellVisible.captionsVisible = it
                                                if (it) {
                                                    displayColumn("字幕")
                                                } else {
                                                    hideColumn("字幕")
                                                }
                                                cellVisible.saveCellVisibleState()
                                            },
                                        )
                                    }
                                )
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(stateVertical)
                        )
                    }
                }
            }


            LaunchedEffect(Unit) {
                window.isAlwaysOnTop = true
                parentWindow.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {
                        window.requestFocus()
                    }

                    override fun windowLostFocus(e: WindowEvent?) {}

                })
            }
            LaunchedEffect(windowState) {
                snapshotFlow { windowState.size }
                    .onEach {
                        // 同步窗口和对话框的大小
                        parentWindow.size = windowState.size.toAwtSize()
                    }
                    .launchIn(this)

                snapshotFlow { windowState.position }
                    .onEach {
                        // 同步窗口和对话框的位置
                        parentWindow.location = windowState.position.toPoint()
                    }
                    .launchIn(this)


            }
        }

    }
}

fun DpSize.toAwtSize(): Dimension {
    return Dimension(width.value.toInt(), height.value.toInt())
}

fun WindowPosition.toPoint(): Point {
    return Point(x.value.toInt(), y.value.toInt())
}
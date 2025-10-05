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

package ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.extras.FlatSVGUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import state.getResourcesFile
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.JFrame
import kotlin.concurrent.schedule

fun notification(
    text:String,
    close: () -> Unit,
    colors: Colors,
) {
    val window = JFrame("")
    window.size = Dimension(206, 86)
    window.isUndecorated = true
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
        NotifactionContent(
            text = text,
            parentWindow = window,
            close = {
                close()
                window.dispose()
            },
            colors = colors,
        )
    }

    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true
}

@Composable
fun NotifactionContent(
    parentWindow: JFrame,
    close: () -> Unit,
    text:String,
    colors: Colors,
){
    val windowState = rememberWindowState(
        size = DpSize(206.dp, 83.dp),
        position = WindowPosition(parentWindow.location.x.dp,parentWindow.location.y.dp)
    )
    MaterialTheme(colors = colors){
        Window(
            title = "",
            onCloseRequest = close,
            undecorated = true,
            transparent = true,
            state = windowState,
        ) {
            Surface(
                elevation = 4.dp,
                modifier = Modifier.fillMaxSize(),
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(20.dp)){
                    if(text == "保存成功" || text == "添加成功" || text == "导出成功"){
                        Icon(
                            imageVector = Icons.Outlined.TaskAlt,
                            contentDescription = "",
                            tint = Color.Green
                        )
                    }
                    Text(
                        text = text,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }

            }

            LaunchedEffect(Unit){
                window.isAlwaysOnTop = true
                Timer().schedule(2000){
                    close()
                    window.dispose()
                }
            }
            LaunchedEffect(windowState){
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
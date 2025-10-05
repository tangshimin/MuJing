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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isMacOS
import state.getAudioDirectory
import state.getResourcesFile
import state.getSettingsDirectory
import ui.window.windowBackgroundFlashingOnCloseFixHack
import java.awt.Desktop
import java.io.File


@Composable
fun SpecialDirectoryDialog(close: () -> Unit) {
    DialogWindow(
        title = "特殊文件夹",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(400.dp, 400.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getSettingsDirectory())
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("软件设置文件夹")
                }
                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getAudioDirectory())
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("单词发音文件夹")
                }

                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getResourcesFile("vocabulary"))
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("内置词库文件夹")
                }

                if (!isMacOS()) {
                    OutlinedButton(
                        onClick = {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.OPEN)
                            ) {
                                Desktop.getDesktop().open(File("."))
                            }
                        },
                        modifier = Modifier.width(140.dp)
                    ) {
                        Text("安装目录")
                    }
                }


            }
        }
    }
}
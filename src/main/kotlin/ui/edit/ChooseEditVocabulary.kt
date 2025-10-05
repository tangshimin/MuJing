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

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.RecentItem
import player.isMacOS
import ui.wordscreen.rememberWordState
import java.io.File
import javax.swing.JOptionPane

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseEditVocabulary(
    close: () -> Unit,
    recentList: List<RecentItem>,
    removeRecentItem:(RecentItem) -> Unit,
    openEditVocabulary: (String) -> Unit,
    colors: Colors,
) {

    Window(
        title = "选择要编辑词库",
        icon = painterResource("logo/logo.png"),
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
        ),
        onCloseRequest = close,
    ) {
        MaterialTheme(colors = colors) {
            Surface {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val wordState = rememberWordState()
                    if (recentList.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 10.dp)
                        ) { Text("最近词库") }
                        Box(
                            Modifier.fillMaxWidth().height(400.dp).padding(10.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                        ) {
                            val stateVertical = rememberScrollState(0)
                            Column(Modifier.verticalScroll(stateVertical)) {
                                if(wordState.vocabularyName.isNotEmpty()){
                                    val name = when (wordState.vocabularyName) {
                                        "FamiliarVocabulary" -> {
                                            "熟悉词库"
                                        }
                                        "HardVocabulary" -> {
                                            "困难词库"
                                        }
                                        else -> {
                                            wordState.vocabularyName
                                        }
                                    }
                                    ListItem(
                                        text = {
                                            Text(
                                                name,
                                                color = MaterialTheme.colors.onBackground
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            if(wordState.vocabularyPath.isNotEmpty()){
                                                openEditVocabulary(wordState.vocabularyPath)
                                            }
                                        },
                                        trailing = {
                                            Text("当前词库    ", color = MaterialTheme.colors.primary)
                                        }
                                    )
                                }

                                recentList.forEach { item ->
                                    if (wordState.vocabularyName != item.name) {
                                        ListItem(
                                            text = { Text(item.name, color = MaterialTheme.colors.onBackground) },
                                            modifier = Modifier.clickable {

                                                val recentFile = File(item.path)
                                                if (recentFile.exists()) {
                                                    openEditVocabulary(item.path)
                                                } else {
                                                    removeRecentItem(item)
                                                    JOptionPane.showMessageDialog(window, "文件地址错误：\n${item.path}")
                                                }



                                            }
                                        )
                                    }

                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical)
                            )
                        }
                    }
                    var showFilePicker by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showFilePicker = true }) {
                        Text(
                            text = "选择词库",
                        )
                    }


                    val extensions = if (isMacOS()) listOf("public.json") else listOf("json")
                    FilePicker(
                        show = showFilePicker,
                        fileExtensions = extensions,
                        initialDirectory = ""
                    ) { pickFile ->
                        if (pickFile != null) {
                            openEditVocabulary(pickFile.path)
                        }

                        showFilePicker = false
                    }
                }

            }
        }


    }
}


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

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lyric.FileManager
import lyric.SongLyric
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.createDragAndDropTarget
import util.shouldStartDragAndDrop
import java.io.File
import javax.swing.JOptionPane

@Composable
fun LyricToSubtitlesDialog(
    close: () -> Unit,
){
    DialogWindow(
        title = "歌词转字幕",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 600.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        val scope = rememberCoroutineScope()
        var inputPath by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("") }
        var convertEnable by remember { mutableStateOf(false) }
        var saveEnable by remember { mutableStateOf(false) }
        var successful by remember { mutableStateOf(false) }
        val songLyric by remember{ mutableStateOf(SongLyric()) }

        val setFile:(File) -> Unit = { file ->
            inputPath = file.absolutePath
            fileName = file.nameWithoutExtension
            convertEnable = true
            saveEnable = false
            successful = false
            songLyric.song.clear()
        }


        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                scope.launch {
                    val file = files.first()
                    if (file.extension == "lrc") {
                        setFile(file)
                    } else {
                        JOptionPane.showMessageDialog(window, "格式不支持")
                    }

                }
            }
        }

        /** 打开文件对话框 */
        val singleLauncher = rememberFilePickerLauncher(
            title = "选择 LRC 格式的歌词",
            type = FileKitType.File(extensions = listOf("lrc")),
            mode = FileKitMode.Single,
        ) { file ->
            scope.launch(Dispatchers.IO){
                file?.let {
                    setFile(file.file)
                }
            }

        }

        val convert: () -> Unit = {
            scope.launch {
                val file = File(inputPath)
                if (file.exists()) {
                    FileManager.readLRC(songLyric, file.absolutePath)
                    saveEnable = true
                    successful = true
                }
            }
        }


        /** 保存文件对话框 */
        val fileSaver = rememberFileSaverLauncher(
            dialogSettings = FileKitDialogSettings.createDefault()
        ) {  platformFile ->
            scope.launch(Dispatchers.IO){
                platformFile?.let{
                    val fileToSave = platformFile.file
                    FileManager.writeSRT(songLyric, fileToSave.absolutePath)
                    songLyric.song.clear()
                    fileName = ""
                    convertEnable = false
                    saveEnable = false
                    successful = false
                }
            }

        }


        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            modifier = Modifier.dragAndDropTarget(
                shouldStartDragAndDrop =shouldStartDragAndDrop,
                target = dropTarget
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()){
                Text("把 LRC 格式的歌词转换成 SRT 格式的字幕")
                Spacer(Modifier.height(20.dp))
                if (fileName.isNotEmpty()) {
                    val bottom = if (successful) 5.dp else 20.dp
                    Text(fileName, modifier = Modifier.padding(bottom = bottom))
                }
                if (successful) {
                    Text(
                        text = "转换成功",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.Center){
                    OutlinedButton(onClick = {singleLauncher.launch()}){
                        Text("打开")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {convert()},
                        enabled = convertEnable
                    ){
                        Text("转换")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {fileSaver.launch(fileName,"srt")},
                        enabled = saveEnable
                    ){
                        Text("保存")
                    }

                }

            }
        }
    }
}
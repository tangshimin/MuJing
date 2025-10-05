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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.createDragAndDropTarget
import util.shouldStartDragAndDrop
import java.io.File
import javax.swing.JOptionPane

@Composable
fun TextFormatDialog(
    close: () -> Unit,
) {
    DialogWindow(
        title = "文本格式化",
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
        var path by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("") }
        var formatEnable by remember { mutableStateOf(false) }
        var saveEnable by remember { mutableStateOf(false) }
        val saveList = remember { mutableStateListOf<String>() }
        var successful by remember { mutableStateOf(false) }
        var limit by remember { mutableStateOf(75) }

        val setFile: (File) -> Unit = { file ->
            path = file.absolutePath
            fileName = file.nameWithoutExtension
            formatEnable = true
            saveEnable = false
            successful = false
            saveList.clear()
        }


        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                scope.launch {
                    val file = files.first()
                    if (file.extension == "txt") {
                        setFile(file)
                    } else {
                        JOptionPane.showMessageDialog(window, "格式不支持")
                    }

                }
            }
        }


        /** 打开文件对话框 */
        val singleLauncher = rememberFilePickerLauncher(
            title = "选择文本",
            type = FileKitType.File(extensions = listOf("txt")),
            mode = FileKitMode.Single,
        ) { file ->
            scope.launch(Dispatchers.IO){
                file?.let {
                    setFile(file.file)
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
                    File(fileToSave.absolutePath).bufferedWriter().use { writer ->
                        saveList.forEach { line ->
                            writer.write(line)
                            writer.newLine()
                        }
                        saveList.clear()
                    }

                    fileName = ""
                    formatEnable = false
                    saveEnable = false
                    successful = false
                }
            }

        }

        val formatText: () -> Unit = {
            scope.launch {
                val file = File(path)
                if (file.exists()) {
                    File(path).useLines { lines ->
                        lines.forEach { line ->
                            if (line.length > limit) {
                                val subLines = split(line,limit)
                                saveList.addAll(subLines)

                            } else {
                                saveList.add(line)
                            }
                        }
                    }

                    saveEnable = true
                    successful = true
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
            Column {
                Divider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "每行最多 75 个字母，如果超出 75 个字母抄写时不能完成显示。\n" +
                                "格式化就是重新调整每一行的字母数量，使其不超过 75 个字母。\n"+
                                "如果要抄写的文本是汉语、日语或韩语，每行的限制不能超过 37 个文字。",
                        modifier = Modifier.padding(top = 20.dp, bottom = 13.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Box (Modifier.padding(bottom = 150.dp)){
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .width(87.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(text = "$limit")
                            Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(87.dp)
                                .height(60.dp)
                        ) {
                            val text = if(limit == 75) "37" else "75"
                            DropdownMenuItem(
                                onClick = {
                                    scope.launch {
                                        limit = if(text=="37") 37 else 75
                                        expanded = false
                                    }
                                },
                                modifier = Modifier.width(87.dp).height(40.dp)
                            ) {

                                Text(text)
                            }

                        }

                    }

                    if (fileName.isNotEmpty()) {
                        val bottom = if (successful) 20.dp else 40.dp
                        Text(fileName, modifier = Modifier.padding(bottom = bottom))
                    }
                    if (successful) {
                        Text(
                            text = "格式化成功",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Row {
                        OutlinedButton(onClick = { singleLauncher.launch() }) {
                            Text("打开")
                        }
                        Spacer(Modifier.width(28.dp))
                        OutlinedButton(
                            onClick = { formatText() },
                            enabled = formatEnable,
                        ) {
                            Text("格式化")
                        }
                        Spacer(Modifier.width(28.dp))
                        OutlinedButton(
                            onClick = {fileSaver.launch("$fileName-formatted","txt") },
                            enabled = saveEnable
                        ) {
                            Text("保存")
                        }
                        Spacer(Modifier.width(28.dp))
                        OutlinedButton(
                            onClick = { close() },
                        ) {
                            Text(if (fileName.isEmpty()) "关闭" else "取消")                        }
                    }
                }
            }

        }

    }
}

private fun split(line: String,limit:Int): List<String> {
    val lines = mutableListOf<String>()
    if (line.length > limit) {
        val end = limit - 1
        val subLine = line.substring(0..end)

        // 为了避免把最后一个英语单词切割了，一行只保留 0 到最后一个空格，
        // 中文不用空格区分单词，所以为 0.
        val index = if(limit == 75){
            subLine.reversed().indexOf(" ")
        }else {
            0
        }

        if (index > 0) {
            val last = subLine.lastIndexOf(" ")
            lines.add(subLine.substring(0, last + 1))
        } else {
            lines.add(subLine)
        }

        val start = limit - index
        val remainString = line.substring(start)
        val subLines = split(remainString,limit)
        lines.addAll(subLines)
    } else {
        lines.add(line)
    }
    return lines
}

/**
 * 这个对话框在抄写文本界面，如果打开的文本文件中有一行超过了 75 个字母时调用
 */
@Composable
fun FormatDialog(
    close: () -> Unit,
    changeTextPath: (File) -> Unit,
    row: Int,
    formatPath: String,
) {
    DialogWindow(
        title = "消息",
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(470.dp, 200.dp)
        ),
    ) {
        val scope = rememberCoroutineScope()
        val formatFile = File(formatPath)
        val fileSaver = rememberFileSaverLauncher(
            dialogSettings = FileKitDialogSettings.createDefault()
        ) {  platformFile ->
            scope.launch(Dispatchers.IO){
                platformFile?.let{
                    val fileToSave = platformFile.file
                    val saveList = mutableListOf<String>()
                    if (formatFile.exists()) {
                        formatFile.useLines { lines ->
                            lines.forEach { line ->
                                if (line.length > 75) {
                                    val subLines = split(line, 75)
                                    saveList.addAll(subLines)
                                } else {
                                    saveList.add(line)
                                }
                            }
                        }
                    }

                    File(fileToSave.absolutePath).bufferedWriter().use { writer ->
                        saveList.forEach { line ->
                            writer.write(line)
                            writer.newLine()
                        }
                        saveList.clear()
                    }

                    changeTextPath(fileToSave)
                    close()
                }
            }

        }


        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column {
                Divider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(start = 10.dp,end = 10.dp)
                ) {
                    Text("文本的第 $row 行超过了 75 个字母，抄写时不能完全显示。\n" +
                            "需要格式化文本才能正确显示，格式化不会覆盖原文件，会生成一个新文件。")
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(onClick = {
                           fileSaver.launch("${formatFile.nameWithoutExtension}-formatted","txt")
                        }) {
                            Text("格式化文本")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { close() }) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}
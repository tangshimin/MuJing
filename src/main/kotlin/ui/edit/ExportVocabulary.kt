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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import data.MutableVocabulary
import data.Vocabulary
import data.VocabularyType
import data.Word
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import state.getResourcesFile
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFrame
import javax.swing.JOptionPane

/**
 * 编辑词库界面的导出词库功能，
 * 写成这样是为了避免打开或关闭对话框时的白色闪光问题
 */
fun exportVocabulary(
    vocabulary: Vocabulary,
    vocabularyPath:String,
    close: () -> Unit,
    colors: Colors,
) {
    val window = JFrame("导出词库")
    window.size = Dimension(650, 650)
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
        ExportVocabulary(
            vocabulary = MutableVocabulary(vocabulary),
            vocabularyPath = vocabularyPath,
            parentWindow = window,
            colors = colors,
            close = {
                close()
                window.dispose()
            },
        )
    }
    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalSerializationApi::class)
@Composable
fun ExportVocabulary(
    vocabulary: MutableVocabulary,
    vocabularyPath:String,
    parentWindow:JFrame,
    close: () -> Unit,
    colors: Colors,
) {
    val windowState = rememberDialogState(
        size = DpSize(650.dp, 650.dp),
        position = WindowPosition(parentWindow.location.x.dp,parentWindow.location.y.dp)
    )


    MaterialTheme(colors = colors) {
        DialogWindow(
            title = "导出词库",
            onCloseRequest = close,
            state = windowState,
        ) {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                window.isAlwaysOnTop = true


                val fileName by remember { mutableStateOf(File(vocabularyPath).nameWithoutExtension) }
                val cellVisible by remember{ mutableStateOf(CellVisibleState(CellVisible())) }
                var format by remember { mutableStateOf("xlsx") }
                val scope = rememberCoroutineScope()
                /** 选择文件对话框 */
                val fileSaver = rememberFileSaverLauncher(
                    dialogSettings = FileKitDialogSettings.createDefault()
                ) {  platformFile ->
                    scope.launch (Dispatchers.IO){
                        platformFile?.let{
                            val fileToSave = platformFile.file
                            if(format == "xlsx"){
                                val workbook = createWorkbook(vocabulary.wordList,vocabulary.type, cellVisible)
                                try{
                                    FileOutputStream(fileToSave).use { out ->
                                        workbook.write(out)
                                    }
                                    notification(
                                        text = "导出成功",
                                        close = {  },
                                        colors = colors
                                    )
                                }catch (e:Exception){
                                    e.printStackTrace()
                                    JOptionPane.showMessageDialog(window,"导出失败,错误信息：\n${e.message}")
                                }
                            }else{
                                val text = createText(vocabulary.wordList)
                                try{
                                    fileToSave.writeText(text)
                                    notification(
                                        text = "导出成功",
                                        close = {  },
                                        colors = colors
                                    )
                                }catch (e:Exception){
                                    e.printStackTrace()
                                    JOptionPane.showMessageDialog(window,"导出失败,错误信息：\n${e.message}")
                                }


                            }
                            close()
                        }
                    }

                }

                Box(Modifier.fillMaxSize()){
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxSize().padding(top = 20.dp)){
                        var show by remember { mutableStateOf(false) }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(250.dp)
                        ){
                            var expanded by remember { mutableStateOf(false) }

                            Text("格式: ")
                            Box{
                                val width = 195.dp
                                val text = when (format) {
                                    "txt" -> "文本文件(*.txt)"
                                    else -> "Excel 工作簿(*.xlsx)"
                                }
                                OutlinedButton(onClick = {expanded = true}){
                                    Text(text)
                                    Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.width(width)
                                        .height(140.dp)
                                ) {
                                    val selectedColor = if(MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                                    val backgroundColor = Color.Transparent
                                    DropdownMenuItem(
                                        onClick = {
                                            format = "txt"
                                            expanded = false

                                        },
                                        modifier = Modifier.width(width).height(40.dp)
                                            .background( if(format == "txt")selectedColor else backgroundColor )
                                    ) {
                                        Text("文本文件(*.txt)")
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            format = "xlsx"
                                            expanded = false
                                        },
                                        modifier = Modifier.width(width).height(40.dp)
                                            .background(if(format == "xlsx") selectedColor else backgroundColor)
                                    ) {
                                        Text("Excel 工作簿(*.xlsx)")

                                    }
                                }
                            }
                        }

                        if(format == "xlsx"){
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                ListItem(
                                    text = { Text("选择要导出的属性", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.onBackground
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .width(250.dp)
                                        .clickable {show = !show }
                                        .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))),
                                )
                            }
                        }else{
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text("文本文件只能导出单词，不能导出其他属性。", color = MaterialTheme.colors.onBackground)
                            }
                        }

                        if(show){
                            Column (
                                modifier = Modifier.width(250.dp)
                            ){
                                ListItem(
                                    text = { Text("单词", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { },
                                    trailing = {}
                                )
                                Divider()
                                ListItem(
                                    text = { Text("中文释义", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.translationVisible = !cellVisible.translationVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.translationVisible,
                                            onCheckedChange = {cellVisible.translationVisible = !cellVisible.translationVisible},
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英文释义", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.definitionVisible = !cellVisible.definitionVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.definitionVisible,
                                            onCheckedChange = {cellVisible.definitionVisible = !cellVisible.definitionVisible },
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英国音标", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.uKPhoneVisible = !cellVisible.uKPhoneVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.uKPhoneVisible,
                                            onCheckedChange = { cellVisible.uKPhoneVisible = !cellVisible.uKPhoneVisible },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("美国音标", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.usPhoneVisible = !cellVisible.usPhoneVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.usPhoneVisible,
                                            onCheckedChange = { cellVisible.usPhoneVisible = !cellVisible.usPhoneVisible },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("词形变化", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.exchangeVisible = !cellVisible.exchangeVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.exchangeVisible,
                                            onCheckedChange = {cellVisible.exchangeVisible = !cellVisible.exchangeVisible},
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("例句", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.sentencesVisible = !cellVisible.sentencesVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.sentencesVisible,
                                            onCheckedChange = {cellVisible.sentencesVisible = !cellVisible.sentencesVisible},
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("字幕", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.captionsVisible = !cellVisible.captionsVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.captionsVisible,
                                            onCheckedChange = {cellVisible.captionsVisible = !cellVisible.captionsVisible},
                                        )
                                    }
                                )
                            }
                        }

                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                    ){
                        OutlinedButton(onClick = {
                            fileSaver.launch(fileName, format)
                        }) {
                            Text("导出")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(onClick = {close()}) {
                            Text("取消")
                        }
                    }
                }

            }

            LaunchedEffect(Unit){
                parentWindow.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {
                        window.requestFocus()
                    }
                    override fun windowLostFocus(e: WindowEvent?) {}

                })
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

/**
 * 保存词库为其他格式，xlsx 或 txt
 * 在生成词库界面，点击保存为其他格式后显示
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalSerializationApi::class)
@Composable
fun SaveOtherVocabulary(
    vocabularyType: VocabularyType,
    wordList:List<Word>,
    fileName:String,
    close: () -> Unit,
    colors: Colors,
){
    MaterialTheme(colors = colors) {
        val windowState = rememberDialogState(
            size = DpSize(650.dp, 650.dp),
            position = WindowPosition(Alignment.Center)
        )
        DialogWindow(
            title = "保存词库",
            onCloseRequest = close,
            state = windowState,
            alwaysOnTop = true
        ) {
            val scope = rememberCoroutineScope()
            var format by remember { mutableStateOf("xlsx") }
            val cellVisible by remember{ mutableStateOf(CellVisibleState(CellVisible())) }
            val launcher = rememberFileSaverLauncher(
                dialogSettings = FileKitDialogSettings.createDefault()
            ) {  platformFile ->
                scope.launch(Dispatchers.IO){
                    platformFile?.let{
                        val fileToSave = platformFile.file
                        if(format == "xlsx"){
                            val workbook = createWorkbook(wordList, vocabularyType,cellVisible)
                            try{
                                if (fileToSave.exists()) {
                                    // 是-0,否-1，取消-2
                                    val answer =
                                        JOptionPane.showConfirmDialog(window, "${fileToSave.nameWithoutExtension}.${format} 已存在。\n要替换它吗？")
                                    if (answer == 0) {
                                        FileOutputStream(fileToSave).use { out ->
                                            workbook.write(out)
                                        }
                                    }
                                } else {
                                    FileOutputStream(fileToSave).use { out ->
                                        workbook.write(out)
                                    }
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window,"保存失败,错误信息：\n${e.message}")
                            }



                        }else{
                            val text = createText(wordList)
                            try{
                                if (fileToSave.exists()) {
                                    // 是-0,否-1，取消-2
                                    val answer =
                                        JOptionPane.showConfirmDialog(window, "${fileToSave.nameWithoutExtension}.${format} 已存在。\n要替换它吗？")
                                    if (answer == 0) {
                                        fileToSave.writeText(text)
                                    }
                                } else {
                                    fileToSave.writeText(text)
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window,"保存失败,错误信息：\n${e.message}")
                            }

                        }
                        close()
                    }
                }

            }

            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                Box(Modifier.fillMaxSize()){
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxSize().padding(top = 20.dp)){


                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(250.dp)
                        ){
                            var expanded by remember { mutableStateOf(false) }

                            Text("格式: ")
                            Box{
                                val width = 195.dp
                                val text = when (format) {
                                    "txt" -> "文本文件(*.txt)"
                                    else -> "Excel 工作簿(*.xlsx)"
                                }
                                OutlinedButton(onClick = {expanded = true}){
                                    Text(text)
                                    Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.width(width)
                                        .height(140.dp)
                                ) {
                                    val selectedColor = if(MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                                    val backgroundColor = Color.Transparent
                                    DropdownMenuItem(
                                        onClick = {
                                            format = "txt"
                                            expanded = false

                                        },
                                        modifier = Modifier.width(width).height(40.dp)
                                            .background( if(format == "txt")selectedColor else backgroundColor )
                                    ) {
                                        Text("文本文件(*.txt)")
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            format = "xlsx"
                                            expanded = false
                                        },
                                        modifier = Modifier.width(width).height(40.dp)
                                            .background(if(format == "xlsx") selectedColor else backgroundColor)
                                    ) {
                                        Text("Excel 工作簿(*.xlsx)")

                                    }
                                }
                            }
                        }

                        var show by remember { mutableStateOf(false) }
                        if(format == "xlsx"){
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                ListItem(
                                    text = { Text("选择要保存的属性", color = MaterialTheme.colors.onBackground) },
                                    trailing = {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.onBackground
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(top = 10.dp)
                                        .width(250.dp)
                                        .clickable {show = !show }
                                        .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))),
                                )
                            }
                        }else{
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text("文本文件只能保存单词，不能保存其他属性。", color = MaterialTheme.colors.onBackground)
                            }
                        }

                        if(show){
                            Column (
                                modifier = Modifier.width(250.dp)
                            ){
                                ListItem(
                                    text = { Text("单词", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { },
                                    trailing = {}
                                )
                                Divider()
                                ListItem(
                                    text = { Text("中文释义", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.translationVisible = !cellVisible.translationVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.translationVisible,
                                            onCheckedChange = {cellVisible.translationVisible = !cellVisible.translationVisible},
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英文释义", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.definitionVisible = !cellVisible.definitionVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.definitionVisible,
                                            onCheckedChange = {cellVisible.definitionVisible = !cellVisible.definitionVisible },
                                        )
                                    }
                                )

                                ListItem(
                                    text = { Text("英国音标", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.uKPhoneVisible = !cellVisible.uKPhoneVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.uKPhoneVisible,
                                            onCheckedChange = { cellVisible.uKPhoneVisible = !cellVisible.uKPhoneVisible },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("美国音标", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.usPhoneVisible = !cellVisible.usPhoneVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.usPhoneVisible,
                                            onCheckedChange = { cellVisible.usPhoneVisible = !cellVisible.usPhoneVisible },
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("词形变化", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.exchangeVisible = !cellVisible.exchangeVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.exchangeVisible,
                                            onCheckedChange = {cellVisible.exchangeVisible = !cellVisible.exchangeVisible},
                                        )
                                    }
                                )
                                ListItem(
                                    text = { Text("字幕", color = MaterialTheme.colors.onBackground) },
                                    modifier = Modifier.clickable { cellVisible.captionsVisible = !cellVisible.captionsVisible },
                                    trailing = {
                                        Switch(
                                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                                            checked = cellVisible.captionsVisible,
                                            onCheckedChange = {cellVisible.captionsVisible = !cellVisible.captionsVisible},
                                        )
                                    }
                                )
                            }
                        }

                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                    ){
                        OutlinedButton(onClick = {
                            launcher.launch(fileName, format)
                        }) {
                            Text("保存")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(onClick = {close()}) {
                            Text("取消")
                        }
                    }
                }
            }

        }
    }

}

fun createWorkbook(
    wordList: List<Word>,
    vocabularyType: VocabularyType,
    cellVisible: CellVisibleState
): Workbook {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("单词列表")
    val headerRow = sheet.createRow(0)
    var cellIndex = 0

    var captionsIndex = -1

    // Header Style
    val headerStyle = workbook.createCellStyle()
    val headerFont = workbook.createFont()
    headerFont.bold = true
    // 背景颜色
    headerStyle.setFillForegroundColor(IndexedColors.GREEN.index)
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    // 字体颜色
    headerFont.color = IndexedColors.WHITE.getIndex()

    headerStyle.setFont(headerFont)
    headerStyle.wrapText = true

    // Header
    val wordCell = headerRow.createCell(cellIndex++)
    wordCell.cellStyle = headerStyle
    wordCell.setCellValue("单词")

    if(cellVisible.translationVisible){
        // 40 个字符宽度
        sheet.setColumnWidth(cellIndex, 256 * 40)
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("中文释义")
    }
    if(cellVisible.definitionVisible){
        // 50 个字符宽度
        sheet.setColumnWidth(cellIndex, 256 * 50)
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("英文释义")
    }
    if(cellVisible.uKPhoneVisible){

        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("英国音标")
    }
    if(cellVisible.usPhoneVisible){
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("美国音标")
    }
    if(cellVisible.exchangeVisible){
        // 40 个字符宽度
        sheet.setColumnWidth(cellIndex, 256 * 40)
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("词形变化")
    }
    if(cellVisible.sentencesVisible){
        // 50 个字符宽度
        sheet.setColumnWidth(cellIndex, 256 * 50)
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("例句")
    }
    if(cellVisible.captionsVisible){
        captionsIndex = cellIndex
        val cell = headerRow.createCell(cellIndex++)
        cell.cellStyle = headerStyle
        cell.setCellValue("字幕")
    }

    val bodyStyle = workbook.createCellStyle()
    bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER)
    bodyStyle.wrapText = true
    var rowIndex = 1
    wordList.forEach { word ->
        val row = sheet.createRow(rowIndex++)
        cellIndex = 0

        val valueCell = row.createCell(cellIndex++)
        valueCell.setCellValue(word.value)
        valueCell.cellStyle = bodyStyle

        if(cellVisible.translationVisible){
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(word.translation)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.definitionVisible){
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(word.definition)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.uKPhoneVisible){
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(word.ukphone)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.usPhoneVisible){
            val cell =  row.createCell(cellIndex++)
            cell.setCellValue(word.usphone)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.exchangeVisible){
            val exchange = displayExchange(word.exchange)
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(exchange)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.sentencesVisible){
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(word.pos)
            cell.cellStyle = bodyStyle
        }
        if(cellVisible.captionsVisible){
            val captions = displayCaptions(word, vocabularyType)
            val cell = row.createCell(cellIndex++)
            cell.setCellValue(captions)
            cell.cellStyle = bodyStyle
        }
    }

    // 自动调整列宽
    for (columnIndex in 0 until cellIndex) {
        // 中文释义、英文释义和词形变化和例句的列宽度不自动调整
        if (columnIndex != 1 && columnIndex != 2 && columnIndex != 5 && columnIndex != 6) {
            sheet.autoSizeColumn(columnIndex)
        }
        // 字幕列宽度最小为 10 个字符宽度
        if(cellVisible.captionsVisible){
           sheet.getColumnWidth(captionsIndex).let {
               if(it < 256 * 10){
                   sheet.setColumnWidth(captionsIndex, 256 * 10)
               }
           }
        }
    }

    return workbook
}

fun createText(wordList: List<Word>): String {
    val sb = StringBuilder()
    wordList.forEach { word ->
        sb.append(word.value)
        sb.append("\n")
    }
    return sb.toString()
}
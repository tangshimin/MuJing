package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.Dictionary
import data.Vocabulary
import data.VocabularyType
import data.saveVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.window.windowBackgroundFlashingOnCloseFixHack
import java.io.File
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView

@Composable
fun WordFrequencyDialog(
    futureFileChooser: FutureTask<JFileChooser>,
    saveToRecentList: (String, String) -> Unit,
    close: () -> Unit
){
    DialogWindow(
        title = "根据词频生成词库",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 600.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            var selectState by remember { mutableStateOf("Idle") }

            /** 新词库 */
            /** 新词库 */
            var newVocabulary by remember { mutableStateOf<Vocabulary?>(null) }
            var start by remember { mutableStateOf(1) }
            var end by remember { mutableStateOf(1000) }
            var saveEnable by remember { mutableStateOf(false) }
            var waiting by remember { mutableStateOf(false) }
            var done by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val generate: () -> Unit = {
                scope.launch (Dispatchers.Default){
                    done = false
                    waiting = true

                    if(start >= end){
                        JOptionPane.showMessageDialog(window,"开始值必须小于结束值")
                    }

                    val list = if (selectState == "BNC") {
                        Dictionary.queryByBncRange(start,end)
                    } else {
                        Dictionary.queryByFrqRange(start,end)
                    }

                    newVocabulary = Vocabulary(
                        name = "",
                        type = VocabularyType.DOCUMENT,
                        language = "english",
                        size = list.size,
                        relateVideoPath = "",
                        subtitlesTrackId = 0,
                        wordList = list.toMutableList()
                    )
                    done = true
                    waiting = false
                    saveEnable = true
                }
            }

            val save:() -> Unit = {
                scope.launch (Dispatchers.IO){
                    val fileChooser = futureFileChooser.get()
                    fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                    fileChooser.dialogTitle = "保存词库"
                    val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                    fileChooser.selectedFile = File("$myDocuments${File.separator}$selectState list $start - $end.json")
                    val userSelection = fileChooser.showSaveDialog(window)
                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        val fileToSave = fileChooser.selectedFile
                        if (newVocabulary != null) {
                            newVocabulary!!.name = fileToSave.nameWithoutExtension
                            try{
                                saveVocabulary(newVocabulary!!, fileToSave.absolutePath)
                                saveToRecentList(fileToSave.nameWithoutExtension, fileToSave.absolutePath)
                            }catch(e:Exception){
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window, "保存词库失败,错误信息：\n${e.message}")
                            }

                        }
                        newVocabulary = null
                        fileChooser.selectedFile = null
                        close()
                    }

                }
            }
            Box(Modifier.fillMaxSize()){
                if(selectState == "Idle"){
                    val bncText =
                        "英国国家语料库（简称 BNC）是目前网络上可直接使用的较大语料库之一，也是世界上最具代表性的当代英语语料库之一，它收集了来自各种来源的书面和口头语言样本，旨在代表 20 世纪后期英国英语的广泛横截面，包括各种文体和领域。"

                    val cocaText =
                        "美国当代英语语料库（COCA）是目前最大的免费英语语料库之一，它用计算机统计英语单词词频并排序，截至2017年已包含5.6亿字的文本。该语料库从1990年至2017年以每年2000万字的速度更新扩充，以保证语料库内容的时效性。其语料均衡分布在口语、小说、杂志、报纸以及学术文章五类文体中，被认为是用来观察美国英语当前发展变化的合适语料库，也是广大英语爱好者的学习宝库。"

                    Column (Modifier.align(Alignment.Center).padding(start = 5.dp,end = 5.dp)){

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                            OutlinedButton(onClick = {selectState = "BNC"}){
                                Text("BNC", modifier = Modifier.padding(end = 1.dp))
                                Text("   词频")
                            }
                        }
                        Text(text = bncText, modifier = Modifier.padding(top = 10.dp))
                        Spacer(Modifier.height(50.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                            OutlinedButton(onClick = {selectState = "COCA"}){
                                Text("COCA 词频")
                            }
                        }

                        Text(text = cocaText, modifier = Modifier.padding(top = 10.dp))
                    }
                }else{


                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center)){
                        if(selectState == "BNC") {
                            Text("提取 BNC 词频 ")
                        }else{
                            Text("提取 COCA 词频 ")
                        }


                        BasicTextField(
                            value = "$start",
                            onValueChange = {
                                val input = it.toIntOrNull()
                                if (input != null) {
                                    if(input>30000){
                                        start = 30000
                                        JOptionPane.showMessageDialog(window,"不能超过最大值 30000")
                                    }else if(input < 0){
                                        JOptionPane.showMessageDialog(window,"不能为负数")
                                        start = 0
                                    }else {
                                        start = input
                                    }
                                }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                lineHeight = LocalTextStyle.current.lineHeight,
                                fontSize = LocalTextStyle.current.fontSize,
                                color = MaterialTheme.colors.onBackground
                            ),
                            decorationBox = { innerTextField ->
                                Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .focusable()
                                .width(50.dp)
                                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                        )
                        Text(" - ")
                        BasicTextField(
                            value = "$end",
                            onValueChange = {
                                val input = it.toIntOrNull()
                                if (input != null) {
                                    if(input>30000){
                                        end = 30000
                                        JOptionPane.showMessageDialog(window,"不能超过最大值 30000")
                                    }else if(input < 0){
                                        JOptionPane.showMessageDialog(window,"不能为负数")
                                        end = 0
                                    }else{
                                        end = input
                                    }
                                }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                lineHeight = LocalTextStyle.current.lineHeight,
                                fontSize = LocalTextStyle.current.fontSize,
                                color = MaterialTheme.colors.onBackground
                            ),
                            decorationBox = { innerTextField ->
                                Row(Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)) {
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .focusable()
                                .width(50.dp)
                                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.6f)))
                        )
                        Text(" 词生成词库")
                        Spacer(Modifier.width(20.dp))

                        OutlinedButton(onClick = { generate() }){
                            Text("开始")
                        }
                        Spacer(Modifier.width(20.dp))
                        OutlinedButton(onClick = {save()}, enabled = saveEnable){
                            Text("保存")
                        }
                    }
                }

                if(waiting){
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = 120.dp))
                }
                if(done){
                    Icon(Icons.Filled.Done,
                        contentDescription = "",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp).width(40.dp).height(40.dp))
                }
            }
        }


    }
}


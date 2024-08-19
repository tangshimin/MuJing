package ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import util.createTransferHandler
import data.*
import ui.window.windowBackgroundFlashingOnCloseFixHack
import java.io.File
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

/**
 * 导入词库到熟悉词库
 */
@Composable
fun FamiliarDialog(
    futureFileChooser: FutureTask<JFileChooser>,
    close: () -> Unit
){
    Dialog(
        title = "导入词库到熟悉词库",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 600.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        var importing by remember { mutableStateOf(false) }
        var processingFile by remember { mutableStateOf("") }

        /** 熟悉词库 */
        val familiarVocabulary = loadMutableVocabularyByName("FamiliarVocabulary")

        /** 导入词库 */
        val import:(List<File>) -> Unit = {files ->
            if(files.size>100){
                JOptionPane.showMessageDialog(null,"一次最多导入 100 个词库")
            }else{
                files.forEach { file ->
                    processingFile = file.nameWithoutExtension
                    val vocabulary = loadVocabulary(file.absolutePath)
                    vocabulary.wordList.forEach { word ->
                        val index = familiarVocabulary.wordList.indexOf(word)
                        // wordList 没有这个单词
                        if (index == -1) {
                            // 如果是视频词库或字幕词库，需要把字幕变成外部字幕
                            if (word.captions.isNotEmpty()) {
                                word.captions.forEach { caption ->
                                    // 创建一条外部字幕
                                    val externalCaption = ExternalCaption(
                                        relateVideoPath = vocabulary.relateVideoPath,
                                        subtitlesTrackId = vocabulary.subtitlesTrackId,
                                        subtitlesName = vocabulary.name,
                                        start = caption.start,
                                        end = caption.end,
                                        content = caption.content
                                    )
                                    word.externalCaptions.add(externalCaption)
                                }
                                word.captions.clear()
                            }
                            familiarVocabulary.wordList.add(word)
                            // wordList 有这个单词
                        } else {
                            val oldWord = familiarVocabulary.wordList[index]
                            // 如果单词有外部字幕，同时已经加入到列表的单词的外部字幕没有超过3个就导入
                            if (word.externalCaptions.isNotEmpty()) {
                                word.externalCaptions.forEach { externalCaption ->
                                    if (oldWord.externalCaptions.size < 3) {
                                        oldWord.externalCaptions.add(externalCaption)
                                    }
                                }
                                // 如果单词是视频或字幕词库中的单词
                            } else if (word.captions.isNotEmpty()) {
                                word.captions.forEach { caption ->
                                    // 创建一条外部字幕
                                    val externalCaption = ExternalCaption(
                                        relateVideoPath = vocabulary.relateVideoPath,
                                        subtitlesTrackId = vocabulary.subtitlesTrackId,
                                        subtitlesName = vocabulary.name,
                                        start = caption.start,
                                        end = caption.end,
                                        content = caption.content
                                    )
                                    if (oldWord.externalCaptions.size < 3) {
                                        oldWord.externalCaptions.add(externalCaption)
                                    }
                                }
                            }

                        }
                    }
                }

                familiarVocabulary.size = familiarVocabulary.wordList.size
                val familiarFile = getFamiliarVocabularyFile()
                try{
                    saveVocabulary(familiarVocabulary.serializeVocabulary, familiarFile.absolutePath)
                    importing = false
                }catch(e:Exception){
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(null,"保存词库失败,错误信息：\n${e.message}")
                    close()
                }

            }
        }

        /** 打开文件对话框 */
        val openFileChooser:()-> Unit = {
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogTitle = "选择词库"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.isAcceptAllFileFilterUsed = false
                fileChooser.isMultiSelectionEnabled = true
                val fileFilter = FileNameExtensionFilter("词库", "json")
                fileChooser.addChoosableFileFilter(fileFilter)
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val files = fileChooser.selectedFiles.toList()
                    importing = true
                    import(files)
                }
                fileChooser.selectedFile = null
                fileChooser.isMultiSelectionEnabled = false
                fileChooser.removeChoosableFileFilter(fileFilter)
        }

        /**  处理拖放文件的函数 */
        val transferHandler = createTransferHandler(
            singleFile = false,
            showWrongMessage = { message ->
                JOptionPane.showMessageDialog(window, message)
            },
            parseImportFile = { files ->
                    importing = true
                    import(files)
            }
        )
        window.transferHandler = transferHandler

        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Box{
                Divider(Modifier.align(Alignment.TopCenter))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ){
                    Text("熟悉词库现在有 ${familiarVocabulary.wordList.size} 个单词",
                    modifier = Modifier.padding(bottom = if(importing) 90.dp else 20.dp))
                    if (importing) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Text(text = "正在读取 $processingFile")
                        }
                    }
                    Row{
                        OutlinedButton(
                            onClick = { openFileChooser() },
                        ){
                            Text("导入")
                        }
                        Spacer(Modifier.width(20.dp))
                        OutlinedButton(
                            onClick = { close() },
                        ){
                            Text("关闭")
                        }
                    }
                }
                if (importing) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = 70.dp))
                }
            }
        }
    }
}
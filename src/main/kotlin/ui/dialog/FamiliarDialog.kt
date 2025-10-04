package ui.dialog

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import data.*
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.createDragAndDropTarget
import util.shouldStartDragAndDrop
import java.io.File
import javax.swing.JOptionPane

/**
 * 导入词库到熟悉词库
 */
@Composable
fun FamiliarDialog(
    close: () -> Unit
){
    DialogWindow(
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
        val scope = rememberCoroutineScope()

        /** 熟悉词库 */
        val familiarVocabulary = loadMutableVocabularyByName("FamiliarVocabulary")


        /** 导入词库 */
        val import:(List<File>) -> Unit = { files ->
            if(files.size>100){
                JOptionPane.showMessageDialog(window,"一次最多导入 100 个词库")
            }else{
                files.forEach { file ->
                    processingFile = file.nameWithoutExtension
                    if(file.extension != "json") {
                        JOptionPane.showMessageDialog(window,"文件 ${file.name} 不是一个有效的词库文件")
                        return@forEach
                    }
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
                    JOptionPane.showMessageDialog(window,"保存词库失败,错误信息：\n${e.message}")
                    close()
                }

            }
        }
        /** 文件选择器 */
        val multipleLauncher = rememberFilePickerLauncher(
            title = "选择词库",
            type = FileKitType.File(extensions = listOf("json")),
            mode = FileKitMode.Multiple(maxItems = 50)
        ) { files ->
            scope.launch(Dispatchers.IO){
                files?.let{
                    importing = true
                    val files = files.map { it.file }
                    scope.launch(Dispatchers.Default) {
                        import(files)
                    }
                }
            }

        }

        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                importing = true
                import(files)
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
                            onClick = {multipleLauncher.launch() },
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
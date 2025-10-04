package ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import data.*
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
import java.util.*
import javax.swing.JOptionPane
import kotlin.concurrent.schedule

@Composable
fun MergeVocabularyDialog(
    saveToRecentList: (String, String) -> Unit,
    close: () -> Unit
) {
    DialogWindow(
        title = "合并词库",
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

        /** 是否启用合并按钮 */
        var mergeEnabled by remember { mutableStateOf(false) }

        /** 选择的词库列表 */
        val selectedFileList = remember { mutableStateListOf<File>() }

        /** 合并后的新词库 */
        var newVocabulary by remember { mutableStateOf<Vocabulary?>(null) }

        /** 合并词库的数量限制 */
        var isOutOfRange by remember { mutableStateOf(false) }

        /** 单词的总数 */
        var size by remember { mutableStateOf(0) }

        /** 正在读取的词库名称 */
        var fileName by remember { mutableStateOf("") }

        /** 更新单词的总数的回调函数 */
        val updateSize: (Int) -> Unit = {
            size = it
        }


        /** 更新正在读取的词库名称的回调函数 */
        val updateFileName: (String) -> Unit = {
            fileName = it
        }



        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                scope.launch {
                    for (file in files) {
                        if (file.extension == "json") {
                            if (selectedFileList.size + 1 < 101) {
                                if (!selectedFileList.contains(file)) {
                                    selectedFileList.add(file)
                                }
                            } else {
                                isOutOfRange = true
                            }

                        } else {
                            JOptionPane.showMessageDialog(window, "词库的格式不对")
                        }
                    }
                    mergeEnabled = selectedFileList.size > 1
                    // 导入了新的词库，重置总计单词的数量。
                    updateSize(0)
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

            Box {
                Divider(Modifier.align(Alignment.TopCenter))
                var merging by remember { mutableStateOf(false) }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!merging) {
                        val height = if (selectedFileList.size < 9) (selectedFileList.size * 48 + 10).dp else 450.dp
                        Box(Modifier.fillMaxWidth().height(height)) {
                            val stateVertical = rememberScrollState(0)
                            Column(
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .fillMaxSize()
                                    .verticalScroll(stateVertical)
                            ) {
                                selectedFileList.forEach { file ->
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = file.nameWithoutExtension,
                                            modifier = Modifier.width(420.dp)
                                        )
                                        IconButton(onClick = {
                                            updateSize(0)
                                            selectedFileList.remove(file)
                                            mergeEnabled = selectedFileList.size > 1
                                        }) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                    }
                                    Divider(Modifier.width(468.dp))
                                }
                            }
                            if (selectedFileList.size >= 9) {
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(stateVertical)
                                )
                            }

                        }

                    }
                    if (isOutOfRange) {
                        Text(
                            text = "词库数量不能超过100个",
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        // 10 秒后消失
                        Timer("重置状态", false).schedule(10000) {
                            isOutOfRange = false
                        }
                    }

                    if (merging) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "正在读取 $fileName")
                        }
                    }

                    if (size > 0) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp, top = 10.dp)
                        ) {
                            Text(text = "总计：")
                            Text(text = "$size", color = MaterialTheme.colors.primary)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        /** 文件选择器 */
                        val multipleLauncher = rememberFilePickerLauncher(
                            title = "选择词库",
                            type = FileKitType.File(extensions = listOf("json")),
                            mode = FileKitMode.Multiple(maxItems = 50)
                        ) { files ->
                            scope.launch(Dispatchers.Default) {
                                files?.let{
                                    files.forEach {
                                        if (selectedFileList.size + 1 < 100) {
                                            val file = it.file
                                            if (!selectedFileList.contains(file)) {
                                                selectedFileList.add(file)
                                            }
                                        } else {
                                            isOutOfRange = true
                                        }
                                    }

                                    mergeEnabled = selectedFileList.size > 1
                                    if (files.isNotEmpty()) {
                                        updateSize(0)
                                    }
                                }
                            }

                        }

                        OutlinedButton(onClick = {
                            multipleLauncher.launch()
                        }, modifier = Modifier.padding(end = 10.dp)) {
                            Text("添加词库")
                        }

                        OutlinedButton(
                            enabled = mergeEnabled,
                            onClick = {
                                scope.launch (Dispatchers.Default){
                                    merging = true
                                    newVocabulary = Vocabulary(
                                        name = "",
                                        type = VocabularyType.DOCUMENT,
                                        language = "english",
                                        size = 0,
                                        relateVideoPath = "",
                                        subtitlesTrackId = 0,
                                        wordList = mutableListOf()
                                    )
                                    val wordList = mutableListOf<Word>()
                                    selectedFileList.forEach { file ->
                                        updateFileName(file.nameWithoutExtension)
                                        val vocabulary = loadVocabulary(file.absolutePath)
                                        vocabulary.wordList.forEach { word ->
                                            val index = wordList.indexOf(word)
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
                                                wordList.add(word)
                                                // wordList 有这个单词
                                            } else {
                                                val oldWord = wordList[index]
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
                                        updateSize(wordList.size)
                                    }
                                    newVocabulary!!.wordList = wordList
                                    newVocabulary!!.size = wordList.size
                                    merging = false
                                    mergeEnabled = false
                                }
                            }, modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text("合并词库")
                        }
                        val fileSaver = rememberFileSaverLauncher(
                            dialogSettings = FileKitDialogSettings.createDefault()
                        ) {  platformFile ->
                            scope.launch(Dispatchers.IO){
                                platformFile?.let{
                                    val fileToSave = platformFile.file
                                    if (newVocabulary != null) {
                                        newVocabulary!!.name = fileToSave.nameWithoutExtension
                                        try{
                                            saveVocabulary(newVocabulary!!, fileToSave.absolutePath)
                                            saveToRecentList(fileToSave.nameWithoutExtension, fileToSave.absolutePath)
                                        }catch(e: Exception){
                                            e.printStackTrace()
                                            JOptionPane.showMessageDialog(window, "保存词库失败,错误信息：\n${e.message}")
                                        }

                                    }
                                    newVocabulary = null
                                    close()
                                }
                            }

                        }

                        OutlinedButton(
                            enabled = !merging && size > 0,
                            onClick = {
                                fileSaver.launch("newVocabulary - ${newVocabulary?.size}","json")
                            }) {
                            Text("保存词库")
                        }
                    }

                }

                if (merging) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = 120.dp))
                }
            }

        }
    }
}
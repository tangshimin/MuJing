package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.*
import kotlinx.coroutines.launch
import state.getResourcesFile
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MatchVocabularyDialog(
    futureFileChooser: FutureTask<JFileChooser>,
    close: () -> Unit
) {
    Dialog(
        title = "匹配词库",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 600.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {

            Box {
                /** 协程构建器 */
                val scope = rememberCoroutineScope()
                Divider(Modifier.align(Alignment.TopCenter))
                var matching by remember { mutableStateOf(false) }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    var matchLemma by remember{ mutableStateOf(true) }
                    var baseline :Vocabulary? by remember { mutableStateOf(null) }
                    var comparison  :Vocabulary? by remember { mutableStateOf(null) }
                    var comparisonDir by remember{ mutableStateOf("") }
                    var result  :Vocabulary? by remember { mutableStateOf(null) }
                    val match:() -> Unit = {
                        if(comparison != null && baseline != null){
                            matching = true
                            scope.launch {
                                matching = false
                                result = Vocabulary(
                                    name = "",
                                    type = comparison!!.type,
                                    language = comparison!!.language,
                                    size = 0,
                                    relateVideoPath = comparison!!.relateVideoPath,
                                    subtitlesTrackId = comparison!!.subtitlesTrackId,
                                    wordList = mutableListOf()
                                )

                                if(matchLemma){
                                    val baselineLemma = mutableSetOf<String>()
                                    val comparisonLemma = mutableMapOf<String,MutableList<Word>>()
                                    baseline!!.wordList.forEach { word ->
                                        val lemma = getWordLemma(word)
                                        baselineLemma.add(lemma)
                                    }
                                    comparison!!.wordList.forEach { word ->
                                        val lemma = getWordLemma(word)
                                        val wordList = comparisonLemma[lemma]
                                        if(wordList == null){
                                            comparisonLemma[lemma] = mutableListOf(word)
                                        }else{
                                            wordList.add(word)
                                        }
                                    }

                                    comparisonLemma.keys.forEach { lemma ->
                                        if (baselineLemma.contains(lemma)) {
                                            val list = comparisonLemma[lemma]
                                            if (list != null) {
                                                result!!.wordList.addAll(list)
                                            }
                                        }
                                    }

                                }else{
                                    comparison!!.wordList.forEach { word ->
                                        if (baseline!!.wordList.contains(word)) {
                                            result!!.wordList.add(word)
                                        }
                                    }
                                }
                                result!!.size = result!!.wordList.size
                            }

                        }

                    }

                    val save:() -> Unit = {
                        Thread {
                            val fileChooser = futureFileChooser.get()
                            fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                            fileChooser.dialogTitle = "保存词库"
                            val fileFilter = FileNameExtensionFilter("词库", "json")
                            fileChooser.addChoosableFileFilter(fileFilter)
                            fileChooser.selectedFile = File("$comparisonDir${File.separator}${comparison!!.name + " - " + result!!.size}.json")
                            val userSelection = fileChooser.showSaveDialog(window)
                            if (userSelection == JFileChooser.APPROVE_OPTION) {
                                val selectedFile = fileChooser.selectedFile
                                val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                                val savePath = Paths.get(selectedFile.absolutePath)
                                if(savePath.startsWith(vocabularyDirPath)){
                                    JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，生成的词库会被删除")
                                }else{
                                    result!!.name = selectedFile.nameWithoutExtension
                                    saveVocabulary(result!!, selectedFile.absolutePath)
                                }

                            }
                            fileChooser.removeChoosableFileFilter(fileFilter)
                        }.start()
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RectangleShape
                                ) {
                                    Row(modifier = Modifier.padding(10.dp)){
                                        Text(text = "比如四、六级词库" )

                                    }

                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset(0.dp,(-2).dp)
                            )
                        ) {
                            Box{
                            var isHover by remember { mutableStateOf(false) }
                            Column (
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier.width(165.dp).height(200.dp)
                                    .clickable {
                                        Thread {
                                            val fileChooser = futureFileChooser.get()
                                            fileChooser.dialogTitle = "选择基准词库"
                                            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                            fileChooser.isAcceptAllFileFilterUsed = false
                                            val fileFilter = FileNameExtensionFilter("词库", "json")
                                            fileChooser.addChoosableFileFilter(fileFilter)
                                            fileChooser.selectedFile = null
                                            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                val path = fileChooser.selectedFile.absolutePath
                                                baseline = loadVocabulary(path)

                                            }
                                            fileChooser.selectedFile = null

                                            fileChooser.removeChoosableFileFilter(fileFilter)
                                        }.start()
                                    }
                                    .onPointerEvent(PointerEventType.Enter){isHover = true}
                                    .onPointerEvent(PointerEventType.Exit){isHover = false}
                                    .border(border = BorderStroke(if(isHover) 2.dp else 1.dp,
                                        if(isHover) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))


                            ){
                                Text(text = "常用词库",modifier = Modifier.padding(top = 10.dp))
                                Column ( horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()){
                                    if(baseline==null || baseline!!.name.isEmpty()){
                                        val tint = if(isHover){
                                            MaterialTheme.colors.primary
                                        }else if(MaterialTheme.colors.isLight){
                                            Color.DarkGray
                                        }else {
                                            MaterialTheme.colors.onBackground
                                        }
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Localized description",
                                            tint =tint,
                                        )
                                    }else{
                                        Text(baseline!!.name)
                                        Text(text = "数量：${baseline!!.wordList.size}")
                                    }
                                }



                            }
                        }
                        }


                        Spacer(Modifier.width(30.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RectangleShape
                                ) {
                                    Row(modifier = Modifier.padding(10.dp)){
                                        Text(text = "要提取四、六级单词的词库" )
                                    }

                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset(0.dp,(-2).dp)
                            )
                        ) {
                            Box{
                                var isHover by remember { mutableStateOf(false) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.width(165.dp).height(200.dp)
                                        .clickable {
                                            Thread {
                                                val fileChooser = futureFileChooser.get()
                                                fileChooser.dialogTitle = "选择对比词库"
                                                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                                fileChooser.isAcceptAllFileFilterUsed = false
                                                val fileFilter = FileNameExtensionFilter("词库", "json")
                                                fileChooser.addChoosableFileFilter(fileFilter)
                                                fileChooser.selectedFile = null
                                                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                    val path = fileChooser.selectedFile.absolutePath
                                                    comparisonDir = fileChooser.selectedFile.parent
                                                    comparison = loadVocabulary(path)

                                                }
                                                fileChooser.selectedFile = null

                                                fileChooser.removeChoosableFileFilter(fileFilter)
                                            }.start()
                                        }
                                        .onPointerEvent(PointerEventType.Enter){isHover = true}
                                        .onPointerEvent(PointerEventType.Exit){isHover = false}
                                        .border(border = BorderStroke(if(isHover) 2.dp else 1.dp,
                                            if(isHover) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                                ){
                                    Text(text = "对比词库",modifier = Modifier.padding(top = 10.dp))
                                    Column ( horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()){
                                        if(comparison == null){
                                            val tint = if(isHover){
                                                MaterialTheme.colors.primary
                                            }else if(MaterialTheme.colors.isLight){
                                                Color.DarkGray
                                            }else {
                                                MaterialTheme.colors.onBackground
                                            }
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "Localized description",
                                                tint =tint,
                                            )

                                        }else{
                                            Text(comparison!!.name)
                                            Text(text = "数量：${comparison!!.wordList.size}")
                                        }
                                    }
                                }

                            }
                        }

                    }

                    if(result!=null){
                        Text(text = "共匹配到${result!!.size}个单词",modifier = Modifier.padding(top = 30.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp,bottom = 5.dp)
                    ) {
                        Text("匹配原型词")
                        Checkbox(
                            checked =matchLemma,
                            onCheckedChange = {matchLemma = it  }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp)
                    ) {
                        OutlinedButton(
                            enabled = true,
                            onClick = {match()}) {
                            Text("匹配")
                        }
                        OutlinedButton(
                            enabled = result != null && result!!.size>0,
                            modifier = Modifier.padding(start = 10.dp),
                            onClick = {save()}) {
                            Text("保存")
                        }
                    }

                }

                if (matching) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).padding(bottom = 120.dp))
                }
            }

        }
    }
}
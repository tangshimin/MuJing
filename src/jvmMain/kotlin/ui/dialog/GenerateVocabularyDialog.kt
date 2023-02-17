package ui.dialog

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.ResourceLoader
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.matthewn4444.ebml.subtitles.SSASubtitles
import data.*
import data.Dictionary
import data.VocabularyType.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import opennlp.tools.langdetect.LanguageDetector
import opennlp.tools.langdetect.LanguageDetectorME
import opennlp.tools.langdetect.LanguageDetectorModel
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.mozilla.universalchardet.UniversalDetector
import player.isWindows
import player.parseTrackList
import state.AppState
import state.composeAppResource
import state.getResourcesFile
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import ui.createTransferHandler
import ui.dialog.FilterState.*
import java.awt.BorderLayout
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.FutureTask
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath


/**
 * 生成词库
 * @param state 应用程序状态
 * @param title 标题
 * @param type 词库类型
 */
@OptIn(
    ExperimentalSerializationApi::class
)
@ExperimentalComposeUiApi
@Composable
fun GenerateVocabularyDialog(
    state: AppState,
    title: String,
    type: VocabularyType
) {
    val windowWidth = if(type == MKV) 1320.dp else 1285.dp
    Dialog(
        title = title,
        onCloseRequest = {
            onCloseRequest(state, title)
        },
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(windowWidth, 850.dp)
        ),
    ) {
        val scope = rememberCoroutineScope()

        val fileFilter = when (title) {
            "过滤词库" -> FileNameExtensionFilter(
                "词库",
                "json",
            )
            "用文档生成词库" -> FileNameExtensionFilter(
                "支持的文件扩展(*.pdf、*.txt)",
                "pdf",
                "txt",
            )
            "用字幕生成词库" -> FileNameExtensionFilter(
                "SRT 格式的字幕文件",
                "srt",
            )

            "用 MKV 视频生成词库" -> FileNameExtensionFilter(
                "mkv 格式的视频文件",
                "mkv",
            )
            else -> null
        }

        /**
         * 选择的文件列表,用于批量处理
         */
        val selectedFileList  = remember { mutableStateListOf<File>() }

        /**
         * 显示任务列表,用于拖拽多个文件进入窗口时，自动为真，
         * 分析完字幕，自动为假，单击【任务列表】显示-隐藏。
         */
        var showTaskList by remember { mutableStateOf(false) }

        /**
         * 任务列表的状态
         */
        val tasksState  = remember{ mutableStateMapOf<File,Boolean>() }

        /**
         * 正在处理的文件
         */
        var currentTask by remember{mutableStateOf<File?>(null)}

        /**
         * 批处理时的错误信息
         */
        val errorMessages = remember{ mutableStateMapOf<File,String>() }

        /**
         * 批量处理时，选择文件，用于删除
         */
        var selectable by remember{ mutableStateOf(false) }

        /**
         * 勾选的文件，用于批量删除
         */
        val checkedFileMap = remember{ mutableStateMapOf<File,Boolean>() }

        /**
         * 是否全选
         */
        var isSelectedAll by remember{ mutableStateOf(false) }

        /**
         * 选择的文件的绝对路径
         */
        var selectedFilePath by remember { mutableStateOf("") }

        /**
         * 选择的字幕名称
         */
        var selectedSubtitlesName by remember { mutableStateOf("    ") }

        /**
         * 预览的单词
         */
        val previewList = remember { mutableStateListOf<Word>() }

        /**
         * 用字幕生成单词 -> 相关视频的地址
         */
        var relateVideoPath by remember { mutableStateOf("") }

        /**
         * 字幕的轨道 ID
         */
        var selectedTrackId by remember { mutableStateOf(0) }

        /**
         * 需要过滤的词库的类型
         */
        var filteringType by remember { mutableStateOf(DOCUMENT) }

        /**
         * 字幕轨道列表
         */
        val trackList = remember { mutableStateListOf<Pair<Int, String>>() }

        /**
         * 这个 filterState 有四个状态：Idle、Parse、Filtering、End
         */
        var filterState by remember { mutableStateOf(Idle) }

        /**
         * 摘要词库
         */
        val summaryVocabulary = loadSummaryVocabulary()

        /**
         * 分析之后得到的单词
         */
        val documentWords = remember { mutableStateListOf<Word>() }

        /**
         * 用于过滤的词库列表
         */
        val vocabularyFilterList = remember { mutableStateListOf<File>() }

        /**
         * 是否过滤所有的数字
         */
        var numberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频前 1000 的单词，最常见的 1000 词
         */
        var bncNumberFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频前 1000 的单词，最常见的 1000 词
         */
        var frqNumFilter by remember{ mutableStateOf(false) }

        /**
         * 是否过滤 BNC 词频为0的单词
         */
        var bncZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否过滤 COCA 词频为0的单词
         */
        var frqZeroFilter by remember { mutableStateOf(false) }

        /**
         * 是否替换索引派生词
         */
        var replaceToLemma by remember { mutableStateOf(false) }

        /** 熟悉词库 */
        val familiarVocabulary = remember{loadMutableVocabularyByName("FamiliarVocabulary")}

        /** 用鼠标删除的单词列表 */
        val removedWords = remember{ mutableStateListOf<Word>() }

        var progressText by remember { mutableStateOf("") }

        var loading by remember { mutableStateOf(false) }

        var sort by remember { mutableStateOf("appearance") }

        /** 文件选择器的标题 */
        val chooseText = when (title) {
            "过滤词库" -> "选择词库"
            "用文档生成词库" -> "选择文档"
            "用字幕生成词库" -> "选择字幕"
            "用 MKV 视频生成词库" -> "选择 MKV 文件"
            else -> ""
        }

        /** 拖放的文件和文件选择器选择的文件都使用这个函数处理 */
        val parseImportFile: (List<File>) -> Unit = { files ->
            scope.launch {
                Thread {
                    if (files.size == 1) {
                        val file = files.first()
                        when (file.extension) {
                            "pdf", "txt" -> {
                                if (type == DOCUMENT) {
                                    selectedFilePath = file.absolutePath
                                    selectedSubtitlesName = "    "
                                } else {
                                    JOptionPane.showMessageDialog(
                                        window,
                                        "如果你想用 ${file.nameWithoutExtension} 文档生成词库，\n请重新选择：词库 -> 用文档生成词库，再拖放文件到这里。"
                                    )
                                }
                            }

                            "srt" -> {
                                if (type == SUBTITLES) {
                                    selectedFilePath = file.absolutePath
                                    selectedSubtitlesName = "    "
                                } else {
                                    JOptionPane.showMessageDialog(
                                        window,
                                        "如果你想用 ${file.nameWithoutExtension} 字幕生成词库，\n请重新选择：词库 -> 用字幕生成词库，再拖放文件到这里。"
                                    )
                                }
                            }

                            "mp4" -> {
                                if (type == SUBTITLES) {
                                    relateVideoPath = file.absolutePath
                                } else {
                                    JOptionPane.showMessageDialog(window, "格式错误")
                                }
                            }

                            "mkv" -> {
                                when (type) {
                                    MKV -> {
                                        // 第一次拖放
                                        if (selectedFilePath.isEmpty() && selectedFileList.isEmpty()) {
                                            loading = true

                                            parseTrackList(
                                                state.videoPlayerComponent,
                                                window,
                                                state.videoPlayerWindow,
                                                file.absolutePath,
                                                setTrackList = {
                                                    trackList.clear()
                                                    trackList.addAll(it)
                                                    if (it.isNotEmpty()) {
                                                        selectedFilePath = file.absolutePath
                                                        relateVideoPath = file.absolutePath
                                                        selectedSubtitlesName = "    "
                                                    }
                                                }
                                            )
                                            loading = false
                                        } else { // 窗口已经有文件了
                                            // 如果之前有一个 MKV 视频,把之前的视频加入到 selectedFileList
                                            if (selectedFilePath.isNotEmpty() && selectedFileList.isEmpty()) {
                                                val f = File(selectedFilePath)
                                                selectedFileList.add(f)
                                                trackList.clear()
                                                selectedSubtitlesName = "    "
                                                selectedFilePath = ""
                                                relateVideoPath = ""
                                            }
                                            selectedFileList.add(file)
                                            selectedFileList.sortBy { it.nameWithoutExtension }
                                            if (selectedFileList.isNotEmpty()) showTaskList = true
                                        }

                                    }

                                    SUBTITLES -> {
                                        relateVideoPath = file.absolutePath
                                    }

                                    else -> {
                                        JOptionPane.showMessageDialog(
                                            window,
                                            "如果你想用 ${file.nameWithoutExtension} 视频生成词库，\n请重新选择：词库 -> 用 MKV 视频生成词库，再拖放文件到这里。"
                                        )
                                    }
                                }
                            }

                            "json" -> {
                                if (title == "过滤词库") {
                                    selectedFilePath = file.absolutePath
                                }
                            }

                            else -> {
                                JOptionPane.showMessageDialog(window, "格式不支持")
                            }
                        }


                    } else if (files.size == 2 && type == SUBTITLES) {
                        val first = files.first()
                        val last = files.last()
                        if (first.extension == "srt" && (last.extension == "mp4" || last.extension == "mkv")) {
                            selectedFilePath = first.absolutePath
                            selectedSubtitlesName = "    "
                            relateVideoPath = last.absolutePath
                            selectedTrackId = -1
                        } else if (last.extension == "srt" && (first.extension == "mp4" || first.extension == "mkv")) {
                            selectedFilePath = last.absolutePath
                            selectedSubtitlesName = "    "
                            relateVideoPath = first.absolutePath
                            selectedTrackId = -1
                        } else if (first.extension == "srt" && last.extension == "srt") {
                            JOptionPane.showMessageDialog(
                                window,
                                "不能接收两个 srt 字幕文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                            )
                        } else if (first.extension == "mp4" && last.extension == "mp4") {
                            JOptionPane.showMessageDialog(
                                window,
                                "不能接收两个 mp4 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                            )
                        } else if (first.extension == "mkv" && last.extension == "mkv") {
                            JOptionPane.showMessageDialog(
                                window,
                                "不能接收两个 mkv 视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                            )
                        } else if (first.extension == "mkv" && last.extension == "mp4") {
                            JOptionPane.showMessageDialog(
                                window,
                                "不能接收两个视频文件，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                            )
                        } else {
                            JOptionPane.showMessageDialog(
                                window,
                                "格式错误，\n需要一个字幕(srt)文件和一个视频（mp4、mkv）文件"
                            )
                        }

                    } else if (files.size in 2..100 && type == MKV) {
                        var extensionWrong = ""
                        files.forEach { file ->
                            if (file.extension == "mkv") {
                                selectedFileList.add(file)
                            } else {
                                extensionWrong = extensionWrong + file.name + "\n"
                            }
                        }
                        selectedFileList.sortBy { it.nameWithoutExtension }
                        if (selectedFileList.isNotEmpty()) showTaskList = true
                        if (extensionWrong.isNotEmpty()) {
                            JOptionPane.showMessageDialog(window, "以下文件不是 mkv 格式\n$extensionWrong")
                        }

                    } else if (files.size > 100 && type == MKV) {
                        JOptionPane.showMessageDialog(window, "批量处理最多不能超过 100 个文件")
                    } else {
                        JOptionPane.showMessageDialog(window, "文件不能超过两个")
                    }
                }.start()
            }
        }

        /** 打开文件时调用的函数 */
        val openFile:() -> Unit = {
            Thread {
                val fileChooser = state.futureFileChooser.get()
                fileChooser.dialogTitle = chooseText
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.isAcceptAllFileFilterUsed = false
                fileChooser.isMultiSelectionEnabled = true
                fileChooser.addChoosableFileFilter(fileFilter)
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val files = fileChooser.selectedFiles.toList()
                    parseImportFile(files)
                }
                fileChooser.selectedFiles = null
                fileChooser.isMultiSelectionEnabled = true
                fileChooser.removeChoosableFileFilter(fileFilter)
            }.start()

        }

        //设置窗口的拖放处理函数
        LaunchedEffect(Unit){
            val transferHandler = createTransferHandler(
                singleFile = false,
                showWrongMessage = { message ->
                    JOptionPane.showMessageDialog(window, message)
                },
                parseImportFile = { parseImportFile(it) }
            )
            window.transferHandler = transferHandler
        }

        /** 全选 */
        val selectAll:() -> Unit = {
            if(!isSelectedAll){
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = true
                }
                isSelectedAll = true
            }else{
                selectedFileList.forEach { file ->
                    checkedFileMap[file] = false
                }
                isSelectedAll = false
            }
        }

        /** 删除 */
        val delete:() -> Unit = {
            val list = mutableListOf<File>()
            checkedFileMap.forEach { (file, checked) ->
                if(checked){
                    list.add(file)
                }
            }
            list.forEach { file ->
                checkedFileMap.remove(file)
                selectedFileList.remove(file)
                tasksState.remove(file)
                errorMessages.remove(file)
                checkedFileMap[file] = false
                if (currentTask == file) {
                    currentTask = null
                }
            }
        }

        /** 打开关联的视频 */
        val openRelateVideo:() -> Unit = {
            val fileChooser = state.futureFileChooser.get()
            fileChooser.dialogTitle = "选择视频"
            fileChooser.isAcceptAllFileFilterUsed = true
            fileChooser.selectedFile = null
            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                relateVideoPath = file.absolutePath
                fileChooser.selectedFile = File("")
            }
            fileChooser.selectedFile = null
            fileChooser.removeChoosableFileFilter(fileFilter)
        }

        /** 分析文件里的单词 */
        val analysis : (String,Int) -> Unit = { pathName,trackId ->
            filterState = Parsing
            vocabularyFilterList.clear()
            documentWords.clear()
            Thread {
                val words = when (type) {
                    DOCUMENT -> {
                        if (title == "过滤词库") {
                            val vocabulary = loadVocabulary(pathName)
                            filteringType = vocabulary.type
                            relateVideoPath = vocabulary.relateVideoPath
                            selectedTrackId = vocabulary.subtitlesTrackId
                            vocabulary.wordList
                        } else {
                            readDocument(
                                pathName = pathName,
                                setProgressText = { progressText = it })
                        }

                    }

                    SUBTITLES -> {
                        readSRT(pathName = pathName, setProgressText = { progressText = it })
                    }

                    MKV -> {
                        readMKV(
                            pathName = pathName,
                            trackId = trackId,
                            setProgressText = { progressText = it },
                        )
                    }
                }
                words.forEach { word -> documentWords.add(word) }
                filterState =
                    if (numberFilter || bncNumberFilter || frqNumFilter ||
                        bncZeroFilter || frqZeroFilter || replaceToLemma ||
                        vocabularyFilterList.isNotEmpty()
                    ) {
                        Filtering
                    } else {
                        End
                    }
                if (filterState == End) {
                    previewList.clear()
                    previewList.addAll(documentWords)
                }
            }.start()
        }

        /** 批量分析文件 MKV 视频里的单词 */
        val batchAnalysis:(String) -> Unit = {language ->

            vocabularyFilterList.clear()
            documentWords.clear()
            Thread {
                val words = batchReadMKV(
                    language = language,
                    selectedFileList = selectedFileList,
                    setCurrentTask = { currentTask = it },
                    setErrorMessages = {
                        errorMessages.clear()
                        errorMessages.putAll(it)
                    },
                    updateTaskState = {
                        tasksState[it.first] = it.second
                    }
                )
                if (words.isNotEmpty()) {
                    showTaskList = false
                    selectable = false
                }
                words.forEach { word -> documentWords.add(word) }
                filterState =
                    if (numberFilter || bncNumberFilter || frqNumFilter ||
                        bncZeroFilter || frqZeroFilter || replaceToLemma ||
                        vocabularyFilterList.isNotEmpty()
                    ) {
                        Filtering
                    } else {
                        End
                    }
                if (filterState == End) {
                    previewList.clear()
                    previewList.addAll(documentWords)
                }

                if (errorMessages.isNotEmpty()) {
                    val string = "有 ${errorMessages.size} 个文件解析失败，请点击 [任务列表] 查看详细信息"
                    JOptionPane.showMessageDialog(window, string)
                }

            }.start()

        }

        /**
         * 手动点击删除的单词，一般都是熟悉的词，
         * 所有需要添加到熟悉词库
         */
        val removeWord:(Word) -> Unit = {
            previewList.remove(it)
            removedWords.add(it)
            // 如果是过滤词库，同时过滤的是熟悉词库，要把删除的单词从内存中的熟悉词库删除
            if (state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary") {
                familiarVocabulary.wordList.remove(it)
            }else{
                // 用字幕生成的词库和用 MKV 生成的词库，需要把内部字幕转换为外部字幕
                if (it.captions.isNotEmpty()) {
                    it.captions.forEach { caption ->
                        val externalCaption = ExternalCaption(
                            relateVideoPath = relateVideoPath,
                            subtitlesTrackId = selectedTrackId,
                            subtitlesName = File(selectedFilePath).nameWithoutExtension,
                            start = caption.start,
                            end = caption.end,
                            content = caption.content
                        )
                        it.externalCaptions.add(externalCaption)
                    }
                    it.captions.clear()
                }

                // 把单词添加到熟悉词库
                if(!familiarVocabulary.wordList.contains(it)){
                    familiarVocabulary.wordList.add(it)
                }
            }
            scope.launch {
                familiarVocabulary.size = familiarVocabulary.wordList.size
                val familiarFile = getFamiliarVocabularyFile()
                saveVocabulary(familiarVocabulary.serializeVocabulary, familiarFile.absolutePath)
            }

        }

        val contentPanel = ComposePanel()
        contentPanel.setContent {
            MaterialTheme(colors = state.colors) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
                    Divider()
                    Row(Modifier.fillMaxWidth()) {
                        // 左边的过滤区
                        val width = if(vocabularyFilterList.isEmpty()) 380.dp else 450.dp
                        Column(Modifier.width(width).fillMaxHeight()) {
                            BasicFilter(
                                numberFilter = numberFilter,
                                changeNumberFilter = {
                                    numberFilter = it
                                    filterState = Filtering
                                },
                                bncNum = state.global.bncNum,
                                setBncNum = {state.global.bncNum = it},
                                bncNumFilter = bncNumberFilter,
                                changeBncNumFilter = {
                                    bncNumberFilter = it
                                    filterState = Filtering
                                },
                                frqNum = state.global.frqNum,
                                setFrqNum = {state.global.frqNum = it},
                                frqNumFilter = frqNumFilter,
                                changeFrqFilter = {
                                    frqNumFilter = it
                                    filterState = Filtering
                                },
                                bncZeroFilter = bncZeroFilter,
                                changeBncZeroFilter = {
                                    bncZeroFilter = it
                                    filterState = Filtering
                                },
                                frqZeroFilter = frqZeroFilter,
                                changeFrqZeroFilter = {
                                    frqZeroFilter = it
                                    filterState = Filtering
                                },
                                replaceToLemma = replaceToLemma,
                                setReplaceToLemma = {
                                    replaceToLemma = it
                                    filterState = Filtering
                                },
                            )
                            VocabularyFilter(
                                futureFileChooser = state.futureFileChooser,
                                vocabularyFilterList = vocabularyFilterList,
                                vocabularyFilterListAdd = {
                                    if (!vocabularyFilterList.contains(it)) {
                                        vocabularyFilterList.add(it)
                                        filterState = Filtering
                                    }
                                },
                                vocabularyFilterListRemove = {
                                    vocabularyFilterList.remove(it)
                                    filterState = Filtering
                                },
                                recentList = state.recentList,
                                removeInvalidRecentItem = {
                                    state.removeRecentItem(it)
                                },
                                familiarVocabulary = familiarVocabulary,
                                updateFamiliarVocabulary = {
                                    val wordList = loadMutableVocabularyByName("FamiliarVocabulary").wordList
                                    familiarVocabulary.wordList.addAll(wordList)
                                }
                            )
                        }
                        Divider(Modifier.width(1.dp).fillMaxHeight())
                        // 生成词库区
                        Column(
                            Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colors.background)
                        ) {

                            SelectFile(
                                type = type,
                                selectedFileList = selectedFileList,
                                selectedFilePath = selectedFilePath,
                                setSelectedFilePath = { selectedFilePath = it },
                                selectedSubtitle = selectedSubtitlesName,
                                setSelectedSubtitle = { selectedSubtitlesName = it },
                                relateVideoPath = relateVideoPath,
                                trackList = trackList,
                                selectedTrackId = selectedTrackId,
                                setSelectedTrackId = { selectedTrackId = it },
                                showTaskList = showTaskList,
                                showTaskListEvent = {
                                    showTaskList = !showTaskList
                                    if(!showTaskList){
                                        selectable = false
                                    }
                               },
                                analysis = { pathName, trackId ->
                                    analysis(pathName, trackId)
                                },
                                batchAnalysis = { batchAnalysis(it) },
                                selectable = selectable,
                                changeSelectable = { selectable = !selectable },
                                selectAll = { selectAll() },
                                delete = { delete() },
                                chooseText = chooseText,
                                openFile = { openFile() },
                                openRelateVideo = { openRelateVideo() }
                            )

                            // 单词预览和任务列表
                            Box(Modifier.fillMaxSize()) {
                                when (filterState) {
                                    Parsing -> {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.width(60.dp).padding(bottom = 60.dp)
                                            )
                                            Text(text = progressText, color = MaterialTheme.colors.onBackground)
                                        }
                                    }
                                    Filtering -> {
                                        CircularProgressIndicator(
                                            Modifier.width(60.dp).align(Alignment.Center)
                                        )
                                        Thread {
                                            // 根据词频或原型过滤单词
                                            val filteredDocumentList = filterDocumentWords(
                                                documentWords,
                                                numberFilter,
                                                state.global.bncNum,
                                                bncNumberFilter,
                                                state.global.frqNum,
                                                frqNumFilter,
                                                bncZeroFilter,
                                                frqZeroFilter,
                                                replaceToLemma,
                                                selectedFileList.isEmpty()
                                            )
                                            previewList.clear()
                                            // 根据选择的词库过滤单词
                                            val filteredList = filterSelectVocabulary(
                                                selectedFileList = vocabularyFilterList,
                                                filteredDocumentList = filteredDocumentList
                                            )
                                            // 过滤手动删除的单词
                                            filteredList.removeAll(removedWords)
                                            previewList.addAll(filteredList)
                                            filterState = End
                                        }.start()


                                    }
                                    End -> {
                                        PreviewWords(
                                            previewList = previewList,
                                            summaryVocabulary = summaryVocabulary,
                                            removeWord = { removeWord(it) },
                                            sort = sort,
                                            changeSort = {sort = it}
                                        )
                                    }
                                    Idle -> {}
                                }

                                    if (loading) {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.align(Alignment.Center).fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(
                                                Modifier.width(60.dp).padding(bottom = 60.dp)
                                            )
                                            val text = if (selectedFileList.isNotEmpty()) {
                                                "正在读取第一个视频的字幕轨道列表"
                                            } else {
                                                "正在读取字幕轨道列表"
                                            }
                                            Text(text = text, color = MaterialTheme.colors.onBackground)
                                        }
                                    }
                                if (showTaskList) {
                                    TaskList(
                                        selectedFileList = selectedFileList,
                                        updateOrder = {
                                            scope.launch {
                                                selectedFileList.clear()
                                                selectedFileList.addAll(it)
                                            }
                                        },
                                        tasksState = tasksState,
                                        currentTask = currentTask,
                                        errorMessages = errorMessages,
                                        selectable = selectable,
                                        checkedFileMap = checkedFileMap,
                                        checkedChange = {
                                            checkedFileMap[it.first] = it.second
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }

        val bottomPanel = ComposePanel()
        bottomPanel.setSize(Int.MAX_VALUE, 54)
        bottomPanel.setContent {
            MaterialTheme(colors = state.colors) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(54.dp).background(MaterialTheme.colors.background)
                ) {
                    Divider()
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        OutlinedButton(
                            enabled = previewList.size > 0,
                            onClick = {
                                Thread {
                                    val fileChooser = state.futureFileChooser.get()
                                    fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                    fileChooser.dialogTitle = "保存词库"
                                    val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                                    val fileName = File(selectedFilePath).nameWithoutExtension
                                    if(state.filterVocabulary && File(selectedFilePath).nameWithoutExtension == "FamiliarVocabulary"){
                                        fileChooser.selectedFile = File(selectedFilePath)
                                    }else{
                                        fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                                    }
                                    val userSelection = fileChooser.showSaveDialog(window)
                                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                                        val selectedFile = fileChooser.selectedFile
                                       val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                                       val savePath = Paths.get(selectedFile.absolutePath)
                                        if(savePath.startsWith(vocabularyDirPath)){
                                            JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，生成的词库会被删除")
                                        }else{
                                            val vType = if (title == "过滤词库"){
                                                filteringType
                                            } else if(selectedFileList.isNotEmpty()){
                                                DOCUMENT
                                            }else type
                                            val vocabulary = Vocabulary(
                                                name = selectedFile.nameWithoutExtension,
                                                type = vType,
                                                language = "english",
                                                size = previewList.size,
                                                relateVideoPath = relateVideoPath,
                                                subtitlesTrackId = selectedTrackId,
                                                wordList = previewList
                                            )
                                            state.saveToRecentList(vocabulary.name, selectedFile.absolutePath,0)
                                            saveVocabulary(vocabulary, selectedFile.absolutePath)

                                            // 清理状态
                                            selectedFileList.clear()
                                            showTaskList = false
                                            tasksState.clear()
                                            currentTask = null
                                            errorMessages.clear()
                                            selectedFilePath = ""
                                            selectedSubtitlesName = ""
                                            previewList.clear()
                                            relateVideoPath = ""
                                            selectedTrackId = 0
                                            filteringType = DOCUMENT
                                            trackList.clear()
                                            filterState = Idle
                                            documentWords.clear()
                                            vocabularyFilterList.clear()
                                            numberFilter = false
                                            bncZeroFilter = false
                                            frqZeroFilter = false
                                            replaceToLemma = false
                                        }


                                    }
                                }.start()

                            }) {
                            Text("保存")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = {
                            onCloseRequest(state, title)
                        }) {
                            Text("取消")
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                }
            }

        }
        SwingPanel(
            background = Color(MaterialTheme.colors.background.toArgb()),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            factory = {
                JPanel().apply {
                    layout = BorderLayout()
                    add(bottomPanel, BorderLayout.SOUTH)
                    add(contentPanel, BorderLayout.CENTER)
                }
            }
        )

    }
}
 fun writeToCSV(
    previewList: SnapshotStateList<Word>,
    selectedFile: File
) {
    val rows = mutableListOf<List<String>>()
    val header = listOf("单词", "中文释义", "英文释义", "字幕")
    rows.add(header)
    previewList.forEach { word ->
        val line = mutableListOf<String>()
        line.add(word.value)
        line.add(word.translation)
        line.add(word.definition)
        var captions = ""
        word.captions.forEach { caption ->
            captions = captions + caption + "\n"
        }
        word.externalCaptions.forEach { caption ->
            captions = captions + caption + "\n"
        }
        line.add(captions)
        rows.add(line)
    }

    csvWriter().writeAll(rows, selectedFile.absolutePath, append = false)
}

@Serializable
data class RecentItem(val time: String, val name: String, val path: String, val index: Int = 0) {
    override fun equals(other: Any?): Boolean {
        val otherItem = other as RecentItem
        return this.name == otherItem.name && this.path == otherItem.path
    }

    override fun hashCode(): Int {
        return name.hashCode() + path.hashCode()
    }
}

/** FilterState 有四个状态：Idle、"Parse"、"Filtering"、"End" */
enum class FilterState {
    /** 空闲状态，预览区为空 */
    Idle,
    /** 正在解析文档或字幕 */
    Parsing,
    /** 正在过滤单词 */
    Filtering,
    /** 单词过滤完成，可以显示了*/
    End
}

@OptIn(ExperimentalSerializationApi::class)
private fun onCloseRequest(state: AppState, title: String) {
    when (title) {
        "过滤词库" -> state.filterVocabulary = false
        "用文档生成词库" -> state.generateVocabularyFromDocument = false
        "用字幕生成词库" -> state.generateVocabularyFromSubtitles = false
        "用 MKV 视频生成词库" -> state.generateVocabularyFromMKV = false
    }

}

private fun getWordLemma(word: Word): String? {
    word.exchange.split("/").forEach { exchange ->
        val pair = exchange.split(":")
        if (pair[0] == "0") return pair[1]
    }
    return null
}

@Composable
fun Summary(
    list: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    sort:String,
    changeSort:(String) -> Unit,
) {

    Column(Modifier.fillMaxWidth()) {
        val height =  61.dp
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(height).padding(start = 10.dp)
        ) {
            val summary = computeSummary(list, summaryVocabulary)
            Text(text = "共 ${list.size} 词  ", color = MaterialTheme.colors.onBackground)
            Text(text = "牛津5000核心词：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["oxford"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[0]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "四级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet4"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[1]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "六级：", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["cet6"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[2]} 词  ", color = MaterialTheme.colors.onBackground)
            }
            Text(text = "GRE: ", color = MaterialTheme.colors.onBackground)
            if (summaryVocabulary["gre"]?.isEmpty() == true) {
                Text(text = "词库缺失 ", color = Color.Red)
            } else {
                Text("${summary[3]} 词", color = MaterialTheme.colors.onBackground)
            }


            var expanded by remember { mutableStateOf(false) }
            Box {
                val width = 195.dp
                val text = when(sort){
                    "appearance" -> "按出现的顺序排序"
                    "bnc" -> "按 BNC 词频排序"
                    else -> "按 COCA 词频排序"
                }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(width)
                        .padding(start =10.dp)
                        .background(Color.Transparent)
                        .border(1.dp, Color.Transparent)
                ) {
                    Text(text = text)
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
                            expanded = false
                            changeSort("bnc")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background( if(sort == "bnc")selectedColor else backgroundColor )
                    ) {
                        Text("BNC    词频")
                    }
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("coca")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if(sort == "coca") selectedColor else backgroundColor)
                    ) {
                        Text("COCA  词频")

                    }


                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            changeSort("appearance")
                        },
                        modifier = Modifier.width(width).height(40.dp)
                            .background(if(sort == "appearance") selectedColor else backgroundColor)
                    ) {
                        Text("出现的顺序")
                    }


                }

            }
        }
        Divider()
    }


}

/**
 * 计算摘要
 */
private fun computeSummary(
    list: List<Word>,
    vocabularySummary: Map<String, List<String>>
): List<Int> {
    var oxfordCount = 0
    var cet4Count = 0
    var cet6Count = 0
    var greCount = 0
    list.forEach { word ->
        if (vocabularySummary["oxford"]?.contains(word.value) == true) {
            oxfordCount++
        }
        if (vocabularySummary["cet4"]?.contains(word.value) == true) {
            cet4Count++
        }
        if (vocabularySummary["cet6"]?.contains(word.value) == true) {
            cet6Count++
        }
        if (vocabularySummary["gre"]?.contains(word.value) == true) {
            greCount++
        }
    }

    return listOf(oxfordCount, cet4Count, cet6Count, greCount)
}

/**
 * 载入摘要词库
 */
private fun loadSummaryVocabulary(): Map<String, List<String>> {

    val oxford = loadVocabulary("vocabulary/牛津核心词/The_Oxford_5000.json").wordList
    val cet4 = loadVocabulary("vocabulary/大学英语/四级.json").wordList
    val cet6 = loadVocabulary("vocabulary/大学英语/六级.json").wordList
    val gre = loadVocabulary("vocabulary/其它/GRE.json").wordList

    val oxfordList = oxford.map { word -> word.value }
    val cet4List = cet4.map { word -> word.value }
    val cet6List = cet6.map { word -> word.value }
    val greList = gre.map { word -> word.value }

    val map = HashMap<String, List<String>>()
    map["oxford"] = oxfordList
    map["cet4"] = cet4List
    map["cet6"] = cet6List
    map["gre"] = greList

    return map
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun BasicFilter(
    numberFilter: Boolean,
    changeNumberFilter: (Boolean) -> Unit,
    bncNum:Int,
    setBncNum:(Int) -> Unit,
    bncNumFilter:Boolean,
    changeBncNumFilter:(Boolean) -> Unit,
    frqNum:Int,
    setFrqNum:(Int) -> Unit,
    frqNumFilter:Boolean,
    changeFrqFilter:(Boolean) -> Unit,
    bncZeroFilter: Boolean,
    changeBncZeroFilter: (Boolean) -> Unit,
    frqZeroFilter: Boolean,
    changeFrqZeroFilter: (Boolean) -> Unit,
    replaceToLemma: Boolean,
    setReplaceToLemma: (Boolean) -> Unit,
) {
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("过滤词库", color = MaterialTheme.colors.onBackground, fontFamily = FontFamily.Default)
        }
        Divider()
        val textWidth = 320.dp
        val textColor = MaterialTheme.colors.onBackground
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                Text("过滤所有数字 ", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = numberFilter,
                onCheckedChange = { changeNumberFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(Modifier.width(textWidth)) {
                Text("过滤 ", color = MaterialTheme.colors.onBackground)
                Text("BNC", color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(end = 1.dp))
                Text("   词频前 ", color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = "$bncNum",
                    onValueChange = {
                        val input = it.toIntOrNull()
                        if (input != null && input >= 0) {
                            setBncNum(input)
                        }else{
                            setBncNum(0)
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
                        .width(IntrinsicSize.Max)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
                Text(" 的单词", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = bncNumFilter,
                onCheckedChange = { changeBncNumFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.width(textWidth)) {
                Text("过滤 COCA 词频前 ", color = MaterialTheme.colors.onBackground)
                Box{
                    BasicTextField(
                        value = "$frqNum",
                        onValueChange = {
                            val input = it.toIntOrNull()
                           if (input != null && input >= 0) {
                               setFrqNum(input)
                            }else{
                               setFrqNum(0)
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
                            .width(IntrinsicSize.Max)
                            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                    )
                }
                Text(" 的单词", color = MaterialTheme.colors.onBackground)
            }
            Checkbox(
                checked = frqNumFilter,
                onCheckedChange = { changeFrqFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {


            Row(Modifier.width(textWidth)) {
                Text("过滤所有 ", color = MaterialTheme.colors.onBackground)

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "英国国家语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {

                    Text("BNC", color = blueColor,
                        modifier = Modifier
                            .clickable {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                        .isSupported(Desktop.Action.BROWSE)
                                ) {
                                    Desktop.getDesktop().browse(URI("https://www.natcorp.ox.ac.uk/"))
                                }
                            }
                            .pointerHoverIcon(PointerIconDefaults.Hand)
                            .padding(end = 3.dp))
                }

                Text("   语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = bncZeroFilter,
                onCheckedChange = { changeBncZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(Modifier.width(textWidth)) {
                Text("过滤所有 ", color = MaterialTheme.colors.onBackground)
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "美国当代英语语料库", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomEnd,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset.Zero
                    )
                ) {
                    Text("COCA", color = blueColor,
                        modifier = Modifier.clickable {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.BROWSE)
                            ) {
                                Desktop.getDesktop().browse(URI("https://www.english-corpora.org/coca/"))
                            }
                        }
                            .pointerHoverIcon(PointerIconDefaults.Hand))
                }

                Text(" 语料库词频顺序为0的词", color = textColor)
            }
            Checkbox(
                checked = frqZeroFilter,
                onCheckedChange = { changeFrqZeroFilter(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "词形还原，例如：\ndid、done、doing、does 全部替换为 do",
                fontFamily = FontFamily.Default,
                color = textColor,
                modifier = Modifier.width(textWidth)
            )
            Checkbox(
                checked = replaceToLemma,
                onCheckedChange = { setReplaceToLemma(it) },
                modifier = Modifier.size(30.dp, 30.dp)
            )
        }
        Divider()
    }

}

@Composable
fun VocabularyFilter(
    futureFileChooser: FutureTask<JFileChooser>,
    vocabularyFilterList: List<File>,
    vocabularyFilterListAdd: (File) -> Unit,
    vocabularyFilterListRemove: (File) -> Unit,
    recentList: List<RecentItem>,
    removeInvalidRecentItem: (RecentItem) -> Unit,
    familiarVocabulary: MutableVocabulary,
    updateFamiliarVocabulary:() -> Unit
) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        var selectedPath: TreePath? = null
        val vocabulary = composeAppResource("vocabulary")
        val pathNameMap = searchPaths(vocabulary)
        val tree = JTree(addNodes(null, vocabulary))

        val treeSelectionListener: TreeSelectionListener = object : TreeSelectionListener {
            override fun valueChanged(event: TreeSelectionEvent?) {
                if (event != null) {
                    val path = event.path
                    val node = path.lastPathComponent as DefaultMutableTreeNode
                    val name = node.userObject.toString()
                    if (node.isLeaf) {
                        val filePath = pathNameMap[name]
                        if (filePath != null) {
                            val file = File(filePath)
                            if (!vocabularyFilterList.contains(file)) {
                                vocabularyFilterListAdd(file)
                            }
                        }

                    }
                    selectedPath = path
                }

            }

        }

        tree.addTreeSelectionListener(treeSelectionListener)

        val scrollPane = JScrollPane(
            tree,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        if (!MaterialTheme.colors.isLight) {
            tree.background = java.awt.Color(32, 33, 34)
            scrollPane.background = java.awt.Color(32, 33, 34)
        }
        scrollPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        Column(Modifier.width(180.dp).fillMaxHeight().background(MaterialTheme.colors.background)) {

            if (recentList.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box(Modifier.width(180.dp).height(IntrinsicSize.Max).padding(top = 10.dp)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        Text(text = "最近使用的词库")
                    }
                    val dropdownMenuHeight = if (recentList.size <= 10) (recentList.size * 40 + 20).dp else 420.dp

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(20.dp, 0.dp),
                        modifier = Modifier.width(IntrinsicSize.Max).height(dropdownMenuHeight)
                    ) {

                        Box(Modifier.fillMaxWidth().height(dropdownMenuHeight)) {
                            val stateVertical = rememberScrollState(0)
                            Box(Modifier.fillMaxSize().verticalScroll(stateVertical)) {
                                Column {
                                    recentList.forEach { recentItem ->

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                                .clickable {
                                                    val recentFile = File(recentItem.path)
                                                    if(recentFile.exists()){
                                                        vocabularyFilterListAdd(recentFile)
                                                    }else{
                                                        // 文件可能被删除了
                                                        removeInvalidRecentItem(recentItem)
                                                        JOptionPane.showMessageDialog(null,"文件地址错误：\n${recentItem.path}")
                                                    }

                                                }
                                        ) {
                                            Text(
                                                text = recentItem.name,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                                color = MaterialTheme.colors.onBackground,
                                                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                                            )

                                        }

                                    }

                                }
                            }

                            VerticalScrollbar(
                                style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = stateVertical
                                )
                            )
                        }

                    }
                }
            }
            Box(Modifier.width(180.dp).height(IntrinsicSize.Max).background(MaterialTheme.colors.background)) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .width(139.dp)
                        .align(Alignment.Center)
                ) {
                    Text(text = "内置词库")
                }
                DropdownMenu(
                    expanded = expanded,
                    offset = DpOffset(20.dp, 0.dp),
                    onDismissRequest = { expanded = false },
                ) {
                    SwingPanel(
                        modifier = Modifier.width(400.dp).height(400.dp),
                        factory = {
                            scrollPane
                        }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        Thread {
                            val fileChooser = futureFileChooser.get()
                            fileChooser.dialogTitle = "选择词库"
                            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                            fileChooser.isAcceptAllFileFilterUsed = false
                            val fileFilter = FileNameExtensionFilter("词库", "json")
                            fileChooser.addChoosableFileFilter(fileFilter)
                            fileChooser.selectedFile = null
                            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val file = fileChooser.selectedFile
                                vocabularyFilterListAdd(File(file.absolutePath))
                            }
                            fileChooser.selectedFile = null
                            fileChooser.removeChoosableFileFilter(fileFilter)
                        }.start()

                    },
                    modifier = Modifier
                        .width(139.dp)
                ) {
                    Text(text = "选择词库")
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    FamiliarDialog(
                        futureFileChooser = futureFileChooser,
                        close = {
                            showDialog = false
                            updateFamiliarVocabulary()
                        },

                        )
                }
                OutlinedButton(
                    onClick = {
                        if (familiarVocabulary.wordList.isEmpty()) {
                            val result = JOptionPane.showConfirmDialog(
                                null,
                                "熟悉词库现在还没有单词，是否导入单词到熟悉词库",
                                "",
                                JOptionPane.YES_NO_OPTION
                            )
                            if (result == 0) {
                                showDialog = true
                            }
                        } else {
                            val familiarFile = getFamiliarVocabularyFile()
                            vocabularyFilterListAdd(File(familiarFile.absolutePath))
                        }

                    },
                    modifier = Modifier
                        .width(139.dp)
                ) {
                    if(familiarVocabulary.wordList.isNotEmpty()){
                        BadgedBox(badge = {
                            Badge {
                                val badgeNumber = "${familiarVocabulary.wordList.size}"
                                Text(
                                    badgeNumber,
                                    modifier = Modifier.semantics {
                                        contentDescription = "$badgeNumber new notifications"
                                    }
                                )
                            }
                        }) {
                            Text(text = "熟悉词库")
                        }
                    }else{
                        Text(text = "熟悉词库")
                    }

                }
            }

        }
        Divider(Modifier.width(1.dp).fillMaxHeight())
        Column(
            Modifier.width(270.dp).fillMaxHeight()
                .background(MaterialTheme.colors.background)
        ) {
            SelectedList(
                vocabularyFilterList,
                removeFile = {
                    if (selectedPath != null) {
                        tree.removeSelectionPath(selectedPath)
                        selectedPath = null
                    }
                    vocabularyFilterListRemove(it)
                })
        }
    }
}

@Composable
fun SelectedList(
    list: List<File>,
    removeFile: (File) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LazyColumn {
            items(list) { file ->

                Box(
                    modifier = Modifier.clickable {}
                        .fillMaxWidth()
                ) {
                    var name = file.nameWithoutExtension
                    if(file.parentFile.nameWithoutExtension == "人教版英语" ||
                        file.parentFile.nameWithoutExtension == "外研版英语"||
                        file.parentFile.nameWithoutExtension == "北师大版高中英语"){
                        if(name.contains(" ")){
                            name = name.split(" ")[1]
                        }
                    }
                    Text(
                        text = name,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).width(225.dp).padding(10.dp)
                    )

                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .clickable { removeFile(file) }
                            .align(Alignment.CenterEnd)
                            .size(30.dp, 30.dp)

                    )
                }
            }
        }
    }


}

/**
 * Map<String,String> 的类型参数，第一个代表文件名，第二个代表文件的绝对路径
 */
fun searchPaths(dir: File): MutableMap<String, String> {
    val pathNameMap: MutableMap<String, String> = hashMapOf()
    dir.listFiles().forEach { file ->
        if (file.isDirectory) {
            pathNameMap.putAll(searchPaths(file))
        }
        if (!file.isDirectory) {
            var name = file.nameWithoutExtension
            if(file.parentFile.nameWithoutExtension == "人教版英语" ||
                file.parentFile.nameWithoutExtension == "外研版英语"||
                file.parentFile.nameWithoutExtension == "北师大版高中英语"){
                if(name.contains(" ")){
                    name = name.split(" ")[1]
                }

            }
            pathNameMap.put(name, file.absolutePath)
        }
    }
    return pathNameMap
}

fun addNodes(curTop: DefaultMutableTreeNode?, dir: File): DefaultMutableTreeNode {
    val curDir = DefaultMutableTreeNode(dir.nameWithoutExtension)
    curTop?.add(curDir)
    val ol = Vector<File>()
    dir.listFiles().forEach { ol.addElement(it) }
    if(dir.nameWithoutExtension.contains("人教版英语")||
        dir.nameWithoutExtension.contains("外研版英语")||
        dir.nameWithoutExtension.contains("北师大版高中英语")
        ){
        ol.sortBy{it.nameWithoutExtension.split(" ")[0].toFloat()}
    }else{
        ol.sort()
    }

    val files = Vector<String>()

    ol.forEach { file ->
        if (file.isDirectory)
            addNodes(curDir, file)
        else{
            var name = file.nameWithoutExtension
            if(file.parentFile.nameWithoutExtension == "人教版英语" ||
                file.parentFile.nameWithoutExtension == "外研版英语"||
                file.parentFile.nameWithoutExtension == "北师大版高中英语"){
                if(name.contains(" ")){
                    name = name.split(" ")[1]
                }

            }
            files.addElement(name)
        }

    }

//    val cmp = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
//    Collections.sort(files, cmp)
    files.forEach {
        curDir.add(DefaultMutableTreeNode(it))
    }
    return curDir
}

@Composable
fun SelectFile(
    type: VocabularyType,
    selectedFileList: List<File>,
    selectedFilePath: String,
    setSelectedFilePath: (String) -> Unit,
    selectedSubtitle: String,
    setSelectedSubtitle: (String) -> Unit,
    relateVideoPath: String,
    trackList: List<Pair<Int, String>>,
    selectedTrackId: Int,
    setSelectedTrackId: (Int) -> Unit,
    showTaskList:Boolean,
    showTaskListEvent:() -> Unit,
    analysis: (String, Int) -> Unit,
    batchAnalysis: (String) -> Unit,
    selectable:Boolean,
    changeSelectable:() -> Unit,
    selectAll:() -> Unit,
    delete:() -> Unit,
    chooseText:String,
    openFile:() -> Unit,
    openRelateVideo:() -> Unit
) {

    Column(Modifier.height(IntrinsicSize.Max)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(start = 10.dp)
        ) {
            Text(chooseText, color = MaterialTheme.colors.onBackground)
            if (type == SUBTITLES || type == DOCUMENT) {
                Spacer(Modifier.width(75.dp))
            } else if (type == MKV) {
                Spacer(Modifier.width(24.dp))
            }
            BasicTextField(
                value = selectedFilePath,
                onValueChange = {
                    setSelectedFilePath(it)
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                textStyle = TextStyle(
                    lineHeight = 29.sp,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onBackground
                ),
                modifier = Modifier
                    .width(300.dp)
                    .padding(start = 8.dp, end = 10.dp)
                    .height(35.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            OutlinedButton(onClick = { openFile() }) {
                Text("打开", fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))
            if (type != MKV && selectedFilePath.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        analysis(selectedFilePath, selectedTrackId)
                    }) {
                    Text("开始", fontSize = 12.sp)
                }
            }
        }

        if ((selectedFilePath.isNotEmpty() || selectedFileList.isNotEmpty()) && type == MKV) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                if (trackList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.width(IntrinsicSize.Max)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(282.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(
                                    text = selectedSubtitle, fontSize = 12.sp,
                                )
                                Icon(
                                    Icons.Default.ExpandMore, contentDescription = "Localized description",
                                    modifier = Modifier.size(20.dp, 20.dp)
                                )
                            }
                            val dropdownMenuHeight = (trackList.size * 40 + 20).dp
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(282.dp)
                                    .height(dropdownMenuHeight)
                            ) {
                                trackList.forEach { (index, description) ->
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            setSelectedSubtitle(description)
                                            setSelectedTrackId(index)
                                        },
                                        modifier = Modifier.width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = "$description ", fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }


                            }

                        }

                    }
                }else{
                    // 批量处理，现在只能批量处理英语字幕，所以就写死了。
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(IntrinsicSize.Max).padding(end = 10.dp)
                    ) {
                        Text(
                            "选择字幕 ",
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(end = 75.dp)
                        )
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier
                                .width(282.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(
                                text = "英语", fontSize = 12.sp,
                            )
                        }
                    }
                }

                if (selectedFileList.isEmpty() && selectedSubtitle != "    " && trackList.isNotEmpty()) {
                    OutlinedButton(onClick = {
                        analysis(selectedFilePath, selectedTrackId)
                    }) {
                        Text("开始", fontSize = 12.sp)
                    }
                }

                if(selectedFileList.isNotEmpty()){
                    OutlinedButton(onClick = { batchAnalysis("English") }) {
                        Text("开始", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { showTaskListEvent() }) {
                        Text("任务列表", fontSize = 12.sp)
                    }
                    if(showTaskList){
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { changeSelectable() }) {
                            Text("选择", fontSize = 12.sp)
                        }
                    }
                    if(selectable){
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { selectAll() }) {
                            Text("全选", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { delete() }) {
                            Text("删除", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (type == SUBTITLES && selectedFilePath.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(start = 10.dp, bottom = 14.dp)
            ) {
                Text("选择对应的视频(可选)", color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = relateVideoPath,
                    onValueChange = {
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        lineHeight = 29.sp,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(start = 8.dp, end = 10.dp)
                        .height(35.dp)
                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                )
                OutlinedButton(onClick = { openRelateVideo() }) {
                    Text("打开")
                }
            }
        }
        Divider()
    }
}


fun filterDocumentWords(
    documentWords: List<Word>,
    numberFilter: Boolean,
    bncNum:Int,
    bncNumFilter:Boolean,
    frqNum:Int,
    frqNumFilter: Boolean,
    bncZeroFilter: Boolean,
    frqZeroFilter: Boolean,
    replaceToLemma: Boolean,
    isBatchMKV:Boolean
): List<Word> {
    val previewList = ArrayList(documentWords)
    /**
     * Key 为需要转换为原型的单词，
     *  Value 是 Key 的原型词，还没有查词典，有可能词典里面没有。
     */
    val lemmaMap = HashMap<Word,String>()

    /** 原型词 > 内部字幕列表 映射 */
    val captionsMap = HashMap<String, MutableList<Caption>>()

    /** 原型词 -> 外部字幕列表映射,批量生成 MKV 词库时，字幕保存在单词的外部字幕列表 */
    val externalCaptionsMap = HashMap<String, MutableList<ExternalCaption>>()

    documentWords.forEach { word ->

        if (numberFilter && (word.value.toDoubleOrNull() != null)){
            // 过滤数字
            previewList.remove(word)
        }else if(bncNumFilter && (word.bnc!! in 1 until bncNum)){
            // 过滤最常见的词
            previewList.remove(word)
        }else if(frqNumFilter && (word.frq!! in 1 until frqNum)){
            // 过滤最常见的词
            previewList.remove(word)
        }else if (bncZeroFilter && word.bnc == 0){
            // 过滤 BNC 词频为 0 的词
            previewList.remove(word)
        }else if (frqZeroFilter && word.frq == 0){
            // 过滤 COCA 词频为 0 的词
            previewList.remove(word)
        }

        val lemma = getWordLemma(word)
        if (replaceToLemma && !lemma.isNullOrEmpty()) {
            lemmaMap[word] = lemma
            // 处理内部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
            if(!isBatchMKV){
                if (captionsMap[lemma].isNullOrEmpty()) {
                    captionsMap[lemma] = word.captions
                } else {
                    // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                    val list = mutableListOf<Caption>()
                    list.addAll(captionsMap[lemma]!!)
                    for (caption in word.captions) {
                        if(list.size<3){
                            list.add(caption)
                        }
                    }
                    captionsMap[lemma] = list
                }
            // 处理外部字幕，批量的用 MKV 生成词库时，字幕保存在外部字幕列表
            }else{
                if (externalCaptionsMap[lemma].isNullOrEmpty()) {
                    externalCaptionsMap[lemma] = word.externalCaptions
                } else {
                    // do 有四个派生词，四个派生词可能在文件的不同位置，可能有四个不同的字幕列表
                    val list = mutableListOf<ExternalCaption>()
                    list.addAll(externalCaptionsMap[lemma]!!)
                    for (externalCaption in word.externalCaptions) {
                        if(list.size<3){
                            list.add(externalCaption)
                        }
                    }
                    externalCaptionsMap[lemma] = list
                }
            }

        }
    }
    // 查询单词原型
    val queryList = lemmaMap.values.toList()
    val result = Dictionary.queryList(queryList)
    val validLemmaMap = HashMap<String,Word>()
    result.forEach { word ->
        // 处理内部字幕
        if(!isBatchMKV){
            val captions = captionsMap[word.value]!!
            word.captions = captions
        // 处理外部字幕
        }else{
            val externalCaptions = externalCaptionsMap[word.value]!!
            word.externalCaptions = externalCaptions
        }

        validLemmaMap[word.value] = word
    }

    val toLemmaList = lemmaMap.keys
    for (word in toLemmaList) {
        val index = previewList.indexOf(word)
        // 有一些词可能 属于 BNC 或 FRQ 为 0 的词，已经被过滤了，所以 index 为 -1
        if(index != -1){
            val lemmaStr = lemmaMap[word]
            val validLemma = validLemmaMap[lemmaStr]
            if(validLemma != null){
                previewList.remove(word)
                if (!previewList.contains(validLemma)){
                    // 默认 add 为真
                    var add = true
                    // 但是，如果单词的词频为 0 或者是最常见的单词就不添加
                    if(bncNumFilter && (validLemma.bnc!! in 1 until bncNum)){
                        add = false
                    }else if(frqNumFilter && (validLemma.frq!! in 1 until frqNum)){
                        add = false
                    }else if (bncZeroFilter && validLemma.bnc == 0){
                        add = false
                    }else if (frqZeroFilter && validLemma.frq == 0){
                        add = false
                    }

                    if(add){
                        previewList.add(index,validLemma)
                    }
                }
            }

        }

    }
    return previewList
}

fun filterSelectVocabulary(
    selectedFileList: List<File>,
    filteredDocumentList: List<Word>
): MutableList<Word> {
    val list = ArrayList(filteredDocumentList)
    selectedFileList.forEach { file ->
        if (file.exists()) {
            val vocabulary = loadVocabulary(file.absolutePath)
            list.removeAll(vocabulary.wordList)
        } else {
            JOptionPane.showMessageDialog(null, "找不到词库：\n${file.absolutePath}")
        }

    }
    return list
}

/**
 * 预览单词
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PreviewWords(
    previewList: List<Word>,
    summaryVocabulary: Map<String, List<String>>,
    removeWord: (Word) -> Unit,
    sort:String,
    changeSort:(String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {

        Summary(previewList, summaryVocabulary,sort, changeSort = {changeSort(it)})
        val sortedList = when(sort){
            "bnc" -> {
                val sorted = previewList.sortedBy { it.bnc }
                val zeroBnc = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for(word in sorted){
                    if(word.bnc == 0){
                        zeroBnc.add(word)
                    }else{
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroBnc)
                greaterThanZero
            }
            "coca" -> {
                val sorted = previewList.sortedBy { it.frq }
                val zeroFrq = mutableListOf<Word>()
                val greaterThanZero = mutableListOf<Word>()
                for(word in sorted){
                    if(word.frq == 0){
                        zeroFrq.add(word)
                    }else{
                        greaterThanZero.add(word)
                    }
                }
                greaterThanZero.addAll(zeroFrq)
                greaterThanZero
            }
            else  -> previewList
        }
        val listState = rememberLazyGridState()
        Box(Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(130.dp),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 60.dp),
                state = listState
            ) {
                itemsIndexed(sortedList) { _: Int, word ->

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape,
                            ) {
                                Column(Modifier.padding(5.dp).width(200.dp)) {
                                    Text(
                                        text = word.value,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val lemma = getWordLemma(word)
                                    if (lemma != null) {
                                        Text(text = "原型:$lemma", fontSize = 12.sp)
                                    }
                                    Row {
                                        Text(text = "BNC  ", fontSize = 12.sp, modifier = Modifier.padding(end = 2.dp))
                                        Text(text = ":${word.bnc}", fontSize = 12.sp)
                                    }

                                    Text(text = "COCA:${word.frq}", fontSize = 12.sp)
                                    Divider()
                                    Text(
                                        text = word.translation,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                    )

                                    if (word.captions.isNotEmpty()) {
                                        Divider()
                                        word.captions.forEachIndexed { index, caption ->
                                            val top = if (index == 0) 5.dp else 0.dp
                                            Text(
                                                text = caption.content,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = top)
                                            )
                                        }

                                    }
                                }
                            }
                        },
                        delayMillis = 50,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.BottomCenter,
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(7.5.dp),
                            elevation = 3.dp
                        ) {
                            var closeVisible by remember { mutableStateOf(false) }
                            Box(Modifier.size(width = 130.dp, height = 65.dp)
                                .onPointerEvent(PointerEventType.Enter) {
                                    closeVisible = true
                                }
                                .onPointerEvent(PointerEventType.Exit) {
                                    closeVisible = false
                                }) {
                                Text(
                                    text = word.value,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colors.onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                if (closeVisible) {
                                    Icon(
                                        Icons.Filled.Close, contentDescription = "",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.clickable { removeWord(word) }.align(Alignment.TopEnd)
                                    )
                                }


                            }
                        }
                    }
                }
            }

// 相关 Issue: https://github.com/JetBrains/compose-jb/issues/2029
//            VerticalScrollbar(
//                style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
//                modifier = Modifier.align(Alignment.CenterEnd)
//                    .fillMaxHeight(),
//                adapter = rememberScrollbarAdapter(
//                    scrollState = listState
//                )
//            )


        }
    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    selectedFileList: List<File>,
    updateOrder: (List<File>) -> Unit,
    tasksState: Map<File,Boolean>,
    currentTask:File?,
    errorMessages:Map<File,String>,
    selectable:Boolean,
    checkedFileMap:Map<File,Boolean>,
    checkedChange:(Pair<File,Boolean>) -> Unit,
) {

    val items = remember { mutableStateOf(selectedFileList) }
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        items.value = items.value.toMutableList().apply {
            add(to.index, removeAt(from.index))
            updateOrder(items.value)
        }
    })


    Box (modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
        LazyColumn(
            state = state.listState,
            modifier = Modifier.fillMaxSize().reorderable(state)
        ) {
            items(items.value, { it }) { item ->
                ReorderableItem(state, orientationLocked = true, key = item) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)
                    Column(
                        modifier = Modifier
                            .shadow(elevation.value)
                            .background(MaterialTheme.colors.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { }
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .detectReorder(state)
                        ) {
                            Text(
                                text = item.nameWithoutExtension,
                                modifier = Modifier.align(Alignment.CenterStart).padding(top = 16.dp, bottom = 16.dp),
                                color = MaterialTheme.colors.onBackground
                            )

                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterEnd)){
                                if(selectable){
                                    val checked = checkedFileMap[item]

                                    Checkbox(
                                        checked = checked == true,
                                        onCheckedChange = { checkedChange(Pair(item,it)) }
                                    )
                                }


                                if(tasksState[item] == true){
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = "完成", modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.TopCenter,
                                            alignment = Alignment.TopCenter,
                                            offset = DpOffset.Zero
                                        ),
//                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.TaskAlt,
                                                contentDescription = "",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                    }


                                }else if(item == currentTask){
                                    CircularProgressIndicator(Modifier
//                                        .align(Alignment.CenterEnd)
                                        .padding(start = 8.dp,end = 16.dp).width(24.dp).height(24.dp))
                                }else if(tasksState[item] == false){

                                    val text = errorMessages[item].orEmpty()
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = text, modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 300,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.CenterStart,
                                            alignment = Alignment.CenterStart,
                                            offset = DpOffset.Zero
                                        ),
//                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(
                                                imageVector = Icons.Outlined.Error,
                                                contentDescription = "",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }



                        }
                        Divider()
                    }
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(state.listState)
        )
    }


}

@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun readDocument(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val file = File(pathName)
    var text = ""
    val extension = file.extension
    val otherExtensions = listOf("txt", "java", "cs", "cpp", "c", "kt", "js", "py", "ts")

    try{
        if (extension == "pdf") {
            setProgressText("正在加载文档")
            val document: PDDocument = PDDocument.load(file)
            //Instantiate PDFTextStripper class
            val pdfStripper = PDFTextStripper()
            text = pdfStripper.getText(document)
            document.close()
        } else if (otherExtensions.contains(extension)) {
            text = file.readText()
        }
    }catch (exception: InvalidPasswordException){
        JOptionPane.showMessageDialog(null,exception.message)
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }


    val set: MutableSet<String> = HashSet()
    val list = mutableListOf<String>()
    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        val model = TokenizerModel(inputStream)
        setProgressText("正在分词")
        val tokenizer: Tokenizer = TokenizerME(model)
        val tokenize = tokenizer.tokenize(text)
        setProgressText("正在处理特殊分隔符")
        tokenize.forEach { word ->
            val lowercase = word.lowercase(Locale.getDefault())
            // 在代码片段里的关键字之间用.符号分隔
            if (lowercase.contains(".")) {
                val split = lowercase.split("\\.").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            // 还有一些关键字之间用 _ 符号分隔
            if (lowercase.matches(Regex("_"))) {
                val split = lowercase.split("_").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            if (!set.contains(lowercase)) {
                list.add(lowercase)
                set.add(lowercase)
            }
            set.add(lowercase)
        }

    }

    setProgressText("从文档提取出 ${set.size} 个单词，正在批量查询单词，如果词典里没有的就丢弃")
    val validList = Dictionary.queryList(list)
    setProgressText("${validList.size} 个有效单词")
    setProgressText("")
    return validList
}


// 提取 srt 字幕 ffmpeg -i input.mkv -map "0:2" output.eng.srt
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
private fun readSRT(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val map: MutableMap<String, MutableList<Caption>> = HashMap()
    // 保存顺序
    val orderList = mutableListOf<String>()
    try{
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { input ->
            val model = TokenizerModel(input)
            val tokenizer: Tokenizer = TokenizerME(model)
            val formatSRT = FormatSRT()
            val file = File(pathName)
            val encoding = UniversalDetector.detectCharset(file)
            val charset =  if(encoding != null){
                Charset.forName(encoding)
            }else{
                Charset.defaultCharset()
            }
            val inputStream: InputStream = FileInputStream(file)

            setProgressText("正在解析字幕文件")
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream,charset)

            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList: Collection<subtitleFile.Caption> = captions.values
            setProgressText("正在分词")
            for (caption in captionList) {
                var content = replaceSpecialCharacter(caption.content)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    // getTime(format) 返回的时间不能播放
                    start = caption.start.getTime("hh:mm:ss.ms"),
                    end = caption.end.getTime("hh:mm:ss.ms"),
                    content = content
                )
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    val lowercase = word.lowercase(Locale.getDefault())
                    if (!map.containsKey(lowercase)) {
                        val list = mutableListOf(dataCaption)
                        map[lowercase] = list
                        orderList.add(lowercase)
                    } else {
                        if (map[lowercase]!!.size < 3 && !map[lowercase]!!.contains(dataCaption)) {
                            map[lowercase]?.add(dataCaption)
                        }
                    }
                }
            }
        }
        setProgressText("从字幕文件中提取出 ${orderList.size} 个单词，正在批量查询单词，如果词典里没有就丢弃")
        val validList = Dictionary.queryList(orderList)
        setProgressText("${validList.size} 个有效单词")
        validList.forEach { word ->
            if (map[word.value] != null) {
                word.captions = map[word.value]!!
            }
        }
        setProgressText("")
        return validList
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }
    return listOf()
}


@OptIn(ExperimentalComposeUiApi::class)
private fun readMKV(
    pathName: String,
    trackId: Int,
    setProgressText: (String) -> Unit,
): List<Word> {
    val map: MutableMap<String, ArrayList<Caption>> = HashMap()
    val orderList = mutableListOf<String>()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        setProgressText("正在解析 MKV 文件")

        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return listOf()
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            return listOf()
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()


        /**
         *   OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
         *       for (int i = 0; i < reader.getSubtitles().size(); i++) {
         *         if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
         *           SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
         *           System.out.println(subs.getHeader());
         *         }
         *       }
         *
         *
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }
        setProgressText("正在分词")
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            val model = TokenizerModel(inputStream)
            val tokenizer: Tokenizer = TokenizerME(model)
            val subtitle = reader.subtitles[trackId]
            var isASS = false
            if (subtitle is SSASubtitles) {
                isASS = true
            }

            val captionList = subtitle.readUnreadSubtitles()
            for (caption in captionList) {
                val captionContent =  if(isASS){
                   caption.formattedVTT.replace("\\N","\n")
                }else{
                    caption.stringData
                }

                var content = replaceSpecialCharacter(captionContent)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    start = caption.startTime.format().toString(),
                    end = caption.endTime.format(),
                    content = content
                )

                content = content.lowercase(Locale.getDefault())
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    if (!map.containsKey(word)) {
                        val list = ArrayList<Caption>()
                        list.add(dataCaption)
                        map[word] = list
                        orderList.add(word)
                    } else {
                        if (map[word]!!.size < 3 && !map[word]!!.contains(dataCaption)) {
                            map[word]!!.add(dataCaption)
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        JOptionPane.showMessageDialog(null,e.message)
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    setProgressText("从视频中提取出${orderList.size}个单词，正在批量查询单词，如果词典里没有就丢弃")
    val validList = Dictionary.queryList(orderList)
    setProgressText("${validList.size}个有效单词")
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    setProgressText("")
    return validList
}

/**
 * 批量读取 MKV
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun batchReadMKV(
    language:String,
    selectedFileList:(List<File>),
    setCurrentTask:(File?) -> Unit,
    setErrorMessages:(Map<File,String>) -> Unit,
    updateTaskState:(Pair<File,Boolean>) -> Unit
):List<Word>{
    val errorMessage = mutableMapOf<File,String>()
    val map: MutableMap<String, ArrayList<ExternalCaption>> = HashMap()
    val orderList = mutableListOf<String>()


    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { tokensInputStream ->
        ResourceLoader.Default.load("opennlp/langdetect-183.bin").use { langdetectInputStream ->
            // 训练分词器
            val tokensModel = TokenizerModel(tokensInputStream)
            val tokenizer: Tokenizer = TokenizerME(tokensModel)

            // 训练语言检测器
            val langModel = LanguageDetectorModel(langdetectInputStream)
            val languageDetector: LanguageDetector = LanguageDetectorME(langModel)

            val englishIetfList = listOf("en","en-US","en-GB")
            val english = listOf("en","eng")
            for(file in selectedFileList){
                setCurrentTask(file)
                var reader: EBMLReader? = null
                try {
                    reader = EBMLReader(file.absolutePath)
                    if (!reader.readHeader()) {
                        println("This is not an mkv file!")
                        errorMessage[file] = "不是 MKV 文件"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                       continue
                    }

                    reader.readTracks()
                    val numSubtitles: Int = reader.subtitles.size
                    if (numSubtitles == 0) {
                        errorMessage[file] = "没有字幕"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                        continue
                    }
                    reader.readCues()
                    for (i in 0 until reader.cuesCount) {
                        reader.readSubtitlesInCueFrame(i)
                    }

                    var trackID = -1

                    for(i in 0 until reader.subtitles.size){
                        val subtitles = reader.subtitles[i]
                        if (englishIetfList.contains(subtitles.languageIetf) || english.contains(subtitles.language)) {
                            trackID = i
                            break
                        } else {
                            // 使用 OpenNLP 的语言检测工具检测字幕的语言
                            var content = ""
                            val subList = subtitles.readUnreadSubtitles().subList(0,10)
                            subList.forEach { caption ->
                                content += caption.stringData
                            }
                            val lang  = languageDetector.predictLanguage(content)
                            if(lang.lang == "eng"){
                                trackID = i
                                break
                            }
                        }
                    }

                    if (trackID != -1) {
                        val subtitle = reader.subtitles[trackID]
                        var isASS = false
                        if (subtitle is SSASubtitles) {
                            isASS = true
                        }
                        val captionList = subtitle.allReadCaptions
                        if(captionList.isEmpty()){
                            captionList.addAll(subtitle.readUnreadSubtitles())
                        }
                        for (caption in captionList) {
                            val captionContent =  if(isASS){
                                caption.formattedVTT.replace("\\N","\n")
                            }else{
                                caption.stringData
                            }
                            var content = replaceSpecialCharacter(captionContent)
                            content = removeLocationInfo(content)
                            val externalCaption = ExternalCaption(
                                relateVideoPath = file.absolutePath,
                                subtitlesTrackId = trackID,
                                subtitlesName = file.nameWithoutExtension,
                                start = caption.startTime.format().toString(),
                                end = caption.endTime.format(),
                                content = content
                            )

                            content = content.lowercase(Locale.getDefault())
                            val tokenize = tokenizer.tokenize(content)
                            for (word in tokenize) {
                                if (!map.containsKey(word)) {
                                    val list = ArrayList<ExternalCaption>()
                                    list.add(externalCaption)
                                    map[word] = list
                                    orderList.add(word)
                                } else {
                                    if (map[word]!!.size < 3 && !map[word]!!.contains(externalCaption)) {
                                        map[word]!!.add(externalCaption)
                                    }
                                }
                            }
                        }
                        updateTaskState(Pair(file, true))
                    } else {
                        errorMessage[file] = "没有找到英语字幕"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                        continue
                    }

                } catch (exception: IOException) {
                    updateTaskState(Pair(file, false))
                    setCurrentTask(null)
                    if(exception.message != null){
                        errorMessage[file] = exception.message.orEmpty()
                   } else{
                        errorMessage[file] =  "IO 异常"
                    }
                    exception.printStackTrace()
                    continue
                }catch (exception: UnSupportSubtitlesException){
                    updateTaskState(Pair(file, false))
                   if(exception.message != null){
                       errorMessage[file] = exception.message.orEmpty()
                   } else {
                       errorMessage[file] = "字幕格式不支持"
                   }
                    exception.printStackTrace()
                    setCurrentTask(null)
                    continue
                } catch (exception:NullPointerException){
                    updateTaskState(Pair(file, false))
                    errorMessage[file] = "空指针异常"
                    exception.printStackTrace()
                    setCurrentTask(null)
                    continue
                }finally {
                    try {
                        reader?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }


    val validList = Dictionary.queryList(orderList)
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.externalCaptions = map[word.value]!!
        }
    }

    setErrorMessages(errorMessage)
return validList
}

/**
 * 替换一些特殊字符
 */
fun replaceSpecialCharacter(captionContent: String): String {
    var content = captionContent
    if (content.startsWith("-")) content = content.substring(1)
    if (content.contains("<i>")) {
        content = content.replace("<i>", "")
    }
    if (content.contains("</i>")) {
        content = content.replace("</i>", "")
    }
    if (content.contains("<br />")) {
        content = content.replace("<br />", " ")
    }
    content = removeLocationInfo(content)
    return content
}

/** 有一些字幕并不是在一个的固定位置，而是标注在人物旁边，这个函数删除位置信息 */
fun removeLocationInfo(content: String): String {
    val pattern = Pattern.compile("\\{.*\\}")
    val matcher = pattern.matcher(content)
    return matcher.replaceAll("")
}

fun removeItalicSymbol(content: String): String {
    var string = content
    if (string.contains("<i>")) {
        string = string.replace("<i>", "")
    }
    if (string.contains("</i>")) {
        string = string.replace("</i>", "")
    }
    return string
}

fun replaceNewLine(content: String): String {
    var string = content
    if (string.contains("\r\n")) {
        string = string.replace("\r\n", " ")
    }
    if (string.contains("\n")) {
        string = string.replace("\n", " ")
    }
    if (string.contains("<br />")) {
        string = string.replace("<br />", " ")
    }
    if (string.endsWith(" ")){
        string = string.substring(0,string.length-1)
    }
    return string
}
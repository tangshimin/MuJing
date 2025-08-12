package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.MediaInfo
import player.PlayerBox
import player.isMacOS
import state.AppState
import state.getResourcesFile
import ui.edit.computeNameMap
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.createDragAndDropTarget
import util.shouldStartDragAndDrop
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView

/**
 * 链接字幕词库窗口
 * 把字幕词库链接到文档词库
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun LinkVocabularyDialog(
    appState: AppState,
    close: () -> Unit
) {
    /**
     * 协程构建器
     */
    val scope = rememberCoroutineScope()

    /**
     * 要链接的词库,通常是内置词库
     */
    var vocabulary by remember{ mutableStateOf<MutableVocabulary?>(null) }



    /**
     * 选择的字幕词库所在的文件夹的绝对路径
     */
    var vocabularyDir by remember { mutableStateOf("") }

    /**
     * 要链接的词库所在的文件夹的绝对路径,比如要给四级词库链接字幕，这个地址就是四级词库所在的文件夹
     */
    var directoryPath by remember { mutableStateOf("") }

    /**
     * 当前词库链接到字幕词库的字幕的数量
     */
    var linkCounter by remember { mutableStateOf(0) }

    /**
     * 准备链接的单词和字幕
     */
    val prepareLinks = remember { mutableStateMapOf<String, List<ExternalCaption>>() }


    /**
     * 字幕名称
     */
    var subtitlesName by remember { mutableStateOf("") }

    var vocabularyType by remember { mutableStateOf(VocabularyType.DOCUMENT) }
    var vocabularyWrong by remember { mutableStateOf(false) }
    var extractCaptionResultInfo by remember { mutableStateOf("") }
    var saveEnable by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }

    /**
     * 点击【链接】后执行的回调函数
     */
    val import: () -> Unit = {
        if (prepareLinks.isNotEmpty()) {
            vocabulary?.wordList?.forEach { word ->
                val links = prepareLinks[word.value]
                if (!links.isNullOrEmpty()) {
                    word.externalCaptions.addAll(links)
                }
            }
            saveEnable = true
        }
    }

    val clear: () -> Unit = {
        linkCounter = 0
        prepareLinks.clear()
        subtitlesName = ""
        extractCaptionResultInfo = ""
        vocabularyWrong = false
        vocabularyType = VocabularyType.DOCUMENT

    }

    /**
     * 用户选择字幕词库后，用这个函数提取相关信息
     */
    val extractCaption: (File) -> Unit = {
        scope.launch (Dispatchers.Default){
                val selectedVocabulary = loadVocabulary(it.absolutePath)
                subtitlesName = if (selectedVocabulary.type == VocabularyType.SUBTITLES) selectedVocabulary.name else ""
                vocabularyType = selectedVocabulary.type
                var linkedCounter = 0

                // 字幕词库或 MKV 词库，字幕保存在单词的 captions 属性中
                if (selectedVocabulary.type != VocabularyType.DOCUMENT) {
                    val wordCaptionsMap = HashMap<String, List<Caption>>()
                    selectedVocabulary.wordList.forEach { word ->
                        wordCaptionsMap.put(word.value, word.captions)
                    }
                    vocabulary?.wordList?.forEach { word ->
                        if (wordCaptionsMap.containsKey(word.value.lowercase(Locale.getDefault()))) {
                            val captions = wordCaptionsMap[word.value]
                            val links = mutableListOf<ExternalCaption>()
                            // 用于预览
                            // 字幕最多3条，这个 counter 是剩余的数量
                            var counter = 3 - word.externalCaptions.size
                            if (counter in 1..3) {
                                captions?.forEachIndexed { _, caption ->

                                    val externalCaption = ExternalCaption(
                                        selectedVocabulary.relateVideoPath,
                                        selectedVocabulary.subtitlesTrackId,
                                        subtitlesName,
                                        caption.start,
                                        caption.end,
                                        caption.content
                                    )

                                    if (counter != 0) {
                                        if (!word.externalCaptions.contains(externalCaption) && !links.contains(
                                                externalCaption
                                            )
                                        ) {
                                            links.add(externalCaption)
                                            counter--
                                        } else {
                                            linkedCounter++
                                        }
                                    }
                                }
                            } else {

                                // 字幕已经有3条了，查询是否有一样的
                                captions?.forEachIndexed { _, caption ->
                                    val externalCaption = ExternalCaption(
                                        selectedVocabulary.relateVideoPath,
                                        selectedVocabulary.subtitlesTrackId,
                                        subtitlesName,
                                        caption.start,
                                        caption.end,
                                        caption.content
                                    )

                                    if (word.externalCaptions.contains(externalCaption)) {
                                        linkedCounter++
                                    }
                                }
                            }
                            if (links.isNotEmpty()) {
                                prepareLinks.put(word.value, links)
                                linkCounter += links.size
                            }

                        }
                    }

                } else {
                    // 文档词库，字幕保存在单词的 externalCaptions 属性中
                    val wordCaptionsMap = HashMap<String, List<ExternalCaption>>()
                    selectedVocabulary.wordList.forEach { word ->
                        wordCaptionsMap.put(word.value, word.externalCaptions)
                    }
                    vocabulary?.wordList?.forEach { word ->
                        if (wordCaptionsMap.containsKey(word.value.lowercase(Locale.getDefault()))) {
                            val externalCaptions = wordCaptionsMap[word.value]
                            val links = mutableListOf<ExternalCaption>()
//                        // 用于预览
                            // 字幕最多3条，这个 counter 是剩余的数量
                            var counter = 3 - word.externalCaptions.size
                            if (counter in 1..3) {
                                externalCaptions?.forEachIndexed { _, externalCaption ->
                                    if (counter != 0) {
                                        if (!word.externalCaptions.contains(externalCaption) && !links.contains(
                                                externalCaption
                                            )
                                        ) {
                                            links.add(externalCaption)
                                            counter--
                                        } else {
                                            linkedCounter++
                                        }
                                    }
                                }
                            } else {
                                // 字幕已经有3条了，查询是否有一样的
                                externalCaptions?.forEachIndexed { _, externalCaption ->
                                    if (word.externalCaptions.contains(externalCaption)) {
                                        linkedCounter++
                                    }
                                }
                            }
                            if (links.isNotEmpty()) {
                                prepareLinks.put(word.value, links)
                                linkCounter += links.size
                            }

                        }
                    }
                }

                // previewWords isEmpty 有两种情况：
                // 1. 已经链接了一次。
                // 2. 没有匹配的字幕
                if (prepareLinks.isEmpty()) {
                    extractCaptionResultInfo = if (linkedCounter == 0) {
                        "没有匹配的字幕，请重新选择"
                    } else {
                        "${selectedVocabulary.name} 有${linkedCounter}条相同的字幕已经链接，请重新选择"
                    }
                    vocabularyWrong = true
                }
        }

    }

    /**
     * 处理输入的文件
     */
    val handleInputFile:(File) -> Unit = {file ->
        // 选择文档词库或内置词库
        if(vocabulary == null){
            val newVocabulary =  MutableVocabulary(loadVocabulary(file.absolutePath))
            if(newVocabulary.type != VocabularyType.DOCUMENT){
                JOptionPane.showMessageDialog(null,
                    "词库的类型不对。\n" +
                        "不能选择用字幕或MKV视频生成的词库。\n" +
                        "如果要链接两个有字幕的词库，请选择合并词库。"
                )
            }else{
                vocabulary = newVocabulary
            }
            directoryPath = file.parentFile.absolutePath
            // 选择字幕词库
        }else{
            vocabularyDir = file.parentFile.absolutePath
            extractCaption(file)
        }
    }



    DialogWindow(
        title = "链接字幕词库",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = {
            clear()
            close()
        },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 650.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()

        // 拖放处理函数
        val dropTarget = remember {
            createDragAndDropTarget { files ->
                val file = files.first()
                scope.launch {
                    if (file.extension == "json") {
                        handleInputFile(file)
                    } else {
                        JOptionPane.showMessageDialog(window, "词库的格式不正确")
                    }
                }
            }
        }


        /** 保存词库 */
        val save:() -> Unit = {
            scope.launch (Dispatchers.IO){

                val fileChooser = appState.futureFileChooser.get()
                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                fileChooser.dialogTitle = "保存词库"
                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                val appVocabulary = getResourcesFile("vocabulary")
                val parent = if (directoryPath.startsWith(appVocabulary.absolutePath)) {
                    myDocuments
                } else directoryPath
                fileChooser.selectedFile = File("$parent${File.separator}${vocabulary?.name}.json")
                val userSelection = fileChooser.showSaveDialog(window)
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    val fileToSave = fileChooser.selectedFile
                    try{
                        if (vocabulary != null) {
                            vocabulary!!.name = fileToSave.nameWithoutExtension
                            saveVocabulary(vocabulary!!.serializeVocabulary, fileToSave.absolutePath)
                        }
                        vocabulary = null
                        fileChooser.selectedFile = null
                        clear()
                        close()
                    }catch(e:Exception){
                        e.printStackTrace()
                        JOptionPane.showMessageDialog(window,"保存词库失败,错误信息：\n${e.message}")
                    }

                }
            }
        }

        WindowDraggableArea {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
                modifier = Modifier.dragAndDropTarget(
                    shouldStartDragAndDrop =shouldStartDragAndDrop,
                    target = dropTarget
                )
            ) {
                Box(Modifier.fillMaxSize()) {
                    Divider(Modifier.align(Alignment.TopCenter))
                    if (prepareLinks.isEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize().align(Alignment.Center)
                        ) {
                            // 当前词库已经链接的外部字幕
                            val externalNameMap = remember { mutableStateMapOf<String,Int>() }
                            var deleted by remember{ mutableStateOf(false)}

                            LaunchedEffect(vocabulary){
                                if(vocabulary!= null){
                                    computeNameMap(vocabulary!!.wordList, externalNameMap)
                                }
                            }
                            LaunchedEffect(deleted){
                                if(vocabulary!= null){
                                    if(deleted){
                                        externalNameMap.clear()
                                        computeNameMap(vocabulary!!.wordList, externalNameMap)
                                        deleted = false
                                    }
                                }

                            }

                            Column(Modifier.width(IntrinsicSize.Max)) {
                                if(vocabulary != null){
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) { Text(vocabulary!!.name) }
                                    val bottom = if(externalNameMap.isEmpty()) 50.dp else 0.dp
                                    Divider(Modifier.padding(bottom = bottom))
                                }
                                if (externalNameMap.isNotEmpty()) {
                                    val boxHeight by remember(externalNameMap.size){
                                        derivedStateOf {
                                            val size = externalNameMap.size
                                            if(size <= 5){
                                                size * 48.dp
                                            }else{
                                                240.dp
                                            }
                                        }
                                    }

                                    Box(Modifier.height(boxHeight).fillMaxWidth()){
                                        val stateVertical = rememberScrollState(0)
                                        Column (Modifier.verticalScroll(stateVertical)){
                                            externalNameMap.forEach { (path, count) ->
                                                var showConfirmationDialog by remember { mutableStateOf(false) }
                                                Row(verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable{}){
                                                    val name = File(path).nameWithoutExtension
                                                    if (showConfirmationDialog) {
                                                        ConfirmDialog(
                                                            message = "确定要删除 $name 的所有字幕吗?",
                                                            confirm = {
                                                                vocabulary?.wordList?.forEach { word ->
                                                                    val tempList = mutableListOf<ExternalCaption>()
                                                                    word.externalCaptions.forEach { externalCaption ->
                                                                        if (externalCaption.relateVideoPath == path || externalCaption.subtitlesName == path) {
                                                                            tempList.add(externalCaption)
                                                                        }
                                                                    }
                                                                    word.externalCaptions.removeAll(tempList)
                                                                }
                                                                // 如果选择的词库有问题，提示用户词库错误，删除词库后就取消提示错误。？
                                                                if (
                                                                    subtitlesName == path) {
                                                                    vocabularyWrong = false
                                                                }
                                                                showConfirmationDialog = false
                                                                saveEnable = true
                                                                deleted = true
                                                            },
                                                            close = { showConfirmationDialog = false }
                                                        )
                                                    }

                                                    Text(
                                                        text = name,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(250.dp).padding(end = 10.dp)
                                                    )
                                                    Text("$count", modifier = Modifier.width(60.dp))
                                                    IconButton(onClick = { showConfirmationDialog = true },
                                                        modifier = Modifier.padding(end = 10.dp)) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "",
                                                            tint = MaterialTheme.colors.onBackground
                                                        )
                                                    }

                                                }
                                            }
                                        }

                                        VerticalScrollbar(
                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                            adapter = rememberScrollbarAdapter(stateVertical),
                                        )
                                    }

                                }

                            }
                            if (vocabularyWrong) {
                                if (extractCaptionResultInfo.isNotEmpty()) {
                                    Text(
                                        text = extractCaptionResultInfo,
                                        color = Color.Red,
                                        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                                    )
                                }

                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    enabled = Objects.isNull(vocabulary),
                                    onClick = {
                                        showFilePicker = true
                                        vocabularyWrong = false

                                }) {
                                    Text("1 选择词库")
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(
                                    enabled = !Objects.isNull(vocabulary),
                                    onClick = {
                                        showFilePicker = true
                                        vocabularyWrong = false

                                }) {
                                    Text("2 选择字幕词库")
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = { save() }, enabled = saveEnable) {
                                    Text("保存")
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = {
                                    clear()
                                    close()
                                }) {
                                    Text("取消")
                                }
                            }
                            val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
                            FilePicker(
                                show = showFilePicker,
                                fileExtensions = extensions,
                                initialDirectory = ""
                            ){pickFile ->
                                if(pickFile != null){
                                    if(pickFile.path.isNotEmpty()){
                                        val file = File(pickFile.path)
                                        handleInputFile(file)
                                    }
                                }
                                showFilePicker = false
                            }
                        }

                        Column (
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)){
                            Text("提示：不要把词库保存到应用程序的安装目录")
                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        elevation = 4.dp,
                                        border = BorderStroke(1.dp,
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                        shape = RectangleShape
                                    ) {
                                        Text(text = "帮助", modifier = Modifier.padding(10.dp))
                                    }
                                },
                                delayMillis = 50,
                                tooltipPlacement = TooltipPlacement.ComponentRect(
                                    anchor = Alignment.BottomCenter,
                                    alignment = Alignment.BottomCenter,
                                    offset = DpOffset.Zero
                                )
                            ) {
                                var documentWindowVisible by remember { mutableStateOf(false) }
                                var currentPage by remember { mutableStateOf("linkVocabulary") }
                                IconButton(onClick = {
                                    documentWindowVisible = true
                                }){
                                    Icon(
                                        Icons.Filled.Help,
                                        contentDescription = "Localized description",
                                        tint =if(MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                                    )
                                }


                                if(documentWindowVisible){
                                    DocumentWindow(
                                        close = {documentWindowVisible = false},
                                        currentPage = currentPage,
                                        setCurrentPage = {currentPage = it}

                                    )
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().align(Alignment.Center)) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)
                            ) {
                                Text("总共${prepareLinks.size}个单词,${linkCounter}条字幕")
                            }
                            Divider()
                            Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                                val scrollState = rememberLazyListState()
                                LazyColumn(Modifier.fillMaxSize(), scrollState) {

                                    items(prepareLinks.toList()) { (word, captions) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                                                .padding(start = 10.dp, end = 10.dp)
                                        ) {

                                            Text(text = word, modifier = Modifier.width(150.dp))
                                            Divider(Modifier.width(1.dp).fillMaxHeight())
                                            Column(verticalArrangement = Arrangement.Center) {
                                                captions.forEachIndexed { index, externalCaption ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}. ${externalCaption.content}",
                                                            modifier = Modifier.padding(5.dp)
                                                        )
                                                        val caption = Caption(
                                                            externalCaption.start,
                                                            externalCaption.end,
                                                            externalCaption.content
                                                        )
                                                        val mediaInfo = MediaInfo(
                                                            caption =caption,
                                                            mediaPath =externalCaption.relateVideoPath,
                                                            trackId =externalCaption.subtitlesTrackId
                                                        )

                                                        PlayerBox(
                                                            mediaInfo = mediaInfo,
                                                            vocabularyDir = File(vocabularyDir),
                                                            volume = appState.global.videoVolume
                                                        )
                                                    }
                                                }

                                            }
                                        }
                                        Divider()
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                )
                            }

                            Divider()
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp)
                            ) {
                                OutlinedButton(onClick = {
                                    import()
                                    clear()
                                }) {
                                    Text("链接")
                                }
                                Spacer(Modifier.width(20.dp))
                                OutlinedButton(onClick = { clear() }) {
                                    Text("取消")
                                }
                            }
                        }
                    }
                }


            }
        }
    }
}
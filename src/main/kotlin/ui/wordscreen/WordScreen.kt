package ui.wordscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import state.ScreenType
import state.getResourcesFile
import theme.LocalCtrl
import tts.AzureTTS
import tts.rememberAzureTTS
import ui.components.MacOSTitle
import ui.components.RemoveButton
import ui.components.Toolbar
import ui.dialog.*
import ui.wordscreen.MemoryStrategy.*
import util.computeVideoSize
import util.rememberMonospace
import util.shouldStartDragAndDrop
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule

/**
 * 应用程序的核心组件，记忆单词界面
 * @param appState 应用程序的全局状态
 * @param wordScreenState 记忆单词界面的状态容器
 * @param videoBounds 视频播放窗口的位置和大小
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WordScreen(
    window: ComposeWindow,
    title: String,
    appState: AppState,
    wordScreenState: WordScreenState,
    videoBounds: Rectangle,
    showPlayer :(Boolean) -> Unit,
    openVideo: (String, String) -> Unit,
    showContext :(MediaInfo) -> Unit
) {

    // 拖放处理函数
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // 处理拖放事件
                val transferable = event.awtTransferable
                // 获取拖放的文件列表，并过滤出 File 类型，避免类型转换警告和异常
                val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)?.filterIsInstance<File>() ?: emptyList()
                if(files.isNotEmpty()){
                    val file = files[0]
                    if(file.isDirectory){
                        JOptionPane.showMessageDialog(window, "不能拖放目录，请拖放文件。")
                        return false
                    }
                    if(file.extension == "json"){
                        // 如果是词库文件，打开词库
                        if (wordScreenState.vocabularyPath != file.absolutePath) {
                            val index = appState.findVocabularyIndex(file)
                            appState.changeVocabulary(file,wordScreenState,index)
                        } else {
                            JOptionPane.showMessageDialog(window, "词库已打开")
                        }
                    }else if(file.extension == "mkv" || file.extension == "mp4" ){
                        openVideo(file.absolutePath, wordScreenState.vocabularyPath)
                    }else{
                        JOptionPane.showMessageDialog(window, "只支持拖放词库文件或视频文件。")
                    }
                    return true
                }

                return false
            }
        }
    }
    Box(Modifier
        .background(MaterialTheme.colors.background)
        .dragAndDropTarget(
            shouldStartDragAndDrop =shouldStartDragAndDrop,
            target = dropTarget
        )
    ) { ->
        /** 单词输入框的焦点请求器*/
        val wordFocusRequester = remember { FocusRequester() }
        /** 当前正在记忆的单词 */
        val currentWord = if(wordScreenState.vocabulary.wordList.isNotEmpty()){
            wordScreenState.getCurrentWord()
        }else  null

        val  wordRequestFocus: () -> Unit = {
            if(currentWord != null){
                wordFocusRequester.requestFocus()
            }
        }
        var showFilePicker by remember {mutableStateOf(false)}
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        var documentWindowVisible by remember { mutableStateOf(false) }
        var generateVocabularyListVisible by remember { mutableStateOf(false) }
        val openChooseVocabulary:(String) ->Unit = { path ->
            val file = File(path)
            val index = appState.findVocabularyIndex(file)
            val changed = appState.changeVocabulary(
                vocabularyFile = file,
                wordScreenState,
                index
            )
            if(changed){
                appState.global.type = ScreenType.WORD
                appState.saveGlobalState()
            }
        }

        Row {
            val dictationState = rememberDictationState()
            val azureTTS = rememberAzureTTS()
            WordScreenSidebar(
                appState = appState,
                wordScreenState = wordScreenState,
                dictationState = dictationState,
                wordRequestFocus = wordRequestFocus,
                azureTTS = azureTTS
                )

            Box(Modifier.fillMaxSize()) {
                if (currentWord != null) {
                    MainContent(
                        appState =appState,
                        wordScreenState = wordScreenState,
                        dictationState = dictationState,
                        azureTTS = azureTTS,
                        currentWord = currentWord,
                        videoBounds = videoBounds,
                        wordFocusRequester = wordFocusRequester,
                        window = window,
                        openVocabulary = { showFilePicker = true },
                        showContext = showContext,
                    )
                } else {
                    VocabularyEmpty(
                        openVocabulary = { showFilePicker = true },
                        openBuiltInVocabulary = {showBuiltInVocabulary = true},
                        generateVocabulary = {generateVocabularyListVisible = true},
                        openDocument = {documentWindowVisible = true},
                        parentWindow = window,
                        futureFileChooser = appState.futureFileChooser,
                        openChooseVocabulary = openChooseVocabulary
                    )
                }

                Header(
                    wordScreenState = wordScreenState,
                    title = title,
                    window = window,
                    wordRequestFocus = wordRequestFocus,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                )
            }
        }

        Row( modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = appState.openSidebar,
                setIsOpen = {
                    appState.openSidebar = it
                    if(!it && currentWord != null){
                        wordFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier,
                globalState = appState.global,
                saveGlobalState = {appState.saveGlobalState()},
                showPlayer = showPlayer,
                openSearch = appState.openSearch,
            )
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shortcutText = if (isMacOS()) "$ctrl O" else "$ctrl+O"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "打开词库文件  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText)
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

                IconButton(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.padding(top = if (isMacOS()) 44.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            RemoveButton(onClick = {
                wordScreenState.index = 0
                wordScreenState.vocabulary.size = 0
                wordScreenState.vocabulary.name = ""
                wordScreenState.vocabulary.relateVideoPath = ""
                wordScreenState.vocabulary.wordList.clear()
                wordScreenState.vocabularyName = ""
                wordScreenState.vocabularyPath = ""
                wordScreenState.saveWordScreenState()
            }, toolTip = "关闭当前词库")
            val extensions = if(isMacOS()) listOf("public.json") else listOf("json")

            FilePicker(
                show = showFilePicker,
                fileExtensions = extensions,
                initialDirectory = ""){pfile ->
                if(pfile != null){
                    if(pfile.path.isNotEmpty()){
                        openChooseVocabulary(pfile.path)
                    }
                }

                showFilePicker = false
            }
        }


        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            openChooseVocabulary = openChooseVocabulary,
            futureFileChooser = appState.futureFileChooser
        )

        GenerateVocabularyListDialog(
            appState = appState,
            show = generateVocabularyListVisible,
            close = {generateVocabularyListVisible = false}
        )

        var currentPage by remember { mutableStateOf("features") }
        if(documentWindowVisible){
            DocumentWindow(
                close = {documentWindowVisible = false},
                currentPage = currentPage,
                setCurrentPage = {currentPage = it}
            )
        }
    }

}


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Header(
    wordScreenState: WordScreenState,
    title:String,
    window: ComposeWindow,
    wordRequestFocus: () -> Unit,
    modifier: Modifier
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ){
        // macOS 的标题栏和 windows 不一样，需要特殊处理
        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center){
            // 记忆单词时的状态信息
            val text = when(wordScreenState.memoryStrategy){
                Normal -> { if(wordScreenState.vocabulary.size>0) "${wordScreenState.index + 1}/${wordScreenState.vocabulary.size}" else ""}
                Dictation -> { "听写单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.dictationWords.size}"}
                DictationTest -> {"听写测试   ${wordScreenState.dictationIndex + 1}/${wordScreenState.reviewWords.size}"}
                NormalReviewWrong -> { "复习错误单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
                DictationTestReviewWrong -> { "听写测试 - 复习错误单词   ${wordScreenState.dictationIndex + 1}/${wordScreenState.wrongWords.size}"}
            }

            val top = if(wordScreenState.memoryStrategy != Normal) 0.dp else 12.dp
            Text(
                text = text,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .testTag("Header")
                    .padding(top = top )
            )
            if(wordScreenState.memoryStrategy != Normal){
                Spacer(Modifier.width(20.dp))
                val tooltip = when (wordScreenState.memoryStrategy) {
                    DictationTest, DictationTestReviewWrong -> {
                        "退出听写测试"
                    }
                    Dictation -> {
                        "退出听写"
                    }
                    else -> {
                        "退出复习"
                    }
                }
                ExitButton(
                    tooltip = tooltip,
                    onClick = {
                    wordScreenState.showInfo()
                    wordScreenState.clearInputtedState()
                    wordScreenState.memoryStrategy = Normal
                    if( wordScreenState.wrongWords.isNotEmpty()){
                        wordScreenState.wrongWords.clear()
                    }
                    if(wordScreenState.reviewWords.isNotEmpty()){
                        wordScreenState.reviewWords.clear()
                    }
                    wordRequestFocus()
                })
            }
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainContent(
    appState: AppState,
    wordScreenState: WordScreenState,
    dictationState: DictationState,
    azureTTS: AzureTTS,
    currentWord:Word,
    videoBounds: Rectangle,
    wordFocusRequester:FocusRequester,
    window: ComposeWindow,
    openVocabulary: () -> Unit,
    showContext:(MediaInfo) -> Unit
){
    var nextButtonVisible by remember{ mutableStateOf(false) }
        /** 协程构建器 */
        val scope = rememberCoroutineScope()

        /** 单词输入错误*/
        var isWrong by remember { mutableStateOf(false) }

        /** 是否正在播放视频 */
        var isPlaying by remember { mutableStateOf(false) }

        /** 正在播放的视频信息 */
        var playMedia :MediaInfo? by remember { mutableStateOf(null) }

        /** 正在使用快捷键播放字幕的索引 */
        var plyingIndex by remember { mutableStateOf(1) }

        /** 显示填充后的书签图标 */
        var showBookmark by remember { mutableStateOf(false) }

        /** 显示删除对话框 */
        var showDeleteDialog by remember { mutableStateOf(false) }

        /** 显示把当前单词加入到熟悉词库的确认对话框 */
        var showFamiliarDialog by remember { mutableStateOf(false) }


        /** 第一条字幕输入框的焦点请求器 */
        val focusRequester1 = remember { FocusRequester() }
        /** 第二条字幕输入框的焦点请求器 */
        val focusRequester2 = remember { FocusRequester() }
        /** 第三条字幕输入框的焦点请求器 */
        val focusRequester3 = remember { FocusRequester() }

        /** 等宽字体*/
        val monospace  = rememberMonospace()

        val audioPlayerComponent = LocalAudioPlayerComponent.current

        val clipboardManager = LocalClipboardManager.current

        /** 是否正在播放单词发音 */
        var isPlayingAudio by remember { mutableStateOf(false) }
        /** 删除当前单词 */
        val deleteWord:() -> Unit = {
            val index = wordScreenState.index
            wordScreenState.vocabulary.wordList.removeAt(index)
            wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
            if(wordScreenState.vocabulary.name == "HardVocabulary"){
                appState.hardVocabulary.wordList.remove(currentWord)
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }
            try{
                wordScreenState.saveCurrentVocabulary()
                wordScreenState.clearInputtedState()
            }catch (e:Exception){
                // 回滚
                wordScreenState.vocabulary.wordList.add(index,currentWord)
                wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    appState.hardVocabulary.wordList.add(currentWord)
                    appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "删除单词失败,错误信息:\n${e.message}")
            }
        }

        /** 把当前单词加入到熟悉词库 */
        val addToFamiliar:() -> Unit = {
            val file = getFamiliarVocabularyFile()
            val familiar =  loadVocabulary(file.absolutePath)
            val familiarWord = currentWord.deepCopy()
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (wordScreenState.vocabulary.type == VocabularyType.MKV ||
                wordScreenState.vocabulary.type == VocabularyType.SUBTITLES
            ) {
                familiarWord.captions.forEach{ caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                        subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                        subtitlesName = wordScreenState.vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    familiarWord.externalCaptions.add(externalCaption)
                }
                familiarWord.captions.clear()

            }
            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }
            if(!familiar.wordList.contains(familiarWord)){
                familiar.wordList.add(familiarWord)
                familiar.size = familiar.wordList.size
            }
            try{
                saveVocabulary(familiar,file.absolutePath)
                deleteWord()
            }catch(e:Exception){
                // 回滚
                if(familiar.wordList.contains(familiarWord)){
                    familiar.wordList.remove(familiarWord)
                    familiar.size = familiar.wordList.size
                }

                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "保存熟悉词库失败,错误信息:\n${e.message}")
            }
            showFamiliarDialog = false
        }

        /** 处理加入到困难词库的函数 */
        val bookmarkClick :() -> Unit = {
            val hardWord = currentWord.deepCopy()
            val contains = appState.hardVocabulary.wordList.contains(currentWord)
            val index = appState.hardVocabulary.wordList.indexOf(currentWord)
            if(contains){
                appState.hardVocabulary.wordList.removeAt(index)
                // 如果当前词库是困难词库，说明用户想把单词从困难词库（当前词库）删除
                if(wordScreenState.vocabulary.name == "HardVocabulary"){
                    wordScreenState.vocabulary.wordList.remove(currentWord)
                    wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size
                    try{
                        wordScreenState.saveCurrentVocabulary()
                    }catch (e:Exception){
                        // 回滚
                        appState.hardVocabulary.wordList.add(index,currentWord)
                        appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
                        wordScreenState.vocabulary.wordList.add(wordScreenState.index,currentWord)
                        wordScreenState.vocabulary.size = wordScreenState.vocabulary.wordList.size

                        e.printStackTrace()
                        JOptionPane.showMessageDialog(window, "保存当前词库失败,错误信息:\n${e.message}")
                    }

                }
            }else{
                val relateVideoPath = wordScreenState.vocabulary.relateVideoPath
                val subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId
                val subtitlesName =
                    if (wordScreenState.vocabulary.type == VocabularyType.SUBTITLES) wordScreenState.vocabulary.name else ""

                currentWord.captions.forEach { caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath,
                        subtitlesTrackId,
                        subtitlesName,
                        caption.start,
                        caption.end,
                        caption.content
                    )
                    hardWord.externalCaptions.add(externalCaption)
                }
                hardWord.captions.clear()
                appState.hardVocabulary.wordList.add(hardWord)
            }
            try{
                appState.saveHardVocabulary()
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }catch(e:Exception){
                // 回滚
                if(contains){
                    appState.hardVocabulary.wordList.add(index,hardWord)
                }else{

                    appState.hardVocabulary.wordList.remove(hardWord)
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(window, "保存困难词库失败,错误信息:\n${e.message}")
            }

        }

        /** 焦点请求 */
        val focusRequest:(Int) -> Unit = { index ->
            try {
                if(wordScreenState.subtitlesVisible){
                    when(index){
                        1 -> focusRequester1.requestFocus()
                        2 -> focusRequester2.requestFocus()
                        3 -> focusRequester3.requestFocus()
                        else -> wordFocusRequester.requestFocus()
                    }
                }else{
                    wordFocusRequester.requestFocus()
                }
            } catch (_: IllegalStateException) {
                // FocusRequester 未初始化时的安全处理
                println("FocusRequester not initialized, skipping focus request for index: $index")
                // 尝试使用主要的 wordFocusRequester 作为后备
                try {
                    wordFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                    println("wordFocusRequester also not initialized, skipping focus request")
                }
            }
        }

        /** 用快捷键播放视频时被调用的函数， */
        val handleMediaPlay: (
                index:Int
                ) -> Unit = { index ->
            plyingIndex = index
            val mediaInfo =  when {
                // 先从 MKV 或 SUBTITLES 类型的词库获取 MediaInfo
                (wordScreenState.getCurrentWord().captions.size >= index) -> {
                    val caption = currentWord.captions[index -1]
                    MediaInfo(
                        caption,
                        wordScreenState.vocabulary.relateVideoPath,
                        wordScreenState.vocabulary.subtitlesTrackId
                    )
                }
                // 混合词库 DOCUMENT 从 ExternalCaptions 获取 MediaInfo
                (wordScreenState.getCurrentWord().externalCaptions.size >= index) -> {
                    getMediaInfoFromExternalCaption(currentWord.externalCaptions, index - 1)
                }
                else -> null
            }

            if(mediaInfo != null){
                // 验证视频文件的路径
                val resolvedPath =  resolveMediaPath(
                    mediaInfo.mediaPath,
                    wordScreenState.getVocabularyDir()
                )
                if(resolvedPath != ""){
                    mediaInfo.mediaPath = resolvedPath
                    isPlaying = true
                    playMedia = mediaInfo
                }else{
                    //TODO 需要提示用户视频文件不存在
                    val message = if(mediaInfo.mediaPath.isEmpty())"视频地址为空" else "视频地址错误"
                    println("resolveMediaPath error: $message")
                }
            }
        }

        /** 处理全局快捷键的回调函数 */
        val globalKeyEvent: (KeyEvent) -> Boolean = {
            // isCtrlPressed
            // macOS 下是 Command 键, windows 下是 Ctrl 键
            val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
            // 删除功能的修饰键
            val isDeleteModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isShiftPressed
            // 删除键在不同的平台上有不同的键值
            val deleteKey = if(isMacOS()) Key.Backspace else Key.Delete
            when {
                (isCtrlPressed && it.isShiftPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordFocusRequester.requestFocus()
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSearch()
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.O && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        openVocabulary()
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.phoneticVisible = !wordScreenState.phoneticVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.phoneticVisible = wordScreenState.phoneticVisible
                            dictationState.saveDictationState()
                        }

                    }
                    true
                }
                (isCtrlPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.morphologyVisible = !wordScreenState.morphologyVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.morphologyVisible = wordScreenState.morphologyVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.E && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.definitionVisible = !wordScreenState.definitionVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.definitionVisible = wordScreenState.definitionVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (isCtrlPressed && (if(isMacOS()) it.key == Key.R else it.key == Key.H) && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.sentencesVisible = !wordScreenState.sentencesVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.sentencesVisible = wordScreenState.sentencesVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.translationVisible = !wordScreenState.translationVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.translationVisible = wordScreenState.translationVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.wordVisible = !wordScreenState.wordVisible
                        wordScreenState.saveWordScreenState()
                    }
                    true
                }

                (isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
                    if (!isPlayingAudio) {
                        scope.launch (Dispatchers.IO){
                            val audioPath =  getAudioPath(
                                word = currentWord.value,
                                audioSet = appState.localAudioSet,
                                addToAudioSet = { appState.localAudioSet.add(it) },
                                pronunciation = wordScreenState.pronunciation,
                                azureTTS = azureTTS
                            )
                            playAudio(
                                word = currentWord.value,
                                audioPath = audioPath,
                                pronunciation =  wordScreenState.pronunciation,
                                volume = appState.global.audioVolume,
                                audioPlayerComponent = audioPlayerComponent,
                                changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                            )
                        }

                    }
                    true
                }
                ((isCtrlPressed &&  (if(isMacOS()) it.isCtrlPressed else it.isAltPressed )) && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSidebar = !appState.openSidebar
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordScreenState.subtitlesVisible = !wordScreenState.subtitlesVisible
                        wordScreenState.saveWordScreenState()
                        if(wordScreenState.memoryStrategy== Dictation || wordScreenState.memoryStrategy== DictationTest ){
                            dictationState.subtitlesVisible = wordScreenState.subtitlesVisible
                            dictationState.saveDictationState()
                        }
                    }
                    true
                }

                (isCtrlPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                    if(!it.isShiftPressed){
                        scope.launch {
                            bookmarkClick()
                        }
                        showBookmark = true
                        true
                    }else false
                }
                (isCtrlPressed && it.key == Key.Y && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                        JOptionPane.showMessageDialog(window, "不能把熟悉词库的单词添加到熟悉词库")
                    }else{
                        showFamiliarDialog = true
                    }
                    true
                }
                (isDeleteModifierPressed && it.key == deleteKey && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        showDeleteDialog = true
                    }
                    true
                }
                (isCtrlPressed &&  it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest ){
                        handleMediaPlay(1)
                    }
                    true
                }
                (isCtrlPressed && it.key == Key.Two && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest){
                        handleMediaPlay(2)
                    }
                    true
                }
                (isCtrlPressed &&  it.key == Key.Three && it.type == KeyEventType.KeyUp) -> {
                    if(wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest){
                        handleMediaPlay(3)
                    }
                    true
                }
                else -> false
            }

        }

        /** 显示本章节已经完成对话框 */
        var showChapterFinishedDialog by remember { mutableStateOf(false) }

        /** 显示整个词库已经学习完成对话框 */
        var isVocabularyFinished by remember { mutableStateOf(false) }

        /** 播放整个章节完成时音效 */
        val playChapterFinished = {
            if (wordScreenState.isPlaySoundTips) {
                playSound("audio/Success!!.wav", wordScreenState.soundTipsVolume)
            }
        }

        /**
         * 在听写模式，闭着眼睛听写单词时，刚拼写完单词，就播放这个声音感觉不好，
         * 在非听写模式下按Enter键就不会有这种感觉，因为按Enter键，
         * 自己已经输入完成了，有一种期待，预测到了将会播放提示音。
         */
        val delayPlaySound:() -> Unit = {
            Timer("playChapterFinishedSound", false).schedule(1000) {
                playChapterFinished()
            }
            showChapterFinishedDialog = true
        }


        /** 增加复习错误单词时的索引 */
        val increaseWrongIndex:() -> Unit = {
            if (wordScreenState.dictationIndex + 1 == wordScreenState.wrongWords.size) {
                delayPlaySound()
            } else wordScreenState.dictationIndex++
        }


        /** 切换到下一个单词 */
        val toNext: () -> Unit = {
            scope.launch {
                wordScreenState.clearInputtedState()
                when (wordScreenState.memoryStrategy) {
                    Normal -> {
                        when {
                            (wordScreenState.index == wordScreenState.vocabulary.size - 1) -> {
                                isVocabularyFinished = true
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            ((wordScreenState.index + 1) % 20 == 0) -> {
                                playChapterFinished()
                                showChapterFinishedDialog = true
                            }
                            else -> wordScreenState.index += 1
                        }
                        wordScreenState.saveWordScreenState()
                    }
                    Dictation -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.dictationWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    DictationTest -> {
                        if (wordScreenState.dictationIndex + 1 == wordScreenState.reviewWords.size) {
                            delayPlaySound()
                        } else wordScreenState.dictationIndex++
                    }
                    NormalReviewWrong -> { increaseWrongIndex() }
                    DictationTestReviewWrong -> { increaseWrongIndex() }
                }

                wordFocusRequester.requestFocus()

            }
        }

        /** 切换到上一个单词,听写时不允许切换到上一个单词 */
        val previous :() -> Unit = {
            scope.launch {
                // 正常记忆单词
                if(wordScreenState.memoryStrategy == Normal){
                    wordScreenState.clearInputtedState()
                    if((wordScreenState.index) % 20 != 0 ){
                        wordScreenState.index -= 1
                        wordScreenState.saveWordScreenState()
                    }
                    // 复习错误单词
                }else if (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong ){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.dictationIndex > 0 ){
                        wordScreenState.dictationIndex -= 1
                    }
                }
                wordFocusRequester.requestFocus()
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
                .onKeyEvent { globalKeyEvent(it) }
                .onPointerEvent(PointerEventType.Move){nextButtonVisible = true}
                .onPointerEvent(PointerEventType.Exit){nextButtonVisible = false}
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(intrinsicSize = IntrinsicSize.Max)
                .background(MaterialTheme.colors.background)
                .focusable(true)
                .align(Alignment.Center)
                .padding(end = 0.dp,bottom = 58.dp)
        ) {

            /** 听写模式的错误单词 */
            val dictationWrongWords = remember { mutableStateMapOf<Word, Int>()}

            /** 显示编辑单词对话框 */
            var showEditWordDialog by remember { mutableStateOf(false) }

            /** 清空听写模式存储的错误单词 */
            val resetChapterTime: () -> Unit = {
                dictationWrongWords.clear()
            }


            /** 播放错误音效 */
            val playBeepSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/beep.wav", wordScreenState.soundTipsVolume)
                }
            }

            /** 播放成功音效 */
            val playSuccessSound = {
                if (wordScreenState.isPlaySoundTips) {
                    playSound("audio/hint.wav", wordScreenState.soundTipsVolume)
                }
            }


            /** 播放按键音效 */
            val playKeySound = {
                if (appState.global.isPlayKeystrokeSound) {
                    playSound("audio/keystroke.wav", appState.global.keystrokeVolume)
                }
            }

            /**
             * 当用户在听写测试按 enter 调用的函数，
             * 在听写测试跳过单词也算一次错误
             */
            val dictationSkipCurrentWord: () -> Unit = {
                if (wordScreenState.wordCorrectTime == 0) {
                    val dictationWrongTime = dictationWrongWords[currentWord]
                    if (dictationWrongTime == null) {
                        dictationWrongWords[currentWord] = 1
                    }
                }
            }

            /** 焦点切换到单词输入框 */
            val jumpToWord:() -> Unit = {
                wordFocusRequester.requestFocus()
            }

            /** 焦点切换到抄写字幕 */
            val jumpToCaptions:() -> Unit = {
                if((wordScreenState.memoryStrategy != Dictation && wordScreenState.memoryStrategy != DictationTest) &&
                    wordScreenState.subtitlesVisible && (currentWord.captions.isNotEmpty() || currentWord.externalCaptions.isNotEmpty())
                ){
                    focusRequester1.requestFocus()
                }
            }

            /** 检查输入的单词 */
            val checkWordInput: (String) -> Unit = { input ->
                if(!isWrong){
                    wordScreenState.wordTextFieldValue = input
                    wordScreenState.wordTypingResult.clear()
                    var done = true
                    /**
                     *  防止用户粘贴内容过长，如果粘贴的内容超过 word.value 的长度，
                     * 会改变 BasicTextField 宽度，和 Text 的宽度不匹配
                     */
                    if (input.length > currentWord.value.length) {
                        wordScreenState.wordTypingResult.clear()
                        wordScreenState.wordTextFieldValue = ""
                    } else {
                        val inputChars = input.toList()
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar == wordChar) {
                                wordScreenState.wordTypingResult.add(Pair(inputChar, true))
                            } else {
                                // 字母输入错误
                                wordScreenState.wordTypingResult.add(Pair(inputChar, false))
                                done = false
                                playBeepSound()
                                isWrong = true
                                wordScreenState.wordWrongTime++
                                // 如果是听写测试，或独立的听写测试，需要汇总错误单词
                                if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                    val dictationWrongTime = dictationWrongWords[currentWord]
                                    if (dictationWrongTime != null) {
                                        dictationWrongWords[currentWord] = dictationWrongTime + 1
                                    } else {
                                        dictationWrongWords[currentWord] = 1
                                    }
                                }
//                                // 再播放一次单词发音
                                if (!isPlayingAudio && wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }

                            }
                        }
                        // 用户输入的单词完全正确
                        if (wordScreenState.wordTypingResult.size == currentWord.value.length && done) {
                            // 输入完全正确
                            playSuccessSound()
                            wordScreenState.wordCorrectTime++
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                Timer("input correct to next", false).schedule(50) {
                                    toNext()
                                }
                            }else if (wordScreenState.isAuto && wordScreenState.wordCorrectTime == wordScreenState.repeatTimes ) {
                                Timer("input correct to next", false).schedule(50) {
                                    toNext()
                                }
                            } else {
                                Timer("input correct clean InputChar", false).schedule(50){
                                    wordScreenState.wordTypingResult.clear()
                                    wordScreenState.wordTextFieldValue = ""
                                }

                                // 再播放一次单词发音
                                if (!isPlayingAudio && wordScreenState.playTimes == 2) {
                                    scope.launch (Dispatchers.IO){
                                        val audioPath =  getAudioPath(
                                            word = currentWord.value,
                                            audioSet = appState.localAudioSet,
                                            addToAudioSet = { appState.localAudioSet.add(it) },
                                            pronunciation = wordScreenState.pronunciation,
                                            azureTTS = azureTTS
                                        )
                                        playAudio(
                                            word = currentWord.value,
                                            audioPath = audioPath,
                                            pronunciation =  wordScreenState.pronunciation,
                                            volume = appState.global.audioVolume,
                                            audioPlayerComponent = audioPlayerComponent,
                                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
//                                        setIsAutoPlay = {}
                                        )
                                    }

                                }
                            }
                        }
                    }
                }else{
                    // 输入错误后继续输入
                    if(input.length > wordScreenState.wordTypingResult.size){
                        // 如果不截取字符串，用户长按某个按键，程序可能会崩溃
                        val inputStr = input.substring(0,wordScreenState.wordTypingResult.size)
                        val inputChars = inputStr.toList()
                        isWrong = false
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar != wordChar) {
                                playBeepSound()
                                isWrong = true
                            }
                        }
                        if(!isWrong){
                            wordScreenState.wordTextFieldValue = inputStr
                        }
                    }else if(input.length == wordScreenState.wordTypingResult.size-1){
                        // 输入错误后按退格键删除错误字母
                        isWrong = false
                            wordScreenState.wordTypingResult.removeLast()
                            wordScreenState.wordTextFieldValue = input
                    }else if(input.isEmpty()){
                        // 输入错误后 Ctrl + A 全选后删除全部输入
                        wordScreenState.wordTextFieldValue = ""
                        wordScreenState.wordTypingResult.clear()
                        isWrong = false
                    }

                }

            }


            /** 检查输入的字幕 */
            val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                when(index){
                    0 -> wordScreenState.captionsTextFieldValue1 = input
                    1 -> wordScreenState.captionsTextFieldValue2 = input
                    2 -> wordScreenState.captionsTextFieldValue3 = input
                }
                val typingResult = wordScreenState.captionsTypingResultMap[index]
                typingResult!!.clear()
                val inputChars = input.toMutableList()
                for (i in inputChars.indices) {
                    val inputChar = inputChars[i]
                    if(i<captionContent.length){
                        val captionChar = captionContent[i]
                        if (inputChar == captionChar) {
                            typingResult.add(Pair(captionChar, true))
                        }else if (inputChar == ' ' && (captionChar == '[' || captionChar == ']')) {
                            typingResult.add(Pair(captionChar, true))
                            // 音乐符号不好输入，所以可以使用空格替换
                        }else if (inputChar == ' ' && (captionChar == '♪')) {
                            typingResult.add(Pair(captionChar, true))
                            // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                            inputChars.add(i,'♪')
                            inputChars.removeAt(i+1)
                            val textFieldValue = String(inputChars.toCharArray())
                            when(index){
                                0 -> wordScreenState.captionsTextFieldValue1 = textFieldValue
                                1 -> wordScreenState.captionsTextFieldValue2 = textFieldValue
                                2 -> wordScreenState.captionsTextFieldValue3 = textFieldValue
                            }
                        } else {
                            typingResult.add(Pair(inputChar, false))
                        }
                    }else{
                        typingResult.add(Pair(inputChar, false))
                    }

                }

            }

            /** 索引递减 */
            val decreaseIndex = {
                if(wordScreenState.index == wordScreenState.vocabulary.size - 1){
                    val mod = wordScreenState.vocabulary.size % 20
                    wordScreenState.index -= (mod-1)
                }else if (wordScreenState.vocabulary.size > 19) wordScreenState.index -= 19
                else wordScreenState.index = 0
            }

            /** 计算正确率 */
            val correctRate: () -> Float = {
                val size = if(wordScreenState.memoryStrategy == Dictation ) wordScreenState.dictationWords.size else wordScreenState.reviewWords.size
                var rate =  (size - dictationWrongWords.size).div(size.toFloat()) .times(1000)
                rate = rate.toInt().toFloat().div(10)
                rate
            }

            /** 重复学习本章 */
            val learnAgain: () -> Unit = {
                decreaseIndex()
                resetChapterTime()
                wordScreenState.saveWordScreenState()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }


            /** 复习错误单词 */
            val reviewWrongWords: () -> Unit = {
                val reviewList = dictationWrongWords.keys.toList()
                if (reviewList.isNotEmpty()) {
                    wordScreenState.showInfo(clear = false)
                    if (wordScreenState.memoryStrategy == DictationTest ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ) {
                        wordScreenState.memoryStrategy = DictationTestReviewWrong
                    }else{
                        wordScreenState.memoryStrategy = NormalReviewWrong
                    }
                    if( wordScreenState.wrongWords.isEmpty()){
                        wordScreenState.wrongWords.addAll(reviewList)
                    }
                    wordScreenState.dictationIndex = 0
                    showChapterFinishedDialog = false
                }
            }

            /** 下一章 */
            val nextChapter: () -> Unit = {

                if (wordScreenState.memoryStrategy == NormalReviewWrong ||
                    wordScreenState.memoryStrategy == DictationTestReviewWrong
                ) {
                    wordScreenState.wrongWords.clear()
                }

                if( wordScreenState.memoryStrategy == Dictation){
                    wordScreenState.showInfo()
                }

                wordScreenState.index += 1
                wordScreenState.chapter++
                resetChapterTime()
                wordScreenState.memoryStrategy = Normal
                wordScreenState.saveWordScreenState()
                showChapterFinishedDialog = false
            }


            /** 正常记忆单词，进入到听写测试，需要的单词 */
            val shuffleNormal:() -> Unit = {
                val wordValue = wordScreenState.getCurrentWord().value
                val shuffledList = wordScreenState.generateDictationWords(wordValue)
                wordScreenState.dictationWords.clear()
                wordScreenState.dictationWords.addAll(shuffledList)
            }
            /** 从独立的听写测试再次进入到听写测试时，需要的单词 */
            val shuffleDictationReview:() -> Unit = {
                var shuffledList = wordScreenState.reviewWords.shuffled()
                // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
                while(shuffledList.first() == currentWord){
                    shuffledList = wordScreenState.reviewWords.shuffled()
                }
                wordScreenState.reviewWords.clear()
                wordScreenState.reviewWords.addAll(shuffledList)
            }
            /** 进入听写模式 */
            val enterDictation: () -> Unit = {
                scope.launch {
                    wordScreenState.saveWordScreenState()
                    when(wordScreenState.memoryStrategy){
                        // 从正常记忆单词第一次进入到听写测试
                        Normal -> {
                            shuffleNormal()
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 正常记忆单词时选择再次听写
                        Dictation ->{
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                        }
                        // 从复习错误单词进入到听写测试，这里有两种情况：
                        // 一种是从正常记忆单词进入到复习错误单词，复习完毕后，再次听写
                        NormalReviewWrong ->{
                            wordScreenState.memoryStrategy = Dictation
                            wordScreenState.wrongWords.clear()
                            shuffleNormal()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 一种是从独立的听写测试进入到复习错误单词，复习完毕后，再次听写
                        DictationTestReviewWrong ->{
                            wordScreenState.memoryStrategy = DictationTest
                            wordScreenState.wrongWords.clear()
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                            wordScreenState.hiddenInfo(dictationState)
                        }
                        // 在独立的听写测试时选择再次听写
                        DictationTest ->{
                            shuffleDictationReview()
                            wordScreenState.dictationIndex = 0
                        }
                    }
                    wordFocusRequester.requestFocus()
                    resetChapterTime()
                    showChapterFinishedDialog = false
                    isVocabularyFinished = false
                }
            }


            /**
             * 重置索引
             * 参数 isShuffle 是否打乱词库
             */
            val resetIndex: (isShuffle: Boolean) -> Unit = { isShuffle ->
                // 如果要打乱顺序
                if (isShuffle) {
                    // 内置词库的地址
                    val path = getResourcesFile("vocabulary").absolutePath
                    // 如果要打乱的词库是内置词库，要选择一个地址，保存打乱后的词库，
                    // 如果不选择地址的话，软件升级后词库会被重置。
                    if(wordScreenState.vocabularyPath.startsWith(path)){
                        val fileChooser = appState.futureFileChooser.get()
                        fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                        fileChooser.dialogTitle = "保存重置后的词库"
                        val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                        val fileName = File(wordScreenState.vocabularyPath).nameWithoutExtension
                        fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                        val userSelection = fileChooser.showSaveDialog(window)
                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                            val selectedFile = fileChooser.selectedFile
                            val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                            val savePath = Paths.get(selectedFile.absolutePath)
                            if(savePath.startsWith(vocabularyDirPath)){
                                JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，词库会被重置或者被删除")
                            }else{
                                wordScreenState.vocabulary.wordList.shuffle()
                                val shuffledList = wordScreenState.vocabulary.wordList
                                val vocabulary = Vocabulary(
                                    name = selectedFile.nameWithoutExtension,
                                    type = VocabularyType.DOCUMENT,
                                    language = "english",
                                    size = wordScreenState.vocabulary.size,
                                    relateVideoPath = wordScreenState.vocabulary.relateVideoPath,
                                    subtitlesTrackId = wordScreenState.vocabulary.subtitlesTrackId,
                                    wordList = shuffledList
                                )

                                try {
                                    saveVocabulary(vocabulary, selectedFile.absolutePath)
                                    appState.changeVocabulary(selectedFile, wordScreenState, 0)
                                    // changeVocabulary 会把内置词库保存到最近列表，
                                    // 保存后，如果再切换列表，就会有两个名字相同的词库，
                                    // 所以需要把刚刚添加的词库从最近列表删除
                                    for (i in 0 until appState.recentList.size) {
                                        val recentItem = appState.recentList[i]
                                        if (recentItem.name == wordScreenState.vocabulary.name) {
                                            appState.removeRecentItem(recentItem)
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    JOptionPane.showMessageDialog(window, "保存词库失败,错误信息:\n${e.message}")
                                }


                            }
                        }
                    }else{
                        try{
                            wordScreenState.vocabulary.wordList.shuffle()
                            wordScreenState.saveCurrentVocabulary()
                        }catch(e:Exception){
                            e.printStackTrace()
                            JOptionPane.showMessageDialog(window, "保存词库失败,错误信息:\n${e.message}")
                        }

                    }

                }

                wordScreenState.index = 0
                wordScreenState.chapter = 1
                wordScreenState.saveWordScreenState()
                resetChapterTime()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }
            val wordKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                when {
                    ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || it.key == Key.DirectionRight)
                            && it.type == KeyEventType.KeyUp) -> {
                        toNext()
                        if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                            dictationSkipCurrentWord()
                        }
                        true
                    }
                    ((it.key == Key.PageUp || it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                        previous()
                        true
                    }
                    (isCtrlPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                        if(!it.isShiftPressed){
                            clipboardManager.setText(AnnotatedString(currentWord.value))
                            true
                        }else false

                    }
                    (isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }

                    (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                        jumpToCaptions()
                        true
                    }
                    (it.type == KeyEventType.KeyDown
                            && it.key != Key.ShiftRight
                            && it.key != Key.ShiftLeft
                            && it.key != Key.CtrlRight
                            && it.key != Key.CtrlLeft
                            && it.key != Key.AltLeft
                            && it.key != Key.AltRight
                            && it.key != Key.Escape
                            && it.key != Key.Enter
                            && it.key != Key.NumPadEnter
                            ) -> {
                        playKeySound()
                        true
                    }
                    else -> false
                }
            }


            LaunchedEffect(appState.vocabularyChanged){
                if(appState.vocabularyChanged){
                    wordScreenState.clearInputtedState()
                    if(wordScreenState.memoryStrategy == NormalReviewWrong ||
                        wordScreenState.memoryStrategy == DictationTestReviewWrong
                    ){
                        wordScreenState.wrongWords.clear()
                    }
                    if (wordScreenState.memoryStrategy == Dictation) {
                        wordScreenState.showInfo()
                        resetChapterTime()
                    }

                    if(wordScreenState.memoryStrategy == DictationTest) wordScreenState.memoryStrategy = Normal


                    appState.vocabularyChanged = false
                }
            }

            var activeMenu by remember { mutableStateOf(false) }
            Box(
                Modifier.onPointerEvent(PointerEventType.Exit) { activeMenu = false }
            ) {
                /** 动态菜单，鼠标移动到单词区域时显示 */
                if (activeMenu) {
                    Row(modifier = Modifier.align(Alignment.TopCenter)) {
                        val contains = appState.hardVocabulary.wordList.contains(currentWord)
                        DeleteButton(onClick = { showDeleteDialog = true })
                        EditButton(onClick = { showEditWordDialog = true })
                        FamiliarButton(onClick = {
                            if(wordScreenState.vocabulary.name == "FamiliarVocabulary"){
                                JOptionPane.showMessageDialog(window, "不能把熟悉词库的单词添加到熟悉词库")
                            }else{
                                showFamiliarDialog = true
                            }

                        })
                        HardButton(
                            onClick = { bookmarkClick() },
                            contains = contains,
                            fontFamily = monospace
                        )
                        CopyButton(wordValue = currentWord.value)
                    }
                }else if(showBookmark){
                    val contains = appState.hardVocabulary.wordList.contains(currentWord)
                    // 这个按钮只显示 0.3 秒后消失
                    BookmarkButton(
                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 96.dp),
                        contains = contains,
                        disappear = {showBookmark = false}
                    )
                }

                Row(Modifier.align(Alignment.Center).padding(top = 48.dp)){
                    Word(
                        word = currentWord,
                        global = appState.global,
                        wordVisible = wordScreenState.wordVisible,
                        pronunciation = wordScreenState.pronunciation,
                        azureTTS = azureTTS,
                        playTimes = wordScreenState.playTimes,
                        isPlaying = isPlayingAudio,
                        setIsPlaying = { isPlayingAudio = it },
                        isDictation = (wordScreenState.memoryStrategy == Dictation ||wordScreenState.memoryStrategy == DictationTest),
                        fontFamily = monospace,
                        audioSet = appState.localAudioSet,
                        addToAudioSet = {appState.localAudioSet.add(it) },
                        correctTime = wordScreenState.wordCorrectTime,
                        wrongTime = wordScreenState.wordWrongTime,
                        textFieldValue = wordScreenState.wordTextFieldValue,
                        typingResult = wordScreenState.wordTypingResult,
                        checkTyping = { checkWordInput(it) },
                        focusRequester = wordFocusRequester,
                        textFieldKeyEvent = {wordKeyEvent(it)},
                        showMenu = {activeMenu = true}
                    )
                }

            }


            Phonetic(
                word = currentWord,
                phoneticVisible = wordScreenState.phoneticVisible,
                fontSize = appState.global.detailFontSize
            )
            Morphology(
                word = currentWord,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                searching = false,
                morphologyVisible = wordScreenState.morphologyVisible,
                fontSize = appState.global.detailFontSize
            )
            Definition(
                word = currentWord,
                definitionVisible = wordScreenState.definitionVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Translation(
                word = currentWord,
                translationVisible = wordScreenState.translationVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Sentences(
                word = currentWord,
                sentencesVisible = wordScreenState.sentencesVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = wordScreenState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )

            val startPadding = if ( isPlaying && !wordScreenState.isChangeVideoBounds) 0.dp else 50.dp
            val captionsModifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(bottom = 0.dp, start = startPadding)
                .onKeyEvent {
                    when {
                        ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown || (it.key == Key.DirectionRight && !it.isShiftPressed))
                                && it.type == KeyEventType.KeyUp
                                ) -> {
                            toNext()
                            if (wordScreenState.memoryStrategy == Dictation || wordScreenState.memoryStrategy == DictationTest) {
                                dictationSkipCurrentWord()
                            }
                            true
                        }
                        ((it.key == Key.PageUp  ||  (it.key == Key.DirectionLeft && !it.isShiftPressed)) && it.type == KeyEventType.KeyUp) -> {
                            previous()
                            true
                        }
                        else -> globalKeyEvent(it)
                    }
                }
            Captions(
                captionsVisible = wordScreenState.subtitlesVisible,
                playTripleMap = getPlayTripleMap(wordScreenState.vocabulary.type,wordScreenState.vocabulary.subtitlesTrackId,wordScreenState.vocabulary.relateVideoPath,  currentWord),
                isPlaying = isPlaying,
                plyingIndex = plyingIndex,
                setPlayingIndex = {plyingIndex = it},
                volume = appState.global.videoVolume,
                setIsPlaying = { isPlaying = it },
                setPlayingMedia = {playMedia = it},
                word = currentWord,
                bounds = videoBounds,
                textFieldValueList = listOf(wordScreenState.captionsTextFieldValue1,wordScreenState.captionsTextFieldValue2,wordScreenState.captionsTextFieldValue3),
                typingResultMap = wordScreenState.captionsTypingResultMap,
                putTypingResultMap = { index, list ->
                    wordScreenState.captionsTypingResultMap[index] = list
                },
                checkTyping = { index, input, captionContent ->
                    checkCaptionsInput(index, input, captionContent)
                },
                playKeySound = { playKeySound() },
                modifier = captionsModifier,
                focusRequesterList = listOf(focusRequester1,focusRequester2,focusRequester3),
                jumpToWord = {jumpToWord()},
                openSearch = {appState.openSearch()},
                fontSize = appState.global.detailFontSize,
                isWriteSubtitles = wordScreenState.isWriteSubtitles,
            )

            if (showDeleteDialog) {
                ConfirmDialog(
                    message = "确定要删除单词 ${currentWord.value} ?",
                    confirm = {
                        scope.launch {
                            deleteWord()
                            showDeleteDialog = false
                        }
                    },
                    close = { showDeleteDialog = false }
                )
            }
            if(showFamiliarDialog){
                ConfirmDialog(
                    message = "确定要把 ${currentWord.value} 加入到熟悉词库？\n" +
                            "加入到熟悉词库后，${currentWord.value} 会从当前词库删除。",
                    confirm = { scope.launch { addToFamiliar() } },
                    close = { showFamiliarDialog = false }
                )

            }
            if (showEditWordDialog) {
                EditWordDialog(
                    word = currentWord,
                    title = "编辑单词",
                    appState = appState,
                    vocabulary = wordScreenState.vocabulary,
                    vocabularyDir = wordScreenState.getVocabularyDir(),
                    save = { newWord ->
                        scope.launch {
                            val index = wordScreenState.index
                            // 触发重组
                            wordScreenState.vocabulary.wordList.removeAt(index)
                            wordScreenState.vocabulary.wordList.add(index, newWord)
                            try{
                                wordScreenState.saveCurrentVocabulary()
                                showEditWordDialog = false
                            }catch(e:Exception){
                                // 回滚
                                wordScreenState.vocabulary.wordList.removeAt(index)
                                wordScreenState.vocabulary.wordList.add(index, currentWord)
                                e.printStackTrace()
                                JOptionPane.showMessageDialog(window, "保存当前词库失败,错误信息:\n${e.message}")
                            }

                        }
                    },
                    close = { showEditWordDialog = false }
                )
            }

            /** 显示独立的听写测试的选择章节对话框 */
            var showChapterDialog by remember { mutableStateOf(false) }
            /** 打开独立的听写测试的选择章节对话框 */
            val openReviewDialog:() -> Unit = {
                showChapterFinishedDialog = false
                showChapterDialog = true
                resetChapterTime()
            }

            if(showChapterDialog){
                SelectChapterDialog(
                    close = {showChapterDialog = false},
                    wordScreenState = wordScreenState,
                    wordRequestFocus = {
                        wordFocusRequester.requestFocus()
                    },
                    isMultiple = true
                )
            }

            /** 关闭当前章节结束时跳出的对话框 */
            val close: () -> Unit = {
                showChapterFinishedDialog = false
                if(isVocabularyFinished) isVocabularyFinished = false
            }
            if (showChapterFinishedDialog) {
                ChapterFinishedDialog(
                    close = { close() },
                    isVocabularyFinished = isVocabularyFinished,
                    correctRate = correctRate(),
                    memoryStrategy = wordScreenState.memoryStrategy,
                    openReviewDialog = {openReviewDialog()},
                    isReviewWrong = (wordScreenState.memoryStrategy == NormalReviewWrong || wordScreenState.memoryStrategy == DictationTestReviewWrong),
                    dictationWrongWords = dictationWrongWords,
                    enterDictation = { enterDictation() },
                    learnAgain = { learnAgain() },
                    reviewWrongWords = { reviewWrongWords() },
                    nextChapter = { nextChapter() },
                    resetIndex = { resetIndex(it) }
                )
            }
        }

            if(isPlaying){
                val videoPlayerSize by remember(appState.global.size){
                    derivedStateOf {
                        computeVideoSize(appState.global.size)
                    }
                }
                MiniVideoPlayer(
                    modifier = Modifier.align(Alignment.Center),
                    size = videoPlayerSize,
                    stop = {
                        isPlaying = false
                        focusRequest(plyingIndex)
                    },
                    volume = appState.global.videoVolume,
                    externalPlayingState = isPlaying,
                    showContextButton = true,
                    showContext ={ playMedia?.let {
                        showContext(it)
                        isPlaying = false
                        focusRequest(plyingIndex)
                    } },
                    mediaInfo = playMedia,
                    externalSubtitlesVisible = wordScreenState.externalSubtitlesVisible
                )
            }


        if (nextButtonVisible) {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "上一个")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { previous() },
                    modifier = Modifier.testTag("PreviousButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "下一个")
                        }
                    }
                },
                delayMillis = 300,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterStart,
                    alignment = Alignment.CenterStart,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(
                    onClick = { toNext()},
                    modifier = Modifier.testTag("NextButton")
                ) {
                    Icon(
                        Icons.Filled.ArrowForwardIos,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
        }


    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VocabularyEmpty(
    openVocabulary: () -> Unit,
    openBuiltInVocabulary: () -> Unit = {},
    generateVocabulary: () -> Unit = {},
    openDocument: () -> Unit = {},
    parentWindow : ComposeWindow,
    futureFileChooser: FutureTask<JFileChooser>,
    openChooseVocabulary: (String) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
                    .width(300.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "打开词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openVocabulary() })
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "使用手册",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = { openDocument() })
                            .width(78.dp)
                            .padding(5.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "生成词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {generateVocabulary()  })
                            .padding(5.dp)
                    )
                }

                var visible by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .onPointerEvent(PointerEventType.Exit) { visible = false }
                ){
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp).onPointerEvent(PointerEventType.Enter) { visible = true }
                    ) {
                        Text(
                            text = "内置词库",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                .padding(5.dp)
                        )
                    }
                    val scope = rememberCoroutineScope()
                    AnimatedVisibility(visible = visible){

                        /** 保存词库 */
                        val save:(File) -> Unit = {file ->
                            scope.launch(Dispatchers.IO) {
                                val name = file.nameWithoutExtension
                                val fileChooser = futureFileChooser.get()
                                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                                fileChooser.dialogTitle = "保存词库"
                                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                                fileChooser.selectedFile = File("$myDocuments${File.separator}${name}.json")
                                val userSelection = fileChooser.showSaveDialog(parentWindow)
                                if (userSelection == JFileChooser.APPROVE_OPTION) {

                                    val fileToSave = fileChooser.selectedFile
                                    if (fileToSave.exists()) {
                                        // 是-0,否-1，取消-2
                                        val answer =
                                            JOptionPane.showConfirmDialog(parentWindow, "${name}.json 已存在。\n要替换它吗？")
                                        if (answer == 0) {
                                            try{
                                                fileToSave.writeBytes(file.readBytes())
                                                openChooseVocabulary(fileToSave.absolutePath)
                                            }catch (e:Exception){
                                                e.printStackTrace()
                                                JOptionPane.showMessageDialog(parentWindow,"保存失败，错误信息：\n${e.message}")
                                            }

                                        }
                                    } else {
                                        try{
                                            fileToSave.writeBytes(file.readBytes())
                                            openChooseVocabulary(fileToSave.absolutePath)
                                        }catch (e:Exception){
                                            e.printStackTrace()
                                            JOptionPane.showMessageDialog(parentWindow,"保存失败，错误信息：\n${e.message}")
                                        }

                                    }

                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 5.dp)
                        ) {
                            Text(
                                text = "四级",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/四级.json")
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "六级",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/大学英语/六级.json")
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "牛津核心3000词",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {
                                    val file = getResourcesFile("vocabulary/牛津核心词/The_Oxford_3000.json")
                                    save(file)
                                })
                                    .padding(5.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "更多",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable(onClick = {openBuiltInVocabulary()})
                                    .padding(5.dp)
                            )
                        }
                    }
                }

            }
        }


    }
}

/**
 * 词型组件
 */
@Composable
fun Morphology(
    word: Word,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    searching: Boolean,
    morphologyVisible: Boolean,
    fontSize: TextUnit
) {
    if (morphologyVisible &&(isChangeVideoBounds || !isPlaying )) {
        val exchanges = word.exchange.split("/")
        var preterite = ""
        var pastParticiple = ""
        var presentParticiple = ""
        var third = ""
        var er = ""
        var est = ""
        var plural = ""
        var lemma = ""

        exchanges.forEach { exchange ->
            val pair = exchange.split(":")
            when (pair[0]) {
                "p" -> {
                    preterite = pair[1]
                }
                "d" -> {
                    pastParticiple = pair[1]
                }
                "i" -> {
                    presentParticiple = pair[1]
                }
                "3" -> {
                    third = pair[1]
                }
                "r" -> {
                    er = pair[1]
                }
                "t" -> {
                    est = pair[1]
                }
                "s" -> {
                    plural = pair[1]
                }
                "0" -> {
                    lemma = pair[1]
                }

            }
        }

        Column {
            SelectionContainer {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.height(IntrinsicSize.Max)
                        .width(if(searching) 600.dp else 554.dp)
                        .padding(start = if(searching) 0.dp else 50.dp)

                ) {
                    val textColor = MaterialTheme.colors.onBackground
                    val plainStyle = SpanStyle(
                        color = textColor,
                        fontSize = fontSize,
                    )


                    Text(
                        buildAnnotatedString {
                            if (lemma.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("原型 ")
                                }
                                withStyle(style = plainStyle.copy(color = Color.Magenta)) {
                                    append(lemma)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (preterite.isNotEmpty()) {
                                var color = textColor
                                if (!preterite.endsWith("ed")) {
                                    color = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

                                }
                                withStyle(style = plainStyle) {
                                    append("过去式 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(preterite)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (pastParticiple.isNotEmpty()) {
                                var color = textColor
                                if (!pastParticiple.endsWith("ed")) {
                                    color =
                                        if (MaterialTheme.colors.isLight) MaterialTheme.colors.primary else Color.Yellow
                                }
                                withStyle(style = plainStyle) {
                                    append("过去分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(pastParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (presentParticiple.isNotEmpty()) {
                                val color = if (presentParticiple.endsWith("ing")) textColor else Color(0xFF303F9F)
                                withStyle(style = plainStyle) {
                                    append("现在分词 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(presentParticiple)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                            if (third.isNotEmpty()) {
                                val color = if (third.endsWith("s")) textColor else Color.Cyan
                                withStyle(style = plainStyle) {
                                    append("第三人称单数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(third)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }

                            if (er.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("比较级 $er;")
                                }
                            }
                            if (est.isNotEmpty()) {
                                withStyle(style = plainStyle) {
                                    append("最高级 $est;")
                                }
                            }
                            if (plural.isNotEmpty()) {
                                val color = if (plural.endsWith("s")) textColor else Color(0xFFD84315)
                                withStyle(style = plainStyle) {
                                    append("复数 ")
                                }
                                withStyle(style = plainStyle.copy(color = color)) {
                                    append(plural)
                                }
                                withStyle(style = plainStyle) {
                                    append(";")
                                }
                            }
                        }
                    )

                }
            }
            if(!searching){
                Divider(Modifier.padding(start = 50.dp))
            }
        }


    }

}

/**
 * 英语定义组件
 */
@Composable
fun Definition(
    word: Word,
    definitionVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    fontSize: TextUnit
) {
    if (definitionVisible && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.definition.length - word.definition.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.definition,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * 中文释义组件
 */
@Composable
fun Translation(
    translationVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (translationVisible && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.translation.length - word.translation.replace("\n", "").length
        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(260.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 8) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.translation,
                        )
                    }
                }
                if (rows > 8) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }

            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/**
 * 例句组件
 */
@Composable
fun Sentences(
    sentencesVisible: Boolean,
    isPlaying: Boolean,
    isChangeVideoBounds:Boolean = false,
    word: Word,
    fontSize: TextUnit
) {
    if (sentencesVisible && word.pos.isNotEmpty() && (isChangeVideoBounds || !isPlaying )) {
        // 计算行数,用于判断是否显示滚动条
        // 通过原始字符串长度减去去掉换行符后的长度，得到换行符的个数
        val rows = word.pos.length - word.pos.replace("\n", "").length

        val width = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                600.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                575.dp
            }
            else -> 555.dp
        }
        val normalModifier = Modifier
            .width(width)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        val greaterThen10Modifier = Modifier
            .width(width)
            .height(180.dp)
            .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
        Column {
            Box(modifier = if (rows > 5) greaterThen10Modifier else normalModifier) {
                val stateVertical = rememberScrollState(0)
                Box(Modifier.verticalScroll(stateVertical)) {
                    SelectionContainer {
                        Text(
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.align(Alignment.CenterStart),
                            text = word.pos,
                        )
                    }
                }
                if (rows > 5) {
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )
                }
            }
            Divider(Modifier.padding(start = 50.dp))
        }

    }
}

/** 字幕列表组件
 * @param captionsVisible 字幕的可见性
 * @param playTripleMap 要显示的字幕。Map 的类型参数说明：
 * - Map 的 Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 * @param isPlaying 是否正在播放视频
 * @param volume 音量
 * @param setIsPlaying 设置是否正在播放视频播放的回调
 * @param word 单词
 * @param bounds 视频播放窗口的位置
 * @param textFieldValueList 用户输入的字幕列表
 * @param typingResultMap 用户输入字幕的结果 Map
 * @param putTypingResultMap 添加当前的字幕到结果Map
 * @param checkTyping 检查用户输入的回调
 * @param playKeySound 当用户输入字幕时播放敲击键盘音效的回调
 * @param modifier 修改器
 */
@ExperimentalComposeUiApi
@Composable
fun Captions(
    captionsVisible: Boolean,
    playTripleMap: Map<Int, Triple<Caption, String, Int>>,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    setPlayingMedia: (MediaInfo) -> Unit,
    plyingIndex: Int,
    setPlayingIndex: (Int) -> Unit,
    volume: Float,
    word: Word,
    bounds: Rectangle,
    textFieldValueList: List<String>,
    typingResultMap: Map<Int, MutableList<Pair<Char, Boolean>>>,
    putTypingResultMap: (Int, MutableList<Pair<Char, Boolean>>) -> Unit,
    checkTyping: (Int, String, String) -> Unit,
    playKeySound: () -> Unit,
    modifier: Modifier,
    focusRequesterList:List<FocusRequester>,
    jumpToWord: () -> Unit,
    openSearch: () -> Unit,
    fontSize: TextUnit,
    isWriteSubtitles:Boolean,
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
                val scope = rememberCoroutineScope()
                playTripleMap.forEach { (index, playTriple) ->
                    var captionContent = playTriple.first.content
                    if(!isWriteSubtitles){
                        if (captionContent.endsWith("\r\n")) {
                            captionContent = captionContent.dropLast(2)
                        } else if (captionContent.endsWith("\n")) {
                            captionContent = captionContent.dropLast(1)
                        }
                    }else{
                        if (captionContent.contains("\r\n")) {
                            captionContent = captionContent.replace("\r\n", " ")
                        } else if (captionContent.contains("\n")) {
                            captionContent = captionContent.replace("\n", " ")
                        }
                    }
                    // 当前的字幕是否获得焦点
                    var focused by remember { mutableStateOf(false) }
                    var textFieldValue = textFieldValueList[index]
                    if(!isWriteSubtitles){
                        textFieldValue = captionContent
                    }
                    var typingResult = typingResultMap[index]
                    if (typingResult == null) {
                        typingResult = mutableListOf()
                        putTypingResultMap(index, typingResult)
                    }
                    var isPlayFailed by remember { mutableStateOf(false) }
                    var failedMessage by remember { mutableStateOf("") }
                    val playCurrentCaption:()-> Unit = {
                        if (!isPlaying) {
                            setPlayingIndex(index+1)
                            setIsPlaying(true)
                            val playMedia = MediaInfo(
                                playTriple.first,
                                playTriple.second,
                                playTriple.third,
                            )
                            setPlayingMedia(playMedia)
                        }
                    }
                    var selectable by remember { mutableStateOf(false) }
                    val focusMoveUp:() -> Unit = {
                        if(index == 0){
                            jumpToWord()
                        }else{
                            focusRequesterList[index-1].requestFocus()
                        }
                    }
                    val focusMoveDown:() -> Unit = {
                        if(index<2 && index + 1 < playTripleMap.size){
                            focusRequesterList[index+1].requestFocus()
                        }
                    }
                    val captionKeyEvent:(KeyEvent) -> Boolean = {
                        val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                        when {
                            (it.type == KeyEventType.KeyDown
                                    && it.key != Key.ShiftRight
                                    && it.key != Key.ShiftLeft
                                    && it.key != Key.CtrlRight
                                    && it.key != Key.CtrlLeft
                                    ) -> {
                                scope.launch { playKeySound() }
                                true
                            }
                            (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                scope.launch { selectable = !selectable }
                                true
                            }
                            (it.key == Key.Tab && it.type == KeyEventType.KeyUp) -> {
                                scope.launch {  playCurrentCaption() }
                                true
                            }
                            (it.key == Key.DirectionDown && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            (it.key == Key.DirectionUp && !it.isShiftPressed && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            else -> false
                        }
                    }

                    Caption(
                        isPlaying = isPlaying,
                        isWriteSubtitles = isWriteSubtitles,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        index = index,
                        playingIndex = plyingIndex,
                        focusRequester = focusRequesterList[index],
                        focused = focused,
                        focusChanged = { focused = it },
                        playCurrentCaption = {playCurrentCaption()},
                        captionKeyEvent = {captionKeyEvent(it)},
                        selectable = selectable,
                        setSelectable = {selectable = it},
                        resetPlayState = {isPlayFailed = false },
                        isPlayFailed = isPlayFailed,
                        failedMessage = failedMessage,
                        openSearch = {openSearch()},
                        fontSize = fontSize
                    )
                }

            }
        }
        if (!isPlaying && (word.captions.isNotEmpty() || word.externalCaptions.isNotEmpty()))
            Divider(Modifier.padding(start = 50.dp))
    }
}

fun replaceSeparator(path:String): String {
    val absPath = if (isWindows()) {
        path.replace('/', '\\')
    } else {
        path.replace('\\', '/')
    }
    return absPath
}

/**
 * 获取字幕
 * @return Map 的类型参数说明：
 * Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 */
fun getPlayTripleMap(
    vocabularyType: VocabularyType,
    subtitlesTrackId: Int,
    relateVideoPath:String,
    word: Word
): MutableMap<Int, Triple<Caption, String, Int>> {

    val playTripleMap = mutableMapOf<Int, Triple<Caption, String, Int>>()
    if (vocabularyType == VocabularyType.DOCUMENT) {
        if (word.externalCaptions.isNotEmpty()) {
            word.externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val playTriple =
                    Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }
        }
    } else {
        if (word.captions.isNotEmpty()) {
            word.captions.forEachIndexed { index, caption ->
                val playTriple =
                    Triple(caption, relateVideoPath, subtitlesTrackId)
                playTripleMap[index] = playTriple
            }

        }
    }
    return playTripleMap
}


fun getMediaInfo(
    vocabularyType: VocabularyType,
    subtitlesTrackId: Int,
    relateVideoPath:String,
    captions:List<Caption> ,
    externalCaptions: List<ExternalCaption>,
): MutableMap<Int, MediaInfo> {

    val mediaInfoMap = mutableMapOf<Int, MediaInfo>()
    if (vocabularyType == VocabularyType.DOCUMENT) {
        if (externalCaptions.isNotEmpty()) {
            externalCaptions.forEachIndexed { index, externalCaption ->
                val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                val mediaInfo = MediaInfo(
                    caption,
                    externalCaption.relateVideoPath,
                    externalCaption.subtitlesTrackId
                )
                mediaInfoMap[index] = mediaInfo

            }
        }
    } else {
        if (captions.isNotEmpty()) {
            captions.forEachIndexed { index, caption ->
                val mediaInfo = MediaInfo(
                    caption,
                    relateVideoPath,
                    subtitlesTrackId
                )
                mediaInfoMap[index] = mediaInfo
            }

        }
    }
    return mediaInfoMap
}

fun secondsToString(seconds: Double): String {
    val duration = Duration.ofMillis((seconds * 1000).toLong())
    return String.format(
        "%02d:%02d:%02d.%03d",
        duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart()
    )
}

/**
 * 字幕组件
 * @param isPlaying 是否正在播放
 * @param isWriteSubtitles 是否抄写字幕
 * @param captionContent 字幕的内容
 * @param textFieldValue 输入的字幕
 * @param typingResult 输入字幕的结果
 * @param checkTyping 输入字幕后被调用的回调
 * @param index 当前字幕的索引
 * @param playingIndex 正在播放的字幕索引
 * @param focusRequester 焦点请求器
 * @param focused 是否获得焦点
 * @param focusChanged 处理焦点变化的函数
 * @param playCurrentCaption 播放当前字幕的函数
 * @param captionKeyEvent 处理当前字幕的快捷键函数
 * @param selectable 是否可选择复制
 * @param setSelectable 设置是否可选择
 * @param isPlayFailed 是否路径错误
 */
@OptIn(
    ExperimentalFoundationApi::class,
)
@Composable
fun Caption(
    isPlaying: Boolean,
    isWriteSubtitles: Boolean,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    index: Int,
    playingIndex: Int,
    focusRequester:FocusRequester,
    focused: Boolean,
    focusChanged:(Boolean) -> Unit,
    playCurrentCaption:()-> Unit,
    captionKeyEvent:(KeyEvent) -> Boolean,
    selectable:Boolean,
    setSelectable:(Boolean) -> Unit,
    isPlayFailed:Boolean,
    resetPlayState:() -> Unit,
    failedMessage:String,
    openSearch: () -> Unit,
    fontSize: TextUnit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        // 字幕的行数
        val row = if(isWriteSubtitles) 1 else captionContent.split("\n").size
        val rowHeight = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                24.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                20.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.subtitle2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body1.fontSize -> {
                16.dp * 2 * row + 4.dp
            }
            MaterialTheme.typography.body2.fontSize -> {
                14.dp * 2 * row + 4.dp
            }
            else -> 16.dp * 2 * row + 4.dp
        }
        val background = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary.copy(alpha = 0.05f) else MaterialTheme.colors.background
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(rowHeight).width(IntrinsicSize.Max).background(background)
        ) {
            val dropMenuFocusRequester = remember { FocusRequester() }
            Box(Modifier.width(IntrinsicSize.Max)) {
                val textHeight = rowHeight -4.dp
                CustomTextMenuProvider {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            checkTyping(index, input, captionContent)
                        },
                        singleLine = isWriteSubtitles,
                        readOnly = !isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = LocalTextStyle.current.copy(
                            color = if(focused && !isWriteSubtitles) MaterialTheme.colors.primary else  MaterialTheme.colors.onBackground,
                            fontSize = fontSize
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(textHeight)
                            .align(Alignment.CenterStart)
                            .focusRequester(focusRequester)
                            .onFocusChanged {focusChanged(it.isFocused)}
                            .onKeyEvent { captionKeyEvent(it) }
                    )
                }

                if(isWriteSubtitles){
                    Text(
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.align(Alignment.CenterStart).height(textHeight),
                        overflow = TextOverflow.Ellipsis,
                        text = buildAnnotatedString(captionContent, typingResult, fontSize)
                    )
                }


                DropdownMenu(
                    expanded = selectable,
                    onDismissRequest = { setSelectable(false) },
                    offset = DpOffset(0.dp, (if(isWriteSubtitles)-30 else -70).dp)
                ) {
                    // 增加一个检查，检查字幕的字符长度，有的字幕是机器生成的，一段可能会有很多字幕，
                    // 可能会超出限制，导致程序崩溃。
                    val content = if(captionContent.length>400){
                       captionContent.substring(0,400)
                    }else captionContent

                    BasicTextField(
                        value = content,
                        onValueChange = {},
                        singleLine = isWriteSubtitles,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle =  LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                            fontSize = fontSize,
                        ),
                        modifier = Modifier.focusable()
                            .focusRequester(dropMenuFocusRequester)
                            .onKeyEvent {
                                val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                                if (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                    scope.launch { setSelectable(!selectable) }
                                    true
                                }else if (isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                    scope.launch { openSearch() }
                                    true
                                } else false
                            }
                    )
                    LaunchedEffect(Unit) {
                        dropMenuFocusRequester.requestFocus()
                    }

                }
            }

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val plus = if (isMacOS()) "" else "+"
                        val shortcutText: Any = when (index) {
                            0 -> "$ctrl+$plus 1"
                            1 -> "$ctrl+$plus 2"
                            2 -> "$ctrl+$plus 3"
                            else -> println("字幕数量超出范围")
                        }
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "播放  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText.toString())
                            }
                        }
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
                    playCurrentCaption()
                },
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    val tint = if(isPlaying && playingIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = tint
                    )
                }
            }
            if (isPlayFailed) {
                Text(failedMessage, color = Color.Red)
                Timer("恢复状态", false).schedule(2000) {
                    resetPlayState()
                }
            }
        }
    }


}

@Composable
fun buildAnnotatedString(
    captionContent:String,
    typingResult:List<Pair<Char, Boolean>>,
    fontSize: TextUnit,
):AnnotatedString{
    return buildAnnotatedString {
        typingResult.forEach { (char, correct) ->
            if (correct) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colors.primary,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
                    )
                ) {
                    append(char)
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        fontSize = fontSize,
                        letterSpacing = LocalTextStyle.current.letterSpacing,
                        fontFamily = LocalTextStyle.current.fontFamily,
                    )
                ) {
                    if (char == ' ') {
                        append("_")
                    } else {
                        append(char)
                    }

                }
            }
        }

        if (!(typingResult.isNotEmpty() && captionContent.length < typingResult.size)) {
            var remainChars = captionContent.substring(typingResult.size)
            // 增加一个检查，检查字幕的字符长度，有的字幕是机器生成的，一段可能会有很多字幕，
            // 可能会超出限制，导致程序崩溃。
            if (remainChars.length > 400) {
                remainChars = remainChars.substring(0, 400)
            }

            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colors.onBackground,
                    fontSize = fontSize,
                    letterSpacing = LocalTextStyle.current.letterSpacing,
                    fontFamily = LocalTextStyle.current.fontFamily,
                )
            ) {
                append(remainChars)
            }
        }

    }
}

/** 删除按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun DeleteButton(
    onClick:()->Unit,
    tooltipAlignment: Alignment = Alignment.TopCenter,
){
    val offset = if(tooltipAlignment == Alignment.TopCenter) DpOffset(0.dp,0.dp) else DpOffset(0.dp,10.dp)
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "删除单词  ")
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "$ctrl Delete" else "$ctrl+Delete"
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = shortcutText)
                    }
                }
            }
        },

        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = tooltipAlignment,
            alignment = tooltipAlignment,
            offset = offset
        )
    ) {
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}
/** 编辑按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun EditButton(onClick: () -> Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "编辑", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = {
//            showEditWordDialog = true
            onClick()
        }) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** 困难单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HardButton(
    contains:Boolean,
    onClick: () -> Unit,
    fontFamily:FontFamily,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    val text = if(contains) "从困难词库中移除  " else "添加到困难词库  "
                    Text(text = text)

                    val ctrl = LocalCtrl.current
                    val shortcut = if (isMacOS()) "$ctrl " else "$ctrl+"
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = "  $shortcut ")
                        Text(text = "I", fontFamily = fontFamily)
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {

        IconButton(onClick = { onClick() }) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            val tint = if(contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = tint
            )
        }
    }
}

/** 熟悉单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FamiliarButton(
    onClick: () -> Unit,
    alignment: Alignment = Alignment.TopCenter,
){
    val offset = if(alignment == Alignment.TopCenter) DpOffset(0.dp,0.dp) else DpOffset(0.dp,10.dp)
    val text = if(alignment == Alignment.TopCenter) "移动到熟悉词库" else "添加到熟悉词库"
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "$text  ")
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "$ctrl Y" else "$ctrl+Y"
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = shortcutText)
                    }
                }
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = alignment,
            alignment = alignment,
            offset = offset
        )
    ) {
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

/** 使用快捷键 Ctrl + I,把当前单词加入到困难单词时显示 0.3 秒后消失 */
@Composable
fun BookmarkButton(
    modifier: Modifier,
    contains:Boolean,
    disappear:() ->Unit
){
        IconButton(onClick = {},modifier = modifier) {
            val icon = if(contains) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
            val tint = if(contains) Color(255, 152, 0) else MaterialTheme.colors.onBackground
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = tint,
            )
            SideEffect{
                Timer("不显示 Bookmark 图标", false).schedule(300) {
                    disappear()
                }
            }
        }

}

/** 复制按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CopyButton(
    wordValue:String,
    alignment: Alignment = Alignment.TopCenter,
){
    val offset = if(alignment == Alignment.TopCenter) DpOffset(0.dp,0.dp) else DpOffset(0.dp,10.dp)

    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "复制  ")
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "$ctrl C" else "$ctrl+C"
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = shortcutText)
                    }
                }

            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = alignment,
            alignment = alignment,
            offset
        )
    ) {
        val clipboardManager = LocalClipboardManager.current
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(wordValue))
        },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                clipboardManager.setText(AnnotatedString(wordValue))
                true
            }else false
        }) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}


/**
 * 从外部字幕中获取媒体信息
 * @param externalCaptions 外部字幕列表
 * @param index links 的 index
 * @return MediaInfo? 如果 index 小于 currentWord.externalCaptions.size，则返回 MediaInfo，否则返回 null
 */
fun getMediaInfoFromExternalCaption(externalCaptions: MutableList<ExternalCaption>, index: Int): MediaInfo? {
    return if (index < externalCaptions.size) {
        val externalCaption = externalCaptions[index]
        val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
        MediaInfo(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
    } else {
        null
    }
}



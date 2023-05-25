package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.*
import state.MemoryStrategy.*
import ui.dialog.ChapterFinishedDialog
import ui.dialog.ConfirmDialog
import ui.dialog.EditWordDialog
import ui.dialog.SelectChapterDialog
import java.awt.Component
import java.awt.Rectangle
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule

/**
 * 应用程序的核心组件，记忆单词界面
 * @param appState 应用程序的全局状态
 * @param typingWord 记忆单词界面的状态容器
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
fun TypingWord(
    window: ComposeWindow,
    title: String,
    appState: AppState,
    typingWord: WordState,
    videoBounds: Rectangle,
    resetVideoBounds :() -> Rectangle,
    showPlayer :(Boolean) -> Unit,
    setVideoPath:(String) -> Unit,
    vocabularyPathChanged:(String) -> Unit
) {


    //设置窗口的拖放处理函数
    LaunchedEffect(Unit){
        setWindowTransferHandler(
            window = window,
            state = appState,
            wordState = typingWord,
            showVideoPlayer = showPlayer,
            setVideoPath = setVideoPath,
            vocabularyPathChanged = vocabularyPathChanged
        )
    }

    Box(Modifier.background(MaterialTheme.colors.background)) {
        Row {
            val dictationState = rememberDictationState()
            TypingWordSidebar(appState,typingWord,dictationState)
            if (appState.openSettings) {
                val topPadding = if (isMacOS()) 30.dp else 0.dp
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(Modifier.fillMaxSize()) {
                /** 当前正在记忆的单词 */
                val currentWord = if(typingWord.vocabulary.wordList.isNotEmpty()){
                    typingWord.getCurrentWord()
                }else  null

                if (currentWord != null) {
                    MainContent(
                        appState =appState,
                        typingWord = typingWord,
                        dictationState = dictationState,
                        currentWord = currentWord,
                        videoBounds = videoBounds,
                        resetVideoBounds = resetVideoBounds,
                        window = window,
                        modifier = Modifier.align(Alignment.Center)
                            .padding(end = 0.dp,bottom = 58.dp)
                    )
                } else {
                    VocabularyEmpty()
                }

                Header(
                    wordState = typingWord,
                    title = title,
                    window = window,
                    modifier = Modifier.align(Alignment.TopCenter)
                )


            }
        }

        Row( modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = appState.openSettings,
                setIsOpen ={ appState.openSettings = it },
                modifier = Modifier,
                globalState = appState.global,
                saveGlobalState = {appState.saveGlobalState()},
                showPlayer = showPlayer
            )
            val ctrl = LocalCtrl.current
            var showFilePicker by remember {mutableStateOf(false)}
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Text(text = "打开词库文件 $ctrl + O", modifier = Modifier.padding(10.dp))
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
                    onClick = {
                        if (isWindows()) {
                            showFilePicker = true
                        } else if (isMacOS()) {
                            Thread {
                                val fileChooser = appState.futureFileChooser.get()
                                fileChooser.dialogTitle = "选择词库"
                                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                fileChooser.selectedFile = null
                                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    val index = appState.findVocabularyIndex(file)
                                    appState.changeVocabulary(
                                        vocabularyFile = file,
                                        typingWord,
                                        index
                                    )
                                    appState.global.type = TypingType.WORD
                                    appState.saveGlobalState()
                                }
                            }.start()
                        }

                    },
                    modifier = Modifier.padding(top = if (isMacOS()) 30.dp else 0.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            RemoveButton(onClick = {
                typingWord.index = 0
                typingWord.vocabulary.size = 0
                typingWord.vocabulary.name = ""
                typingWord.vocabulary.relateVideoPath = ""
                typingWord.vocabulary.wordList.clear()
                typingWord.vocabularyName = ""
                typingWord.vocabularyPath = ""
                typingWord.saveTypingWordState()
            }, toolTip = "移除当前词库")
            FilePicker(
                show = showFilePicker,
                fileExtension = "json",
                initialDirectory = ""){path ->
                if(!path.isNullOrEmpty()){
                    val file = File(path)
                    val index = appState.findVocabularyIndex(file)
                    appState.changeVocabulary(
                        vocabularyFile = file,
                        typingWord,
                        index
                    )
                    appState.global.type = TypingType.WORD
                    appState.saveGlobalState()
                }
                showFilePicker = false
            }
        }

    }

}


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun Header(
    wordState: WordState,
    title:String,
    window: ComposeWindow,
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
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center){
            // 记忆单词时的状态信息
            val text = when(wordState.memoryStrategy){
                Normal -> { if(wordState.vocabulary.size>0) "${wordState.index + 1}/${wordState.vocabulary.size}" else ""}
                Dictation -> { "听写测试   ${wordState.dictationIndex + 1}/${wordState.dictationWords.size}"}
                Review -> {"听写复习   ${wordState.dictationIndex + 1}/${wordState.reviewWords.size}"}
                NormalReviewWrong -> { "复习错误单词   ${wordState.dictationIndex + 1}/${wordState.wrongWords.size}"}
                DictationReviewWrong -> { "听写复习 - 复习错误单词   ${wordState.dictationIndex + 1}/${wordState.wrongWords.size}"}
            }

            val top = if(wordState.memoryStrategy == Review || wordState.memoryStrategy == DictationReviewWrong) 0.dp else 12.dp
            Text(
                text = text,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(top = top )
            )
            if(wordState.memoryStrategy == Review || wordState.memoryStrategy == DictationReviewWrong){
                Spacer(Modifier.width(20.dp))
                ExitButton(onClick = {
                    wordState.showInfo()
                    wordState.memoryStrategy = Normal
                    if( wordState.wrongWords.isNotEmpty()){
                        wordState.wrongWords.clear()
                    }
                    if(wordState.reviewWords.isNotEmpty()){
                        wordState.reviewWords.clear()
                    }

                })
            }
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalSerializationApi::class
)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainContent(
    appState: AppState,
    typingWord: WordState,
    dictationState:  DictationState,
    currentWord:Word,
    videoBounds: Rectangle,
    resetVideoBounds :() -> Rectangle,
    window: ComposeWindow,
    modifier: Modifier
){
    Box(
        modifier = modifier
    ) {

        /** 协程构建器 */
        val scope = rememberCoroutineScope()

        /** 当前单词的正确次数 */
        var wordCorrectTime by remember {mutableStateOf(0)}

        /** 当前单词的错误次数 */
        var wordWrongTime by remember {mutableStateOf(0)}

        /** 单词输入错误*/
        var isWrong by remember { mutableStateOf(false) }

        /** 单词输入框里的字符串*/
        var wordTextFieldValue by remember { mutableStateOf("") }

        /** 第一条字幕的输入字符串*/
        var captionsTextFieldValue1 by remember { mutableStateOf("") }

        /** 第二条字幕的输入字符串*/
        var captionsTextFieldValue2 by remember { mutableStateOf("") }

        /** 第三条字幕的输入字符串*/
        var captionsTextFieldValue3 by remember { mutableStateOf("") }

        /** 单词输入框输入的结果*/
        val wordTypingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }

        /** 字幕输入框的结果 */
        val captionsTypingResultMap =
            remember { mutableStateMapOf<Int, MutableList<Pair<Char, Boolean>>>() }

        /** 是否正在播放视频 */
        var isPlaying by remember { mutableStateOf(false) }

        /** 快捷键播放字幕的索引 */
        var plyingIndex by remember { mutableStateOf(0) }

        /** 显示填充后的书签图标 */
        var showBookmark by remember { mutableStateOf(false) }

        /** 显示删除对话框 */
        var showDeleteDialog by remember { mutableStateOf(false) }

        /** 显示把当前单词加入到熟悉词库的确认对话框 */
        var showFamiliarDialog by remember { mutableStateOf(false) }

        /** 单词输入框的焦点请求器*/
        val wordFocusRequester = remember { FocusRequester() }

        /** 字幕输入框焦点请求器*/
        val (focusRequester1,focusRequester2,focusRequester3) = remember { FocusRequester.createRefs() }

        /** 等宽字体*/
        val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }

        val audioPlayerComponent = LocalAudioPlayerComponent.current

        val clipboardManager = LocalClipboardManager.current
        /** 单词发音的本地路径，这个路径是根据单词进行计算的，
         * 如果单词改变了，单词发音就跟着改变。*/
        val audioPath by remember(currentWord){
            derivedStateOf {
                getAudioPath(
                    word = currentWord.value,
                    audioSet = appState.localAudioSet,
                    addToAudioSet = {appState.localAudioSet.add(it)},
                    pronunciation = typingWord.pronunciation
                )
            }
        }

        /** 是否正在播放单词发音 */
        var isPlayingAudio by remember { mutableStateOf(false) }

        /**
         * 用快捷键播放视频时被调用的函数，
         * Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
         */
        @OptIn(ExperimentalSerializationApi::class)
        val shortcutPlay: (playTriple: Triple<Caption, String, Int>?) -> Unit = { playTriple ->
            if (playTriple != null && !isPlaying) {
                scope.launch {
                    val file = File(playTriple.second)
                    if (file.exists()) {
                        isPlaying = true
                        play(
                            window = appState.videoPlayerWindow,
                            setIsPlaying = { isPlaying = it },
                            appState.global.videoVolume,
                            playTriple,
                            appState.videoPlayerComponent,
                            videoBounds,
                            typingWord.externalSubtitlesVisible,
                            resetVideoBounds = resetVideoBounds,
                            isVideoBoundsChanged = appState.isChangeVideoBounds,
                            setIsVideoBoundsChanged = {appState.isChangeVideoBounds = it}
                        )
                    }
                }
            }
        }

        /** 清除当前单词的状态 */
        val clear:() -> Unit = {
            wordTypingResult.clear()
            wordTextFieldValue = ""
            captionsTypingResultMap.clear()
            captionsTextFieldValue1 = ""
            captionsTextFieldValue2 = ""
            captionsTextFieldValue3 = ""
            wordCorrectTime = 0
            wordWrongTime = 0
        }



        /** 删除当前单词 */
        val deleteWord:() -> Unit = {
            val index = typingWord.index
            typingWord.vocabulary.wordList.removeAt(index)
            typingWord.vocabulary.size = typingWord.vocabulary.wordList.size
            if(typingWord.vocabulary.name == "HardVocabulary"){
                appState.hardVocabulary.wordList.remove(currentWord)
                appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            }
            clear()
            typingWord.saveCurrentVocabulary()
        }

        /** 把当前单词加入到熟悉词库 */
        val addToFamiliar:() -> Unit = {
            val file = getFamiliarVocabularyFile()
            val familiar =  loadVocabulary(file.absolutePath)
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (typingWord.vocabulary.type == VocabularyType.MKV ||
                typingWord.vocabulary.type == VocabularyType.SUBTITLES
            ) {
                currentWord.captions.forEach{caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = typingWord.vocabulary.relateVideoPath,
                        subtitlesTrackId = typingWord.vocabulary.subtitlesTrackId,
                        subtitlesName = typingWord.vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    currentWord.externalCaptions.add(externalCaption)
                }
                currentWord.captions.clear()

            }
            if(!familiar.wordList.contains(currentWord)){
                familiar.wordList.add(currentWord)
                familiar.size = familiar.wordList.size
            }
            saveVocabulary(familiar,file.absolutePath)
            deleteWord()
            showFamiliarDialog = false
        }

        /** 处理加入到困难词库的函数 */
        val bookmarkClick :() -> Unit = {
            val contains = appState.hardVocabulary.wordList.contains(currentWord)
            if(contains){
                appState.hardVocabulary.wordList.remove(currentWord)
                if(typingWord.vocabulary.name == "HardVocabulary"){
                    typingWord.vocabulary.wordList.remove(currentWord)
                    typingWord.vocabulary.size = typingWord.vocabulary.wordList.size
                    typingWord.saveCurrentVocabulary()
                }
            }else{
                val relateVideoPath = typingWord.vocabulary.relateVideoPath
                val subtitlesTrackId = typingWord.vocabulary.subtitlesTrackId
                val subtitlesName =
                    if (typingWord.vocabulary.type == VocabularyType.SUBTITLES) typingWord.vocabulary.name else ""
                val hardWord = currentWord.deepCopy()

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
            appState.hardVocabulary.size = appState.hardVocabulary.wordList.size
            appState.saveHardVocabulary()
        }

        /** 处理全局快捷键的回调函数 */
        val globalKeyEvent: (KeyEvent) -> Boolean = {
            when {
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        wordFocusRequester.requestFocus()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSearch()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.phoneticVisible = !typingWord.phoneticVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.morphologyVisible = !typingWord.morphologyVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.E && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.definitionVisible = !typingWord.definitionVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.translationVisible = !typingWord.translationVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.wordVisible = !typingWord.wordVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }

                (it.isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
                    if (!isPlayingAudio) {
                        playAudio(
                            word = currentWord.value,
                            audioPath = audioPath,
                            pronunciation =  typingWord.pronunciation,
                            volume = appState.global.audioVolume,
                            audioPlayerComponent = audioPlayerComponent,
                            changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                            setIsAutoPlay = {}
                        )
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        typingWord.subtitlesVisible = !typingWord.subtitlesVisible
                        typingWord.saveTypingWordState()
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        appState.openSettings = !appState.openSettings
                    }
                    true
                }
                (it.isCtrlPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        bookmarkClick()
                    }
                    showBookmark = true
                    true
                }
                (it.isCtrlPressed && it.key == Key.Y && it.type == KeyEventType.KeyUp) -> {
                    if(typingWord.vocabulary.name == "FamiliarVocabulary"){
                        JOptionPane.showMessageDialog(window, "不能把熟悉词库的单词添加到熟悉词库")
                    }else{
                        showFamiliarDialog = true
                    }
                    true
                }
                (it.isShiftPressed && it.key == Key.Delete && it.type == KeyEventType.KeyUp) -> {
                    scope.launch {
                        showDeleteDialog = true
                    }
                    true
                }
                else -> false
            }

        }

        val globalPreviewKeyEvent: (KeyEvent) -> Boolean = {
            when{
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.Z && it.type == KeyEventType.KeyUp) -> {
                    if(typingWord.memoryStrategy != Dictation && typingWord.memoryStrategy != Review ){
                        val playTriple = if (typingWord.vocabulary.type == VocabularyType.DOCUMENT) {
                            getPayTriple(currentWord, 0)
                        } else {
                            val caption = typingWord.getCurrentWord().captions[0]
                            Triple(caption, typingWord.vocabulary.relateVideoPath, typingWord.vocabulary.subtitlesTrackId)
                        }
                        plyingIndex = 0
                        if (playTriple != null && typingWord.subtitlesVisible &&  typingWord.isWriteSubtitles ) focusRequester1.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.X && it.type == KeyEventType.KeyUp) -> {
                    if(typingWord.memoryStrategy != Dictation && typingWord.memoryStrategy != Review){
                        val playTriple = if (typingWord.getCurrentWord().externalCaptions.size >= 2) {
                            getPayTriple(currentWord, 1)
                        } else if (typingWord.getCurrentWord().captions.size >= 2) {
                            val caption = typingWord.getCurrentWord().captions[1]
                            Triple(caption, typingWord.vocabulary.relateVideoPath, typingWord.vocabulary.subtitlesTrackId)
                        }else null
                        plyingIndex = 1
                        if (playTriple != null && typingWord.subtitlesVisible && typingWord.isWriteSubtitles) focusRequester2.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                (it.isCtrlPressed && it.isShiftPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                    if(typingWord.memoryStrategy != Dictation && typingWord.memoryStrategy != Review){
                        val playTriple = if (typingWord.getCurrentWord().externalCaptions.size >= 3) {
                            getPayTriple(currentWord, 2)
                        } else if (typingWord.getCurrentWord().captions.size >= 3) {
                            val caption = typingWord.getCurrentWord().captions[2]
                            Triple(caption, typingWord.vocabulary.relateVideoPath, typingWord.vocabulary.subtitlesTrackId)
                        }else null
                        plyingIndex = 2
                        if (playTriple != null && typingWord.subtitlesVisible && typingWord.isWriteSubtitles) focusRequester3.requestFocus()
                        shortcutPlay(playTriple)
                    }
                    true
                }
                else -> false
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .onKeyEvent { globalKeyEvent(it) }
                .onPreviewKeyEvent { globalPreviewKeyEvent(it) }
                .width(intrinsicSize = IntrinsicSize.Max)
                .background(MaterialTheme.colors.background)
                .focusable(true)
        ) {

            /** 当前章节的正确数，主要用于听写模式计算正确率 */
            var chapterCorrectTime by remember { mutableStateOf(0F)}

            /** 当前章节的错误数，主要用于听写模式计算正确率 */
            var chapterWrongTime by remember { mutableStateOf(0F)}

            /** 听写模式的错误单词，主要用于听写模式计算正确率*/
            val dictationWrongWords = remember { mutableStateMapOf<Word, Int>()}

            /** 显示本章节已经完成对话框 */
            var showChapterFinishedDialog by remember { mutableStateOf(false) }

            /** 显示整个词库已经学习完成对话框 */
            var isVocabularyFinished by remember { mutableStateOf(false) }

            /** 显示编辑单词对话框 */
            var showEditWordDialog by remember { mutableStateOf(false) }



            /** 重置章节计数器,清空听写模式存储的错误单词 */
            val resetChapterTime: () -> Unit = {
                chapterCorrectTime = 0F
                chapterWrongTime = 0F
                dictationWrongWords.clear()
            }



            /** 播放错误音效 */
            val playBeepSound = {
                if (typingWord.isPlaySoundTips) {
                    playSound("audio/beep.wav", typingWord.soundTipsVolume)
                }
            }

            /** 播放成功音效 */
            val playSuccessSound = {
                if (typingWord.isPlaySoundTips) {
                    playSound("audio/hint.wav", typingWord.soundTipsVolume)
                }
            }

            /** 播放整个章节完成时音效 */
            val playChapterFinished = {
                if (typingWord.isPlaySoundTips) {
                    playSound("audio/Success!!.wav", typingWord.soundTipsVolume)
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
                if (wordCorrectTime == 0) {
                    chapterWrongTime++
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
                if((typingWord.memoryStrategy != Dictation && typingWord.memoryStrategy != Review) &&
                    typingWord.subtitlesVisible && (currentWord.captions.isNotEmpty() || currentWord.externalCaptions.isNotEmpty())
                ){
                    focusRequester1.requestFocus()
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
                if (typingWord.dictationIndex + 1 == typingWord.wrongWords.size) {
                    delayPlaySound()
                } else typingWord.dictationIndex++
            }

            /** 切换到下一个单词 */
            val toNext: () -> Unit = {
                scope.launch {
                    clear()
                }
                scope.launch {
                    when (typingWord.memoryStrategy) {
                        Normal -> {
                            when {
                                (typingWord.index == typingWord.vocabulary.size - 1) -> {
                                    isVocabularyFinished = true
                                    playChapterFinished()
                                    showChapterFinishedDialog = true
                                }
                                ((typingWord.index + 1) % 20 == 0) -> {
                                    playChapterFinished()
                                    showChapterFinishedDialog = true
                                }
                                else -> typingWord.index += 1
                            }
                            typingWord.saveTypingWordState()
                        }
                        Dictation -> {
                            if (typingWord.dictationIndex + 1 == typingWord.dictationWords.size) {
                                delayPlaySound()
                            } else typingWord.dictationIndex++
                        }
                        Review -> {
                            if (typingWord.dictationIndex + 1 == typingWord.reviewWords.size) {
                                delayPlaySound()
                            } else typingWord.dictationIndex++
                        }
                        NormalReviewWrong -> { increaseWrongIndex() }
                        DictationReviewWrong -> { increaseWrongIndex() }
                    }

                    wordFocusRequester.requestFocus()
                }
            }

            /** 切换到上一个单词,听写时不允许切换到上一个单词 */
            val previous :() -> Unit = {
                scope.launch {
                    // 正常记忆单词
                    if(typingWord.memoryStrategy == Normal){
                        clear()
                        if((typingWord.index) % 20 != 0 ){
                            typingWord.index -= 1
                            typingWord.saveTypingWordState()
                        }
                        // 复习错误单词
                    }else if (typingWord.memoryStrategy == NormalReviewWrong || typingWord.memoryStrategy == DictationReviewWrong ){
                        clear()
                        if(typingWord.dictationIndex > 0 ){
                            typingWord.dictationIndex -= 1
                        }
                    }
                }
            }

            /** 检查输入的单词 */
            val checkWordInput: (String) -> Unit = { input ->
                if(!isWrong){
                    wordTextFieldValue = input
                    wordTypingResult.clear()
                    var done = true
                    /**
                     *  防止用户粘贴内容过长，如果粘贴的内容超过 word.value 的长度，
                     * 会改变 BasicTextField 宽度，和 Text 的宽度不匹配
                     */
                    if (input.length > currentWord.value.length) {
                        wordTypingResult.clear()
                        wordTextFieldValue = ""
                    } else {
                        val inputChars = input.toList()
                        for (i in inputChars.indices) {
                            val inputChar = inputChars[i]
                            val wordChar = currentWord.value[i]
                            if (inputChar == wordChar) {
                                wordTypingResult.add(Pair(inputChar, true))
                            } else {
                                // 字母输入错误
                                wordTypingResult.add(Pair(wordChar, false))
                                done = false
                                playBeepSound()
                                isWrong = true
                                wordWrongTime++
                                // 如果是听写测试，或听写复习，需要汇总错误单词
                                if (typingWord.memoryStrategy == Dictation || typingWord.memoryStrategy == Review) {
                                    chapterWrongTime++
                                    val dictationWrongTime = dictationWrongWords[currentWord]
                                    if (dictationWrongTime != null) {
                                        dictationWrongWords[currentWord] = dictationWrongTime + 1
                                    } else {
                                        dictationWrongWords[currentWord] = 1
                                    }
                                }

                                Timer("input wrong cleanInputChar", false).schedule(500) {
                                    wordTextFieldValue = ""
                                    wordTypingResult.clear()
                                    isWrong = false
                                }
                            }
                        }
                        // 用户输入的单词完全正确
                        if (wordTypingResult.size == currentWord.value.length && done) {
                            // 输入完全正确
                            playSuccessSound()
                            if (typingWord.memoryStrategy == Dictation || typingWord.memoryStrategy == Review) chapterCorrectTime++
                            if (typingWord.isAuto) {

                                Timer("input correct to next", false).schedule(50) {
                                    toNext()
                                }
                            } else {
                                wordCorrectTime++

                                Timer("input correct clean InputChar", false).schedule(50){
                                    wordTypingResult.clear()
                                    wordTextFieldValue = ""
                                }
                            }
                        }
                    }
                }

            }


            /** 检查输入的字幕 */
            val checkCaptionsInput: (Int, String, String) -> Unit = { index, input, captionContent ->
                when(index){
                    0 -> captionsTextFieldValue1 = input
                    1 -> captionsTextFieldValue2 = input
                    2 -> captionsTextFieldValue3 = input
                }
                val typingResult = captionsTypingResultMap[index]
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
                                0 -> captionsTextFieldValue1 = textFieldValue
                                1 -> captionsTextFieldValue2 = textFieldValue
                                2 -> captionsTextFieldValue3 = textFieldValue
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
                if(typingWord.index == typingWord.vocabulary.size - 1){
                    val mod = typingWord.vocabulary.size % 20
                    typingWord.index -= (mod-1)
                }else if (typingWord.vocabulary.size > 19) typingWord.index -= 19
                else typingWord.index = 0
            }

            /** 计算正确率 */
            val correctRate: () -> Float = {
                if (chapterCorrectTime == 0F) {
                    0F
                } else {
                    val rateDouble = chapterCorrectTime.div(chapterCorrectTime + chapterWrongTime).toDouble()
                    val rateD = BigDecimal(rateDouble).setScale(3, RoundingMode.HALF_EVEN)

                    rateD.times(BigDecimal(100)).toFloat()
                }

            }

            /** 重复学习本章 */
            val learnAgain: () -> Unit = {
                decreaseIndex()
                resetChapterTime()
                typingWord.saveTypingWordState()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }


            /** 复习错误单词 */
            val reviewWrongWords: () -> Unit = {
                val reviewList = dictationWrongWords.keys.toList()
                if (reviewList.isNotEmpty()) {
                    typingWord.showInfo(clear = false)
                    if (typingWord.memoryStrategy == Review ||
                        typingWord.memoryStrategy == DictationReviewWrong
                    ) {
                        typingWord.memoryStrategy = DictationReviewWrong
                    }else{
                        typingWord.memoryStrategy = NormalReviewWrong
                    }

                    typingWord.wrongWords.addAll(reviewList)
                    typingWord.dictationIndex = 0
                    resetChapterTime()
                    showChapterFinishedDialog = false
                }
            }

            /** 下一章 */
            val nextChapter: () -> Unit = {

                if (typingWord.memoryStrategy == NormalReviewWrong ||
                    typingWord.memoryStrategy == DictationReviewWrong
                ) {
                    typingWord.wrongWords.clear()
                }

                if( typingWord.memoryStrategy == Dictation){
                    typingWord.showInfo()
                }

                typingWord.index += 1
                typingWord.chapter++
                resetChapterTime()
                typingWord.memoryStrategy = Normal
                typingWord.saveTypingWordState()
                showChapterFinishedDialog = false
            }


            /** 正常记忆单词，进入到听写测试，需要的单词 */
            val shuffleNormal:() -> Unit = {
                val wordValue = typingWord.getCurrentWord().value
                val shuffledList = typingWord.generateDictationWords(wordValue)
                typingWord.dictationWords.clear()
                typingWord.dictationWords.addAll(shuffledList)
            }
            /** 从听写复习再次进入到听写测试时，需要的单词 */
            val shuffleDictationReview:() -> Unit = {
                var shuffledList = typingWord.reviewWords.shuffled()
                // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
                while(shuffledList.first() == currentWord){
                    shuffledList = typingWord.reviewWords.shuffled()
                }
                typingWord.reviewWords.clear()
                typingWord.reviewWords.addAll(shuffledList)
            }
            /** 进入听写模式 */
            val enterDictation: () -> Unit = {
                scope.launch {
                    typingWord.saveTypingWordState()
                    when(typingWord.memoryStrategy){
                        // 从正常记忆单词第一次进入到听写测试
                        Normal -> {
                            shuffleNormal()
                            typingWord.memoryStrategy = Dictation
                            typingWord.dictationIndex = 0
                            typingWord.hiddenInfo(dictationState)
                        }
                        // 正常记忆单词时选择再次听写
                        Dictation ->{
                            shuffleNormal()
                            typingWord.dictationIndex = 0
                        }
                        // 从复习错误单词进入到听写测试，这里有两种情况：
                        // 一种是从正常记忆单词进入到复习错误单词，复习完毕后，再次听写
                        NormalReviewWrong ->{
                            typingWord.memoryStrategy = Dictation
                            typingWord.wrongWords.clear()
                            shuffleNormal()
                            typingWord.dictationIndex = 0
                            typingWord.hiddenInfo(dictationState)
                        }
                        // 一种是从听写复习进入到复习错误单词，复习完毕后，再次听写
                        DictationReviewWrong ->{
                            typingWord.memoryStrategy = Review
                            typingWord.wrongWords.clear()
                            shuffleDictationReview()
                            typingWord.dictationIndex = 0
                            typingWord.hiddenInfo(dictationState)
                        }
                        // 在听写复习时选择再次听写
                        Review ->{
                            shuffleDictationReview()
                            typingWord.dictationIndex = 0
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
                    if(typingWord.vocabularyPath.startsWith(path)){
                        val fileChooser = appState.futureFileChooser.get()
                        fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                        fileChooser.dialogTitle = "保存重置后的词库"
                        val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                        val fileName = File(typingWord.vocabularyPath).nameWithoutExtension
                        fileChooser.selectedFile = File("$myDocuments${File.separator}$fileName.json")
                        val userSelection = fileChooser.showSaveDialog(window)
                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                            val selectedFile = fileChooser.selectedFile
                            val vocabularyDirPath =  Paths.get(getResourcesFile("vocabulary").absolutePath)
                            val savePath = Paths.get(selectedFile.absolutePath)
                            if(savePath.startsWith(vocabularyDirPath)){
                                JOptionPane.showMessageDialog(null,"不能把词库保存到应用程序安装目录，因为软件更新或卸载时，词库会被重置或者被删除")
                            }else{
                                typingWord.vocabulary.wordList.shuffle()
                                val shuffledList = typingWord.vocabulary.wordList
                                val vocabulary = Vocabulary(
                                    name = selectedFile.nameWithoutExtension,
                                    type = VocabularyType.DOCUMENT,
                                    language = "english",
                                    size = typingWord.vocabulary.size,
                                    relateVideoPath = typingWord.vocabulary.relateVideoPath,
                                    subtitlesTrackId = typingWord.vocabulary.subtitlesTrackId,
                                    wordList = shuffledList
                                )

                                saveVocabulary(vocabulary, selectedFile.absolutePath)
                                appState.changeVocabulary(selectedFile,typingWord,0)
                                // changeVocabulary 会把内置词库保存到最近列表，
                                // 保存后，如果再切换列表，就会有两个名字相同的词库，
                                // 所以需要把刚刚添加的词库从最近列表删除
                                for(i in 0 until appState.recentList.size){
                                    val recentItem = appState.recentList[i]
                                    if(recentItem.name == typingWord.vocabulary.name){
                                        appState.removeRecentItem(recentItem)
                                        break
                                    }
                                }
                            }
                        }
                    }else{
                        typingWord.vocabulary.wordList.shuffle()
                        typingWord.saveCurrentVocabulary()
                    }

                }

                typingWord.index = 0
                typingWord.chapter = 1
                typingWord.saveTypingWordState()
                resetChapterTime()
                showChapterFinishedDialog = false
                isVocabularyFinished = false
            }
            val wordKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                when {
                    ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown)
                            && it.type == KeyEventType.KeyUp) -> {
                        toNext()
                        if (typingWord.memoryStrategy == Dictation || typingWord.memoryStrategy == Review) {
                            dictationSkipCurrentWord()
                        }
                        true
                    }
                    (it.key == Key.PageUp && it.type == KeyEventType.KeyUp) -> {
                        previous()
                        true
                    }
                    (it.isCtrlPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                        clipboardManager.setText(AnnotatedString(currentWord.value))
                        true
                    }
                    (it.isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                        // 消耗快捷键，消耗之后，就不会触发 Ctrl + I 了
                        true
                    }
                    (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
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
                    clear()
                    if(typingWord.memoryStrategy == NormalReviewWrong ||
                        typingWord.memoryStrategy == DictationReviewWrong
                    ){
                        typingWord.wrongWords.clear()
                    }
                    if (typingWord.memoryStrategy == Dictation) {
                        typingWord.showInfo()
                        resetChapterTime()
                    }

                    if(typingWord.memoryStrategy == Review) typingWord.memoryStrategy = Normal


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
                            if(typingWord.vocabulary.name == "FamiliarVocabulary"){
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

                Row(Modifier.align(Alignment.Center)){
                    Word(
                        word = currentWord,
                        global = appState.global,
                        wordVisible = typingWord.wordVisible,
                        pronunciation = typingWord.pronunciation,
                        isDictation = (typingWord.memoryStrategy == Dictation ||typingWord.memoryStrategy == Review),
                        fontFamily = monospace,
                        audioPath = audioPath,
                        correctTime = wordCorrectTime,
                        wrongTime = wordWrongTime,
                        textFieldValue = wordTextFieldValue,
                        typingResult = wordTypingResult,
                        checkTyping = { checkWordInput(it) },
                        focusRequester = wordFocusRequester,
                        textFieldKeyEvent = {wordKeyEvent(it)},
                        showMenu = {activeMenu = true}
                    )
                }

            }


            Phonetic(
                word = currentWord,
                phoneticVisible = typingWord.phoneticVisible,
            )
            Morphology(
                word = currentWord,
                isPlaying = isPlaying,
                isChangeVideoBounds = appState.isChangeVideoBounds,
                searching = false,
                morphologyVisible = typingWord.morphologyVisible,
                fontSize = appState.global.detailFontSize
            )
            Definition(
                word = currentWord,
                definitionVisible = typingWord.definitionVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = appState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )
            Translation(
                word = currentWord,
                translationVisible = typingWord.translationVisible,
                isPlaying = isPlaying,
                isChangeVideoBounds = appState.isChangeVideoBounds,
                fontSize = appState.global.detailFontSize
            )

            val videoSize = videoBounds.size
            val startPadding = if ( isPlaying && !appState.isChangeVideoBounds) 0.dp else 50.dp
            val captionsModifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max)
                .padding(bottom = 0.dp, start = startPadding)
                .onKeyEvent {
                    when {
                        ((it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.PageDown)
                                && it.type == KeyEventType.KeyUp
                                ) -> {
                            toNext()
                            if (typingWord.memoryStrategy == Dictation || typingWord.memoryStrategy == Review) {
                                dictationSkipCurrentWord()
                            }
                            true
                        }
                        (it.key == Key.PageUp && it.type == KeyEventType.KeyUp) -> {
                            previous()
                            true
                        }
                        else -> globalKeyEvent(it)
                    }
                }
            Captions(
                captionsVisible = typingWord.subtitlesVisible,
                playTripleMap = getPlayTripleMap(typingWord, currentWord),
                videoPlayerWindow = appState.videoPlayerWindow,
                videoPlayerComponent = appState.videoPlayerComponent,
                isPlaying = isPlaying,
                plyingIndex = plyingIndex,
                setPlayingIndex = {plyingIndex = it},
                volume = appState.global.videoVolume,
                setIsPlaying = { isPlaying = it },
                word = currentWord,
                bounds = videoBounds,
                textFieldValueList = listOf(captionsTextFieldValue1,captionsTextFieldValue2,captionsTextFieldValue3),
                typingResultMap = captionsTypingResultMap,
                putTypingResultMap = { index, list ->
                    captionsTypingResultMap[index] = list
                },
                checkTyping = { index, input, captionContent ->
                    checkCaptionsInput(index, input, captionContent)
                },
                playKeySound = { playKeySound() },
                modifier = captionsModifier,
                focusRequesterList = listOf(focusRequester1,focusRequester2,focusRequester3),
                jumpToWord = {jumpToWord()},
                externalVisible = typingWord.externalSubtitlesVisible,
                openSearch = {appState.openSearch()},
                fontSize = appState.global.detailFontSize,
                resetVideoBounds = resetVideoBounds,
                isVideoBoundsChanged = appState.isChangeVideoBounds,
                setIsChangeBounds = { appState.isChangeVideoBounds = it },
                isWriteSubtitles = typingWord.isWriteSubtitles
            )
            if (isPlaying && !appState.isChangeVideoBounds) Spacer(
                Modifier.height((videoSize.height).dp).width(videoSize.width.dp)
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
                    state = appState,
                    wordState = typingWord,
                    save = { newWord ->
                        scope.launch {
                            val current = typingWord.getCurrentWord()
                            val index = typingWord.index
                            newWord.captions = current.captions
                            newWord.externalCaptions = current.externalCaptions
                            typingWord.vocabulary.wordList.removeAt(index)
                            typingWord.vocabulary.wordList.add(index, newWord)
                            typingWord.saveCurrentVocabulary()
                            showEditWordDialog = false
                        }

                    },
                    close = { showEditWordDialog = false }
                )
            }

            /** 显示听写复习的选择章节对话框 */
            var showChapterDialog by remember { mutableStateOf(false) }
            /** 打开听写复习的选择章节对话框 */
            val openReviewDialog:() -> Unit = {
                showChapterFinishedDialog = false
                showChapterDialog = true
                resetChapterTime()
            }

            if(showChapterDialog){
                SelectChapterDialog(
                    close = {showChapterDialog = false},
                    typingWordState = typingWord,
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
                    memoryStrategy = typingWord.memoryStrategy,
                    openReviewDialog = {openReviewDialog()},
                    isReviewWrong = (typingWord.memoryStrategy == NormalReviewWrong || typingWord.memoryStrategy == DictationReviewWrong),
                    dictationWrongWords = dictationWrongWords,
                    enterDictation = { enterDictation() },
                    learnAgain = { learnAgain() },
                    reviewWrongWords = { reviewWrongWords() },
                    nextChapter = { nextChapter() },
                    resetIndex = { resetIndex(it) }
                )
            }
        }

    }
}

@Composable
fun VocabularyEmpty() {
    Surface(Modifier.fillMaxSize()) {

        Box( modifier = Modifier.fillMaxSize()){
            Column(verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ){
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("请选择一个词库,也可以拖放词库到这里。", style = MaterialTheme.typography.h6)
                }
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top =30.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append("如果要记忆高考单词四六级单词，请从")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append("词库菜单栏")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append(" > ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append("选择内置词库")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append("，选择词库到本地文件系统。")
                        }
                    }
                    Text(annotatedString, style = MaterialTheme.typography.h6)
                }

                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top =30.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append("如果要使用电影美剧的字幕生成词库，请从")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append("词库菜单栏")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append(" > ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append("用字幕生成词库")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                            append(" 或 ")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append("用MKV视频生成词库。")
                        }
                    }
                    Text(annotatedString, style = MaterialTheme.typography.h6)
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
                            text = word.definition,
                        )
                    }
                }
                if (rows > 5) {
                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
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
        Column {
            val width = when (fontSize) {
                MaterialTheme.typography.h5.fontSize -> {
                    600.dp
                }
                MaterialTheme.typography.h6.fontSize -> {
                    575.dp
                }
                else -> 555.dp
            }
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .width(width)
                    .padding(start = 50.dp, top = 5.dp, bottom = 5.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = word.translation,
                        textAlign = TextAlign.Start,
                        fontSize = fontSize,
                        color = MaterialTheme.colors.onBackground
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
 * @param videoPlayerWindow 视频播放窗口
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
    videoPlayerWindow: JFrame,
    videoPlayerComponent: Component,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
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
    externalVisible:Boolean,
    openSearch: () -> Unit,
    fontSize: TextUnit,
    resetVideoBounds :() ->  Rectangle,
    isVideoBoundsChanged:Boolean,
    setIsChangeBounds:(Boolean) -> Unit = {},
    isWriteSubtitles:Boolean,
) {
    if (captionsVisible) {
        val horizontalArrangement = if (isPlaying && !isVideoBoundsChanged) Arrangement.Center else Arrangement.Start
        Row(
            horizontalArrangement = horizontalArrangement,
            modifier = modifier
        ) {
            Column {
                val scope = rememberCoroutineScope()
                playTripleMap.forEach { (index, playTriple) ->
                    var captionContent = playTriple.first.content
                    if (captionContent.contains("\r\n")) {
                        captionContent = captionContent.replace("\r\n", " ")
                    } else if (captionContent.contains("\n")) {
                        captionContent = captionContent.replace("\n", " ")
                    }
                    val textFieldValue = textFieldValueList[index]
                    var typingResult = typingResultMap[index]
                    if (typingResult == null) {
                        typingResult = mutableListOf()
                        putTypingResultMap(index, typingResult)
                    }
                    var isPathWrong by remember { mutableStateOf(false) }
                    val playCurrentCaption:()-> Unit = {
                        if (!isPlaying) {
                            val file = File(playTriple.second)
                            if (file.exists()) {
                                setIsPlaying(true)
                                scope.launch {
                                    setPlayingIndex(index)
                                    play(
                                        window = videoPlayerWindow,
                                        setIsPlaying = { setIsPlaying(it) },
                                        volume = volume,
                                        playTriple = playTriple,
                                        videoPlayerComponent = videoPlayerComponent,
                                        bounds = bounds,
                                        externalSubtitlesVisible = externalVisible,
                                        resetVideoBounds = resetVideoBounds,
                                        isVideoBoundsChanged = isVideoBoundsChanged,
                                        setIsVideoBoundsChanged = setIsChangeBounds
                                    )
                                }

                            } else {
                                isPathWrong = true
                                Timer("恢复状态", false).schedule(2000) {
                                    isPathWrong = false
                                }
                            }
                        }
                        if(isWriteSubtitles){
                            focusRequesterList[index].requestFocus()
                        }else{
                            jumpToWord()
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
                            (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                scope.launch { selectable = !selectable }
                                true
                            }
                            (it.key == Key.Tab && it.type == KeyEventType.KeyUp) -> {
                                scope.launch {  playCurrentCaption() }
                                true
                            }
                            (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            (it.key == Key.DirectionUp && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                                focusMoveUp()
                                true
                            }
                            (it.isCtrlPressed && it.isShiftPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                                focusMoveDown()
                                true
                            }
                            else -> false
                        }
                    }

                    Caption(
                        isPlaying = isPlaying,
                        captionContent = captionContent,
                        textFieldValue = textFieldValue,
                        typingResult = typingResult,
                        checkTyping = { editIndex, input, editContent ->
                            checkTyping(editIndex, input, editContent)
                        },
                        index = index,
                        playingIndex = plyingIndex,
                        focusRequester = focusRequesterList[index],
                        playCurrentCaption = {playCurrentCaption()},
                        captionKeyEvent = {captionKeyEvent(it)},
                        selectable = selectable,
                        setSelectable = {selectable = it},
                        isPathWrong = isPathWrong,
                        openSearch = {openSearch()},
                        fontSize = fontSize
                    )
                }

            }
        }
        if ((!isPlaying || isVideoBoundsChanged) && (word.captions.isNotEmpty() || word.externalCaptions.isNotEmpty()))
            Divider(Modifier.padding(start = 50.dp))
    }
}

/**
 * 获取字幕
 * @return Map 的类型参数说明：
 * Int      -> index,主要用于删除字幕，和更新时间轴
 * - Triple 的 Caption  -> caption.content 用于输入和阅读，caption.start 和 caption.end 用于播放视频
 * - Triple 的 String   -> 字幕对应的视频地址
 * - Triple 的 Int      -> 字幕的轨道
 */
fun getPlayTripleMap(wordState: WordState, word: Word): MutableMap<Int, Triple<Caption, String, Int>> {

    val playTripleMap = mutableMapOf<Int, Triple<Caption, String, Int>>()
    if (wordState.vocabulary.type == VocabularyType.DOCUMENT) {
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
                    Triple(caption, wordState.vocabulary.relateVideoPath, wordState.vocabulary.subtitlesTrackId)
                playTripleMap[index] = playTriple
            }

        }
    }
    return playTripleMap
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
 * @param captionContent 字幕的内容
 * @param textFieldValue 输入的字幕
 * @param typingResult 输入字幕的结果
 * @param checkTyping 输入字幕后被调用的回调
 * @param index 当前字幕的索引
 * @param playingIndex 正在播放的字幕索引
 * @param focusRequester 焦点请求器
 * @param playCurrentCaption 播放当前字幕的函数
 * @param captionKeyEvent 处理当前字幕的快捷键函数
 * @param selectable 是否可选择复制
 * @param setSelectable 设置是否可选择
 * @param isPathWrong 是否路径错误
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun Caption(
    isPlaying: Boolean,
    captionContent: String,
    textFieldValue: String,
    typingResult: List<Pair<Char, Boolean>>,
    checkTyping: (Int, String, String) -> Unit,
    index: Int,
    playingIndex: Int,
    focusRequester:FocusRequester,
    playCurrentCaption:()-> Unit,
    captionKeyEvent:(KeyEvent) -> Boolean,
    selectable:Boolean,
    setSelectable:(Boolean) -> Unit,
    isPathWrong:Boolean,
    openSearch: () -> Unit,
    fontSize: TextUnit
) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        val rowHeight = when (fontSize) {
            MaterialTheme.typography.h5.fontSize -> {
                24.dp * 2 + 4.dp
            }
            MaterialTheme.typography.h6.fontSize -> {
                20.dp * 2 + 4.dp
            }
            MaterialTheme.typography.subtitle1.fontSize -> {
                16.dp * 2 + 4.dp
            }
            MaterialTheme.typography.subtitle2.fontSize -> {
                14.dp * 2 + 4.dp
            }
            MaterialTheme.typography.body1.fontSize -> {
                16.dp * 2 + 4.dp
            }
            MaterialTheme.typography.body2.fontSize -> {
                14.dp * 2 + 4.dp
            }
            else -> 16.dp * 2 + 4.dp
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(rowHeight).width(IntrinsicSize.Max)
        ) {
            val dropMenuFocusRequester = remember { FocusRequester() }
            Box(Modifier.width(IntrinsicSize.Max).padding(top = 8.dp, bottom = 8.dp)) {
                val textHeight = rowHeight -4.dp
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { input ->
                            checkTyping(index, input, captionContent)
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = LocalTextStyle.current.copy(color = Color.Transparent,fontSize = fontSize),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(textHeight)
                        .align(Alignment.CenterStart)
                        .focusRequester(focusRequester)
                        .onKeyEvent { captionKeyEvent(it) }
                )
                Text(
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.CenterStart).height(textHeight),
                    overflow = TextOverflow.Ellipsis,
                    text = buildAnnotatedString {
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

                    },
                )

                DropdownMenu(
                    expanded = selectable,
                    focusable = true,
                    onDismissRequest = { setSelectable(false) },
                    offset = DpOffset(0.dp, (-30).dp)
                ) {

                    // 增加一个检查，检查字幕的字符长度，有的字幕是机器生成的，一段可能会有很多字幕，
                    // 可能会超出限制，导致程序崩溃。
                    val content = if(captionContent.length>400){
                       captionContent.substring(0,400)
                    }else captionContent

                    BasicTextField(
                        value = content,
                        onValueChange = {},
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle =  LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                            fontSize = fontSize,
                        ),
                        modifier = Modifier.focusable()
                            .focusRequester(dropMenuFocusRequester)
                            .onKeyEvent {
                                if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                    scope.launch { setSelectable(!selectable) }
                                    true
                                }else if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
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
                        val shift = if (isMacOS()) "⇧" else "Shift"
                        val text: Any = when (index) {
                            0 -> "播放 $ctrl+$shift+Z"
                            1 -> "播放 $ctrl+$shift+X"
                            2 -> "播放 $ctrl+$shift+C"
                            else -> println("字幕数量超出范围")
                        }
                        Text(text = text.toString(), modifier = Modifier.padding(10.dp))
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
//                    focusRequester.requestFocus()
                }) {
                    val tint = if(isPlaying && playingIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Localized description",
                        tint = tint
                    )
                }
            }
            if (isPathWrong) {
                Text("视频地址错误", color = Color.Red)
            }
        }
    }


}

/** 删除按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun DeleteButton(onClick:()->Unit){
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
                    Text(text = "删除单词")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        val shift = if (isMacOS()) "⇧" else "Shift"
                        Text(text = " $shift + Delete ")
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
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
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
                Icons.Filled.Edit,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
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
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "加入到困难词库")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + ")
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
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 熟悉单词按钮 */
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun FamiliarButton(
    onClick: () -> Unit,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "加入到熟悉词库")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + Y")
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
        IconButton(onClick = { onClick() },modifier = Modifier.onKeyEvent { keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                onClick()
                true
            }else false
        }) {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
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
            Icon(
                icon,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary,
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun CopyButton(wordValue:String){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                val ctrl = LocalCtrl.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "复制")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + C")
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
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/** 退出按钮*/
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ExitButton(onClick: () -> Unit){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = "退出听写复习", modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.BottomCenter,
            alignment = Alignment.BottomCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = {
            onClick()
        }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 *
 * 移除按钮
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RemoveButton(
    toolTip:String,
    onClick: () -> Unit
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = toolTip, modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 50,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.BottomCenter,
            alignment = Alignment.BottomCenter,
            offset = DpOffset.Zero
        )
    ) {
        val color = MaterialTheme.colors.onBackground
        var tint by remember(color){mutableStateOf(color)}
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(top = if (isMacOS()) 30.dp else 0.dp)
                .onPointerEvent(PointerEventType.Enter){
                    tint = Color.Red
                }
                .onPointerEvent(PointerEventType.Exit){
                    tint = color
                }
        ) {
            Icon(
                Icons.Filled.HighlightOff,
                contentDescription = "Localized description",
                tint = tint
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SearchResultInfo(
    word: Word,
    appState: AppState,
    typingState: WordState,
){
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()){
        AudioButton(
            word = word,
            state = appState,
            typingState = typingState,
            volume = appState.global.audioVolume,
            pronunciation = typingState.pronunciation,
        )
    }
    Divider()
    Morphology(
        word = word,
        isPlaying = false,
        searching = true,
        morphologyVisible = true,
        fontSize = appState.global.detailFontSize
    )
    Spacer(Modifier.height(8.dp))
    Divider()
    SelectionContainer {
        Text(text = word.definition,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
    SelectionContainer {
        Text(word.translation,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
}



/**
 * @param currentWord 当前正在记忆的单词
 * @param index links 的 index
 * @return Triple<Caption, String, Int>? ,视频播放器需要的信息
 */
fun getPayTriple(currentWord: Word, index: Int): Triple<Caption, String, Int>? {

    return if (index < currentWord.externalCaptions.size) {
        val externalCaption = currentWord.externalCaptions[index]
        val caption = Caption(externalCaption.start, externalCaption.end, externalCaption.content)
        Triple(caption, externalCaption.relateVideoPath, externalCaption.subtitlesTrackId)
    } else {
        null
    }
}
/**  设置处理拖放文件的函数 */
@OptIn(ExperimentalSerializationApi::class)
fun setWindowTransferHandler(
    window: ComposeWindow,
    state: AppState,
    wordState: WordState,
    showVideoPlayer:(Boolean) -> Unit,
    setVideoPath:(String) -> Unit,
    vocabularyPathChanged:(String) -> Unit
){
    window.transferHandler = createTransferHandler(
        showWrongMessage = { message ->
            JOptionPane.showMessageDialog(window, message)
        },
        parseImportFile = {files ->
            val file = files.first()
            if (file.extension == "json") {
                if (wordState.vocabularyPath != file.absolutePath) {
                    val index = state.findVocabularyIndex(file)
                    state.changeVocabulary(file,wordState,index)
                } else {
                    JOptionPane.showMessageDialog(window, "词库已打开")
                }

            } else if (file.extension == "mkv" || file.extension == "mp4") {
                showVideoPlayer(true)
                setVideoPath(file.absolutePath)
                vocabularyPathChanged(wordState.vocabularyPath)
            } else {
                JOptionPane.showMessageDialog(window, "文件格式不支持")
            }
        }
    )
}
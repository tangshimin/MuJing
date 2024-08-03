package ui.subtitleScreen

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Caption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.*
import state.GlobalState
import state.SubtitlesState
import ui.MacOSTitle
import ui.RemoveButton
import ui.Toolbar
import ui.playSound
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import java.util.concurrent.FutureTask
import java.util.regex.Pattern
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.TransferHandler
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView


/** 支持的视频类型 */
val videoFormatList = listOf("mp4","mkv")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun SubtitleScreen(
    subtitlesState: SubtitlesState,
    globalState: GlobalState,
    saveSubtitlesState: () -> Unit,
    saveGlobalState: () -> Unit,
    isOpenSettings: Boolean,
    setIsOpenSettings: (Boolean) -> Unit,
    window: ComposeWindow,
    title: String,
    playerWindow: JFrame,
    videoVolume: Float,
    mediaPlayerComponent: Component,
    futureFileChooser: FutureTask<JFileChooser>,
    openLoadingDialog: () -> Unit,
    closeLoadingDialog: () -> Unit,
    openSearch: () -> Unit,
    showPlayer :(Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val timedCaption = rememberTimedCaption()
    var isPlaying by remember { mutableStateOf(false) }
    var showOpenFile by remember { mutableStateOf(false) }
    var selectedPath by remember { mutableStateOf("") }
    var showSelectTrack by remember { mutableStateOf(false) }
    val trackList = remember { mutableStateListOf<Pair<Int, String>>() }
    var textRect by remember{ mutableStateOf(Rect(0.0F,0.0F,0.0F,0.0F))}
    val videoPlayerBounds by remember { mutableStateOf(Rectangle(0, 0, 540, 303)) }
    val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }
    var loading by remember { mutableStateOf(false) }
    var mediaType by remember { mutableStateOf(computeMediaType(subtitlesState.mediaPath)) }
    var pgUp by remember { mutableStateOf(false) }
    val audioPlayerComponent = LocalAudioPlayerComponent.current
    var isVideoBoundsChanged by remember{ mutableStateOf(false) }
    /** 如果移动了播放器的位置，用这个变量保存计算的位置，点击恢复按钮的时候用这个临时的变量恢复 */
    val playerPoint1 by remember{mutableStateOf(Point(0,0))}
    /** 如果移动了播放器的位置，用这个变量保存计算的位置，点击恢复按钮的时候用这个临时的变量恢复,多行模式的位置 */
    var playerPoint2 by remember{mutableStateOf(Point(0,0))}
    var charWidth by remember{ mutableStateOf(computeCharWidth(subtitlesState.trackDescription)) }
    /** 一次播放多条字幕 */
    val multipleLines = rememberMultipleLines()
    /** 启动播放多行字幕后，在这一行显示播放按钮 */
    var playIconIndex  by remember{mutableStateOf(0)}


    val eventListener = object:MediaPlayerEventAdapter() {
        override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
            if(multipleLines.enabled) {
                subtitlesState.currentIndex = timedCaption.getCaptionIndex(newTime, subtitlesState.currentIndex)
            }
        }
    }

    LaunchedEffect(Unit){
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
    }



    /** 读取字幕文件*/
    if (subtitlesState.subtitlesPath.isNotEmpty() && timedCaption.isEmpty()) {
        parseSubtitles(
            subtitlesPath = subtitlesState.subtitlesPath,
            setMaxLength = {
                scope.launch {
                    subtitlesState.sentenceMaxLength = it
                    saveSubtitlesState()
                }
            },
            setCaptionList = {
                timedCaption.setCaptionList(it)
            },
            resetSubtitlesState = {
                selectedPath = ""
                trackList.clear()
                subtitlesState.mediaPath = ""
                subtitlesState.subtitlesPath = ""
                subtitlesState.trackID = -1
                subtitlesState.trackDescription = ""
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.sentenceMaxLength = 0
                subtitlesState.saveTypingSubtitlesState()
            }
        )
    }

    /** 播放按键音效 */
    val playKeySound = {
        if (globalState.isPlayKeystrokeSound) {
            playSound("audio/keystroke.wav", globalState.keystrokeVolume)
        }
    }

    /** 设置字幕列表的被回调函数 */
    val setTrackList: (List<Pair<Int, String>>) -> Unit = {
        trackList.clear()
        trackList.addAll(it)
    }
    /** 支持的媒体类型 */
    val formatList = listOf("wav","mp3","aac","mp4","mkv")
    /** 支持的音频类型*/
    val audioFormatList = listOf("wav","mp3","aac")

    /** 解析打开的文件 */
    val parseImportFile: (List<File>, OpenMode) -> Unit = { files, openMode ->
        if(files.size == 1){
            val file = files.first()
            loading = true
            scope.launch (Dispatchers.Default){
                    if (file.extension == "mkv") {
                        if (subtitlesState.mediaPath != file.absolutePath) {
                            selectedPath = file.absolutePath
                            parseTrackList(
                                mediaPlayerComponent,
                                window,
                                playerWindow,
                                file.absolutePath,
                                setTrackList = { setTrackList(it) },
                            )
                            if (showOpenFile) showOpenFile = false
                        } else {
                            JOptionPane.showMessageDialog(window, "文件已打开")
                        }

                    } else if (formatList.contains(file.extension)) {
                        JOptionPane.showMessageDialog(window, "需要同时选择 ${file.extension} 视频 + srt 字幕")
                    } else if (file.extension == "srt") {
                        JOptionPane.showMessageDialog(window, "需要同时选择1个视频(mp4、mkv) + 1个srt 字幕")
                    } else if (file.extension == "json") {
                        JOptionPane.showMessageDialog(window, "想要打开词库文件，需要先切换到记忆单词界面")
                    } else {
                        JOptionPane.showMessageDialog(window, "格式不支持")
                    }

                    loading = false
            }
        }else if(files.size == 2){
            val first = files.first()
            val last = files.last()
            val modeString = if(openMode== OpenMode.Open) "打开" else "拖拽"


            if(first.extension == "srt" && formatList.contains(last.extension)){
                subtitlesState.trackID = -1
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.subtitlesPath = first.absolutePath
                subtitlesState.mediaPath = last.absolutePath
                subtitlesState.trackDescription = first.nameWithoutExtension
                timedCaption.clear()
                mediaType = computeMediaType(subtitlesState.mediaPath)
                if(openMode == OpenMode.Open) showOpenFile = false
                if(showOpenFile) showOpenFile = false
            }else if(formatList.contains(first.extension) && last.extension == "srt"){
                subtitlesState.trackID = -1
                subtitlesState.trackSize = 0
                subtitlesState.currentIndex = 0
                subtitlesState.firstVisibleItemIndex = 0
                subtitlesState.mediaPath = first.absolutePath
                subtitlesState.subtitlesPath = last.absolutePath
                subtitlesState.trackDescription = last.nameWithoutExtension
                timedCaption.clear()
                mediaType = computeMediaType(subtitlesState.mediaPath)
                if(openMode == OpenMode.Open) showOpenFile = false
                if(showOpenFile) showOpenFile = false
            }else if(first.extension == "mp4" && last.extension == "mp4"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个 MP4 格式的视频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(first.extension == "mkv" && last.extension == "mkv"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个 MKV 格式的视频，\n"
                        +"可以选择一个有字幕的 mkv 格式的视频，\n或者一个 MKV 格式的视频和1个 srt 字幕")
            }else if(first.extension == "srt" && last.extension == "srt"){
                JOptionPane.showMessageDialog(window, "${modeString}了2个字幕，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(videoFormatList.contains(first.extension) && videoFormatList.contains(last.extension)){
                JOptionPane.showMessageDialog(window, "${modeString}了2个视频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else if(audioFormatList.contains(first.extension) &&  audioFormatList.contains(last.extension)){
                JOptionPane.showMessageDialog(window, "${modeString}了2个音频，\n需要1个媒体（mp3、aac、wav、mp4、mkv）和1个 srt 字幕")
            }else {
                JOptionPane.showMessageDialog(window, "文件格式不支持")
            }
        }else{
            JOptionPane.showMessageDialog(window, "不能超过两个文件")
        }
        subtitlesState.saveTypingSubtitlesState()
    }

    /** 打开文件对话框 */
    val openFileChooser: () -> Unit = {

        // 打开 windows 的文件选择器很慢，有时候会等待超过2秒
        openLoadingDialog()
        scope.launch (Dispatchers.Default) {
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogTitle = "打开"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.isAcceptAllFileFilterUsed = false
                fileChooser.isMultiSelectionEnabled = true
                val fileFilter = FileNameExtensionFilter(
                    "1个 mkv 视频，或 1个媒体(mp3、wav、aac、mp4、mkv) + 1个字幕(srt)",
                    "mp3",
                    "wav",
                    "aac",
                    "mkv",
                    "srt",
                    "mp4"
                )
                fileChooser.addChoosableFileFilter(fileFilter)
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val files = fileChooser.selectedFiles.toList()
                    parseImportFile(files, OpenMode.Open)
                    closeLoadingDialog()
                } else {
                    closeLoadingDialog()
                }
                fileChooser.selectedFile = null
                fileChooser.isMultiSelectionEnabled = false
                fileChooser.removeChoosableFileFilter(fileFilter)
        }
    }

    /** 关闭当前字幕*/
    val removeSubtitles:() -> Unit = {
        selectedPath = ""
        trackList.clear()
        subtitlesState.trackID = -1
        subtitlesState.trackSize = 0
        subtitlesState.currentIndex = 0
        subtitlesState.firstVisibleItemIndex = 0
        subtitlesState.subtitlesPath = ""
        subtitlesState.mediaPath = ""
        subtitlesState.trackDescription = ""
        timedCaption.clear()
        subtitlesState.saveTypingSubtitlesState()
    }

    val resetVideoBounds:() -> Rectangle = {
        isVideoBoundsChanged = false
        if(multipleLines.enabled){
            Rectangle(playerPoint2.x, playerPoint2.y, 540, 303)
        }else{
            Rectangle(playerPoint1.x, playerPoint1.y, 540, 303)
        }

    }

    /**  使用按钮播放视频时调用的回调函数   */
    val playCaption: (Caption) -> Unit = { caption ->
        val file = File(subtitlesState.mediaPath)
        if (file.exists() ) {
            if (!isPlaying) {
                scope.launch {
                    isPlaying = true

                    // 音频
                    if(file.extension == "wav" || file.extension == "mp3"|| file.extension == "aac"){
                        play(
                            setIsPlaying = {isPlaying = it},
                            audioPlayerComponent = audioPlayerComponent,
                            volume = videoVolume,
                            caption = caption,
                            videoPath = subtitlesState.mediaPath,
                        )
                    // 视频
                    } else {
                        // 使用内部字幕轨道
                        if (subtitlesState.trackID != -1) {
                            val playTriple = Triple(caption, subtitlesState.mediaPath, subtitlesState.trackID)
                            corePlay(
                                window = playerWindow,
                                setIsPlaying = { isPlaying = it },
                                volume = videoVolume,
                                playTriple = playTriple,
                                videoPlayerComponent = mediaPlayerComponent,
                                bounds = videoPlayerBounds,
                                resetVideoBounds = resetVideoBounds,
                                isVideoBoundsChanged = isVideoBoundsChanged,
                                setIsVideoBoundsChanged = {isVideoBoundsChanged = it}
                            )
                            // 使用外部字幕
                        } else {
                            val externalPlayTriple = Triple(caption, subtitlesState.mediaPath, -1)
                            corePlay(
                                window = playerWindow,
                                setIsPlaying = { isPlaying = it },
                                volume = videoVolume,
                                playTriple = externalPlayTriple,
                                videoPlayerComponent = mediaPlayerComponent,
                                bounds = videoPlayerBounds,
                                externalSubtitlesVisible = subtitlesState.externalSubtitlesVisible,
                                resetVideoBounds = resetVideoBounds,
                                isVideoBoundsChanged = isVideoBoundsChanged,
                                setIsVideoBoundsChanged = {isVideoBoundsChanged = it}
                            )
                        }
                    }
                }
            }

        } else {
            JOptionPane.showMessageDialog(null,"视频地址错误:${file.absolutePath}\n" +
                    "可能原视频被移动、删除或者重命名了。")
        }

    }

    /** 保存轨道 ID 时被调用的回调函数 */
    val saveTrackID: (Int) -> Unit = {
        scope.launch {
            subtitlesState.trackID = it
            saveSubtitlesState()
        }
    }

    /** 保存轨道名称时被调用的回调函数 */
    val saveTrackDescription: (String) -> Unit = {
        scope.launch {
            subtitlesState.trackDescription = it
            charWidth = computeCharWidth(it)
            saveSubtitlesState()
        }
    }

    /** 保存轨道数量时被调用的回调函数 */
    val saveTrackSize: (Int) -> Unit = {
        scope.launch {
            subtitlesState.trackSize = it
            saveSubtitlesState()
        }
    }

    /** 保存视频路径时被调用的回调函数 */
    val saveVideoPath: (String) -> Unit = {
        subtitlesState.mediaPath = it
        mediaType = "video"
        saveSubtitlesState()
    }

    /** 保存一个新的字幕时被调用的回调函数 */
    val saveSubtitlesPath: (String) -> Unit = {
        scope.launch {
            subtitlesState.subtitlesPath = it
            subtitlesState.firstVisibleItemIndex = 0
            subtitlesState.currentIndex = 0
            // 清除 focus 后，当前正在抄写的字幕数据会被清除
            focusManager.clearFocus()
            /** 把之前的字幕列表清除才能触发解析字幕的函数重新运行 */
            timedCaption.clear()
            saveSubtitlesState()
        }
    }

    /** 设置是否启用击键音效时被调用的回调函数 */
    val setIsPlayKeystrokeSound: (Boolean) -> Unit = {
        scope.launch {
            globalState.isPlayKeystrokeSound = it
            saveGlobalState()
        }
    }

    /** 选择字幕轨道 */
    val selectTypingSubTitles:() -> Unit = {
        val videoFile = File(subtitlesState.mediaPath)
        if (trackList.isEmpty() && videoFile.exists()) {
            loading = true
            scope.launch (Dispatchers.Default){
                showSelectTrack = true
                    parseTrackList(
                        mediaPlayerComponent = mediaPlayerComponent,
                        parentComponent = window,
                        playerWindow = playerWindow,
                        videoPath = subtitlesState.mediaPath,
                        setTrackList = {
                            setTrackList(it)
                        },
                    )
                    loading = false
            }
        }else if(!videoFile.exists()){
            JOptionPane.showMessageDialog(null,"视频地址错误:${videoFile.absolutePath}\n" +
                    "可能原视频被移动、删除或者重命名了。")
        }
    }


    val setTranscriptionCaption: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.transcriptionCaption = it
            saveSubtitlesState()
        }
    }

    /** 设置当前字幕的可见性 */
    val setCurrentCaptionVisible: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.currentCaptionVisible = it
            saveSubtitlesState()
        }
    }

    /** 设置未抄写字幕的可见性 */
    val setNotWroteCaptionVisible: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.notWroteCaptionVisible = it
            saveSubtitlesState()
        }
    }

    /** 设置外部字幕的可见性 */
    val setExternalSubtitlesVisible: (Boolean) -> Unit = {
        scope.launch {
            subtitlesState.externalSubtitlesVisible = it
            saveSubtitlesState()
        }
    }

    /** 当前界面的快捷键 */
    val boxKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            (keyEvent.isCtrlPressed && keyEvent.key == Key.O && keyEvent.type == KeyEventType.KeyUp) -> {
                openFileChooser()
                showOpenFile = true
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.F && keyEvent.type == KeyEventType.KeyUp) -> {
                scope.launch { openSearch() }
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.One && keyEvent.type == KeyEventType.KeyUp) -> {
                setIsOpenSettings(!isOpenSettings)
                true
            }
            (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp) -> {
                if(multipleLines.enabled){
                    multipleLines.enabled = false
                    playIconIndex = 0
                }
                true
            }
            ((keyEvent.key == Key.Tab) && keyEvent.type == KeyEventType.KeyUp) -> {
                if(multipleLines.enabled){
                    val playItem = Caption(multipleLines.startTime ,multipleLines.endTime ,"")
                    playCaption(playItem)
                }else{
                    val caption =  timedCaption.captionList[subtitlesState.currentIndex]
                    playCaption(caption)
                }

                true
            }
            else -> false
        }
    }

    //设置窗口的拖放处理函数
    LaunchedEffect(Unit){
        val transferHandler = createTransferHandler(
            singleFile = false,
            showWrongMessage = { message ->
                JOptionPane.showMessageDialog(window, message)
            },
            parseImportFile = { parseImportFile(it, OpenMode.Drag) }
        )
        window.transferHandler = transferHandler
    }

    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colors.background)
            .focusRequester(focusRequester)
            .onKeyEvent(boxKeyEvent)
            .focusable()
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Row(Modifier.fillMaxSize()) {
            SubtitlesSidebar(
                isOpen = isOpenSettings,
                transcriptionCaption = subtitlesState.transcriptionCaption,
                setTranscriptionCaption = {setTranscriptionCaption(it)},
                currentCaptionVisible = subtitlesState.currentCaptionVisible,
                setCurrentCaptionVisible = {setCurrentCaptionVisible(it)},
                notWroteCaptionVisible = subtitlesState.notWroteCaptionVisible,
                setNotWroteCaptionVisible = {setNotWroteCaptionVisible(it)},
                externalSubtitlesVisible = subtitlesState.externalSubtitlesVisible,
                setExternalSubtitlesVisible = {setExternalSubtitlesVisible(it)},
                trackSize = subtitlesState.trackSize,
                selectTrack = { selectTypingSubTitles() },
                isPlayKeystrokeSound = globalState.isPlayKeystrokeSound,
                setIsPlayKeystrokeSound = { setIsPlayKeystrokeSound(it) },
            )
            val topPadding = if (isMacOS()) 78.dp else 48.dp
            if (isOpenSettings) {
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(Modifier.fillMaxSize().padding(top = topPadding)) {

                if (timedCaption.isNotEmpty()) {
                    val captionList = timedCaption.captionList
                    val listState = rememberLazyListState(subtitlesState.firstVisibleItemIndex)
                    val stateHorizontal = rememberScrollState(0)
                    val isAtTop by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                        }
                    }

                    val startPadding = 20.dp
                    val endPadding = 10.dp
                    val indexWidth = (captionList.size.toString().length * 14).dp + 96.dp
                    val buttonWidth = 48.dp
                    var rowWidth = indexWidth + startPadding + (subtitlesState.sentenceMaxLength * charWidth).dp +  endPadding + buttonWidth

                    if(subtitlesState.sentenceMaxLength < 50) rowWidth += 120.dp

                    LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1100.dp)
                            .fillMaxHeight()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .horizontalScroll(stateHorizontal),
                    ) {
                        itemsIndexed(captionList) { index, caption ->
                            val captionContent = caption.content
                            // 当 709 行的 BasicTextField 失去焦点时自动清理 typingResult 和 textFieldValue
                            val typingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }
                            var textFieldValue by remember { mutableStateOf("") }
                            var selectable by remember { mutableStateOf(false) }
                            val selectRequester = remember { FocusRequester() }
                            val textFieldRequester = remember { FocusRequester() }
                            val next :() -> Unit = {
                                scope.launch {
                                    val end =
                                        listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size - 2
                                    if (index >= end) {
                                        listState.scrollToItem(index)
                                    }
                                   if(index+1 != captionList.size){
                                       subtitlesState.currentIndex = subtitlesState.currentIndex + 1
                                   }
                                }
                            }
                            val previous :() -> Unit = {
                                scope.launch {
                                    if(index == listState.firstVisibleItemIndex+1){
                                        var top = index - listState.layoutInfo.visibleItemsInfo.size
                                        if(top < 0) top = 0
                                        listState.scrollToItem(top)
                                        subtitlesState.currentIndex = index-1
                                        pgUp = true
                                    }else if(subtitlesState.currentIndex > 0){
                                        subtitlesState.currentIndex = subtitlesState.currentIndex - 1
                                    }

                                }
                            }
                            /** 检查输入的回调函数 */
                            val checkTyping: (String) -> Unit = { input ->
                                    if (textFieldValue.length > captionContent.length) {
                                        typingResult.clear()
                                        textFieldValue = ""

                                    } else if (input.length <= captionContent.length) {
                                        textFieldValue = input
                                        typingResult.clear()
                                        val inputChars = input.toMutableList()
                                        for (i in inputChars.indices) {
                                            val inputChar = inputChars[i]
                                            val char = captionContent[i]
                                            if (inputChar == char) {
                                                typingResult.add(Pair(inputChar, true))
                                                // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                                            } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                                typingResult.add(Pair(char, true))
                                                // 音乐符号不好输入，所以可以使用空格替换
                                            }else if (inputChar == ' ' && (char == '♪')) {
                                                typingResult.add(Pair(char, true))
//                                              // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                                                inputChars.add(i,'♪')
                                                inputChars.removeAt(i+1)
                                                textFieldValue = String(inputChars.toCharArray())
                                            } else {
                                                typingResult.add(Pair(inputChar, false))
                                            }
                                        }
                                        if(input.length == captionContent.length){
                                            next()
                                        }

                                    }
                            }

                            val enableMultipleLines:() -> Unit = {
                                if(!multipleLines.enabled){
                                    multipleLines.enabled = true
                                    multipleLines.startIndex = index
                                    multipleLines.endIndex = index
                                    playIconIndex = index

                                    multipleLines.startTime = caption.start
                                    multipleLines.endTime = caption.end
                                }else if(multipleLines.startIndex > index){
                                    multipleLines.startIndex = index
                                    playIconIndex = index

                                    multipleLines.startTime = caption.start
                                    // 播放器的位置向上偏移
                                    multipleLines.isUp = true
                                }else if(multipleLines.startIndex < index){
                                    multipleLines.endIndex = index
                                    playIconIndex = index

                                    multipleLines.endTime = caption.end
                                    // 播放器的位置向下偏移
                                    multipleLines.isUp = false
                                }
                            }

                            val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                when {
                                    ((it.key != Key.ShiftLeft && it.key != Key.ShiftRight &&
                                            it.key != Key.AltLeft && it.key != Key.AltRight &&
                                            it.key != Key.CtrlLeft && it.key != Key.CtrlRight)
                                            && it.type == KeyEventType.KeyDown) -> {
                                        playKeySound()
                                        true
                                    }
                                    ((it.key == Key.Enter ||it.key == Key.NumPadEnter || it.key == Key.DirectionDown) && it.type == KeyEventType.KeyUp) -> {
                                        next()
                                        true
                                    }

                                    ((it.key == Key.DirectionUp) && it.type == KeyEventType.KeyUp) -> {
                                        previous()
                                        true
                                    }
                                    ((it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current-20)
                                        }
                                        true
                                    }
                                    ((it.key == Key.DirectionRight) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current+20)
                                        }
                                        true
                                    }
                                    (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            if(subtitlesState.transcriptionCaption){
                                                selectable = !selectable
                                            }
                                        }
                                        true
                                    }
                                    (it.isCtrlPressed && it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch { enableMultipleLines() }
                                        true
                                    }
                                    else -> false
                                }

                            }

                            // alpha 越小颜色越透明，越大颜色越深。
                            var alpha by remember{
                                if(subtitlesState.currentIndex == index){
                                    mutableStateOf(1.0f)
                                }else{
                                    mutableStateOf(0.74f)
                                }
                            }
                            var rowBackgroundColor by remember{ mutableStateOf(Color.Transparent) }
                            LaunchedEffect(multipleLines.enabled,multipleLines.startIndex,multipleLines.endIndex){
                                if(multipleLines.enabled && index in multipleLines.startIndex .. multipleLines.endIndex){
                                    rowBackgroundColor =     if(globalState.isDarkTheme) Color(38,38,38) else Color(204,204,204)
                                    alpha = 1.0f
                                }else{
                                    rowBackgroundColor = Color.Transparent
                                    alpha = if(subtitlesState.currentIndex == index) 1.0f else 0.74f
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(rowWidth)
                                    .padding(start = 150.dp)
                                    .background(rowBackgroundColor)
                            ) {

                                val lineColor =  if(index <  subtitlesState.currentIndex){
                                    MaterialTheme.colors.primary.copy(alpha = if(MaterialTheme.colors.isLight) ContentAlpha.high else ContentAlpha.medium)
                                }else if(subtitlesState.currentIndex == index){
                                    if(multipleLines.enabled || !subtitlesState.transcriptionCaption) {
                                        MaterialTheme.colors.primary.copy(alpha = if(MaterialTheme.colors.isLight) ContentAlpha.high else ContentAlpha.medium)
                                    }else if(subtitlesState.currentCaptionVisible){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }else{
                                    if(subtitlesState.notWroteCaptionVisible || !subtitlesState.transcriptionCaption){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }
                                val indexColor =  if(index <=  subtitlesState.currentIndex){
                                    MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium)
                                }else{
                                    if(subtitlesState.notWroteCaptionVisible){
                                        MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                    }else{
                                        Color.Transparent
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(indexWidth)
                                ){
                                    Row(Modifier.width(96.dp)){
                                        if (multipleLines.enabled && playIconIndex == index) {
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
                                                            Text(text = "退出" )
                                                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                                                Text(text = " Esc")
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
                                                    multipleLines.enabled = false
                                                    playIconIndex = 0
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Close,
                                                        contentDescription = "Localized description",
                                                        tint = MaterialTheme.colors.primary
                                                    )
                                                }
                                            }

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
                                                            Text(text = "播放" )
                                                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                                                Text(text = " Tab ")
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
                                                val density = LocalDensity.current.density
                                                IconButton(onClick = {
                                                    val playItem = Caption(multipleLines.startTime ,multipleLines.endTime ,"")
                                                    playCaption(playItem)
                                                },
                                                    modifier = Modifier
                                                        .onGloballyPositioned{coordinates ->
                                                            val rect = coordinates.boundsInWindow()
                                                            if (multipleLines.isUp) {
                                                                playerPoint2.y =
                                                                    window.y + rect.top.toInt() - ((303 - 48) * density).toInt()
                                                            } else {
                                                                playerPoint2.y =
                                                                    window.y + rect.top.toInt() + (100 * density).toInt()
                                                            }
                                                            playerPoint2.x =
                                                                window.x + rect.left.toInt() - (270 * density).toInt()
                                                            if(!isVideoBoundsChanged){
                                                                // 播放按钮可以显示
                                                                if(!rect.isEmpty){
                                                                    videoPlayerBounds.location = playerPoint2
                                                                    adjustPosition(density, videoPlayerBounds)
                                                                    playerPoint2 = videoPlayerBounds.location
                                                                }
                                                            }


                                                        }
                                                ) {
                                                    val icon = if (mediaType == "audio" && !isPlaying) {
                                                        Icons.Filled.VolumeDown
                                                    } else if (mediaType == "audio") {
                                                        Icons.Filled.VolumeUp
                                                    } else Icons.Filled.PlayArrow

                                                    Icon(
                                                        icon,
                                                        contentDescription = "Localized description",
                                                        tint = MaterialTheme.colors.primary
                                                    )
                                                }
                                            }


                                        }
                                    }
                                    val ctrl = LocalCtrl.current
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
                                                    Text(text = "选择多行字幕" )
                                                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                                        Text(text = " $ctrl + N ")
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
                                        Text(
                                            modifier = Modifier.clickable {
                                                enableMultipleLines()
                                            },
                                            text = buildAnnotatedString {
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = indexColor,
                                                        fontSize = MaterialTheme.typography.h5.fontSize,
                                                        letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                        fontFamily = MaterialTheme.typography.h5.fontFamily,
                                                    )
                                                ) {
                                                    append("${index+1}")
                                                }
                                            },
                                        )
                                    }

                                }

                                Spacer(Modifier.width(20.dp))
                                Box(Modifier.width(IntrinsicSize.Max)) {
                                    if (subtitlesState.currentIndex == index) {
                                        Divider(
                                            Modifier.align(Alignment.BottomCenter)
                                                .background(MaterialTheme.colors.primary)
                                        )
                                    }

                                    BasicTextField(
                                        value =  if(subtitlesState.transcriptionCaption) textFieldValue else captionContent,
                                        onValueChange = { checkTyping(it) },
                                        singleLine = true,
                                        readOnly = !subtitlesState.transcriptionCaption,
                                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                        textStyle = MaterialTheme.typography.h5.copy(
                                            color =  if(!subtitlesState.transcriptionCaption) lineColor else Color.Transparent,
                                            fontFamily = monospace
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp)
                                            .align(Alignment.CenterStart)
                                            .focusable(subtitlesState.transcriptionCaption)
                                            .onKeyEvent { textFieldKeyEvent(it) }
                                            .focusRequester(textFieldRequester)
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    scope.launch {
                                                        if (!multipleLines.enabled) {
                                                            subtitlesState.currentIndex = index
                                                            subtitlesState.firstVisibleItemIndex =
                                                                listState.firstVisibleItemIndex
                                                            saveSubtitlesState()
                                                        }

                                                    }
                                                } else if (textFieldValue.isNotEmpty()) {
                                                    typingResult.clear()
                                                    textFieldValue = ""
                                                }
                                            }
                                    )
                                    if(pgUp){
                                        SideEffect {
                                            if(subtitlesState.currentIndex == index){
                                                textFieldRequester.requestFocus()
                                                pgUp = false
                                            }
                                        }
                                    }
                                    SideEffect {
                                        if (subtitlesState.currentIndex == index) {
                                            textFieldRequester.requestFocus()
                                        }
                                    }

                                    if(subtitlesState.transcriptionCaption){
                                        Text(
                                            text = buildAnnotatedString {

                                                typingResult.forEach { (char, correct) ->
                                                    if (correct) {
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = MaterialTheme.colors.primary.copy(alpha = alpha),
                                                                fontSize = MaterialTheme.typography.h5.fontSize,
                                                                letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                                fontFamily = monospace,
                                                            )
                                                        ) {
                                                            append(char)
                                                        }
                                                    } else {
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = Color.Red,
                                                                fontSize = MaterialTheme.typography.h5.fontSize,
                                                                letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                                fontFamily = monospace,
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
                                                val remainChars = captionContent.substring(typingResult.size)


                                                withStyle(
                                                    style = SpanStyle(
                                                        color = lineColor,
                                                        fontSize = MaterialTheme.typography.h5.fontSize,
                                                        letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                        fontFamily = monospace,
                                                    )
                                                ) {
                                                    append(remainChars)
                                                }
                                            },
                                            textAlign = TextAlign.Start,
                                            color = MaterialTheme.colors.onBackground,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(bottom = 5.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    if (subtitlesState.currentIndex == index) {
                                                        // 如果视频播放按钮被遮挡，就使用这个位置计算出视频播放器的位置
                                                        textRect = coordinates.boundsInWindow()
                                                    }

                                                }
                                        )
                                    }



                                    DropdownMenu(
                                        expanded = selectable,
                                        onDismissRequest = {
                                            selectable = false
                                        },
                                        offset = DpOffset(0.dp, (-50).dp)
                                    ) {
                                        BasicTextField(
                                            value = captionContent,
                                            onValueChange = {},
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            textStyle = MaterialTheme.typography.h5.copy(
                                                fontFamily = monospace,
                                                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                                            ),
                                            modifier = Modifier.focusable()
                                                .height(32.dp)
                                                .focusRequester(selectRequester)
                                                .onKeyEvent {
                                                    if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                                        scope.launch {
                                                            if(subtitlesState.transcriptionCaption){
                                                                selectable = !selectable
                                                            }
                                                        }
                                                        true
                                                    }else if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                                        scope.launch { openSearch() }
                                                        true
                                                    } else false
                                                }
                                        )
                                        LaunchedEffect(Unit) {
                                            selectRequester.requestFocus()
                                        }

                                    }

                                }

                                Row(Modifier.width(48.dp).height(IntrinsicSize.Max)) {
                                    if (subtitlesState.currentIndex == index && !multipleLines.enabled) {
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
                                                        Text(text = "播放" )
                                                        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                                            Text(text = " Tab")
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
                                            val density = LocalDensity.current.density
                                            IconButton(onClick = {
                                                playCaption(caption)
                                                textFieldRequester.requestFocus()
                                            },
                                                modifier = Modifier
                                                    .onGloballyPositioned { coordinates ->
                                                        val rect = coordinates.boundsInWindow()
                                                        if(!isVideoBoundsChanged){
                                                            if(!rect.isEmpty){
                                                                // 视频播放按钮没有被遮挡
                                                                videoPlayerBounds.x = window.x + rect.left.toInt() + (48 * density).toInt()
                                                                videoPlayerBounds.y = window.y + rect.top.toInt() - (100 * density).toInt()
                                                            }else{
                                                                // 视频播放按钮被遮挡
                                                                videoPlayerBounds.x = window.x + textRect.right.toInt()
                                                                videoPlayerBounds.y = window.y + textRect.top.toInt() - (100 * density).toInt()
                                                            }
                                                            // 根据一些特殊情况调整播放器的位置， 比如显示器缩放，播放器的位置超出屏幕边界。
                                                            adjustPosition(density, videoPlayerBounds)
                                                        }else{
                                                            if(!rect.isEmpty){
                                                                // 视频播放按钮没有被遮挡
                                                                playerPoint1.x = window.x + rect.left.toInt() + (48 * density).toInt()
                                                                playerPoint1.y = window.y + rect.top.toInt() - (100 * density).toInt()
                                                            }else{
                                                                // 视频播放按钮被遮挡
                                                                playerPoint1.x = window.x + textRect.right.toInt()
                                                                playerPoint1.y = window.y + textRect.top.toInt() - (100 * density).toInt()
                                                            }
                                                        }

                                                    }
                                            ) {
                                                val icon = if(mediaType=="audio" && !isPlaying) {
                                                    Icons.Filled.VolumeDown
                                                } else if(mediaType=="audio"){
                                                    Icons.Filled.VolumeUp
                                                }else Icons.Filled.PlayArrow

                                                Icon(
                                                    icon,
                                                    contentDescription = "Localized description",
                                                    tint = if(isPlaying)MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                                                )
                                            }

                                        }

                                    }
                                }

                            }

                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                    HorizontalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        adapter = rememberScrollbarAdapter(stateHorizontal)
                    )
                    if (!isAtTop) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(0)
                                    subtitlesState.currentIndex = 0
                                    subtitlesState.firstVisibleItemIndex = 0
                                    focusManager.clearFocus()
                                    saveSubtitlesState()
                                }
                            },
                            backgroundColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 100.dp, bottom = 100.dp)
                        ) {
                            Icon(
                                Icons.Filled.North,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }

                if (showOpenFile || selectedPath.isNotEmpty() || timedCaption.isEmpty()) {
                    OpenFileComponent(
                        parentComponent = window,
                        cancel = { showOpenFile = false },
                        openFileChooser = { openFileChooser() },
                        showCancel = timedCaption.isNotEmpty(),
                        setTrackId = { saveTrackID(it) },
                        setTrackDescription = { saveTrackDescription(it) },
                        trackList = trackList,
                        setTrackList = { setTrackList(it) },
                        setVideoPath = { saveVideoPath(it) },
                        selectedPath = selectedPath,
                        setSelectedPath = { selectedPath = it },
                        setSubtitlesPath = { saveSubtitlesPath(it) },
                        setTrackSize = { saveTrackSize(it) },
                    )
                }
                if (showSelectTrack) {
                    Box(
                        Modifier.fillMaxSize()
                            .align(Alignment.Center)
                            .background(MaterialTheme.colors.background)
                    ) {
                        Row(Modifier.align(Alignment.Center)) {
                            SelectTrack(
                                close = { showSelectTrack = false },
                                parentComponent = window,
                                setTrackId = { saveTrackID(it) },
                                setTrackDescription = { saveTrackDescription(it) },
                                trackList = trackList,
                                setTrackList = { setTrackList(it) },
                                setVideoPath = { saveVideoPath(it) },
                                selectedPath = subtitlesState.mediaPath,
                                setSelectedPath = { selectedPath = it },
                                setSubtitlesPath = { saveSubtitlesPath(it) },
                                setTrackSize = { saveTrackSize(it) },
                                setIsLoading = { loading = it }
                            )
                            OutlinedButton(onClick = {
                                showSelectTrack = false
                                setTrackList(listOf())
                            }) {
                                Text("取消")
                            }
                        }

                    }
                }
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 200.dp)
                    )
                }
            }

        }

        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp)
            )
        }
        Row(modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = isOpenSettings,
                setIsOpen = setIsOpenSettings,
                modifier = Modifier,
                globalState = globalState,
                saveGlobalState = saveGlobalState,
                showPlayer = showPlayer
            )

            val ctrl = LocalCtrl.current
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Text(text = "打开文件 $ctrl + O", modifier = Modifier.padding(10.dp))
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
                        showOpenFile = true
                        openFileChooser()
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
            RemoveButton( onClick = {removeSubtitles()},toolTip = "关闭当前字幕")
        }


    }

}

/**
 *  根据一些特殊情况调整播放器的位置，
 * 比如显示器缩放，播放器的位置超出屏幕边界。
 * */
private fun adjustPosition(density: Float, videoPlayerBounds: Rectangle) {
    val graphicsDevice =
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    // 只要一个显示器时，才判断屏幕边界
    if (GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size == 1) {
        val width = graphicsDevice.displayMode.width
        val height = graphicsDevice.displayMode.height
        val actualWidth = (540 * density).toInt()
        if (videoPlayerBounds.x + actualWidth > width) {
            videoPlayerBounds.x = width - actualWidth
        }
        val actualHeight = (330 * density).toInt()
        if (videoPlayerBounds.y < 0) videoPlayerBounds.y = 0
        if (videoPlayerBounds.y + actualHeight > height) {
            videoPlayerBounds.y = height - actualHeight
        }
    }

    // 显示器缩放
    if (density != 1f) {
        videoPlayerBounds.x = videoPlayerBounds.x.div(density).toInt()
        videoPlayerBounds.y = videoPlayerBounds.y.div(density).toInt()
    }
}

enum class OpenMode {
    Open, Drag,
}
@Composable
fun OpenFileComponent(
    parentComponent: Component,
    cancel: () -> Unit,
    openFileChooser: () -> Unit,
    showCancel: Boolean,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
) {

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        var loading by remember { mutableStateOf(false) }
        Column( modifier = Modifier.width(IntrinsicSize.Max).align(Alignment.Center)){
            Text(
                text = "可以拖放一个有字幕的 MKV 视频到这里或\n"+
                        "一个字幕(SRT) + 一个媒体(MKV、MP4、MP3、WAV、AAC、)一起放到这里。\n",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,

            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 20.dp),
                    onClick = { openFileChooser() }) {
                    Text("打开")
                }

                SelectTrack(
                    close = { cancel() },
                    parentComponent = parentComponent,
                    setTrackId = { setTrackId(it) },
                    setTrackDescription = { setTrackDescription(it) },
                    trackList = trackList,
                    setTrackList = { setTrackList(it) },
                    setVideoPath = { setVideoPath(it) },
                    selectedPath = selectedPath,
                    setSelectedPath = { setSelectedPath(it) },
                    setSubtitlesPath = { setSubtitlesPath(it) },
                    setTrackSize = { setTrackSize(it) },
                    setIsLoading = { loading = it }
                )
                if (showCancel) {
                    OutlinedButton(onClick = {
                        setTrackList(listOf())
                        setSelectedPath("")
                        cancel()
                    }) {
                        Text("取消")
                    }
                }
            }
        }

        if (loading) {
            CircularProgressIndicator(Modifier.width(60.dp).align(Alignment.Center).padding(bottom = 220.dp))
        }
    }

}

@Composable
fun SelectTrack(
    close: () -> Unit,
    parentComponent: Component,
    setTrackId: (Int) -> Unit,
    setTrackDescription: (String) -> Unit,
    trackList: List<Pair<Int, String>>,
    setTrackList: (List<Pair<Int, String>>) -> Unit,
    setVideoPath: (String) -> Unit,
    selectedPath: String,
    setSelectedPath: (String) -> Unit,
    setSubtitlesPath: (String) -> Unit,
    setTrackSize: (Int) -> Unit,
    setIsLoading: (Boolean) -> Unit,
) {
    if (trackList.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        val selectedSubtitle by remember { mutableStateOf("    ") }
        val scope = rememberCoroutineScope()
        Box(Modifier.width(IntrinsicSize.Max).padding(end = 20.dp)) {
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
                trackList.forEach { (trackId, description) ->
                    DropdownMenuItem(
                        onClick = {
                            setIsLoading(true)
                            scope.launch(Dispatchers.IO) {
                                expanded = false
                                val subtitles = writeToFile(selectedPath, trackId, parentComponent)
                                if (subtitles != null) {
                                    setSubtitlesPath(subtitles.absolutePath)
                                    setTrackId(trackId)
                                    setTrackDescription(description)
                                    setVideoPath(selectedPath)

                                    setTrackSize(trackList.size)
                                    setTrackList(listOf())
                                    setSelectedPath("")
                                    close()
                                }
                                setIsLoading(false)
                            }

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
}

@Composable
fun Settings(
    modifier: Modifier
) {
    Icon(
        Icons.Filled.ArrowBack,
        contentDescription = "Localized description",
        tint = MaterialTheme.colors.primary,
        modifier = modifier,
    )
}


@Composable
fun SubtitlesSidebar(
    isOpen: Boolean,
    transcriptionCaption: Boolean,
    setTranscriptionCaption:(Boolean) -> Unit,
    currentCaptionVisible: Boolean,
    setCurrentCaptionVisible:(Boolean) -> Unit,
    notWroteCaptionVisible: Boolean,
    setNotWroteCaptionVisible:(Boolean) -> Unit,
    externalSubtitlesVisible: Boolean,
    setExternalSubtitlesVisible:(Boolean) -> Unit,
    isPlayKeystrokeSound: Boolean,
    setIsPlayKeystrokeSound: (Boolean) -> Unit,
    trackSize: Int,
    selectTrack: () -> Unit,
) {
    if (isOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 78.dp else 48.dp))
            Divider()
            val ctrl = LocalCtrl.current
            val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground

            if (trackSize > 1) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { selectTrack() }
                        .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("选择字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {

                Text("抄写字幕", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = transcriptionCaption,
                    onCheckedChange = { setTranscriptionCaption(!transcriptionCaption) },
                )
            }
            if(transcriptionCaption){
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("当前字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))

                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = currentCaptionVisible,
                        onCheckedChange = { setCurrentCaptionVisible(!currentCaptionVisible) },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {

                    Text("未写字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))

                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = notWroteCaptionVisible,
                        onCheckedChange = {setNotWroteCaptionVisible(!notWroteCaptionVisible) },
                    )
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("外部字幕", color = MaterialTheme.colors.onBackground)

                }

                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = externalSubtitlesVisible,
                    onCheckedChange = {setExternalSubtitlesVisible(!externalSubtitlesVisible) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Text("击键音效", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isPlayKeystrokeSound,
                    onCheckedChange = { setIsPlayKeystrokeSound(it) },
                )
            }

        }
    }
}

/** 创建拖放处理器
 * @param singleFile 是否只接收单个文件
 * @param parseImportFile 处理导入的文件的函数
 * @param showWrongMessage 显示提示信息的函数
 */
fun createTransferHandler(
    singleFile: Boolean = true,
    parseImportFile: (List<File>) -> Unit,
    showWrongMessage: (String) -> Unit,
): TransferHandler {
    return object : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val transferable = support.transferable
            try {
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                if (singleFile) {
                    if (files.size == 1) {
                        parseImportFile(files)
                    } else {
                        showWrongMessage("一次只能读取一个文件")
                    }
                } else {
                    parseImportFile(files)
                }


            } catch (exception: UnsupportedFlavorException) {
                return false
            } catch (exception: IOException) {
                return false
            }
            return true
        }
    }
}

 private fun computeCharWidth(description:String):Int{
    val regex = "Chinese|Japanese|Korean"
    val pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(description)
    return if(matcher.find()) 24 else 12
}

/**
 * 跟读的时候，播放多条字幕
 */
class MultipleLines{

    /** 启动 */
    var enabled by mutableStateOf(false)

    /** 开始索引 */
    var startIndex by mutableStateOf(0)

    /** 结束索引 */
    var endIndex by mutableStateOf(0)

    /** 开始时间 */
    var startTime by mutableStateOf("")

    /** 结束时间 */
    var endTime by mutableStateOf("")

    /** 播放器的位置是否向上偏移,
     * 播放多条字幕要选择两个索引
     * 如果后选择的是开始索引，就向上偏移
     * 如果后现在的是结束索引，就向下偏移 */
    var isUp by mutableStateOf(false)
}

@Composable
fun rememberMultipleLines(): MultipleLines = remember{
    MultipleLines()
}
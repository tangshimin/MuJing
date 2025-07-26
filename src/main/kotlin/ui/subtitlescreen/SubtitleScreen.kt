package ui.subtitlescreen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.North
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Caption
import ffmpeg.writeSubtitleToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.*
import state.GlobalState
import theme.LocalCtrl
import ui.components.MacOSTitle
import ui.components.RemoveButton
import ui.components.Toolbar
import ui.wordscreen.playSound
import util.computeMediaType
import util.createTransferHandler
import util.parseSubtitles
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.io.File
import java.util.concurrent.FutureTask
import java.util.regex.Pattern
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

/** 支持的视频类型 */
val videoFormatList = listOf("mp4","mkv")

@OptIn(ExperimentalFoundationApi::class)
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
    var loading by remember { mutableStateOf(false) }
    var mediaType by remember { mutableStateOf(computeMediaType(subtitlesState.mediaPath)) }
    val audioPlayerComponent = LocalAudioPlayerComponent.current
    var charWidth by remember{ mutableStateOf(computeCharWidth(subtitlesState.trackDescription)) }
    /** 一次播放多条字幕 */
    val multipleLines = rememberMultipleLines()
    /** 启动播放多行字幕后，多行工具栏显示的位置索引 */
    var toolbarDisplayIndex by remember{mutableStateOf(0)}

    // 添加 PiP 相关状态
    val pipWindow = remember {
        PiPVideoWindow(onClose = { isPlaying = false })
    }



    val startPiPPlayback: (MediaInfo) -> Unit = { playMedia ->
        if (pipWindow.isInPiPMode()) {
            // 如果已经在画中画模式，不要退出，而是什么都不做
            // 让 playCaption 函数处理暂停/恢复逻辑
        } else {
            val pipBounds = validateAndGetVideoBounds(subtitlesState.videoBounds)
            pipWindow.enterPiPMode(
                mediaInfo = playMedia,
                volume = videoVolume,
                timeChanged = {
                    if(multipleLines.enabled) {
                        val currentIndex = timedCaption.getCaptionIndex(it, subtitlesState.currentIndex)
                        if(currentIndex <= multipleLines.endIndex){
                            subtitlesState.currentIndex = currentIndex
                        }
                    }
                },
                externalSubtitlesVisible = subtitlesState.externalSubtitlesVisible,
                bounds = pipBounds,
                updateBounds = { newBounds ->
                    subtitlesState.videoBounds.x = newBounds.x
                    subtitlesState.videoBounds.y = newBounds.y
                    subtitlesState.videoBounds.width = newBounds.width
                    subtitlesState.videoBounds.height = newBounds.height

                    subtitlesState.saveTypingSubtitlesState()
                },
                onPlayingStateChanged = { it ->
                    // 当画中画播放器内部状态改变时，同步更新字幕浏览器的播放状态
                    isPlaying = it
                },
                initialPlayingState = true // 传递初始播放状态为 true
            )

        }
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
                    if (file.extension == "mkv" || file.extension == "mp4") {
                        if (subtitlesState.mediaPath != file.absolutePath) {
                            selectedPath = file.absolutePath
                            parseTrackList(
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
                        subtitlesState.trackID = -1
                        subtitlesState.trackSize = 0
                        subtitlesState.currentIndex = 0
                        subtitlesState.firstVisibleItemIndex = 0
                        subtitlesState.subtitlesPath = file.absolutePath
                        subtitlesState.mediaPath = ""
                        subtitlesState.trackDescription = file.nameWithoutExtension
                        timedCaption.clear()
                        mediaType = computeMediaType(subtitlesState.mediaPath)
                        if(openMode == OpenMode.Open) showOpenFile = false
                        if(showOpenFile) showOpenFile = false
                        JOptionPane.showMessageDialog(window, "只打开了一个字幕，部分功能将无法使用")
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
                    "1个 mkv 或 mp4 视频，或 1个字幕(srt) + 1个媒体(mp4、mkv、mp3、wav、aac)",
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


    /**  使用按钮播放视频时调用的回调函数   */
    val playCaption: (Caption) -> Unit = { caption ->
        if(subtitlesState.mediaPath.isNotEmpty()){
            val file = File(subtitlesState.mediaPath)
            if (file.exists() ) {
                if (!isPlaying) {
                    // 播放逻辑
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
                            // 如果已经在画中画模式且视频暂停，直接恢复播放
                            if (pipWindow.isInPiPMode() && !pipWindow.isVideoPlaying()) {
                                pipWindow.resumeVideo()
                            } else {
                                // 使用内部字幕轨道
                                if (subtitlesState.trackID != -1) {
                                   val  playMedia = MediaInfo(
                                        mediaPath =  subtitlesState.mediaPath,
                                        caption = caption,
                                        trackId = subtitlesState.trackID,
                                    )
                                    startPiPPlayback(playMedia)
                                    // 使用外部字幕
                                } else {
                                    val playMedia = MediaInfo(
                                        mediaPath =  subtitlesState.mediaPath,
                                        caption = caption,
                                        trackId = -1,
                                    )
                                    startPiPPlayback(playMedia)
                                }
                            }
                        }
                    }
                } else {
                    // 暂停逻辑
                    scope.launch {
                        // 音频暂停
                        if(file.extension == "wav" || file.extension == "mp3"|| file.extension == "aac"){
                            if(audioPlayerComponent.mediaPlayer().status().isPlaying) {
                                audioPlayerComponent.mediaPlayer().controls().pause()
                            }
                            isPlaying = false
                        } else {
                            // 视频暂停 - 暂停画中画播放器而不是退出

                            if (pipWindow.isInPiPMode() && pipWindow.isVideoPlaying()) {
                                pipWindow.pauseVideo()
                                // 注意：不在这里设置 isPlaying = false，因为 pauseVideo() 会通过回调设置
                            }
                        }
                    }
                }

            } else {
                JOptionPane.showMessageDialog(null,"视频地址错误:${file.absolutePath}\n" +
                        "可能原视频被移动、删除或者重命名了。")
            }
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
                    // 如果画中画窗口存在（无论是否正在播放），都要停止并关闭窗口
                    if (pipWindow.isInPiPMode()) {
                        pipWindow.stopVideo()
                    }
                    // 退出多行模式
                    multipleLines.enabled = false
                    toolbarDisplayIndex = 0
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
                    // 移动到顶部
                    val scrollToTop: () -> Unit = {
                        scope.launch {
                            listState.scrollToItem(0)
                            subtitlesState.currentIndex = 0
                            subtitlesState.firstVisibleItemIndex = 0
                            focusManager.clearFocus()
                            saveSubtitlesState()
                        }
                    }

                    val startPadding = 20.dp
                    val endPadding = 10.dp
                    val indexWidth = (captionList.size.toString().length * 14).dp + 144.dp
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
                            var selectable by remember { mutableStateOf(false) }
                            val textFieldRequester = remember { FocusRequester() }
                            val next :() -> Unit = {
                                scope.launch {
                                    val end =
                                        listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size - 2
                                    if (index >= end) {
                                        listState.scrollToItem(index)
                                    }
                                   if(index+1 != captionList.size){
                                       subtitlesState.currentIndex += 1
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
                                    }else if(subtitlesState.currentIndex > 0){
                                        subtitlesState.currentIndex -= 1
                                    }

                                }
                            }


                            val enableMultipleLines:() -> Unit = {
                                if(!multipleLines.enabled){
                                    // 启用多行字幕
                                    multipleLines.enabled = true

                                    //如果 startIndex 是最后一条字幕就显示在倒数第二条字幕
                                    if(index == captionList.lastIndex){
                                        multipleLines.startIndex = index - 1
                                        multipleLines.endIndex = index - 1
                                        toolbarDisplayIndex = index -1
                                    }else{
                                        multipleLines.startIndex = index
                                        multipleLines.endIndex = index
                                        toolbarDisplayIndex = index + 1
                                    }

                                    multipleLines.startTime = caption.start
                                    multipleLines.endTime = caption.end
                                }else if(multipleLines.startIndex > index){// 设置开始
                                    multipleLines.startIndex = index
                                    // 如果 startIndex 是第一条字幕就显示在第二条字幕
                                    // 如果是多行字幕的第一个字幕，工具条显示在第一个字幕的上方
                                    toolbarDisplayIndex = if(index == 0) 1 else index - 1

                                    multipleLines.startTime = caption.start
                                }else if(multipleLines.startIndex < index){// 设置结束
                                    multipleLines.endIndex = index
                                    // 如果 endIndex 是最后一条字幕就显示在倒数第二条字幕
                                    toolbarDisplayIndex = if(index == captionList.lastIndex){
                                        index - 1
                                    }else{
                                        index + 1
                                    }

                                    multipleLines.endTime = caption.end

                                }
                            }

                            val selectAll:() -> Unit = {
                                toolbarDisplayIndex = 0
                                multipleLines.startIndex = 0
                                multipleLines.endIndex = captionList.lastIndex
                                multipleLines.startTime = captionList.first().start
                                multipleLines.endTime = captionList.last().end
                                scrollToTop()
                            }

                            val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
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
                                    (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            if(subtitlesState.transcriptionCaption){
                                                selectable = !selectable
                                            }
                                        }
                                        true
                                    }
                                    (isCtrlPressed && it.key == Key.N && it.type == KeyEventType.KeyUp) -> {
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

                            Box{
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .width(rowWidth)
                                        .padding(start = 150.dp)
                                        .background(rowBackgroundColor)
                                ) {

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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        NumButton(
                                            index = index,
                                            indexColor = indexColor,
                                            onClick = enableMultipleLines,
                                        )

                                    }

                                    Spacer(Modifier.width(20.dp))

                                    Caption(
                                        caption = caption,
                                        showUnderline = subtitlesState.currentIndex == index,
                                        isTranscribe = subtitlesState.transcriptionCaption,
                                        notWroteCaptionVisible = subtitlesState.notWroteCaptionVisible,
                                        index = index,
                                        currentIndex = subtitlesState.currentIndex,
                                        currentIndexChanged = {
                                            subtitlesState.currentIndex = index
                                            subtitlesState.firstVisibleItemIndex = listState.firstVisibleItemIndex
                                            saveSubtitlesState()
                                        },
                                        visible = subtitlesState.currentCaptionVisible,
                                        multipleLines = multipleLines,
                                        next = next,
                                        alpha = alpha,
                                        keyEvent = textFieldKeyEvent,
                                        focusRequester = textFieldRequester,
                                        selectable = selectable,
                                        exitSelection = {selectable = false},
                                    )

                                    Row(Modifier.width(48.dp).height(IntrinsicSize.Max)) {
                                        if (subtitlesState.currentIndex == index && !multipleLines.enabled) {
                                            PlayButton(
                                                caption = caption,
                                                isPlaying = isPlaying,
                                                playCaption = playCaption,
                                                textFieldRequester = textFieldRequester,
                                                mediaType = mediaType,
                                            )
                                        }
                                    }

                                }
                                MultipleLinesToolbar(
                                    modifier = Modifier.padding(start = 150.dp),
                                    index = index,
                                    isPlaying = isPlaying,
                                    toolbarDisplayIndex = toolbarDisplayIndex,
                                    multipleLines = multipleLines,
                                    mediaType = mediaType,
                                    cancel = {
                                        // 如果画中画窗口存在（无论是否正在播放），都要停止并关闭窗口
                                        if (pipWindow.isInPiPMode()) {
                                            pipWindow.stopVideo()
                                        }
                                        // 退出多行模式
                                        multipleLines.enabled = false
                                        toolbarDisplayIndex = 0
                                    },
                                    selectAll = selectAll,
                                    playCaption = playCaption,
                                )
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
                            onClick = scrollToTop,
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
            Column(Modifier.align(Alignment.TopCenter)){
                MacOSTitle(
                    title = title,
                    window = window,
                    modifier = Modifier.height(44.dp).fillMaxWidth()
                )
            }
        }
        Row(modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = isOpenSettings,
                setIsOpen = setIsOpenSettings,
                modifier = Modifier,
                globalState = globalState,
                saveGlobalState = saveGlobalState,
                showPlayer = showPlayer,
                openSearch = openSearch
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
                            Text(text = "打开文件  " )
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
                    onClick = {
                        showOpenFile = true
                        openFileChooser()
                    },
                    modifier = Modifier.padding(top = if (isMacOS()) 44.dp else 0.dp)
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


enum class OpenMode {
    Open, Drag,
}
@Composable
fun OpenFileComponent(
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

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()){
                val text = if(trackList.isEmpty()){
                    "可以拖放一个有字幕的 MKV 或 MP4 视频到这里\n"+
                    "也可以拖放一个 SRT 字幕加一个视频一起到这里"
                }else{
                    ""
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground,
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if(trackList.isEmpty()){
                    OutlinedButton(
                        modifier = Modifier.padding(top = 10.dp,end = 20.dp),
                        onClick = { openFileChooser() }) {
                        Text("打开")
                    }
                }else{
                    Text(
                        text = "选择字幕",
                        modifier = Modifier.padding(top = 10.dp,end = 20.dp),
                        color = MaterialTheme.colors.onBackground,
                    )
                }


                SelectTrack(
                    close = { cancel() },
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
                if (showCancel || trackList.isNotEmpty()) {
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
        val scope = rememberCoroutineScope()
        Box(Modifier.width(IntrinsicSize.Max).padding(end = 20.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .width(282.dp)
                    .background(Color.Transparent)
                    .border(1.dp, Color.Transparent)
            ) {
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
                                val subtitles = writeSubtitleToFile(selectedPath, trackId)
                                if (subtitles != null && subtitles.exists()) {
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

 private fun computeCharWidth(description:String):Int{
    val regex = "Chinese|Japanese|Korean"
    val pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(description)
    return if(matcher.find()) 24 else 12
}

/**
 * 对视频窗口位置和大小进行校验，如果没有设置过位置和大小，就显示在屏幕右上角。
 */
private fun validateAndGetVideoBounds(b: Rectangle): Rectangle{
    if(b.x == 0 && b.y == 0 && b.width == 0 && b.height == 0){
        // 如果没有设置过位置和大小，则使用默认值
        // 计算 PiP 窗口位置（屏幕右上角）
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val bounds = Rectangle(
            screenSize.width - 540 - 20,
            44,
            540,
            303
        )
        return bounds
    }


    return b
}
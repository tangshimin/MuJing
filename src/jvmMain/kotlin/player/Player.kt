package player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import ui.createTransferHandler
import ui.dialog.MessageDialog
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.Timer
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

/**
 * 视频播放器，可以显示单词弹幕
 * 等 Jetbrains 修复了 https://github.com/JetBrains/compose-jb/issues/1800，要执行一次重构。
 * 如果 SwingPanel 不再显示到屏幕最前面之后，也需要重构一次。
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun Player(
    close: () -> Unit,
    videoPath: String = "",
    videoPathChanged:(String) -> Unit,
    vocabulary: MutableVocabulary? = null,
    vocabularyPath:String = "",
    vocabularyPathChanged:(String) -> Unit,
    audioSet: MutableSet<String>,
    pronunciation:String,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    futureFileChooser: FutureTask<JFileChooser>,
) {

    /** 窗口的大小和位置 */
    val windowState = rememberWindowState(
        size = DpSize(1289.dp, 854.dp),
        position = WindowPosition(Alignment.Center)
    )

    /** 播放器的大小和位置 */
    val playerWindowState = rememberDialogState(
        width = 1289.dp,
        height = 854.dp,
        position = WindowPosition(Alignment.Center)
    )

    /** 播放器的设置 */
    val playerState by rememberPlayerState()

    /** 标题 */
    val title by remember (videoPath){
        derivedStateOf {
            if(videoPath.isEmpty()){
                "视频播放器"
            }else{
                File(videoPath).name
            }
        }
    }

    /** 显示视频的窗口 */
    var playerWindow by remember { mutableStateOf<ComposeDialog?>(null) }

    /** 控制视频显示的窗口，弹幕显示到这个窗口 */
    var controlWindow by remember { mutableStateOf<ComposeDialog?>(null) }

    /** 是否全屏，如果使用系统的全屏，播放器窗口会黑屏 */
    var isFullscreen by remember { mutableStateOf(false) }

    /** 全屏之前的位置 */
    var fullscreenBeforePosition by remember { mutableStateOf(WindowPosition(0.dp,0.dp)) }

    /** 全屏之前的尺寸 */
    var fullscreenBeforeSize by remember{ mutableStateOf(DpSize(1289.dp, 854.dp)) }

    /** 显示消息对话框 */
    var showMessageDialog by remember { mutableStateOf(false) }

    /** 要显示到消息对话框的消息 */
    var message by remember { mutableStateOf("") }

    /** VLC 是视频播放组件 */
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }

    /** VLC 是音频播放组件 */
    val audioPlayerComponent = LocalAudioPlayerComponent.current

    /** 是否正在播放视频 */
    var isPlaying by remember { mutableStateOf(false) }

    /** 时间进度条 */
    var timeProgress by remember { mutableStateOf(0f) }

    /** 当前时间 */
    var timeText by remember { mutableStateOf("") }

    /** 查询弹幕 */
    var searchDanmaku by remember { mutableStateOf("") }

    /** 弹幕计数器，用于快速定位弹幕 */
    var counter by remember { mutableStateOf(1) }

    /** 这个视频的所有弹幕 */
    val danmakuMap = rememberDanmakuMap(videoPath, vocabulary)

    /** 正在显示的弹幕,数字定位 */
    val showingDanmakuNum = remember { mutableStateMapOf<Int, DanmakuItem>() }

    /** 正在显示的弹幕,单词定位 */
    val showingDanmakuWord = remember { mutableStateMapOf<String, DanmakuItem>() }

    /** 需要添加到正在显示的弹幕列表的弹幕 */
    val shouldAddDanmaku = remember { mutableStateMapOf<Int, DanmakuItem>() }

    /** 通用的暂停操作，比如空格键，双击视频触发的暂停。
     * 使用这种方式触发暂停后，可以查看多个弹幕的解释，不会触发播放函数 */
    var isNormalPause by remember { mutableStateOf(false) }

    /** 播放器控制区的可见性 */
    var controlBoxVisible by remember { mutableStateOf(false) }

    /** 展开设置菜单 */
    var settingsExpanded by remember { mutableStateOf(false) }

    var showSubtitleMenu by remember{mutableStateOf(false)}

    /** 弹幕从右到左需要的时间，单位为毫秒 */
    var widthDuration by remember { mutableStateOf(playerWindowState.size.width.value.div(3).times(30).toInt()) }

    /** 动作监听器每次需要删除的弹幕列表 */
    val removedList = remember { mutableStateListOf<DanmakuItem>() }

    /** 正在显示单词详情 */
    var showingDetail by remember { mutableStateOf(false) }

    /** 显示右键菜单 */
    var showDropdownMenu by remember { mutableStateOf(false) }

    /** 显示视频文件选择器 */
    var showFilePicker by remember {mutableStateOf(false)}

    /** 显示词库文件选择器 */
    var showVocabularyPicker by remember {mutableStateOf(false)}

    /** 显示字幕选择器 */
    var showSubtitlePicker by remember{mutableStateOf(false)}

    /** 支持的视频类型 */
    val videoFormatList = remember{ mutableStateListOf("mp4","mkv") }

    /** 字幕列表 */
    val subtitleTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** 字幕轨道列表 */
    val audioTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** 当前正在显示的字幕轨道 */
    var currentSubtitleTrack by remember{mutableStateOf(0)}

    /** 当前正在播放的音频轨道 */
    var currentAudioTrack by remember{mutableStateOf(0)}

    /** 使弹幕从右往左移动的定时器 */
    val danmakuTimer by remember {
        mutableStateOf(
            Timer(30) {
                if(playerState.danmakuVisible){
                    val showingList = showingDanmakuNum.values.toList()
                    for (i in showingList.indices) {
                        val danmakuItem = showingList.getOrNull(i)
                        if ((danmakuItem != null) && !danmakuItem.isPause) {
                            if (danmakuItem.position.x > -30) {
                                danmakuItem.position = danmakuItem.position.copy(x = danmakuItem.position.x - 3)
                            } else {
                                danmakuItem.show = false
                                removedList.add(danmakuItem)
                            }
                        }
                    }
                    removedList.forEach { danmakuItem ->
                        showingDanmakuNum.remove(danmakuItem.sequence)
                        showingDanmakuWord.remove(danmakuItem.content)
                    }
                    removedList.clear()
                    shouldAddDanmaku.forEach{(sequence,danmakuItem) ->
                        showingDanmakuNum.putIfAbsent(sequence,danmakuItem)
                        showingDanmakuWord.putIfAbsent(danmakuItem.content,danmakuItem)
                    }
                    shouldAddDanmaku.clear()
                }

            }
        )
    }

    /** 关闭窗口 */
    val closeWindow: () -> Unit = {
        danmakuTimer.stop()
        close()
    }

    /** 播放 */
    val play: () -> Unit = {
        if (isPlaying) {
            danmakuTimer.stop()
            isPlaying = false
            videoPlayerComponent.mediaPlayer().controls().pause()
        } else {
            danmakuTimer.restart()
            isPlaying = true
            videoPlayerComponent.mediaPlayer().controls().play()
        }
    }

    /** 手动触发暂停，与之对应的是，用鼠标移动弹幕触发的自动暂停和用快速定位触发的自动自动暂停。*/
    val normalPause: () -> Unit = {
        isNormalPause = !isNormalPause
    }

    /** 清理弹幕 */
    val cleanDanmaku: () -> Unit = {
        showingDanmakuNum.clear()
        removedList.clear()
        shouldAddDanmaku.clear()
    }

    /** 播放单词发音 */
    val playAudio:(String) -> Unit = { word ->
        val audioPath = getAudioPath(
            word = word,
            audioSet = audioSet,
            addToAudioSet = {audioSet.add(it)},
            pronunciation = pronunciation
        )
        playAudio(
            word,
            audioPath,
            pronunciation =  pronunciation,
            audioVolume,
            audioPlayerComponent,
            changePlayerState = { },
            setIsAutoPlay = { })
    }

    /** 打开视频 */
    val openVideo:() -> Unit = {
        if(isWindows()) {
            showFilePicker = true
        }else if(isMacOS()){
            Thread {
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogTitle = "打开视频"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val path = fileChooser.selectedFile.absolutePath
                    if (!path.isNullOrEmpty()) {
                        videoPathChanged(path)
                    }
                }
            }.start()
        }
    }

    val addVocabulary:() -> Unit = {
        if(isWindows()) {
            showVocabularyPicker = true
        }else if(isMacOS()){
            Thread {
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogTitle = "添加词库"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val path = fileChooser.selectedFile.absolutePath
                    if (!path.isNullOrEmpty()) {
                        vocabularyPathChanged(path)
                    }
                }
            }.start()
        }
    }

    /** 使用这个函数处理拖放的文件 */
    val parseImportFile: (List<File>) -> Unit = { files ->
        if(files.size == 1){
            val file = files.first()
            /** 拖放的是视频。*/
            if(videoFormatList.contains(file.extension)){
                videoPathChanged(file.absolutePath)
            /** 拖放的可能是词库。*/
            }else if(file.extension == "json"){
                vocabularyPathChanged(file.absolutePath)
            }
        }else if(files.size == 2){
            val first = files.first()
            val last = files.last()
            /** 第一个文件为视频文件，第二个文件为词库。*/
            if(videoFormatList.contains(first.extension) && last.extension == "json"){
                videoPathChanged(first.absolutePath)
                vocabularyPathChanged(last.absolutePath)
            /** 第一个文件为词库，第二个文件为视频。*/
            }else if(first.extension == "json" && videoFormatList.contains(last.extension)){
                vocabularyPathChanged(first.absolutePath)
                videoPathChanged(last.absolutePath)
             /** 拖放了两个视频，只处理第一个视频。*/
            }else if(videoFormatList.contains(first.extension) && videoFormatList.contains(last.extension)){
                videoPathChanged(first.absolutePath)
            /** 拖放了两个词库，只处理第一个词库。 */
            }else if(first.extension == "json" && last.extension == "json"){
                vocabularyPathChanged(first.absolutePath)
            }
        }
    }

    val setCurrentSubtitleTrack:(Int)-> Unit = {
        currentSubtitleTrack = it
        videoPlayerComponent.mediaPlayer().subpictures().setTrack(it)
    }

    val setCurrentAudioTrack:(Int)-> Unit = {
        currentAudioTrack = it
        videoPlayerComponent.mediaPlayer().audio().setTrack(it)
    }

    val addSubtitle:(String) -> Unit = {path->
        videoPlayerComponent.mediaPlayer().subpictures().setSubTitleFile(path)
        java.util.Timer("update subtitle track list", false).schedule(500) {
            subtitleTrackList.clear()
            videoPlayerComponent.mediaPlayer().subpictures().trackDescriptions().forEach { trackDescription ->
                subtitleTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
            }
            val count = videoPlayerComponent.mediaPlayer().subpictures().trackCount()
            currentSubtitleTrack = count
        }

    }
    DisposableEffect(Unit){
        onDispose {
            videoPlayerComponent.mediaPlayer().release()
        }
    }

    Window(
        title = title,
        state = windowState,
        icon = painterResource("logo/logo.png"),
        undecorated = true,
        transparent = true,
        resizable = true,
        onCloseRequest = { closeWindow() },
    ){
        /** 最小化 */
        val minimized:() -> Unit = {
            window.isMinimized = true
        }

        /** 全屏 */
        val fullscreen:()-> Unit = {
            if(isFullscreen){
                isFullscreen = false
                playerWindowState.position =  fullscreenBeforePosition
                playerWindowState.size = fullscreenBeforeSize
                controlWindow?.isResizable = true
                playerWindow?.requestFocus()
            }else{
                isFullscreen = true
                fullscreenBeforePosition = WindowPosition(playerWindowState.position.x,playerWindowState.position.y)
                fullscreenBeforeSize =  playerWindowState.size
                playerWindowState.position = WindowPosition((-1).dp, 0.dp)
                val windowSize = Toolkit.getDefaultToolkit().screenSize.size.toComposeSize()
                playerWindowState.size = windowSize.copy(width = windowSize.width + 1.dp)
                controlWindow?.isResizable = false
                playerWindow?.requestFocus()
            }
        }

        Dialog(
            title = title,
            icon = painterResource("logo/logo.png"),
            state = playerWindowState,
            undecorated = true,
            resizable = false,
            onCloseRequest = { closeWindow() },
        ) {
            playerWindow = window
            Column(Modifier.fillMaxSize()) {
                if(isFullscreen){
                    Divider(color = Color(0xFF121212),modifier = Modifier.height(1.dp))
                }else{
                    Box(
                        Modifier.fillMaxWidth().height(40.dp)
                            .background(if (MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
                    )
                }

                Box(Modifier.fillMaxSize()) {
                    val videoSize by remember(playerWindowState.size) {
                        derivedStateOf { Dimension(window.size.width, window.size.height - 40) }
                    }
                    videoPlayerComponent.size = videoSize
                    SwingPanel(
                        background = Color.Transparent,
                        modifier = Modifier.fillMaxSize(),
                        factory = { videoPlayerComponent },
                        update = {}
                    )

                }
            }
        }


        Dialog(
            onCloseRequest = { closeWindow() },
            title = title,
            transparent = true,
            undecorated = true,
            state = playerWindowState,
            icon = painterResource("logo/logo.png"),
            onPreviewKeyEvent ={ keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    play()
                    normalPause()
                    true
                } else if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown) {
                    if(isFullscreen){
                        fullscreen()
                        true
                    }else false
                }  else if (keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(+5000L)
                    cleanDanmaku()
                    true
                } else if (keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(-5000L)
                    cleanDanmaku()
                    true
                } else false

            }
        ) {
            controlWindow = window

            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxSize()
                    .border(border = BorderStroke(1.dp, if(isFullscreen) Color.Transparent else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                    .combinedClickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = null,
                        onDoubleClick = {
                            if (showingDetail) {
                                showingDetail = false
                            } else if(isWindows()){
                                fullscreen()
                            }
                        },
                        onClick = {},
                        onLongClick = {}
                    )
                    .onPointerEvent(PointerEventType.Enter) {
                        if (!controlBoxVisible) {
                            controlBoxVisible = true
                        }
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        if (isPlaying && !settingsExpanded && !showSubtitleMenu) {
                            controlBoxVisible = false
                        }
                    }
            ) {




                Column {
                    if(isFullscreen){
                        Divider(color = Color(0xFF121212),modifier = Modifier.height(1.dp))
                    }else{
                        WindowDraggableArea {
                            TitleBar(title, closeWindow,isFullscreen,fullscreen,minimized)
                        }
                    }


                    Box(Modifier
                        .fillMaxSize()
                        .onClick(
                            matcher = PointerMatcher.mouse(PointerButton.Secondary), // add onClick for every required PointerButton
                            keyboardModifiers = { true }, // e.g { isCtrlPressed }; Remove it to ignore keyboardModifiers
                            onClick = { showDropdownMenu = true}
                        )) {

                        /** 如果手动触发了暂停，就不处理播放函数 */
                        val playEvent: () -> Unit = {
                            if (!isNormalPause) {
                                play()
                            }
                        }
                        val showingDetailChanged:(Boolean) -> Unit = {
                            showingDetail = it
                        }

                        DanmakuBox(
                            vocabulary,
                            vocabularyPath,
                            playerState,
                            showingDanmakuNum,
                            playEvent,
                            playAudio,
                            playerWindowState.size.height.value.toInt(),
                            showingDetail,
                            showingDetailChanged
                        )
                        if(isFullscreen){
                            var titleBarVisible by remember{ mutableStateOf(false) }
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .align(Alignment.TopCenter)
                                .onPointerEvent(PointerEventType.Enter){titleBarVisible = true}
                                .onPointerEvent(PointerEventType.Exit){titleBarVisible = false}
                            ){
                                AnimatedVisibility(titleBarVisible){
                                    TitleBar(title, closeWindow,isFullscreen,fullscreen,minimized)
                                }
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                        ) {
                            if (controlBoxVisible) {
                                // 进度条
                                var sliderVisible by remember { mutableStateOf(false) }
                                Box(
                                    Modifier
                                        .fillMaxWidth().padding(start = 5.dp, end = 5.dp, bottom = 10.dp)
                                        .offset(x = 0.dp, y = 20.dp)
                                        .onPointerEvent(PointerEventType.Enter) { sliderVisible = true }
                                        .onPointerEvent(PointerEventType.Exit) { sliderVisible = false }
                                ) {
                                    val animatedPosition by animateFloatAsState(
                                        targetValue = timeProgress,
                                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                                    )
                                    if (sliderVisible) {
                                        Slider(
                                            value = timeProgress,
                                            modifier = Modifier.align(Alignment.Center)
                                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                                            onValueChange = {
                                                timeProgress = it
                                                cleanDanmaku()
                                                videoPlayerComponent.mediaPlayer().controls().setPosition(timeProgress)
                                            })
                                    } else {
                                        LinearProgressIndicator(
                                            progress = animatedPosition,
                                            modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                                                .offset(x = 0.dp, y = (-20).dp).padding(top = 20.dp)
                                        )
                                    }
                                }
                                // 暂停、音量、时间、弹幕、设置
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    IconButton(onClick = {
                                        play()
                                        normalPause()
                                    }) {
                                        Icon(
                                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Localized description",
                                            tint = Color.White,
                                        )
                                    }
                                    var volumeOff by remember { mutableStateOf(false) }
                                    var volumeSliderVisible by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .onPointerEvent(PointerEventType.Enter) { volumeSliderVisible = true }
                                            .onPointerEvent(PointerEventType.Exit) { volumeSliderVisible = false }
                                    ) {
                                        IconButton(onClick = {
                                            volumeOff = !volumeOff
                                            if(volumeOff){
                                                videoPlayerComponent.mediaPlayer().audio()
                                                    .setVolume(0)
                                            }
                                        }) {
                                            Icon(
                                                if (volumeOff) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        AnimatedVisibility (visible = volumeSliderVisible) {
                                            Slider(
                                                value = videoVolume,
                                                valueRange = 1f..100f,
                                                onValueChange = {
                                                    videoVolumeChanged (it)
                                                    if(it > 1f){
                                                        volumeOff = false
                                                        videoPlayerComponent.mediaPlayer().audio()
                                                            .setVolume(videoVolume.toInt())
                                                    }else{
                                                        volumeOff = true
                                                        videoPlayerComponent.mediaPlayer().audio()
                                                            .setVolume(0)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .onPointerEvent(PointerEventType.Enter) {
                                                        volumeSliderVisible = true
                                                    }
                                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            )
                                        }
                                    }

                                    // 时间
                                    Text(" $timeText ", color = Color.White)
                                    // 设置按钮
                                    Box {
                                        IconButton(onClick = { settingsExpanded = true }) {
                                            Icon(
                                                Icons.Filled.Settings,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = settingsExpanded,
                                            offset = DpOffset(x = (-60).dp, y = 0.dp),
                                            onDismissRequest = {
                                                settingsExpanded = false
                                                controlBoxVisible = true
                                            },
                                            modifier = Modifier
                                                .onPointerEvent(PointerEventType.Enter) {
                                                    controlBoxVisible = true
                                                }
                                                .onPointerEvent(PointerEventType.Exit) {
                                                    controlBoxVisible = true
                                                }
                                        ) {

                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("单词定位弹幕")
                                                    Switch(checked = !playerState.showSequence, onCheckedChange = {
                                                        playerState.showSequence = !it
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("数字定位弹幕")
                                                    Switch(checked = playerState.showSequence, onCheckedChange = {
                                                        playerState.showSequence = it
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                            DropdownMenuItem(onClick = { }) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("弹幕")
                                                    Switch(checked = playerState.danmakuVisible, onCheckedChange = {
                                                        if (playerState.danmakuVisible) {
                                                            playerState.danmakuVisible = false
                                                            shouldAddDanmaku.clear()
                                                            showingDanmakuNum.clear()
                                                            danmakuTimer.stop()
                                                        } else {
                                                            playerState.danmakuVisible = true
                                                            danmakuTimer.restart()
                                                        }
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                        }

                                    }

                                    // 字幕和声音选择按钮
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                elevation = 4.dp,
                                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                shape = RectangleShape
                                            ) {
                                                Text(text = "字幕和声音", modifier = Modifier.padding(10.dp))
                                            }
                                        },
                                        delayMillis = 100,
                                        tooltipPlacement = TooltipPlacement.ComponentRect(
                                            anchor = Alignment.TopCenter,
                                            alignment = Alignment.TopCenter,
                                            offset = DpOffset.Zero
                                        )
                                    ) {
                                        IconButton(onClick = {showSubtitleMenu = !showSubtitleMenu  },
                                            enabled = videoPath.isNotEmpty()) {
                                            Icon(
                                                Icons.Filled.Subtitles,
                                                contentDescription = "Localized description",
                                                tint = if(videoPath.isNotEmpty()) Color.White else Color.Gray
                                            )
                                        }
                                    }

                                    var height = (subtitleTrackList.size * 40 + 60).dp
                                    if(height>740.dp) height = 740.dp
                                    DropdownMenu(
                                        expanded = showSubtitleMenu,
                                        onDismissRequest = {showSubtitleMenu = false},
                                        modifier = Modifier.width(282.dp).height(height)
                                            .onPointerEvent(PointerEventType.Enter) {
                                                controlBoxVisible = true
                                            }
                                            .onPointerEvent(PointerEventType.Exit) {
                                                controlBoxVisible = true
                                            },
                                        offset = DpOffset(x = 170.dp, y = (-20).dp),
                                    ){
                                        var state by remember { mutableStateOf(0) }
                                        TabRow(
                                            selectedTabIndex = state,
                                            backgroundColor = Color.Transparent,
                                            modifier = Modifier.width(282.dp).height(40.dp)
                                        ) {
                                            Tab(
                                                text = { Text("字幕") },
                                                selected = state == 0,
                                                onClick = { state = 0 }
                                            )
                                            Tab(
                                                text = { Text("声音") },
                                                selected = state == 1,
                                                onClick = { state = 1 }
                                            )
                                        }
                                        when (state) {
                                            0 -> {
                                                Column (Modifier.width(282.dp).height(700.dp)){
                                                    DropdownMenuItem(
                                                        onClick = {
                                                            if(isWindows()){
                                                                showSubtitlePicker = true
                                                            }else if(isMacOS()){
                                                                Thread {
                                                                    val fileChooser = futureFileChooser.get()
                                                                    fileChooser.dialogTitle = "添加字幕"
                                                                    fileChooser.fileSystemView =
                                                                        FileSystemView.getFileSystemView()
                                                                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                                                    fileChooser.selectedFile = null
                                                                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                                                        val path = fileChooser.selectedFile.absolutePath
                                                                        if (!path.isNullOrEmpty()) {
                                                                            addSubtitle(path)
                                                                        }
                                                                    }
                                                                }.start()
                                                            }

                                                        },
                                                        modifier = Modifier.width(282.dp).height(40.dp)
                                                    ) {
                                                        Text(
                                                            text = "添加字幕",
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                    Divider()
                                                    Box(Modifier.width(282.dp).height(650.dp)){
                                                        val scrollState = rememberLazyListState()
                                                        LazyColumn(Modifier.fillMaxSize(),scrollState){
                                                            items(subtitleTrackList){(track,description) ->
                                                                DropdownMenuItem(
                                                                    onClick = {
                                                                        showSubtitleMenu = false
                                                                        setCurrentSubtitleTrack(track)
                                                                    },
                                                                    modifier = Modifier.width(282.dp).height(40.dp)
                                                                ){

                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.fillMaxWidth()) {
                                                                        val color = if(currentSubtitleTrack == track)  MaterialTheme.colors.primary else  Color.Transparent
                                                                        Spacer(Modifier
                                                                            .background(color)
                                                                            .height(16.dp)
                                                                            .width(2.dp)
                                                                        )

                                                                        Text(
                                                                            text = description,
                                                                            color = if(currentSubtitleTrack == track) MaterialTheme.colors.primary else  Color.Unspecified,
                                                                            fontSize = 12.sp,
                                                                            modifier = Modifier.padding(start = 10.dp)
                                                                        )
                                                                    }

                                                                }
                                                            }
                                                        }
                                                        VerticalScrollbar(
                                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                                            adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                                            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                                                        )
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Box(Modifier.width(282.dp).height(650.dp)){
                                                    val scrollState = rememberLazyListState()
                                                    LazyColumn(Modifier.fillMaxSize(),scrollState){
                                                        items(audioTrackList){(track,description) ->
                                                            DropdownMenuItem(
                                                                onClick = {
                                                                    showSubtitleMenu = false
                                                                    setCurrentAudioTrack(track)
                                                                },
                                                                modifier = Modifier.width(282.dp).height(40.dp)
                                                            ){

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.fillMaxWidth()) {
                                                                    val color = if(currentAudioTrack == track)  MaterialTheme.colors.primary else  Color.Transparent
                                                                    Spacer(Modifier
                                                                        .background(color)
                                                                        .height(16.dp)
                                                                        .width(2.dp)
                                                                    )

                                                                    Text(
                                                                        text = description,
                                                                        color = if(currentAudioTrack == track) MaterialTheme.colors.primary else  Color.Unspecified,
                                                                        fontSize = 12.sp,
                                                                        modifier = Modifier.padding(start = 10.dp)
                                                                    )
                                                                }

                                                            }
                                                        }
                                                    }
                                                    VerticalScrollbar(
                                                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                                        adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                                                    )
                                                }
                                            }
                                        }


                                    }

                                    // 输入框
                                    if (playerState.danmakuVisible && vocabularyPath.isNotEmpty()) {

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)))
                                        ) {
                                            fun searchDanmaku() {
                                                if (playerState.showSequence && searchDanmaku.isNotEmpty()) {
                                                    val num = searchDanmaku.toIntOrNull()
                                                    if (num != null) {
                                                        val danmakuItem = showingDanmakuNum.get(num)
                                                        if (danmakuItem != null) {
                                                            danmakuItem.isPause = true
                                                            showingDetail = true
                                                            if (!isNormalPause) {
                                                                play()
                                                            }
                                                        }
                                                    }

                                                }else if(!playerState.showSequence  && searchDanmaku.isNotEmpty()){
                                                    val danmakuItem = showingDanmakuWord.get(searchDanmaku)
                                                    if (danmakuItem != null) {
                                                        danmakuItem.isPause = true
                                                        showingDetail = true
                                                        if (!isNormalPause) {
                                                            play()
                                                        }
                                                    }
                                                }
                                            }
                                            Box(modifier = Modifier.width(110.dp).padding(start = 5.dp)) {
                                                BasicTextField(
                                                    value = searchDanmaku,
                                                    singleLine = true,
                                                    onValueChange = { searchDanmaku = it },
                                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                                    textStyle = MaterialTheme.typography.h5.copy(
                                                        color = Color.White,
                                                    ),
                                                    modifier = Modifier.onKeyEvent { keyEvent ->
                                                        if ((keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && keyEvent.type == KeyEventType.KeyUp) {
                                                            searchDanmaku()
                                                            true
                                                        } else false
                                                    }
                                                )
                                                if (searchDanmaku.isEmpty()) {
                                                    val text = if(playerState.showSequence) "输入数字" else "输入单词"
                                                    Text(text, color = Color.White)
                                                }
                                            }


                                            TooltipArea(
                                                tooltip = {
                                                    Surface(
                                                        elevation = 4.dp,
                                                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                                        shape = RectangleShape
                                                    ) {
                                                        Text(text = "搜索  Enter", modifier = Modifier.padding(10.dp))
                                                    }
                                                },
                                                delayMillis = 100,
                                                tooltipPlacement = TooltipPlacement.ComponentRect(
                                                    anchor = Alignment.TopCenter,
                                                    alignment = Alignment.TopCenter,
                                                    offset = DpOffset.Zero
                                                )
                                            ) {
                                                IconButton(
                                                    onClick = { searchDanmaku() },
                                                    modifier = Modifier.size(40.dp, 40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Navigation,
                                                        contentDescription = "Localized description",
                                                        tint = Color.White,
                                                    )
                                                }

                                            }


                                        }

                                    }
                                }
                            }

                        }
                        if(videoPath.isEmpty()){
                            MaterialTheme(colors = darkColors(primary = Color.LightGray)) {
                                Row( modifier = Modifier.align(Alignment.Center)){
                                    OutlinedButton(onClick = { openVideo() }){
                                        Text("打开视频")
                                    }
                                }
                            }
                        }
                        // 视频文件选择器
                        FilePicker(
                            show = showFilePicker,
                            initialDirectory = ""
                        ){path ->
                            if(!path.isNullOrEmpty()){
                                videoPathChanged(path)
                            }
                            showFilePicker = false
                        }
                        // 词库文件选择器
                        FilePicker(
                            show = showVocabularyPicker,
                            fileExtension = "json",
                            initialDirectory = ""
                        ){path ->
                            if(!path.isNullOrEmpty()){
                                vocabularyPathChanged(path)
                            }
                            showVocabularyPicker = false
                        }
                        // 字幕文件选择器
                        FilePicker(
                            show = showSubtitlePicker,
//                        fileExtension = "srt",
                            initialDirectory = ""
                        ){path ->
                            if(!path.isNullOrEmpty()){
                                addSubtitle(path)
                            }
                            showSubtitlePicker = false
                        }

                        // 右键菜单
                        CursorDropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = {showDropdownMenu = false},
                        ){
                            DropdownMenuItem(onClick = {
                                openVideo()
                                showDropdownMenu = false
                            }) {
                                Text("打开视频")
                            }
                            DropdownMenuItem(
                                enabled = videoPath.isNotEmpty(),
                                onClick = {
                                    addVocabulary()
                                    showDropdownMenu = false
                                }) {
                                Text("添加词库")
                            }
                        }

                    }
                }

                MessageDialog(
                    show = showMessageDialog,
                    close = { showMessageDialog = false },
                    message = message
                )

            }


            /** 播放器显示后只执行一次，设置最小尺寸，绑定时间进度条，和时间,设置拖放函数 */
            LaunchedEffect(Unit) {
                if(playerState.danmakuVisible && videoPath.isNotEmpty() && danmakuMap.isNotEmpty()){
                    danmakuTimer.start()
                }
                window.minimumSize = Dimension(900,854)
                val eventListener = object:MediaPlayerEventAdapter() {
                    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                        val videoDuration = videoPlayerComponent.mediaPlayer().media().info().duration()
                        timeProgress = (newTime.toFloat()).div(videoDuration)
                        var startText: String
                        timeProgress.times(videoDuration).toInt().milliseconds.toComponents { hours, minutes, seconds, _ ->
                            startText = timeFormat(hours, minutes, seconds)
                        }
                        videoDuration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                            val durationText = timeFormat(hours, minutes, seconds)
                            timeText = "$startText / $durationText"
                        }

                    }

                    private fun timeFormat(hours: Long, minutes: Int, seconds: Int): String {
                        val h = if (hours < 10) "0$hours" else "$hours"
                        val m = if (minutes < 10) "0$minutes" else "$minutes"
                        val s = if (seconds < 10) "0$seconds" else "$seconds"
                        return "$h:$m:$s"
                    }
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                        mediaPlayer.audio().setVolume(videoVolume.toInt())
                        currentSubtitleTrack = mediaPlayer.subpictures().track()
                        currentAudioTrack = mediaPlayer.audio().track()

                        if(subtitleTrackList.isNotEmpty()) subtitleTrackList.clear()
                        if(audioTrackList.isNotEmpty()) audioTrackList.clear()

                        mediaPlayer.subpictures().trackDescriptions().forEach { trackDescription ->
                            subtitleTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                        }
                        mediaPlayer.audio().trackDescriptions().forEach { trackDescription ->
                            audioTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                        }
                    }

                    override fun finished(mediaPlayer: MediaPlayer?) {
                        isPlaying = false
                    }

                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                /** 设置拖放函数 */
                val transferHandler = createTransferHandler(
                    singleFile = false,
                    showWrongMessage = { message ->
                        JOptionPane.showMessageDialog(window, message)
                    },
                    parseImportFile = {  parseImportFile(it)}
                )
                window.transferHandler = transferHandler
            }
            /** 保存 mediaPlayerEventListener 的引用，用于删除。*/
            var mediaPlayerEventListener by remember{ mutableStateOf<MediaPlayerEventAdapter?>(null) }
            /** 启动的时候执行一次，每次添加词库后再执行一次 */
            LaunchedEffect(vocabularyPath) {
                if(mediaPlayerEventListener != null){
                    videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(mediaPlayerEventListener)
                }
                var lastTime = -1
                var lastMaxLength = 0
                val eventListener = object:MediaPlayerEventAdapter() {
                    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                        // 单位为秒
                        val startTime = (newTime.milliseconds.inWholeSeconds + widthDuration.div(3000)).toInt()
                        // 每秒执行一次
                        if (playerState.danmakuVisible && danmakuMap.isNotEmpty() && startTime != lastTime) {
                            val danmakuList = danmakuMap.get(startTime)
                            var offsetY = if(isFullscreen) 50 else 20
                            val sequenceWidth = if (playerState.showSequence) counter.toString().length * 12 else 0
                            val offsetX = sequenceWidth + lastMaxLength * 12 + 30
                            var maxLength = 0
                            danmakuList?.forEach { danmakuItem ->
                                if (offsetY > 395) offsetY = 10
                                danmakuItem.position = IntOffset(window.size.width + offsetX, offsetY)
                                offsetY += 35
                                if (danmakuItem.content.length > maxLength) {
                                    maxLength = danmakuItem.content.length
                                }

                                // TODO 这里还是有多线程问题，可能会出现：这里刚刚添加，在另一个控制动画的 Timer 里面马上就删除了。
                                danmakuItem.sequence = counter
                                shouldAddDanmaku.put(counter++, danmakuItem)
                                if(counter == 100) counter = 1
                            }
                            lastMaxLength = maxLength
                            lastTime = startTime
                        }
                    }
                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                mediaPlayerEventListener = eventListener
            }

            /** 打开视频后自动播放 */
            LaunchedEffect(videoPath) {
                if(videoPath.isNotEmpty()){
                    videoPlayerComponent.mediaPlayer().media().play(videoPath,":sub-autodetect-file")
                    isPlaying = true
                    if(playerState.danmakuVisible && !danmakuTimer.isRunning){
                        danmakuTimer.restart()
                    }
                    if(danmakuTimer.isRunning){
                        showingDanmakuNum.clear()
                    }
                }
            }

            /** 同步窗口尺寸 */
            LaunchedEffect(playerWindowState) {
                snapshotFlow { playerWindowState.size }
                    .onEach {
                        // 同步窗口和对话框的大小
                        windowState.size = playerWindowState.size
                        val titleBarHeight = if(isFullscreen) 1 else 40
                        videoPlayerComponent.size =
                            Dimension(playerWindowState.size.width.value.toInt(), playerWindowState.size.height.value.toInt() - titleBarHeight)
                        widthDuration = playerWindowState.size.width.value.div(3).times(30).toInt()
                        // 改变窗口的宽度后，有的弹幕会加速移动，还有一些弹幕会重叠，所以要把弹幕全部清除。
                        cleanDanmaku()
                    }
                    .launchIn(this)

                snapshotFlow { playerWindowState.position }
                    .onEach {
                        // 同步窗口和对话框的位置
                        windowState.position = playerWindowState.position
                    }
                    .launchIn(this)
            }
        }
    }

}
@Composable
fun TitleBar(
    title: String,
    closeWindow: () -> Unit,
    isFullscreen:Boolean,
    fullscreen:() -> Unit,
    minimized:() -> Unit,
) {
    Box(
        Modifier.fillMaxWidth()
            .height(40.dp)
            .background(if (MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
    ) {
        Text(
            title,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colors.onBackground
        )
        if(isMacOS()){
            Row(Modifier.align(Alignment.TopStart).padding(top = 8.dp)) {
                ThirteenPixelCircle(onClick = {closeWindow()},color = Color(246, 95, 87),modifier = Modifier.padding(start = 8.dp))
                ThirteenPixelCircle(onClick = {minimized()},color = Color(250, 188, 47),modifier = Modifier.padding(start = 9.dp))
                ThirteenPixelCircle(onClick = {},color = Color(0xFF9B9B9B),modifier = Modifier.padding(start = 9.dp))
            }

        }else {
            Row(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { minimized()
                }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Localized description",
                        tint = Color(140, 140, 140),
                    )
                }


                    IconButton(onClick = { fullscreen() },
                        modifier = Modifier.size(40.dp)) {
                        Icon(
                            if(isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Localized description",
                            tint = Color(140, 140, 140),
                        )
                    }


                IconButton(
                    onClick = { closeWindow() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = Color(140, 140, 140),
                    )
                }

            }
        }


    }
}

@Composable
fun ThirteenPixelCircle(
    onClick :() -> Unit,
    color: Color,
    modifier:Modifier
) {
    BoxWithConstraints {
        Box(
            modifier = modifier
                .clickable { onClick() }
                .size(13.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}


fun Dimension.toComposeSize(): DpSize = DpSize(width.dp, height.dp)

@Composable
fun DanmakuBox(
    vocabulary: MutableVocabulary?,
    vocabularyPath:String,
    playerState: PlayerState,
    showingDanmaku: SnapshotStateMap<Int, DanmakuItem>,
    playEvent: () -> Unit,
    playAudio: (String) -> Unit,
    windowHeight: Int,
    showingDetail:Boolean,
    showingDetailChanged:(Boolean) -> Unit
) {

    /** 删除单词 */
    val deleteWord: (DanmakuItem) -> Unit = { danmakuItem ->
        if (danmakuItem.word != null) {
            val word = danmakuItem.word
            vocabulary!!.wordList.remove(word)
            vocabulary.size = vocabulary.wordList.size
            saveVocabulary(vocabulary.serializeVocabulary,vocabularyPath)
        }
        showingDanmaku.remove(danmakuItem.sequence)
        showingDetailChanged(false)
        playEvent()
    }

    /** 把单词加入到熟悉词库 */
    val addToFamiliar: (DanmakuItem) -> Unit = { danmakuItem ->
        val word = danmakuItem.word
        if (word != null) {

            val file = getFamiliarVocabularyFile()
            val familiar = loadVocabulary(file.absolutePath)
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (vocabulary!!.type == VocabularyType.MKV ||
                vocabulary.type == VocabularyType.SUBTITLES
            ) {
                word.captions.forEach { caption ->
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
            if (!familiar.wordList.contains(word)) {
                familiar.wordList.add(word)
                familiar.size = familiar.wordList.size
            }
            saveVocabulary(familiar, file.absolutePath)
            deleteWord(danmakuItem)
        }

    }

    /** 等宽字体*/
    val monospace by remember {
        mutableStateOf(
            FontFamily(
                Font(
                    "font/Inconsolata-Regular.ttf",
                    FontWeight.Normal,
                    FontStyle.Normal
                )
            )
        )
    }

    // 在这个 Box 使用 Modifier.fillMaxSize() 可能会导致 DropdownMenu 显示的位置不准。
    Box {
        showingDanmaku.forEach { (_, danmakuItem) ->
            Danmaku(
                playerState,
                danmakuItem,
                playEvent,
                playAudio,
                monospace,
                windowHeight,
                deleteWord,
                addToFamiliar,
                showingDetail,
                showingDetailChanged
            )
        }
    }
}

@Composable
fun rememberDanmakuMap(
    videoPath: String,
    vocabulary: MutableVocabulary?
) = remember(videoPath, vocabulary){
    // Key 为秒 > 这一秒出现的单词列表
    val timeMap = mutableMapOf<Int, MutableList<DanmakuItem>>()
    if (vocabulary != null) {
        // 使用字幕和MKV 生成的词库
        if (vocabulary.relateVideoPath == videoPath) {
            vocabulary.wordList.forEach { word ->
                if (word.captions.isNotEmpty()) {
                    word.captions.forEach { caption ->

                        val startTime = floor(parseTime(caption.start)).toInt()
                        addDanmakuToMap(timeMap, startTime, word)
                    }
                } else {
                    word.externalCaptions.forEach { externalCaption ->
                        val startTime = floor(parseTime(externalCaption.start)).toInt()
                        addDanmakuToMap(timeMap, startTime, word)
                    }
                }
            }
        // 文档词库，或混合词库
        }else{
            vocabulary.wordList.forEach { word ->
                word.externalCaptions.forEach{externalCaption ->
                    if(externalCaption.relateVideoPath == videoPath){
                        val startTime = floor(parseTime(externalCaption.start)).toInt()
                        addDanmakuToMap(timeMap, startTime, word)
                    }
                }
            }
        }
    }
    timeMap
}

private fun addDanmakuToMap(
    timeMap: MutableMap<Int, MutableList<DanmakuItem>>,
    startTime: Int,
    word: Word
) {
    val dList = timeMap.get(startTime)
    val item = DanmakuItem(word.value, true, startTime, 0, false, IntOffset(0, 0), word)
    if (dList == null) {
        val newList = mutableListOf(item)
        timeMap.put(startTime, newList)
    } else {
        dList.add(item)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberPlayerState() = remember {
    val playerSettings = getPlayerSettingsFile()
    if (playerSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys }
            val playerData = decodeFormat.decodeFromString<PlayerData>(playerSettings.readText())
            mutableStateOf(PlayerState(playerData))
        } catch (exception: Exception) {
            println("解析视频播放器的设置失败，将使用默认值")
            val playerState = PlayerState(PlayerData())
            mutableStateOf(playerState)
        }
    } else {
        val playerState = PlayerState(PlayerData())
        mutableStateOf(playerState)
    }
}

@ExperimentalSerializationApi
@Serializable
data class PlayerData(
    var showSequence: Boolean = false,
    var danmakuVisible: Boolean = false,
    var autoCopy: Boolean = false,
    var autoSpeak: Boolean = true,
    var preferredChinese: Boolean = true
)

@OptIn(ExperimentalSerializationApi::class)
class PlayerState(playerData: PlayerData) {
    var showSequence by mutableStateOf(playerData.showSequence)
    var danmakuVisible by mutableStateOf(playerData.danmakuVisible)
    var autoCopy by mutableStateOf(playerData.autoCopy)
    var autoSpeak by mutableStateOf(playerData.autoSpeak)
    var preferredChinese by mutableStateOf(playerData.preferredChinese)

    fun savePlayerState() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        runBlocking {
            launch {
                val playerData = PlayerData(
                    showSequence, danmakuVisible, autoCopy, autoSpeak, preferredChinese
                )
                val json = encodeBuilder.encodeToString(playerData)
                val playerSettings = getPlayerSettingsFile()
                playerSettings.writeText(json)
            }
        }
    }
}

private fun getPlayerSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "PlayerSettings.json")
}
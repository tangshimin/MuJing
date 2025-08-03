package player

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import event.EventBus
import event.PlayerEventType
import icons.ArrowDown
import kotlinx.coroutines.*
import tts.rememberAzureTTS
import ui.wordscreen.rememberPronunciation
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import util.findSubtitleFiles
import util.getSubtitleLangLabel
import util.parseSubtitles
import util.readCaptionList
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun AnimatedVideoPlayer(
    visible: Boolean,
    state: PlayerState,
    audioSet: MutableSet<String>,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    windowState: WindowState,
    close: () -> Unit,
    eventBus: EventBus
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight } // 从下方进入
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight } // 向下方退出
        )
    ) {
        VideoPlayer(
            state = state,
            audioSet = audioSet,
            audioVolume = audioVolume,
            videoVolume = videoVolume,
            videoVolumeChanged = videoVolumeChanged,
            windowState = windowState,
            close = close,
            eventBus = eventBus
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun VideoPlayer(
    state: PlayerState,
    audioSet: MutableSet<String>,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    windowState: WindowState,
    close: () -> Unit,
    eventBus: EventBus
){

    val videoPath = state.videoPath
    val videoPathChanged = state.videoPathChanged
    val vocabularyPathChanged = state.vocabularyPathChanged

    /** 视频播放组件 */
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }
    val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
    val surface = remember {
        SkiaImageVideoSurface().also {
            videoPlayer.videoSurface().set(it)
        }
    }
    val pronunciation = rememberPronunciation()
    /**音频播放组件 */
    val audioPlayerComponent by remember{mutableStateOf(AudioPlayerComponent())}

    /** 是否正在播放视频 */
    var isPlaying by remember { mutableStateOf(false) }

    /** 时间进度条 */
    var timeProgress by remember { mutableStateOf(0f) }
    /** 显示时间 */
    var timeText by remember { mutableStateOf("") }
    /** 视频时长 */
    var videoDuration by remember { mutableStateOf(0L) }
    var durationText by remember { mutableStateOf("00:00:00") }

    /** 字幕管理器 */
    val timedCaption = rememberPlayerTimedCaption()
    /** 当前显示的字幕内容 */
    var caption by remember { mutableStateOf("") }
    /** 播放器控制区的可见性 */
    var controlBoxVisible by remember { mutableStateOf(false) }
    var timeSliderPress by remember { mutableStateOf(false) }
    var audioSliderPress by remember { mutableStateOf(false) }
    var playerCursor by remember{ mutableStateOf(PointerIcon.Default) }
    /** 展开设置菜单 */
    var settingsExpanded by remember { mutableStateOf(false) }

    var showSubtitleMenu by remember{mutableStateOf(false)}

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

    /** 内部字幕轨道列表 */
    val subtitleTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /**  外部字幕列表 */
    val extSubList = remember { mutableStateListOf<Pair<String,File>>() }

    /** 当前设置的外部字幕轨道,默认值为 -1 表示为设置 */
    var extSubIndex by remember { mutableStateOf(-1) }

    /** 音频轨道列表 */
    val audioTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** 当前正在显示的字幕轨道 */
    var currentSubtitleTrack by remember{mutableStateOf(0)}

    /** 当前正在播放的音频轨道 */
    var currentAudioTrack by remember{mutableStateOf(0)}

    var hideControlBoxTask : TimerTask? by remember{ mutableStateOf(null) }
    /** 焦点请求器 */
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val azureTTS = rememberAzureTTS()

    /** 播放 */
    val play: () -> Unit = {
        if (state.videoPath.isNotEmpty()){
            if (isPlaying) {
                isPlaying = false
                videoPlayerComponent.mediaPlayer().controls().pause()
            } else {
                isPlaying = true
                videoPlayerComponent.mediaPlayer().controls().play()
            }
        }else{
            println("VideoPath is Empty")
        }
    }
    /** 全屏 */
    val fullscreen:() -> Unit = {
        windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
            WindowPlacement.Floating
        } else {
            WindowPlacement.Fullscreen
        }
    }

    /** 播放单词发音 */
    val playAudio:(String) -> Unit = { word ->
        val audioPath = getAudioPath(
            word = word,
            audioSet = audioSet,
            addToAudioSet = {audioSet.add(it)},
            pronunciation = pronunciation,
            azureTTS = azureTTS,
        )
        playAudio(
            word,
            audioPath,
            pronunciation =  pronunciation,
            audioVolume,
            audioPlayerComponent,
            changePlayerState = { },
        )
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

    /** 设置内置字幕 */
    val setCurrentSubtitleTrack:(Int)-> Unit = { trackId ->
        // disable-禁用字幕的 TrackID 为 -1
        currentSubtitleTrack = trackId
        println("Track ID: $trackId")
        println("设置了内置字幕，禁用外部字幕")
        extSubIndex = -2 // 禁用外部字幕

        scope.launch(Dispatchers.Default) {
//        videoPlayerComponent.mediaPlayer().subpictures().setTrack(trackId)
            if(trackId  != -1){
                val list = readCaptionList(videoPath = videoPath, subtitleId = trackId)
                timedCaption.setCaptionList(list)
            }else{
                // 禁用字幕
                println("禁用字幕 clear timedCaption")
                timedCaption.clear()
            }
        }

    }

    /** 设置外部字幕 */
    val setExternalSubtitle :(Int,File) -> Unit = {index, file ->
        println("设置外部字幕: $index, ${file.name}")
        println("设置了外部字幕，禁用内置字幕")
        currentSubtitleTrack = -2 // 禁用内置字幕
        scope.launch (Dispatchers.Default){
            val captions = parseSubtitles(file.absolutePath)
            timedCaption.setCaptionList(captions)
            extSubIndex = index
        }
    }

    val setCurrentAudioTrack:(Int)-> Unit = {
        currentAudioTrack = it
        videoPlayerComponent.mediaPlayer().audio().setTrack(it)
    }

    // 手动添加字幕
    val addSubtitle:(String) -> Unit = {path->
        scope.launch(Dispatchers.Default) {
            val file = File(path)

            val baseName = File(videoPath).nameWithoutExtension
            val lang = getSubtitleLangLabel(baseName,file.name)
            extSubList.add(lang to file)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,  // 使用圆角形状
        modifier = Modifier.fillMaxSize()
            .pointerHoverIcon(playerCursor)
            .onPointerEvent(PointerEventType.Enter) {
                if (!controlBoxVisible) {
                    controlBoxVisible = true
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                if (isPlaying && !settingsExpanded && !showSubtitleMenu && !timeSliderPress && !audioSliderPress) {
                    controlBoxVisible = false
                }
            }
            .onPointerEvent(PointerEventType.Move) {
                controlBoxVisible = true
                hideControlBoxTask?.cancel()
                hideControlBoxTask = Timer("Hide ControlBox", false).schedule(10000) {
                    controlBoxVisible = false
                }
            }
            .focusRequester(focusRequester)
            .focusable(enabled = true)
    ) {

        LaunchedEffect(Unit){

            eventBus.events.collect { event ->
                if (event is PlayerEventType) {
                    if(event == PlayerEventType.PLAY) {
                        play()
                    }else if( event == PlayerEventType.ESC) {
                      if(windowState.placement == WindowPlacement.Fullscreen){
                            windowState.placement = WindowPlacement.Floating
                      }
                    }else if(event == PlayerEventType.FULL_SCREEN) {
                       fullscreen()
                    }

                }
            }
        }

        LaunchedEffect(controlBoxVisible){
            playerCursor = if(controlBoxVisible) PointerIcon.Default else PointerIcon.None
        }

        Row(Modifier.fillMaxSize()){
            // 视频播放器区域
            Box(Modifier
                .weight(1f)
                .onClick(
                    matcher = PointerMatcher.mouse(PointerButton.Secondary), // add onClick for every required PointerButton
                    keyboardModifiers = { true }, // e.g { isCtrlPressed }; Remove it to ignore keyboardModifiers
                    onClick = { showDropdownMenu = true}
                )

            ) {

                if(state.videoPath.isNotEmpty()){
                    // 视频渲染
                    CustomCanvas(
                        modifier =  Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .align(Alignment.Center)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { play() }, // 单击调用 play 函数
                                    onDoubleTap = {
                                        fullscreen()
                                    } // 双击切换全屏
                                )
                            },
                        surface = surface
                    )
                }else{
                    // 如果没有视频路径，则显示一个黑色背景
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .align(Alignment.Center)
                    )
                }


                // 非全屏的时候才显示关闭按钮
                if(windowState.placement != WindowPlacement.Fullscreen){
                    if(isMacOS()){
                        Row(Modifier
                            .align (Alignment.TopStart)
                            .padding(start = 72.dp, top = 8.dp)){
                            // 关闭视频播放器
                            Icon(
                                Icons.Filled.ArrowDown,
                                contentDescription = "Close Video Player",
                                tint = Color.White,
                                modifier = Modifier
                                    .clickable(onClick = close)
                                    .focusable(false)// 不让按钮自动获取焦点
                            )

                        }
                    }

                }

                // 字幕显示和控制栏
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                ) {
                    // 显示字幕
                    if(caption.isNotEmpty()){
                        SelectionContainer {
                            Text(
                                text = caption,
                                color = Color.White,
                                style = MaterialTheme.typography.h4,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    // 底部控制栏
                    if (controlBoxVisible) {
                        // 进度条
                        Box(
                            Modifier
                                .fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                                .offset(x = 0.dp, y = 20.dp)

                        ) {

                            Slider(
                                value = timeProgress,
                                modifier = Modifier.align(Alignment.Center)
                                    .onPointerEvent(PointerEventType.Press){ timeSliderPress = true }
                                    .onPointerEvent(PointerEventType.Release){ timeSliderPress = false }
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                                onValueChange = {
                                    timeProgress = it
                                    videoPlayerComponent.mediaPlayer().controls().setPosition(timeProgress)
                                },
                                colors = SliderDefaults.colors(
                                    inactiveTrackColor = Color.DarkGray // 这里设置未激活轨道颜色
                                ))
                        }
                        //时间、播放、音量、弹幕、设置
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        ) {
                            val focusManager = LocalFocusManager.current
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ){
                                // 播放按钮
                                PlayButton(
                                    onClick = play,
                                    isPlaying = isPlaying
                                )
                                // 停止按钮
                                StopButton(
                                    enabled = videoPath.isNotEmpty(),
                                    onClick = {
                                        videoPlayer.controls().stop()
                                        isPlaying = false
                                        state.videoPath = ""
                                        state.startTime = "00:00:00"
                                        caption = ""
                                        timeProgress = 0f
                                        timeText = ""
                                        extSubList.clear()
                                        timedCaption.clear()
                                        extSubIndex = -1
                                        // 清理音频轨道和字幕轨道状态
                                        audioTrackList.clear()
                                        currentAudioTrack = 0
                                        subtitleTrackList.clear()
                                        currentSubtitleTrack = 0
                                    }
                                )
                                // 音量
                                VolumeControl(
                                    videoVolume = videoVolume,
                                    videoVolumeChanged = videoVolumeChanged,
                                    videoPlayerComponent = videoPlayerComponent,
                                    audioSliderPress = audioSliderPress,
                                    onAudioSliderPressChanged = { audioSliderPress = it }
                                )
                                // 时间
                                Text(" $timeText ", color =Color.White)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ){


                                // 字幕和音频轨道选择按钮
                                SubtitleAndAudioSelector(
                                    videoPath = videoPath,
                                    subtitleTrackList = subtitleTrackList,
                                    extSubList = extSubList,
                                    audioTrackList = audioTrackList,
                                    currentSubtitleTrack = currentSubtitleTrack,
                                    currentAudioTrack = currentAudioTrack,
                                    showSubtitleMenu = showSubtitleMenu,
                                    onShowSubtitleMenuChanged = {
                                        showSubtitleMenu = it
                                        focusManager.clearFocus() // 点击后清除焦点
                                    },
                                    setExternalSubtitle = setExternalSubtitle,
                                    extSubIndex = extSubIndex,
                                    onShowSubtitlePicker = { showSubtitlePicker = true },
                                    onSubTrackChanged = setCurrentSubtitleTrack,
                                    onAudioTrackChanged = setCurrentAudioTrack,
                                    onKeepControlBoxVisible = { controlBoxVisible = true }
                                )

                                // 设置按钮
                                PlayerSettingsButton(
                                    settingsExpanded = settingsExpanded,
                                    onSettingsExpandedChanged = {
                                        settingsExpanded = it
                                        focusManager.clearFocus() // 点击后清除焦点
                                    },
                                    playerState = state,
                                    onKeepControlBoxVisible = { controlBoxVisible = true }
                                )

                                // 全屏按钮
                                FullScreenButton(
                                    isFullscreen = windowState.placement == WindowPlacement.Fullscreen,
                                    onToggleFullscreen = {
                                        fullscreen()
                                        focusManager.clearFocus() // 点击后清除焦点
                                    }
                                )


                                // 字幕列表按钮
                                CaptionListButton(
                                    onClick = {
                                        state.showCaptionList = !state.showCaptionList
                                        focusManager.clearFocus() // 点击后清除焦点
                                    }
                                )

                            }


                        }
                    }
                }

                // 刚打开播放器时的打开视频按钮
                if(state.videoPath.isEmpty()){
                    MaterialTheme(colors = darkColors(primary = Color.LightGray)) {
                        Column(Modifier
                            .align(Alignment.Center)
                            .width(600.dp)
                        ) {

                            var alert by remember { mutableStateOf(false) }
                            if(state.recentList.isNotEmpty()){
                                Row( modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically){
                                    Text("最近播放", color = MaterialTheme.colors.onSurface)
                                    OutlinedButton(onClick = { state.clearRecentList() },
                                    ){
                                        Text(text = "清除记录",color = MaterialTheme.colors.onSurface)
                                    }
                                }

                                val height = if(state.recentList.size > 10) 480.dp else (state.recentList.size * 48).dp
                                Box(Modifier
                                    .background(Color(0xFF111111))
                                    .fillMaxWidth()
                                    .height(height)){
                                    val stateVertical = rememberScrollState(0)
                                    Column(Modifier.verticalScroll(stateVertical)) {
                                       state.recentList.forEach { item ->
                                            ListItem(
                                                text = { Text(item.name, color = MaterialTheme.colors.onSurface) },
                                                trailing = {
                                                    Text(
                                                        text = item.lastPlayedTime,
                                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                modifier = Modifier.clickable {
                                                    if (File(item.path).exists()) {
                                                        state.videoPath = item.path
                                                        state.startTime = item.lastPlayedTime
                                                        val newItem = item.copy(time = LocalDateTime.now().toString())
                                                        state.saveToRecentList(newItem)
                                                    }else{
                                                        state.removeRecentItem(item)
                                                        alert = true
                                                    }
                                                },

                                            )
                                        }
                                    }

                                    VerticalScrollbar(
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
                                        adapter = rememberScrollbarAdapter(stateVertical)
                                    )
                                }
                            }
                            if(alert){
                                AlertDialog(
                                    onDismissRequest = { alert = false },
                                    title = { Text("错误",color = MaterialTheme.colors.error) },
                                    text = { Text("读取文件地址时发生错误，已自动移除。",color = MaterialTheme.colors.onSurface) },
                                    confirmButton = {
                                        OutlinedButton(onClick = { alert = false }) {
                                            Text("确定",color = MaterialTheme.colors.onSurface)
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colors.surface),
                                )
                            }
                            Row( modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                horizontalArrangement = Arrangement.Center){
                                OutlinedButton(onClick = { showFilePicker = true }){
                                    Text(text = "打开视频",color = MaterialTheme.colors.onSurface)
                                }
                            }
                        }
                    }
                }
                // 视频文件选择器
                FilePicker(
                    show = showFilePicker || showVocabularyPicker || showSubtitlePicker,
                    fileExtensions = when {
                        showVocabularyPicker -> if(isMacOS()) listOf("public.json") else listOf("json")
                        else -> emptyList()
                    },
                    initialDirectory = ""
                ) { file ->
                    if (file != null && file.path.isNotEmpty()) {
                        when {
                            showFilePicker -> {
                                // 提取 file 的名称
                                val name = File(file.path).nameWithoutExtension
                                val newItem = RecentVideo(
                                    time = LocalDateTime.now().toString(),
                                    name = name,
                                    path = file.path,
                                    lastPlayedTime = "00:00:00",
                                )
                                state.saveToRecentList(newItem)

                                videoPathChanged(file.path)
                                showFilePicker = false
                            }
                            showVocabularyPicker -> {
                                vocabularyPathChanged(file.path)
                                showVocabularyPicker = false
                            }
                            showSubtitlePicker -> {
                                addSubtitle(file.path)
                                showSubtitlePicker = false
                            }
                        }
                    } else {
                        // 取消选择时重置所有状态
                        showFilePicker = false
                        showVocabularyPicker = false
                        showSubtitlePicker = false
                    }
                }

                // 右键菜单
                CursorDropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = {showDropdownMenu = false},
                ){
                    DropdownMenuItem(onClick = {
                        showFilePicker = true
                        showDropdownMenu = false
                    }) {
                        Text("打开视频",color = MaterialTheme.colors.onSurface)
                    }
                }
            }

            CaptionList(
                show = state.showCaptionList ,
                timedCaption = timedCaption,
                play = {
                    videoPlayer.controls().setTime(it)
                    caption =  timedCaption.getCaption(it)
                    if(!videoPlayer.status().isPlaying){
                        isPlaying = true
                        videoPlayer.controls().play()

                    }
                }
            )
        }

        /** 打开视频后自动播放 */
        LaunchedEffect(videoPath) {

            if(videoPath.isNotEmpty()){
                val startTime = convertTimeToSeconds(state.startTime)
                videoPlayer.media().play(videoPath,":no-sub-autodetect-file",":start-time=${startTime}")
                isPlaying = true

                withContext(Dispatchers.IO){
                    // 自动加载第一个内置字幕
                    val list = readCaptionList(videoPath = videoPath, subtitleId = 0)
                    timedCaption.setCaptionList(list)

                    // 自动探测外部字幕
                    val subtitleFiles =  findSubtitleFiles(videoPath)
                    extSubList.addAll(subtitleFiles)

                    // 如果没有内置字幕，加载语言标签为英语的外部字幕
                    if (list.isEmpty() && extSubList.isNotEmpty()) {
                        var foundIndex = -1
                        var foundFile: File? = null
                        for ((idx, value) in subtitleFiles.withIndex()) {
                            val (lang, file) = value
                            if (lang == "en" || lang == "eng" || lang == "English" || lang == "english" || lang == "英文"
                                || lang == "简体&英文" || lang == "繁体&英文"
                                || lang == "英语" || lang == "英语（美国）" || lang == "英语（英国）"
                            ) {
                                foundIndex = idx
                                foundFile = file
                                break
                            }
                        }
                        if (foundFile != null) {
                            val captions = parseSubtitles(foundFile.absolutePath)
                            timedCaption.setCaptionList(captions)
                            extSubIndex = foundIndex
                        }
                    }
                }

            }
        }

        LaunchedEffect(Unit){
            val eventListener = object: MediaPlayerEventAdapter() {
                override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                    if(videoDuration == 0L) return
                    timeProgress = (newTime.toFloat()).div(videoDuration)
                    timeProgress.times(videoDuration).toInt().milliseconds.toComponents { hours, minutes, seconds, _ ->
                        val startText = timeFormat(hours, minutes, seconds)
                        timeText = "$startText / $durationText"
                    }
                }

                override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                    // 获取视频时长
                    videoDuration = mediaPlayer.media().info().duration()
                    videoDuration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                        durationText = timeFormat(hours, minutes, seconds)
                    }
                    // 设置视频音量
                    mediaPlayer.audio().setVolume(videoVolume.toInt())
                    // 初始化字幕和音频轨道列表
//                    currentSubtitleTrack = mediaPlayer.subpictures().track()
                    currentAudioTrack = mediaPlayer.audio().track()
                    if(subtitleTrackList.isNotEmpty()) subtitleTrackList.clear()
                    if(audioTrackList.isNotEmpty()) audioTrackList.clear()
                    // 更新字幕轨道
                    mediaPlayer.subpictures().trackDescriptions().forEachIndexed { index,trackDescription ->
                        // trackList 的 index 从 -1 开始，表示禁用
                        // 使用 index 作为字幕轨道 ID 是为了便于 ffmpeg 提取字幕
                        // trackDescription.id() 是 vlc 内部的 ID 暂时不使用
                        subtitleTrackList.add(Pair(index - 1,trackDescription.description()))
                    }
                    // 更新音频轨道
                    mediaPlayer.audio().trackDescriptions().forEach { trackDescription ->
                        audioTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                    }
                }

                override fun finished(mediaPlayer: MediaPlayer?) {
                    isPlaying = false
                }
            }
            videoPlayer.events().addMediaPlayerEventListener(eventListener)
        }

       // 更新字幕
        LaunchedEffect(Unit) {
            var lastTime = 0L
            var lastSecond = -1L
            while (isActive) {
                if(videoPlayer.status().isPlaying) {
                    val time = videoPlayer.status().time()
                    // 只有时间变化时才更新字幕
                    if(time != lastTime) {
                        val content = timedCaption.getCaption(time)
                        // 如果字幕内容不等于当前字幕内容，则更新字幕,减少不必要的重组
                        if(content != caption) {
                            caption = content
                        }
                        lastTime = time
                    }
                    // 每秒记录一次播放时间
                    val currentSecond = time / 1000L
                    if(currentSecond != lastSecond){
                        lastSecond = currentSecond
                        time.milliseconds.toComponents { hours, minutes, seconds, _ ->
                            val lastPlayed= timeFormat(hours,minutes,seconds)
                            state.updateLastPlayedTime(lastPlayed)
                        }
                    }

                }
                // 相当于 60 FPS 的更新频率
                // 16 毫秒大约是 60 FPS 的一帧
                delay(16) // 保持适当的延迟
            }
        }

        DisposableEffect(Unit){
            onDispose {
                videoPlayer.release()
                audioPlayerComponent.mediaPlayer().release()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SubtitleAndAudioSelector(
    videoPath: String,
    subtitleTrackList: List<Pair<Int,String>>,
    extSubList: List<Pair<String,File>> = emptyList(),
    audioTrackList: List<Pair<Int,String>>,
    currentSubtitleTrack: Int,
    currentAudioTrack: Int,
    showSubtitleMenu: Boolean,
    onShowSubtitleMenuChanged: (Boolean) -> Unit,
    onShowSubtitlePicker: () -> Unit,
    onSubTrackChanged: (Int) -> Unit,
    extSubIndex: Int,
    setExternalSubtitle: (Int,File) -> Unit,
    onAudioTrackChanged: (Int) -> Unit,
    onKeepControlBoxVisible: () -> Unit
) {
    Box{
        // 字幕和声音轨道选择按钮
        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "字幕和声音轨道",
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(10.dp))
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
                onClick = { onShowSubtitleMenuChanged(!showSubtitleMenu) },
                enabled = videoPath.isNotEmpty()
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Localized description",
                    tint = if(videoPath.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }

        var height = ((subtitleTrackList.size + extSubList.size) * 40 + 100).dp
        if(height > 740.dp) height = 740.dp

        DropdownMenu(
            expanded = showSubtitleMenu,
            onDismissRequest = { onShowSubtitleMenuChanged(false) },
            modifier = Modifier.width(282.dp).height(height)
                .onPointerEvent(PointerEventType.Enter) {
                    onKeepControlBoxVisible()
                }
                .onPointerEvent(PointerEventType.Exit) {
                    onKeepControlBoxVisible()
                },
            offset = DpOffset(x = 141.dp, y = (-20).dp),
        ) {
            var state by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = state,
                backgroundColor = Color.Transparent,
                modifier = Modifier.width(282.dp).height(40.dp)
            ) {
                Tab(
                    text = { Text(text = "字幕",color = MaterialTheme.colors.onSurface) },
                    selected = state == 0,
                    onClick = { state = 0 }
                )
                Tab(
                    text = { Text("声音",color = MaterialTheme.colors.onSurface) },
                    selected = state == 1,
                    onClick = { state = 1 }
                )
            }
            when (state) {
                0 -> {
                    Column(Modifier.width(282.dp).height(height)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = onShowSubtitlePicker)
                                .width(282.dp).height(40.dp)
                        ){
                            Text(
                                text = "添加字幕",
                                color = MaterialTheme.colors.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Divider()
                        Box(Modifier.width(282.dp).height(height)) {
                            val scrollState = rememberLazyListState()
                            LazyColumn(Modifier.fillMaxSize(), scrollState) {
                                items(subtitleTrackList) { (trackId, description) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                onShowSubtitleMenuChanged(false)
                                                onSubTrackChanged(trackId)
                                            })
                                            .width(282.dp).height(40.dp)
                                    ) {
                                        val color = if(currentSubtitleTrack == trackId)
                                            MaterialTheme.colors.primary else Color.Transparent
                                        Spacer(Modifier
                                            .background(color)
                                            .height(16.dp)
                                            .width(2.dp)
                                        )
                                        val desc = if(description == "Disable" && trackId == -1) "关闭字幕" else description
                                        Text(
                                            text = desc,
                                            color = if(currentSubtitleTrack == trackId)
                                                MaterialTheme.colors.primary else  MaterialTheme.colors.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }

                                }

                                itemsIndexed(extSubList) { index,(lang,file) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable(onClick = {setExternalSubtitle(index,file)})
                                            .width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if(extSubIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState = scrollState),
                            )
                        }
                    }
                }
                1 -> {
                    Box(Modifier.width(282.dp).height(height)) {
                        val scrollState = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize(), scrollState) {
                            items(audioTrackList) { (track, description) ->
                                DropdownMenuItem(
                                    onClick = {
                                        onShowSubtitleMenuChanged(false)
                                        onAudioTrackChanged(track)
                                    },
                                    modifier = Modifier.width(282.dp).height(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val color = if(currentAudioTrack == track)
                                            MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                        Spacer(Modifier
                                            .background(color)
                                            .height(16.dp)
                                            .width(2.dp)
                                        )
                                        Text(
                                            text = description,
                                            color = if(currentAudioTrack == track)
                                                MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
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
                        )
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerSettingsButton(
    settingsExpanded: Boolean,
    onSettingsExpandedChanged: (Boolean) -> Unit,
    playerState: PlayerState,
    onKeepControlBoxVisible: () -> Unit
) {
    Box {

        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(
                        text = "设置",
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            delayMillis = 100,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.TopCenter,
                alignment = Alignment.TopCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(onClick = { onSettingsExpandedChanged(true) }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Localized description",
                    tint = Color.White,
                )
            }
        }

        DropdownMenu(
            expanded = settingsExpanded,
            offset = DpOffset(x = (-60).dp, y = 0.dp),
            onDismissRequest = {
                onSettingsExpandedChanged(false)
                onKeepControlBoxVisible()
            },
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) {
                    onKeepControlBoxVisible()
                }
                .onPointerEvent(PointerEventType.Exit) {
                    onKeepControlBoxVisible()
                }
        ) {
            DropdownMenuItem(onClick = { }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("单词定位弹幕",color = MaterialTheme.colors.onSurface)
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
                    Text("数字定位弹幕",color = MaterialTheme.colors.onSurface)
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
                    Text("弹幕",color = MaterialTheme.colors.onSurface)
                    Switch(checked = playerState.danmakuVisible, onCheckedChange = {
                        playerState.danmakuVisible = !playerState.danmakuVisible
                        playerState.savePlayerState()
                    })
                }
            }
        }
    }
}

val PointerIcon.Companion.None: PointerIcon
    get() {
        val toolkit = Toolkit.getDefaultToolkit()
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val transparentCursor = toolkit.createCustomCursor(image, Point(0, 0), "transparentCursor")
        return PointerIcon(transparentCursor)
    }

private fun timeFormat(hours: Long, minutes: Int, seconds: Int): String {
    val h = if (hours < 10) "0$hours" else "$hours"
    val m = if (minutes < 10) "0$minutes" else "$minutes"
    val s = if (seconds < 10) "0$seconds" else "$seconds"
    return "$h:$m:$s"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeControl(
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    videoPlayerComponent: Component,
    audioSliderPress: Boolean,
    onAudioSliderPressChanged: (Boolean) -> Unit
) {
    // 音量
    var volumeOff by remember { mutableStateOf(false) }
    var volumeSliderVisible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { volumeSliderVisible = true }
            .onPointerEvent(PointerEventType.Exit) { if(!audioSliderPress) volumeSliderVisible = false }
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
                    .width(100.dp)
                    .padding(end = 16.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        volumeSliderVisible = true
                    }
                    .onPointerEvent(PointerEventType.Press){ onAudioSliderPressChanged(true) }
                    .onPointerEvent(PointerEventType.Release){ onAudioSliderPressChanged(false) }
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun CaptionListButton(
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(
                    text = "字幕列表",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "字幕列表",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CaptionList(
    show: Boolean,
    timedCaption: VideoPlayerTimedCaption,
    play: (Long) -> Unit,
){
    AnimatedVisibility(
        visible = show,
        enter =fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        /** 字幕列表宽度状态 */
        var subtitleListWidth by remember { mutableStateOf(400.dp) }

        Column(Modifier
            .width(subtitleListWidth)
            .background(Color(0xFF1E1E1E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ){
                Text(
                    text = "字幕列表",
                    color = Color.White
                )
            }
            Divider()
            Row (Modifier){
                val densityValue = LocalDensity.current.density
                // 拖拽分隔条
                VerticalSplitter(
                    onResize = { delta ->
                        val newWidth = subtitleListWidth - (delta / densityValue).dp
                        // 限制最小宽度为300dp，最大宽度为800dp
                        subtitleListWidth = newWidth.coerceIn(300.dp, 800.dp)
                    }
                )
                Box{
                    val listState = rememberLazyListState(0)
                    var lazyColumnHeight by remember{mutableStateOf(0)}
                    // 字幕列表内容
                    Column(
                        modifier = Modifier
                            .width(subtitleListWidth)
                            .fillMaxHeight(), // 使用深灰色背景
                    ){
                        if(timedCaption.isNotEmpty()){
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { coordinates ->
                                        lazyColumnHeight =coordinates.size.height
                                    },
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                itemsIndexed(timedCaption.captionList) { index, caption ->
                                    val focusManager = LocalFocusManager.current
                                    var background by remember { mutableStateOf(Color.Transparent) }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(background)
                                            .onClick(
                                                onClick = {
                                                    play(caption.start)
                                                    focusManager.clearFocus() // 释放焦点
                                                }
                                            )
                                            .onPointerEvent(PointerEventType.Enter) {
                                                background = Color.White.copy(alpha = 0.08f) // 柔和的悬停色

                                            }
                                            .onPointerEvent(PointerEventType.Exit){
                                                background = Color.Transparent // 鼠标移出时还原
                                            }

                                    ){
                                        SelectionContainer {
                                            Text(
                                                text = caption.content.removeSuffix("\n"),
                                                style = MaterialTheme.typography.subtitle1,
                                                color = if(index == timedCaption.currentIndex) MaterialTheme.colors.primary else  Color.LightGray,
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                            )
                                        }

                                    }
                                }
                            }
                        }

                        LaunchedEffect(timedCaption.currentIndex){
                            // 确保当前字幕在可见范围内
                            if(timedCaption.currentIndex >= 0 && timedCaption.currentIndex < listState.layoutInfo.totalItemsCount) {
                                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                val height = firstVisibleItem?.size ?: 0
                                val offset = -(lazyColumnHeight/2) + height
                                listState.scrollToItem(timedCaption.currentIndex,offset)
                            }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )

                    // 底部的工具栏
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFF1E1E1E))
                            .fillMaxWidth()
                            .padding(bottom = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Divider(Modifier.padding(bottom = 10.dp))
                        Text("一些按钮",color = Color.Transparent, modifier = Modifier.size(48.dp))

                    }
                }

            }
        }

    }
}

@Composable
fun VerticalSplitter(
    onResize: (delta: Float) -> Unit,
){
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    onResize(delta)
                },
                startDragImmediately = true,
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenButton(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isFullscreen) "退出全屏（f）" else "进入全屏（f)",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (isFullscreen) "退出全屏" else "进入全屏",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
){


    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                val text = if(isPlaying) "暂停（空格）" else "播放（空格）"
                Text(
                    text = text,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        // 播放按钮
        IconButton(onClick =onClick) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Localized description",
                tint = Color.White,
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "停止播放",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconButton(onClick = onClick,enabled = enabled) {
            Icon(
                imageVector =  Icons.Filled.Stop,
                contentDescription = "停止播放",
                tint = Color.White
            )
        }
    }
}
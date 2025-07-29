package player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import icons.ArrowDown
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import tts.rememberAzureTTS
import ui.wordscreen.rememberPronunciation
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import util.readCaptionList
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun AnimatedVideoPlayer(
    visible: Boolean,
    playerState: PlayerState,
    audioSet: MutableSet<String>,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    windowState: WindowState,
    close: () -> Unit
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
            playerState = playerState,
            audioSet = audioSet,
            audioVolume = audioVolume,
            videoVolume = videoVolume,
            videoVolumeChanged = videoVolumeChanged,
            windowState = windowState,
            close = close
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayer(
    playerState: PlayerState,
    audioSet: MutableSet<String>,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    windowState: WindowState,
    close: () -> Unit
){

    val videoPath = playerState.videoPath
    val videoPathChanged = playerState.videoPathChanged
    val vocabularyPathChanged = playerState.vocabularyPathChanged

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
    var showCaptionList by remember { mutableStateOf(false) }

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

    /** 字幕轨道列表 */
    val subtitleTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** 音频轨道列表 */
    val audioTrackList = remember{mutableStateListOf<Pair<Int,String>>()}

    /** 当前正在显示的字幕轨道 */
    var currentSubtitleTrack by remember{mutableStateOf(0)}

    /** 当前正在播放的音频轨道 */
    var currentAudioTrack by remember{mutableStateOf(0)}

    var hideControlBoxTask : TimerTask? by remember{ mutableStateOf(null) }
    /** 焦点请求器 */
    val focusRequester = remember { FocusRequester() }


    val azureTTS = rememberAzureTTS()

    /** 播放 */
    val play: () -> Unit = {
        if (videoPath.isNotEmpty()){
            if (isPlaying) {
                isPlaying = false
                videoPlayerComponent.mediaPlayer().controls().pause()
            } else {
                isPlaying = true
                videoPlayerComponent.mediaPlayer().controls().play()
            }
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
        Timer("update subtitle track list", false).schedule(500) {
            subtitleTrackList.clear()
            videoPlayerComponent.mediaPlayer().subpictures().trackDescriptions().forEach { trackDescription ->
                subtitleTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
            }
            val count = videoPlayerComponent.mediaPlayer().subpictures().trackCount()
            currentSubtitleTrack = count
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,  // 使用圆角形状
        modifier = Modifier.fillMaxSize()
            .pointerHoverIcon(playerCursor)
            .combinedClickable(
                interactionSource = remember(::MutableInteractionSource),
                indication = null,
                onDoubleClick = {
                    if (showingDetail) {
                        showingDetail = false
                    } else if(isWindows()){
                        // 处理双击全屏
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
            .onKeyEvent{ keyEvent ->
                // 处理键盘事件
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) { // 空格键
                    play()
                    true // 事件已处理
                } else if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp) { // 空格键
                   close()
                    true // 事件已处理
                } else {
                    false // 事件未处理
                }
            }

    ) {

        LaunchedEffect(Unit){
            focusRequester.requestFocus()
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

                // 视频渲染
                CustomCanvas(
                    modifier =  Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .align(Alignment.Center),
                    surface = surface
                )

                // 非全屏的时候才显示关闭按钮
                if(windowState.placement != WindowPlacement.Fullscreen){
                    Row(Modifier
                        .align (Alignment.TopStart)
                        .padding(start = 72.dp, top = 8.dp)){
                        // 关闭视频播放器
                        Icon(
                            Icons.Filled.ArrowDown,
                            contentDescription = "Close Video Player",
                            tint = Color.White,
                            modifier = Modifier.clickable(onClick = close)
                        )

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
                        var sliderVisible by remember { mutableStateOf(false) }
                        Box(
                            Modifier
                                .fillMaxWidth().padding(start = 5.dp, end = 5.dp, bottom = 10.dp)
                                .offset(x = 0.dp, y = 20.dp)
                                .onPointerEvent(PointerEventType.Enter) { sliderVisible = true }
                                .onPointerEvent(PointerEventType.Exit) {
                                    if(!timeSliderPress){
                                        sliderVisible = false
                                    }
                                }
                        ) {
                            val animatedPosition by animateFloatAsState(
                                targetValue = timeProgress,
                                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                            )
                            if (sliderVisible) {
                                Slider(
                                    value = timeProgress,
                                    modifier = Modifier.align(Alignment.Center)
                                        .onPointerEvent(PointerEventType.Press){ timeSliderPress = true }
                                        .onPointerEvent(PointerEventType.Release){ timeSliderPress = false }
                                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                                    onValueChange = {
                                        timeProgress = it
                                        videoPlayerComponent.mediaPlayer().controls().setPosition(timeProgress)
                                    })
                            } else {
                                LinearProgressIndicator(
                                    progress = animatedPosition,
                                    modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                                        .padding(horizontal = 14.dp) // 添加水平 padding 与 Slider 对齐
                                        .offset(x = 0.dp, y = (-20).dp).padding(top = 20.dp)
                                )
                            }
                        }
                        //时间、播放、音量、弹幕、设置
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            // 时间
                            Text(" $timeText ", color =Color.White)
                            // 播放按钮
                            IconButton(onClick = {
                                play()
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Localized description",
                                    tint = Color.White,
                                )
                            }
                            // 音量
                            VolumeControl(
                                videoVolume = videoVolume,
                                videoVolumeChanged = videoVolumeChanged,
                                videoPlayerComponent = videoPlayerComponent,
                                audioSliderPress = audioSliderPress,
                                onAudioSliderPressChanged = { audioSliderPress = it }
                            )

                            // 设置按钮
                            PlayerSettingsButton(
                                settingsExpanded = settingsExpanded,
                                onSettingsExpandedChanged = { settingsExpanded = it },
                                playerState = playerState,
                                onKeepControlBoxVisible = { controlBoxVisible = true }
                            )
                            // 字幕和音频轨道选择按钮
                            SubtitleAndAudioSelector(
                                videoPath = videoPath,
                                subtitleTrackList = subtitleTrackList,
                                audioTrackList = audioTrackList,
                                currentSubtitleTrack = currentSubtitleTrack,
                                currentAudioTrack = currentAudioTrack,
                                showSubtitleMenu = showSubtitleMenu,
                                onShowSubtitleMenuChanged = { showSubtitleMenu = it },
                                onShowSubtitlePicker = { showSubtitlePicker = true },
                                onSubtitleTrackChanged = setCurrentSubtitleTrack,
                                onAudioTrackChanged = setCurrentAudioTrack,
                                onKeepControlBoxVisible = { controlBoxVisible = true }
                            )

                            // 字幕列表按钮
                            CaptionListButton(
                                onClick = {showCaptionList = !showCaptionList }
                            )

                        }
                    }
                }

                // 刚打开播放器时的打开视频按钮
                if(videoPath.isEmpty()){
                    MaterialTheme(colors = darkColors(primary = Color.LightGray)) {
                        Row( modifier = Modifier
                            .align(Alignment.Center)){
                            OutlinedButton(onClick = { showFilePicker = true }){
                                Text(text = "打开视频",color = MaterialTheme.colors.onSurface)
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
                show = showCaptionList ,
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
                videoPlayer.media().play(videoPath,":sub-autodetect-file")
                isPlaying = true

                // 自动加载第一个字幕
                val list = readCaptionList(videoPath = videoPath, subtitleId = 0)
                timedCaption.setCaptionList(list)
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
        }

        // 或者添加条件判断减少不必要的操作
        LaunchedEffect(Unit) {
            var lastTime = 0L
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
                }
                // 相当于 60 FPS 的更新频率
                // 16 毫秒大约是 60 FPS 的一帧
                delay(16) // 保持适当的延迟
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SubtitleAndAudioSelector(
    videoPath: String,
    subtitleTrackList: List<Pair<Int,String>>,
    audioTrackList: List<Pair<Int,String>>,
    currentSubtitleTrack: Int,
    currentAudioTrack: Int,
    showSubtitleMenu: Boolean,
    onShowSubtitleMenuChanged: (Boolean) -> Unit,
    onShowSubtitlePicker: () -> Unit,
    onSubtitleTrackChanged: (Int) -> Unit,
    onAudioTrackChanged: (Int) -> Unit,
    onKeepControlBoxVisible: () -> Unit
) {
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

    var height = (subtitleTrackList.size * 40 + 100).dp
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
        offset = DpOffset(x = 170.dp, y = (-20).dp),
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
                Column(Modifier.width(282.dp).height(700.dp)) {
                    DropdownMenuItem(
                        onClick = {
                            onShowSubtitlePicker()
                        },
                        modifier = Modifier.width(282.dp).height(40.dp)
                    ) {
                        Text(
                            text = "添加字幕",
                            color = MaterialTheme.colors.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Divider()
                    Box(Modifier.width(282.dp).height(650.dp)) {
                        val scrollState = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize(), scrollState) {
                            items(subtitleTrackList) { (track, description) ->
                                DropdownMenuItem(
                                    onClick = {
                                        onShowSubtitleMenuChanged(false)
                                        onSubtitleTrackChanged(track)
                                    },
                                    modifier = Modifier.width(282.dp).height(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val color = if(currentSubtitleTrack == track)
                                            MaterialTheme.colors.primary else Color.Transparent
                                        Spacer(Modifier
                                            .background(color)
                                            .height(16.dp)
                                            .width(2.dp)
                                        )
                                        Text(
                                            text = description,
                                            color = if(currentSubtitleTrack == track)
                                                MaterialTheme.colors.primary else  MaterialTheme.colors.onSurface,
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
            1 -> {
                Box(Modifier.width(282.dp).height(650.dp)) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerSettingsButton(
    settingsExpanded: Boolean,
    onSettingsExpandedChanged: (Boolean) -> Unit,
    playerState: PlayerState,
    onKeepControlBoxVisible: () -> Unit
) {
    Box {
        IconButton(onClick = { onSettingsExpandedChanged(true) }) {
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
                        if (playerState.danmakuVisible) {
                            playerState.danmakuVisible = false
                        } else {
                            playerState.danmakuVisible = true
                        }
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
                    .width(60.dp)
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
                Icons.Filled.Dehaze,
                contentDescription = "字幕列表",
                tint = Color.White
            )
        }
    }
}

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
                Box(){
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { play(caption.start) }
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
            .width(1.dp)
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
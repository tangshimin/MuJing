/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import event.EventBus
import event.PlayerEventType
import icons.ArrowDown
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import player.danmaku.CanvasDanmakuContainer
import player.danmaku.DanmakuStateManager
import player.danmaku.TimelineSynchronizer
import state.getSettingsDirectory
import theme.LocalCtrl
import theme.rememberDarkThemeSelectionColors
import tts.rememberAzureTTS
import ui.components.MacOSTitle
import ui.wordscreen.rememberPronunciation
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import util.*
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
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
    eventBus: EventBus,
    window: ComposeWindow,
    title: String = "视频播放器",
    openSearch: () -> Unit,
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
        CompositionLocalProvider(
            LocalTextSelectionColors provides rememberDarkThemeSelectionColors()
        ){

            val close = {
                state.visible = false
                state.videoPath = ""
                state.showCaptionList = false
                state.showContextTrackId = 0
                // 退出全屏
                if(!isMacOS() && windowState.placement == WindowPlacement.Fullscreen){
                    windowState.placement = WindowPlacement.Floating
                }


            }

            VideoPlayer(
                state = state,
                audioSet = audioSet,
                audioVolume = audioVolume,
                videoVolume = videoVolume,
                videoVolumeChanged = videoVolumeChanged,
                windowState = windowState,
                close = close,
                eventBus = eventBus,
                window = window,
                title = title,
                openSearch = openSearch
            )
        }

    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun VideoPlayer(
    state: PlayerState,
    audioSet: MutableSet<String>,
    audioVolume: Float,
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    windowState: WindowState,
    close: () -> Unit,
    eventBus: EventBus,
    window: ComposeWindow,
    title : String = "视频播放器",
    openSearch: () -> Unit,
){

    val videoPath = state.videoPath
    val videoPathChanged = state.videoPathChanged
    val vocabularyPathChanged = state.vocabularyPathChanged

    /** 视频播放组件 */
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent2()) }
    val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
    val surface = remember {
        SkiaImageVideoSurface().also {
            videoPlayer.videoSurface().set(it)
        }
    }

    /** 音频播放组件 */
    val audioPlayerComponent by remember{mutableStateOf(AudioPlayerComponent())}
    val pronunciation = rememberPronunciation()


    /** 是否正在播放视频 */
    var isPlaying by remember { mutableStateOf(false) }
    /** 是否激活自动暂停 */
    var autoPauseActive by remember { mutableStateOf(false) }

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


    /** 是否手动跳转字幕。
     * 用于重复播放当前字幕，播放上一句字幕，播放下一句字幕,
     * 字幕列表手动点击一条字幕。 */
    var isManualSeeking by remember { mutableStateOf(false) }

    /** 播放器控制区的可见性 */
    var controlBoxVisible by remember { mutableStateOf(false) }
    var timeSliderPress by remember { mutableStateOf(false) }
    var audioSliderPress by remember { mutableStateOf(false) }
    var playerCursor by remember{ mutableStateOf(PointerIcon.Default) }
    /** 展开设置菜单 */
    var settingsExpanded by remember { mutableStateOf(false) }

    var showSubtitleMenu by remember{mutableStateOf(false)}

    /** 正在显示单词详情 */
    var showDictPopup by remember { mutableStateOf(false) }
    var isCaptionAreaHovered by remember { mutableStateOf(false) }

    /** 显示右键菜单 */
    var showDropdownMenu by remember { mutableStateOf(false) }

    /** 初始界面的打开视频按钮 */
    var openVideo by remember {mutableStateOf(false)}

    /** 支持的视频类型 */
    val videoFormatList = remember { mutableStateListOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "ts", "m4v", "3gp", "mpeg", "mpg") }

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

    /** 当前正在显示的字幕的语言标签 */
    var subtitleDescription by remember { mutableStateOf("") }
    var primaryCaptionVisible by remember { mutableStateOf(true) }
    var secondaryCaptionVisible by remember { mutableStateOf(true) }
    var isSwap by remember { mutableStateOf(false) }

    /** 当前正在播放的音频轨道 */
    var currentAudioTrack by remember{mutableStateOf(0)}

    var hideControlBoxJob by remember { mutableStateOf<Job?>(null) }

    /** 焦点请求器 */
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val azureTTS = rememberAzureTTS()

    val focusManager = LocalFocusManager.current
    // FileKit 文件选择器启动器
    // 打开视频按钮、右键菜单的打开视频、播放列表的添加视频都使用这个
    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = videoFormatList.toTypedArray()),
        title = "选择视频文件",
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { platformFile ->
            if (platformFile != null) {
                val file = platformFile.file
                // 打开视频
                if(openVideo){
                    val name = File(file.path).nameWithoutExtension
                    val newItem = RecentVideo(
                        dateTime = LocalDateTime.now().toString(),
                        name = name,
                        path = file.path,
                        lastPlayedTime = "00:00:00",
                    )
                    state.saveToRecentList(newItem)

                    videoPathChanged(file.path)
                    openVideo = false
                }else{
                    // 添加视频到播放列表
                    val playlistItem = PlaylistItem(
                        name = file.nameWithoutExtension,
                        path = file.absolutePath,
                        lastPlayedTime = "00:00:00",
                        isCurrentlyPlaying = false,
                        type = PlaylistItemType.DEFAULT
                    )
                    state.playlist.add(playlistItem)
                    state.showNotification("已添加: ${file.nameWithoutExtension}")
                }

            }
            focusManager.clearFocus()
        }
    )

    // FileKit 文件夹选择器启动器
    val directoryPickerLauncher = rememberDirectoryPickerLauncher(
        title = "选择视频文件夹",
        onResult = { platformFile ->
            if (platformFile != null) {
                // 检查是否是目录
                val directory = platformFile.file
                if (directory.isDirectory) {
                    // 添加文件夹所有视频到播放列表
                    val videoFormatList = listOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "ts", "m4v", "3gp", "mpeg", "mpg")
                    val videoFiles = directory.listFiles { f ->
                        f.isFile && videoFormatList.contains(f.extension.lowercase())
                    }?.sortedBy { it.name }

                    if (videoFiles != null && videoFiles.isNotEmpty()) {
                        videoFiles.forEach { videoFile ->
                            val playlistItem = PlaylistItem(
                                name = videoFile.nameWithoutExtension,
                                path = videoFile.absolutePath,
                                lastPlayedTime = "00:00:00",
                                isCurrentlyPlaying = false,
                                type = PlaylistItemType.DEFAULT
                            )
                            state.playlist.add(playlistItem)
                        }
                        state.showNotification("已添加 ${videoFiles.size} 个视频")
                    } else {
                        state.showNotification("文件夹中没有视频文件", NotificationType.ACTION)
                    }
                }
            }
        }
    )

    // 创建媒体时间流
    val mediaTimeFlow = remember { MutableStateFlow(0L) }

    var danmakuManager by remember { mutableStateOf<DanmakuStateManager?>(null) }
    var timelineSynchronizer by remember {mutableStateOf<TimelineSynchronizer?>(null)  }
    /** Windows 的全屏要特殊处理 */
    var isWindowsFullscreen by remember { mutableStateOf(false) }
    /** 保持屏幕常亮的协程任务 */
    var keepScreenAwake by remember { mutableStateOf<Job?>(null) }
    // 使用 PlayerState 中的 playlistSelectedTab 状态

    /** 播放 */
    val play: () -> Unit = {
        if (state.videoPath.isNotEmpty()){
            if (isPlaying) {
                isPlaying = false
                videoPlayerComponent.mediaPlayer().controls().pause()
                state.showNotification("暂停", NotificationType.ACTION)
            } else {
                isPlaying = true
                videoPlayerComponent.mediaPlayer().controls().play()
                state.showNotification("继续", NotificationType.ACTION)
                // 播放时取消自动暂停
                if(state.autoPause){
                    autoPauseActive = false
                    caption = ""
                }
            }
        }else{
            println("VideoPath is Empty")
        }
    }


    /** 全屏 */
    val fullscreen:() -> Unit = {
        if(isMacOS()){
            if (windowState.placement == WindowPlacement.Fullscreen) {
                windowState.placement =  WindowPlacement.Floating
            } else {
                windowState.placement =   WindowPlacement.Fullscreen
            }
        }else{
            isWindowsFullscreen = !isWindowsFullscreen
            FullScreenManager.toggle( window.windowHandle)
            if(isWindowsFullscreen){
                // 全屏时隐藏标题栏
                window.rootPane.putClientProperty("JRootPane.useWindowDecorations", false)
                window.rootPane.putClientProperty("JRootPane.menuBarEmbedded", false)
            }else{
                // 退出全屏时显示标题栏
                window.rootPane.putClientProperty("JRootPane.useWindowDecorations", true)
                window.rootPane.putClientProperty("JRootPane.menuBarEmbedded", true)
            }
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

    // 拖放处理
    val dropTarget = remember {
        createDragAndDropTarget { files ->
            parseImportFile(files)
        }
    }

    /** 更新字幕索引 */
    val updateCaptionIndex:() -> Unit = {
        val newTime = videoPlayer.status().time()
        timedCaption.updateCurrentIndex(newTime)
    }

    /** 设置内置字幕 */
    val setCurrentSubtitleTrack:(Int,String)-> Unit = { trackId,description ->
        // disable-禁用字幕的 TrackID 为 -1
        currentSubtitleTrack = trackId
        subtitleDescription = getLanguageLabel(description)

        println("Track ID: $trackId")
        println("设置了内置字幕，禁用外部字幕")
        extSubIndex = -2 // 禁用外部字幕
        // 取消主次字幕切换
        isSwap = false

        scope.launch(Dispatchers.Default) {
            if(trackId  != -1){
                val list = readCaptionList(
                    videoPath = videoPath,
                    subtitleId = trackId,
                    showMessage = {state.showNotification(it)}
                )
                timedCaption.setCaptionList(list)
                if(list.isNotEmpty()){
                    val applicationDir = getSettingsDirectory()
                    val subPath = "$applicationDir/VideoPlayer/subtitle.srt"
                    generateMatchedVocabulary(
                        videoPath = videoPath,
                        subPath =subPath,
                        trackId = currentSubtitleTrack,
                        state = state,
                    )
                }
                // 保存内部字幕偏好设置
                saveSubtitlePreference(videoPath, "internal", trackId,description = subtitleDescription)
            }else{
                // 禁用字幕
                println("禁用字幕 clear timedCaption")
                timedCaption.clear()
                // 保存禁用字幕偏好设置
                saveSubtitlePreference(videoPath, "disabled",description = "")
            }
            // 更新字幕索引
            updateCaptionIndex()
        }

    }

    /** 设置外部字幕 */
    val setExternalSubtitle :(Int,File,String) -> Unit = {index, file,description ->
        println("设置外部字幕: $index, ${file.name}")
        println("设置了外部字幕，禁用内置字幕")
        currentSubtitleTrack = -2 // 禁用内置字幕
        subtitleDescription = description
        // 取消主次字幕切换
        isSwap = false

        scope.launch (Dispatchers.Default){
            // 把 ASS 字幕转换成 SRT 字幕
            if(file.extension == "ass"){
                loadAndConvertAssSubtitle(
                    file = file,
                    onSuccess = { captions ->
                        timedCaption.setCaptionList(captions)
                    },
                    onFailure = { errorMessage ->
                        state.showNotification(errorMessage)
                    }
                )
            }else{
                val captions = parseSubtitles(file.absolutePath)
                timedCaption.setCaptionList(captions)
            }

           if(timedCaption.isNotEmpty()){
               extSubIndex = index
               // 更新字幕索引
               updateCaptionIndex()

               // 保存外部字幕偏好设置
               saveSubtitlePreference(videoPath, "external", -1, file.absolutePath,description)

               generateMatchedVocabulary(
                   videoPath = videoPath,
                   subPath = file.absolutePath,
                   trackId = currentSubtitleTrack,
                   state = state,
               )
           }

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
            // 检查是否已经添加过相同路径的字幕
            val alreadyExists = extSubList.any { it.second.absolutePath == file.absolutePath }
            if(!alreadyExists){
                extSubList.add(lang to file)
            }

        }
    }

    // 添加字幕到字幕轨道
    val subtitlePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("srt","ass")),
        title = "选择字幕文件",
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { platformFile ->
            if (platformFile != null) {
                val file = platformFile.file
                addSubtitle(file.path)
            }
            focusManager.clearFocus()
        }
    )

    /** 播放上一条字幕 */
    val  previousCaption: () -> Unit =  {
        if (videoPlayer.status().isPlayable && timedCaption.isNotEmpty()){
            // A 键 跳转到上一条字幕
            val previousTime = timedCaption.getPreviousCaptionTime()
            if (previousTime >= 0) {
                videoPlayer.controls().setTime(previousTime)
                caption = timedCaption.getCaption(previousTime)
                if (!isPlaying) {
                    isPlaying = true
                    videoPlayer.controls().play()
                }
                autoPauseActive = false
                isManualSeeking = true
            }
        }
    }

   /** 播放下一条字幕 */
    val  nextCaption: () -> Unit =  {
       if (videoPlayer.status().isPlayable && timedCaption.isNotEmpty()){
           // D 键 跳转到下一条字幕
           val nextTime = timedCaption.getNextCaptionTime()
           if (nextTime >= 0) {
               videoPlayer.controls().setTime(nextTime)
               caption = timedCaption.getCaption(nextTime)
               if (!isPlaying) {
                   isPlaying = true
                   videoPlayer.controls().play()
               }
               autoPauseActive = false
               isManualSeeking = true
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
            pronunciation = pronunciation,
            audioVolume,
            audioPlayerComponent,
            changePlayerState = { },
        )
    }


    /** 重复播放当前字幕 */
    val  replayCaption: () -> Unit = {
        if(videoPlayer.status().isPlayable && timedCaption.isNotEmpty()){
            val time = timedCaption.getCurrentCaptionTime()
            if (time >= 0) {
                videoPlayer.controls().setTime(time)
                caption = timedCaption.getCaption(time)
                if (!isPlaying) {
                    isPlaying = true
                    videoPlayer.controls().play()
                }
                autoPauseActive = false
                isManualSeeking = true
            }
        }
    }


    Surface(
        shape = MaterialTheme.shapes.medium,  // 使用圆角形状
        modifier = Modifier.fillMaxSize()
            .pointerHoverIcon(playerCursor)
            .dragAndDropTarget(
                shouldStartDragAndDrop =shouldStartDragAndDrop,
                target = dropTarget
            )
            .onPointerEvent(PointerEventType.Enter) {
                if (!controlBoxVisible) {
                    controlBoxVisible = true
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                if (isPlaying && !settingsExpanded && !showSubtitleMenu &&
                    !timeSliderPress && !audioSliderPress && !isCaptionAreaHovered
                    && !showDictPopup) {
                    controlBoxVisible = false
                }
            }
            .onPointerEvent(PointerEventType.Move) {
                controlBoxVisible = true
                hideControlBoxJob?.cancel()
                hideControlBoxJob = scope.launch {
                    delay(10000)
                    if (!isCaptionAreaHovered && !showDictPopup) {
                        controlBoxVisible = false
                    }
                }
            }
            .focusRequester(focusRequester)
            .focusable(enabled = true)
    ) {

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

                    Box(modifier =  Modifier
                        .fillMaxSize()){
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
                        // 弹幕渲染
                        if (state.danmakuVisible) {
                            // 弹幕显示区域
                            CanvasDanmakuContainer(
                                modifier = Modifier.fillMaxSize(),
                                fontSize = 20,
                                speed = 2f,
                                maxDanmakuCount = 50,
                                mediaTimeFlow = mediaTimeFlow,
                                isPaused = !isPlaying,
                                playerState = state,
                                onDanmakuManagerCreated = { manager ->
                                    danmakuManager = manager
                                },
                                onTimelineSynchronizerCreated = { synchronizer ->
                                    timelineSynchronizer = synchronizer
                                },
                                playAudio = playAudio,
                                onHoverChanged = { isHovering ->
                                    showDictPopup = isHovering
                                }
                            )
                        }
                    }


                }else{
                    // 如果没有视频路径，则显示一个黑色背景
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .align(Alignment.Center)
                    )
                }


                if (isMacOS() && windowState.placement != WindowPlacement.Fullscreen) {
                    Column (Modifier.align(Alignment.TopCenter).background(Color.Black)) {
                        MacOSTitle(title = title, window = window, modifier = Modifier.height(38.dp))
                    }
                }

                Box(Modifier
                    .align (Alignment.TopStart)
                    .padding(start = 72.dp, top = if(isMacOS())0.dp else 8.dp)
                ){
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                val ctrl = LocalCtrl.current
                                val shortcut = if (isMacOS()) "$ctrl W" else "$ctrl+W"
                                Row(modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ){
                                    Text(text = "关闭播放器  ",color = MaterialTheme.colors.onSurface)
                                    Text(text =shortcut,color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                }

                            }
                        },
                        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.BottomCenter,
                            alignment = Alignment.BottomCenter,
                            offset = DpOffset.Zero
                        )

                    ) {

                        // 非全屏的时候显示一个关闭 Icon
                        if(isMacOS() && (windowState.placement != WindowPlacement.Fullscreen)){
                            Icon(
                                Icons.Filled.ArrowDown,
                                contentDescription = "Close Video Player",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable(onClick = close)
                                    .focusable(false)
                            )
                        }else{

                            if(controlBoxVisible){

                                if((isMacOS() && state.showClose) || !isMacOS()){
                                    Surface(
                                        modifier = Modifier
                                            .padding(top = if(isMacOS())16.dp else 0.dp) // macOS 全屏模式时顶部有 16 dp 不可点击区域
                                            .size(38.dp),
                                        elevation = 0.dp,
                                        color =  Color.Black.copy(alpha = 0.5f),
                                        border = BorderStroke((0.5).dp, Color.White.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp),
                                    ){
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                                .clickable{close()}
                                                .focusable(false)

                                        ){
                                            Icon(
                                                Icons.Filled.ArrowDown,
                                                contentDescription = "Close Video Player",
                                                tint = Color.White,
                                            )
                                        }

                                    }

                                }


                            }


                        }

                    }

                }

                // 在这里显示通知
                if (state.showNotification) {
                    val alignment = if(state.notificationType == NotificationType.INFO) Alignment.TopStart else Alignment.Center
                    NotificationMessage(
                        message = state.notificationMessage,
                        type = state.notificationType,
                        onDismiss = { state.showNotification = false },
                        modifier = Modifier
                            .align(alignment)
                            .padding(start = 10.dp,top = if(isMacOS())54.dp else 10.dp)
                    )
                }

                // 字幕显示和控制栏
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                ) {

                    var prevPlayState by remember { mutableStateOf<Boolean?>(null) }
                    val pauseIfPlaying: () -> Unit = {
                        if (videoPlayer.status().isPlayable && isPlaying) {
                            videoPlayer.controls().pause()
                            isPlaying = false
                        }
                    }

                    val tryRestorePlayback: () -> Unit = {
                        if (!isCaptionAreaHovered && !showDictPopup) {
                            prevPlayState?.let { prev ->
                                if (videoPlayer.status().isPlayable) {
                                    if (prev && !videoPlayer.status().isPlaying) {
                                        videoPlayer.controls().play()
                                        isPlaying = true
                                    } else if (!prev && videoPlayer.status().isPlaying) {
                                        videoPlayer.controls().pause()
                                        isPlaying = false
                                    }
                                }
                                prevPlayState = null
                            }
                        }
                    }

                    // 显示字幕
                    if(caption.isNotEmpty()){
                        var isSelectionActivated by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                        ){
                            Box(
                                Modifier
                                    .background(if(isCaptionAreaHovered) Color(29,30,31) else Color.Black.copy(alpha = 0.5f))
                                    .onPointerEvent(PointerEventType.Enter) {
                                        isCaptionAreaHovered = true
                                    }
                                    .onPointerEvent(PointerEventType.Exit) {
                                        isCaptionAreaHovered = false
                                    }
                            ){

                                if(!isSelectionActivated){

                                        HoverableCaption(
                                            caption =caption.removeSuffix("\n"),
                                            playAudio = playAudio,
                                            playerState = state,
                                            primaryCaptionVisible = primaryCaptionVisible,
                                            secondaryCaptionVisible = secondaryCaptionVisible,
                                            isBilingual = subtitleDescription.contains("&"),
                                            swapEnabled = isSwap,
                                            modifier = Modifier.align(Alignment.Center)
                                                .padding(48.dp)
                                            ,
                                            onPopupHoverChanged = { hovering ->
                                                showDictPopup = hovering
                                                if (hovering) {
                                                    if (prevPlayState == null) {
                                                        prevPlayState = isPlaying
                                                    }
                                                    pauseIfPlaying()
                                                } else {
                                                    scope.launch {
                                                        delay(50)
                                                        tryRestorePlayback()
                                                    }
                                                }
                                            },
                                            addWord ={
                                                if(it.captions.size<3){
                                                    val playerCaption= timedCaption.getCurrentPlayerCaption()
                                                    val dataCaption = playerCaption.toDataCaption()
                                                    it.captions.add(dataCaption)
                                                }
                                                state.addWord(it)
                                            },
                                            addToFamiliar = {
                                                if(it.captions.size<3){
                                                    val playerCaption= timedCaption.getCurrentPlayerCaption()
                                                    val dataCaption = playerCaption.toDataCaption()
                                                    it.captions.add(dataCaption)
                                                }
                                                state.addToFamiliar(it)
                                            }
                                        )


                                }else{
                                    CustomTextMenuProvider {
                                        val textFieldRequester by remember { mutableStateOf(FocusRequester()) }
                                        BasicTextField(
                                            value = caption.removeSuffix("\n"),
                                            onValueChange = {  },
                                            singleLine = false,
                                            textStyle = MaterialTheme.typography.h4.copy(
                                                color = Color.White,
                                                textAlign = TextAlign.Start,
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .focusRequester(textFieldRequester)
                                                .onPointerEvent(PointerEventType.Enter){isCaptionAreaHovered = true}
                                                .padding(48.dp)
                                                .onKeyEvent {
                                                    val isModifierPressed = if (isMacOS()) it.isMetaPressed else it.isCtrlPressed
                                                    if (it.type == KeyEventType.KeyUp && it.key == Key.Escape) {
                                                        isSelectionActivated = false
                                                        focusManager.clearFocus()
                                                        true
                                                    }else if ( isModifierPressed && it.type == KeyEventType.KeyUp && it.key == Key.A) {
                                                        // 消耗事件，防止全选的时候触发 A 快捷键切换到上一条字幕
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                            ,
                                        )
                                        LaunchedEffect(Unit){
                                            textFieldRequester.requestFocus()
                                        }

                                    }
                                }

                                // 字幕工具栏
                                if(isCaptionAreaHovered){
                                    CaptionToolbar(
                                        subtitleDescription = subtitleDescription,
                                        primaryCaptionVisible = primaryCaptionVisible,
                                        secondaryCaptionVisible = secondaryCaptionVisible,
                                        isSwap = isSwap,
                                        isSelectionActivated = isSelectionActivated,
                                        onPrimaryCaptionToggle = { primaryCaptionVisible = !primaryCaptionVisible },
                                        onSecondaryCaptionToggle = { secondaryCaptionVisible = !secondaryCaptionVisible },
                                        onSwapToggle = { isSwap = !isSwap },
                                        onSelectionToggle = { isSelectionActivated = !isSelectionActivated },
                                        onFocusClear = { focusManager.clearFocus() },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp)
                                    )
                                }
                            }
                            DisposableEffect(Unit){
                                onDispose {
                                    isCaptionAreaHovered = false
                                }
                            }

                        }

                        // 如果控制栏不可见，则在字幕下方添加间隔
                        if(!controlBoxVisible){
                            Box(Modifier.fillMaxWidth().height(20.dp))
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

                                    val newTime = videoPlayer.status().time()
                                    newTime.milliseconds.toComponents { hours, minutes, seconds, _ ->
                                        val lastPlayed= timeFormat(hours,minutes,seconds)
                                        state.updateLastPlayedTime(lastPlayed)
                                    }
                                    updateCaptionIndex()
                                    if(state.autoPause){
                                        autoPauseActive = false
                                        isManualSeeking = true
                                    }
                                    focusManager.clearFocus()
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
                                        state.showContextTrackId = 0
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
                                        extSubList.clear()
                                        currentSubtitleTrack = 0
                                        // 清理弹幕
                                        timelineSynchronizer?.clear()
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

                                // 单词弹幕按钮
                                DanmakuButton(
                                    isEnabled = state.danmakuVisible,
                                    onCheckedChange = {
                                        state.danmakuVisible = it
                                        // 点击后清除焦点
                                        focusManager.clearFocus()
                                        state.savePlayerState()
                                    }
                                )

                                // 自动暂停按钮
                                AutoPauseButton(
                                    isEnabled = state.autoPause,
                                    active = autoPauseActive,
                                    onCheckedChange = {
                                        state.autoPause = it
                                        state.savePlayerState()
                                        // 点击后清除焦点
                                        focusManager.clearFocus()
                                    }
                                )

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
                                    onShowSubtitlePicker = {
                                        subtitlePickerLauncher.launch()
                                    },
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

                                val isFullscreen = if(isWindows()){
                                    isWindowsFullscreen
                                }else{
                                    windowState.placement == WindowPlacement.Fullscreen
                                }

                                // 全屏按钮
                                FullScreenButton(
                                    isFullscreen = isFullscreen,
                                    onToggleFullscreen = {
                                        fullscreen()
                                        focusManager.clearFocus() // 点击后清除焦点
                                    }
                                )


                                // 字幕列表按钮
                                ListButton(
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
                                    .fillMaxWidth()
                                    .height(height)
                                    .shadow(elevation = 0.dp,shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF111111))
                                )
                                {
                                    val stateVertical = rememberScrollState(0)
                                    Column(Modifier.verticalScroll(stateVertical)) {
                                       state.recentList.forEach { item ->
                                           var hovered by remember { mutableStateOf(false) }
                                           ListItem(
                                               text = {
                                                   Text(
                                                       item.name,
                                                       color = MaterialTheme.colors.onSurface,
                                                       maxLines = 1,
                                                       softWrap = false,
                                                       overflow = TextOverflow.Ellipsis,
                                                   )
                                               },
                                                trailing = {
                                                    Row(verticalAlignment = Alignment.CenterVertically){
                                                        Text(
                                                            text = item.lastPlayedTime,
                                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                                            fontSize = 12.sp
                                                        )
                                                       if(hovered){
                                                           Icon(
                                                               Icons.Filled.Close,
                                                               contentDescription = "Remove",
                                                               tint = MaterialTheme.colors.onSurface.copy(alpha = 0.9f),
                                                               modifier = Modifier
                                                                   .padding(start = 4.dp)
                                                                   .size(20.dp)
                                                                   .clickable {
                                                                       state.removeRecentItem(item)
                                                                   }
                                                           )
                                                       }else{
                                                           // 占位
                                                           Box(Modifier.padding(start = 4.dp).size(20.dp))
                                                       }

                                                    }

                                                },
                                                modifier = Modifier
                                                    .onPointerEvent(PointerEventType.Enter) { hovered = true }
                                                    .onPointerEvent(PointerEventType.Exit) { hovered = false }
                                                    .clickable {
                                                    scope.launch (Dispatchers.Default){
                                                        val lastPlayedTime = item.lastPlayedTime
                                                        if (File(item.path).exists()) {
                                                            val playerComponent = createMediaPlayerComponent2()
                                                            val mediaPlayer = playerComponent.mediaPlayer()
                                                            mediaPlayer.media().prepare(item.path)
                                                            mediaPlayer.controls().start()
                                                            delay(100)
                                                            val duration = mediaPlayer.media().info().duration()
                                                            mediaPlayer.controls().stop()
                                                            playerComponent.release()
                                                            duration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                                                                val durationStr = timeFormat(hours, minutes, seconds)
                                                                if(durationStr == lastPlayedTime){
                                                                    state.startTime = "00:00:00"
                                                                }else{
                                                                    state.startTime = lastPlayedTime
                                                                }
                                                            }
                                                            state.videoPathChanged(item.path)
                                                            val newItem = item.copy(
                                                                dateTime = LocalDateTime.now().toString(),
                                                                lastPlayedTime = lastPlayedTime
                                                            )
                                                            state.saveToRecentList(newItem)
                                                        }else{
                                                            state.removeRecentItem(item)
                                                            alert = true
                                                        }
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
                                    text = { Text("文件不存在，已从最近列表中移除。",color = MaterialTheme.colors.onSurface) },
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
                                OutlinedButton(onClick = {
                                    openVideo = true
                                    filePickerLauncher.launch()

                                }){
                                    Text(text = "打开视频",color = MaterialTheme.colors.onSurface)
                                }
                            }
                        }
                    }
                }



                // 右键菜单
                CursorDropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = {
                        showDropdownMenu = false
                        focusManager.clearFocus()
                    },
                ){
                    DropdownMenuItem(onClick = {
                        openVideo = true
                        filePickerLauncher.launch()
                        showDropdownMenu = false
                    }) {
                        Text("打开视频",color = MaterialTheme.colors.onSurface)
                    }
                }
            }

            CaptionAndVideoList(
                show = state.showCaptionList ,
                timedCaption = timedCaption,
                play = {
                    videoPlayer.controls().setTime(it)
                    caption =  timedCaption.getCaption(it)
                    if(!videoPlayer.status().isPlaying){
                        isPlaying = true
                        videoPlayer.controls().play()
                    }
                    autoPauseActive = false
                    isManualSeeking = true
                },
                previousCaption = previousCaption,
                replayCaption = replayCaption,
                nextCaption =  nextCaption,
                playerState = state,
                listSelectedTab = state.listSelectedTab,
                onListSelectedTabChanged = { state.listSelectedTab = it },
                filePickerLauncher = filePickerLauncher,
                directoryPickerLauncher = directoryPickerLauncher
            )
        }

        // 键盘快捷键
        LaunchedEffect(Unit){
            eventBus.events.collect { event ->
                if (event is PlayerEventType) {
                    when (event) {
                        PlayerEventType.PLAY -> {
                            play()
                        }
                        PlayerEventType.ESC -> {
                            if(windowState.placement == WindowPlacement.Fullscreen){
                                windowState.placement = WindowPlacement.Floating
                            }
                        }
                        PlayerEventType.OPEN_SEARCH -> {
                            openSearch()
                        }
                        PlayerEventType.FULL_SCREEN -> {
                            fullscreen()
                        }
                        PlayerEventType.CLOSE_PLAYER -> {
                            close()
                        }
                        PlayerEventType.DIRECTION_LEFT -> {
                            // 左方向键
                            if (videoPlayer.status().isPlayable) {
                                if (isPlaying) {
                                    videoPlayer.controls().skipTime(-5000) // 快退 5 秒
                                } else {
                                    val newTime = videoPlayer.status().time() - 5000
                                    videoPlayer.controls().setTime(newTime.coerceAtLeast(0)) // 设置时间
                                    surface.setRenderingEnabled(true) // 确保渲染启用
                                }
                            }
                        }
                        PlayerEventType.DIRECTION_RIGHT -> {
                            // 右方向键
                            if (videoPlayer.status().isPlayable) {
                                if (isPlaying) {
                                    videoPlayer.controls().skipTime(5000) // 快进 5 秒
                                } else {
                                    val newTime = videoPlayer.status().time() + 5000
                                    videoPlayer.controls().setTime(newTime.coerceAtMost(videoDuration)) // 设置时间
                                    surface.setRenderingEnabled(true) // 确保渲染启用
                                }
                            }

                        }
                        PlayerEventType.PREVIOUS_CAPTION -> {
                            // A 键 跳转到上一条字幕
                            state.showNotification("上一条字幕", NotificationType.ACTION)
                            previousCaption()
                        }
                        PlayerEventType.NEXT_CAPTION -> {
                            // D 键 跳转到下一条字幕
                            state.showNotification("下一条字幕", NotificationType.ACTION)
                            nextCaption()
                        }
                        PlayerEventType.REPEAT_CAPTION -> {
                            // S 键 重复当前字幕
                            state.showNotification("重复当前字幕", NotificationType.ACTION)
                            replayCaption()
                        }
                        PlayerEventType.AUTO_PAUSE -> {
                            if(state.autoPause){
                                autoPauseActive = false
                            }
                            state.autoPause = !state.autoPause
                            state.savePlayerState()
                        }
                        PlayerEventType.DIRECTION_UP -> {
                            scope.launch {
                                // 音量增加
                                val currentVolume = videoPlayerComponent.mediaPlayer().audio().volume().toFloat()
                                val volume =(currentVolume + 5f).coerceAtMost(100f)
                                videoVolumeChanged(volume)
                                videoPlayerComponent.mediaPlayer().audio().setVolume(volume.toInt())

                                state.showNotification("音量: ${volume.toInt()}", NotificationType.ACTION)
                            }
                        }
                        PlayerEventType.DIRECTION_DOWN -> {
                            scope.launch {
                                // 音量减少
                                val currentVolume = videoPlayerComponent.mediaPlayer().audio().volume().toFloat()
                                val volume =(currentVolume - 5f).coerceAtLeast(0f)
                                videoVolumeChanged(volume)
                                videoPlayerComponent.mediaPlayer().audio().setVolume(volume.toInt())

                                state.showNotification("音量: ${volume.toInt()}", NotificationType.ACTION)
                            }
                        }

                        PlayerEventType.TOGGLE_FIRST_CAPTION ->{
                            primaryCaptionVisible = !primaryCaptionVisible
                            val message = "${if(primaryCaptionVisible) "显示" else "隐藏"}第一语言字幕"
                            state.showNotification(message, NotificationType.ACTION)
                        }
                        PlayerEventType.TOGGLE_SECOND_CAPTION -> {
                            secondaryCaptionVisible = !secondaryCaptionVisible
                            val message = "${if(secondaryCaptionVisible) "显示" else "隐藏"}第二语言字幕"
                            state.showNotification(message, NotificationType.ACTION)
                        }
                    }
                }
            }
        }


        /** 打开视频后自动播放 */
        LaunchedEffect(videoPath) {

            if(videoPath.isNotEmpty()){
                val startTime = convertTimeToSeconds(state.startTime)
                videoPlayer.media().play(videoPath,":no-sub-autodetect-file",":start-time=${startTime}")
                isPlaying = true
                withContext(Dispatchers.IO){
                    loadSubtitleAndVocabulary(
                        videoPath = videoPath,
                        extSubList = extSubList,
                        state = state,
                        timedCaption = timedCaption,
                        currentSubtitleTrack = currentSubtitleTrack,
                        subtitleTrackList = subtitleTrackList,
                        updateCaptionIndex = updateCaptionIndex,
                        updateCurrentSubtitleTrack = { currentSubtitleTrack = it },
                        updateExtSubIndex = { extSubIndex = it },
                        updateSubtitleDescription = { subtitleDescription = it }
                    )
                }

            }
        }

        /** 加载弹幕 */
        LaunchedEffect(timelineSynchronizer,videoPath,  state.vocabulary) {
            if(videoPath.isNotEmpty() && timelineSynchronizer != null){
                withContext(Dispatchers.Default){
                    if(state.vocabularyPath.isNotEmpty() && state.vocabulary != null){
                        timelineSynchronizer?.loadTimedDanmakusFromVocabulary(
                            videoPath,
                            state.vocabularyPath,
                            state.vocabulary
                        )
                    }

                }
            }
        }

        DisposableEffect(Unit){
            val eventListener = object: MediaPlayerEventAdapter() {
                override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                    if(videoDuration == 0L) return
                    timeProgress = (newTime.toFloat()).div(videoDuration)
                    timeProgress.times(videoDuration).toInt().milliseconds.toComponents { hours, minutes, seconds, _ ->
                        val startText = timeFormat(hours, minutes, seconds)
                        timeText = "$startText / $durationText"
                    }

                    // 更新时间流
                    mediaTimeFlow.value = newTime
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

                    //不使用 VLC 渲染字幕
                    videoPlayerComponent.mediaPlayer().subpictures().setTrack(-1)

                    // 更新音频轨道
                    mediaPlayer.audio().trackDescriptions().forEach { trackDescription ->
                        audioTrackList.add(Pair(trackDescription.id(),trackDescription.description()))
                    }
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    // 更新最后播放时间
                    val duration = mediaPlayer.media().info().duration()
                    duration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                        val lastPlayed= timeFormat(hours,minutes,seconds)
                        state.updateLastPlayedTime(lastPlayed)
                    }
                    isPlaying = false
                }
            }
            videoPlayer.events().addMediaPlayerEventListener(eventListener)

            onDispose {
                try{
                    // 停止播放
                    if(videoPlayer.status().isPlaying) {
                        videoPlayer.controls().stop()
                    }
                    // 释放视频表面资源
                    surface.release()
                    videoPlayerComponent.release()
                    audioPlayerComponent.release()
                    System.gc()

                }catch (e: Exception) {
                    e.printStackTrace()
                    println("释放视频播放器资源时发生错误: ${e.message}")
                }
            }

        }

        // 更新字幕
        LaunchedEffect(Unit) {
            var lastTime = 0L
            var lastSecond = -1L
            while (isActive) {
                if(videoPlayer.status().isPlaying) {
                    val newTime = videoPlayer.status().time()
                    // 只有时间变化时才更新字幕
                    if(newTime != lastTime) {
                        val content = timedCaption.getCaption(newTime)
                        if(content != caption){
                            // 触发自动暂停
                            if(state.autoPause){
                                if(!autoPauseActive && caption.isNotEmpty() && !isManualSeeking) {
                                    // 暂停播放
                                    videoPlayer.controls().pause()
                                    isPlaying = false
                                    autoPauseActive = true
                                    state.showNotification("自动暂停", NotificationType.ACTION)
                                } else if(!autoPauseActive){
                                    caption = content
                                    // 播放结束，重复结束
                                    isManualSeeking = false
                                }
                            }else{
                                caption = content
                            }

                        }
                        lastTime = newTime
                    }
                    // 每秒记录一次播放时间
                    val currentSecond = newTime / 1000L
                    if(currentSecond != lastSecond){
                        lastSecond = currentSecond
                        newTime.milliseconds.toComponents { hours, minutes, seconds, _ ->
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

        // 窗口最小化时暂停视频渲染
        LaunchedEffect(windowState.isMinimized,isPlaying) {
            if(windowState.isMinimized || !isPlaying){
                // 当窗口最小化时，暂停视频渲染
                surface.setRenderingEnabled(false)
                stopKeepingScreenAwake(keepScreenAwake)
            }else{
                // 当窗口恢复时，继续渲染视频
                surface.setRenderingEnabled(true)
                 keepScreenAwake = keepScreenAwake(scope)

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

fun timeFormat(hours: Long, minutes: Int, seconds: Int): String {
    val h = if (hours < 10) "0$hours" else "$hours"
    val m = if (minutes < 10) "0$minutes" else "$minutes"
    val s = if (seconds < 10) "0$seconds" else "$seconds"
    return "$h:$m:$s"
}


fun removeSuffix(description:String):String{
   return description.removeSuffix(".srt").removeSuffix(".ass")
}

private fun loadSubtitleAndVocabulary(
    videoPath: String,
    extSubList: SnapshotStateList<Pair<String, File>>,
    state: PlayerState,
    timedCaption: VideoPlayerTimedCaption,
    currentSubtitleTrack: Int,
    subtitleTrackList: SnapshotStateList<Pair<Int, String>>,
    updateCaptionIndex: () -> Unit,
    updateCurrentSubtitleTrack:(Int) -> Unit,
    updateExtSubIndex:(Int) -> Unit,
    updateSubtitleDescription:(String) -> Unit,
) {
        // 读取字幕偏好设置
        val subtitlePreference = readSubtitlePreference(videoPath)

        // 自动探测外部字幕
        val subtitleFiles = findSubtitleFiles(videoPath)
        extSubList.clear()
        extSubList.addAll(subtitleFiles)

        val applicationDir = getSettingsDirectory()
        var subPath = ""
        var list: List<PlayerCaption>

       // 根据偏好设置加载字幕
        when {
            // 如果从查看语境功能进来，优先使用语境字幕轨道
            state.showContextTrackId != 0 -> {
                list = readCaptionList(videoPath = videoPath, subtitleId = state.showContextTrackId)
                timedCaption.setCaptionList(list)
                updateCurrentSubtitleTrack(state.showContextTrackId)
                if(list.isNotEmpty()) {
                    subPath = "$applicationDir/VideoPlayer/subtitle.srt"
                }
            }
            // 有字幕偏好设置，按偏好加载
            subtitlePreference != null -> {
                updateSubtitleDescription(subtitlePreference.description)

                when (subtitlePreference.subtitleType) {
                    "internal" -> {
                        // 加载内部字幕
                        list = readCachedSubtitle(videoPath, subtitlePreference.trackId)
                        if (list.isEmpty()) {
                            list = readCaptionList(videoPath = videoPath, subtitleId = subtitlePreference.trackId)
                        }
                        timedCaption.setCaptionList(list)
                        updateCurrentSubtitleTrack(subtitlePreference.trackId)
                        if(list.isNotEmpty()) {
                            subPath = "$applicationDir/VideoPlayer/subtitle.srt"
                        }
                    }
                    "external" -> {
                        // 加载外部字幕
                        val externalFile = File(subtitlePreference.subtitlePath)
                        if (externalFile.exists()) {
                            // 在外部字幕列表中找到对应的索引
                            var foundIndex = -1
                            for ((idx, value) in subtitleFiles.withIndex()) {
                                val (_, file) = value
                                if (file.absolutePath == externalFile.absolutePath) {
                                    foundIndex = idx
                                    break
                                }
                            }

                            if (externalFile.extension == "ass") {
                                loadAndConvertAssSubtitle(
                                    file = externalFile,
                                    onSuccess = { captions ->
                                        timedCaption.setCaptionList(captions)
                                    },
                                    onFailure = { errorMessage ->
                                        state.showNotification(errorMessage)
                                    }
                                )
                            } else {
                                val captions = parseSubtitles(externalFile.absolutePath)
                                timedCaption.setCaptionList(captions)
                            }
                            updateExtSubIndex(foundIndex)
                            updateCurrentSubtitleTrack(-2)
                            subPath = externalFile.absolutePath
                        }
                    }
                    "disabled" -> {
                        // 禁用字幕
                        timedCaption.clear()
                        updateCurrentSubtitleTrack(-1)
                        updateSubtitleDescription("")
                    }
                }

            }

            // 没有偏好设置，使用默认逻辑
            else -> {
                list = readCaptionList(videoPath = videoPath, subtitleId = 0)
                timedCaption.setCaptionList(list)
                if(list.isNotEmpty()) {

                    // 字幕轨道列表可能没有及时更新，异步等待并重试
                    CoroutineScope(Dispatchers.Main).launch {
                        while (isActive) {
                            // 先执行一次尝试获取语言标签
                            var foundLabel = false
                            subtitleTrackList.forEach { (id, description) ->
                                if (id == 0) {
                                    updateSubtitleDescription(getLanguageLabel(description))
                                    foundLabel = true
                                    return@forEach
                                }
                            }

                            // 如果找到了语言标签，跳出循环
                            if (foundLabel) {
                                return@launch
                            }

                            // 如果没有找到，等待5秒后重试
                            delay(5000)
                        }
                    }


                    updateCurrentSubtitleTrack(0)
                    subPath = "$applicationDir/VideoPlayer/subtitle.srt"
                }

                // 如果没有内置字幕，尝试加载英语外部字幕
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
                            updateSubtitleDescription(removeSuffix(lang))
                            break
                        }
                    }

                    if (foundFile != null) {
                        val captions = parseSubtitles(foundFile.absolutePath)
                        timedCaption.setCaptionList(captions)
                        updateExtSubIndex(foundIndex)
                        updateCurrentSubtitleTrack(-2)
                        subPath = foundFile.absolutePath
                    }
                }

            }
        }

        generateMatchedVocabulary(
            videoPath = videoPath,
            subPath = subPath,
            trackId = currentSubtitleTrack,
            state = state,
        )

        // 更新字幕索引
        updateCaptionIndex()
}

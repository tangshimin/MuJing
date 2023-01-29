package player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
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
import state.WordState
import state.getSettingsDirectory
import ui.dialog.MessageDialog
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.io.File
import javax.swing.Timer
import kotlin.time.Duration.Companion.milliseconds

/**
 * 视频播放器，可以显示单词弹幕
 * 等 Jetbrains 修复了 https://github.com/JetBrains/compose-jb/issues/1800，要执行一次重构。
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun Player(
    close:()->Unit,
    videoPath:String,
    wordState: WordState
){
    val windowState = rememberWindowState(
        size = DpSize(1289.dp,854.dp),
        placement = WindowPlacement.Floating,
        position = WindowPosition(Alignment.Center)
    )

    val playerState by rememberPlayerState()
    val title  by remember { mutableStateOf(File(videoPath).name) }
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }
    var timeProgress by remember { mutableStateOf(0f) }

    var volumeProgress by remember { mutableStateOf(100f) }
    var timeText by remember { mutableStateOf("") }
    val videoDuration by remember(videoPath){
        videoPlayerComponent.mediaPlayer().media().prepare(videoPath)
        derivedStateOf{videoPlayerComponent.mediaPlayer().media().info().duration()}
    }
    var playerWindow by remember { mutableStateOf<ComposeWindow?>(null) }
    var controlWindow by remember { mutableStateOf<ComposeWindow?>(null) }
    /** 用户输入的弹幕编号 */
    var danmakuNum by remember{mutableStateOf("")}
    /** 弹幕计数器，用于快速定位弹幕 */
    var counter by remember { mutableStateOf(0) }
    val showingDanmaku = remember{ mutableStateMapOf<Int,DanmakuItem>()}
    val shouldAddDanmaku = remember{ mutableStateMapOf<Int,DanmakuItem>()}
    val danmakuMap = rememberDanmakuMap(videoPath,wordState.vocabulary)
    var showMessageDialog by remember{mutableStateOf(false)}
    var message by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    /** 手动触发的暂停，比如空格键，双击视频触发的暂停。区别于鼠标移动到弹幕触发的自动暂停。*/
    var isManualPause by remember { mutableStateOf(false) }
    /** 播放器控制区的可见性 */
    var controlBoxVisible by remember { mutableStateOf(false) }
    /** 展开设置菜单 */
    var settingsExpanded by remember{ mutableStateOf(false) }

    /** 弹幕从右到左需要的时间，单位为毫秒 */
    var widthDuration by remember{ mutableStateOf(windowState.size.width.value.div(3).times(30).toInt()) }

    val removedList = remember{ mutableStateListOf<DanmakuItem>()}
    /** 使弹幕从右往左移动的定时器 */
    val danmakuTimer by remember{ mutableStateOf(
        Timer(30) {
            val danmakuList = showingDanmaku.values.toList()
            for(i in danmakuList.indices){
                val danmakuItem = danmakuList.getOrNull(i)
                if((danmakuItem != null) && !danmakuItem.isPause){
                        if(danmakuItem.position.x > -30){
                            danmakuItem.position = danmakuItem.position.copy(x = danmakuItem.position.x -3)
                        }else{
                            danmakuItem.show = false
                            removedList.add(danmakuItem)
                        }
                }
            }
            removedList.forEach { danmakuItem ->
                showingDanmaku.remove(danmakuItem.sequence)
            }
            showingDanmaku.putAll(shouldAddDanmaku)
            shouldAddDanmaku.clear()
        }
    ) }

    val closeWindow: () -> Unit = {
        videoPlayerComponent.mediaPlayer().release()
        close()
    }

    val play: () -> Unit = {
        if(isPlaying){
            danmakuTimer.stop()
            isPlaying = false
            videoPlayerComponent.mediaPlayer().controls().pause()
        }else {
            danmakuTimer.restart()
            isPlaying = true
            videoPlayerComponent.mediaPlayer().controls().play()
        }
    }
    val manualPause:() -> Unit = {
        isManualPause = !isManualPause
    }

    val cleanDanmaku:() -> Unit = {
        showingDanmaku.clear()
        shouldAddDanmaku.clear()
    }


    Window(
        title = title,
        icon = painterResource("logo/logo.png"),
        state = windowState,
        undecorated = true,
        alwaysOnTop = true,
        onCloseRequest = { closeWindow() },
    ) {
        playerWindow = window
        Column (Modifier.fillMaxSize()){
            Box(Modifier.fillMaxWidth().height(40.dp)
                .background(if(MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
            )
            Box(Modifier.fillMaxSize()){
                val videoSize by remember(windowState.size){
                    derivedStateOf { Dimension(window.size.width,window.size.height - 40) }
                }
                videoPlayerComponent.size = videoSize
                SwingPanel(
                    background = Color.Transparent,
                    modifier = Modifier.fillMaxSize(),
                    factory = { videoPlayerComponent },
                    update = {}
                )

                LaunchedEffect(Unit){
                    window.addWindowListener(object:WindowListener{
                        override fun windowActivated(e: WindowEvent?) {
                            controlWindow?.requestFocus()
                        }
                        override fun windowOpened(e: WindowEvent?) {}
                        override fun windowClosing(e: WindowEvent?) {}
                        override fun windowClosed(e: WindowEvent?) {}
                        override fun windowIconified(e: WindowEvent?) {}
                        override fun windowDeiconified(e: WindowEvent?) {}
                        override fun windowDeactivated(e: WindowEvent?) {}
                    })
                }

            }
        }
    }

    Window(
        onCloseRequest = { closeWindow() },
        title = title,
        transparent = true,
        undecorated = true,
        alwaysOnTop = true,
        state = windowState,
        icon = painterResource("logo/logo.png"),
        onKeyEvent = {keyEvent ->
            if(keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp){
                play()
                manualPause()
                true
            }else if(keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyUp){
                videoPlayerComponent.mediaPlayer().controls().skipTime(+5000L)
                cleanDanmaku()
                true
            }else if(keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp){
                videoPlayerComponent.mediaPlayer().controls().skipTime(-5000L)
                cleanDanmaku()
                true
            }else false

        }
    ) {
        controlWindow = window
        Surface(
            color =Color.Transparent,
            modifier = Modifier.fillMaxSize()
                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                .onPointerEvent(PointerEventType.Enter){
                    if(!controlBoxVisible) {
                        controlBoxVisible = true
                    }
                }
                .onPointerEvent(PointerEventType.Exit){
                    if(isPlaying && !settingsExpanded){
                        controlBoxVisible = false
                    }
                }


        ) {

            WindowDraggableArea (
                Modifier
                    .combinedClickable(
                        interactionSource = remember(::MutableInteractionSource),
                        indication = null,
                onDoubleClick = {
                    play()
                    manualPause()
                                },
                onClick = {},
                onLongClick = {}
            )
            ){
                Column {
                    Box(Modifier.fillMaxWidth()
                        .height(40.dp)
                        .background(if(MaterialTheme.colors.isLight) Color.White else Color(48, 50, 52))
                    ){
                        Text(title, modifier = Modifier.align(Alignment.Center),color = MaterialTheme.colors.onBackground)
                        Row(Modifier.align(Alignment.CenterEnd)){
                            IconButton(onClick = {
                                windowState.isMinimized = true
                            },modifier = Modifier.size(40.dp)){
                                Icon(
                                    Icons.Filled.Remove,
                                    contentDescription = "Localized description",
                                    tint = Color(140, 140, 140),
                                )
                            }
                            IconButton(onClick = { closeWindow()},
                                modifier = Modifier.size(40.dp)
                            ){
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Localized description",
                                    tint = Color(140, 140, 140),
                                )
                            }

                        }

                    }

                    Box(Modifier.fillMaxSize()){
                        DanmakuBox(wordState,playerState,showingDanmaku,isManualPause,play,windowState.size.height.value.toInt())
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom =5.dp )
                        ){
                            if(controlBoxVisible){
                                // 进度条
                                var sliderVisible by remember{ mutableStateOf(false) }
                                Box(
                                    Modifier
                                        .fillMaxWidth().padding(start = 5.dp,end = 5.dp,bottom = 10.dp).offset(x = 0.dp,y = 20.dp)
                                        .onPointerEvent(PointerEventType.Enter) { sliderVisible = true }
                                        .onPointerEvent(PointerEventType.Exit) { sliderVisible = false }
                                ) {
                                    val animatedPosition by animateFloatAsState(
                                        targetValue = timeProgress,
                                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                                    )
                                    if(sliderVisible){
                                        Slider(
                                            value = timeProgress,
                                            modifier = Modifier.align(Alignment.Center)
                                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                                            onValueChange = {
                                                timeProgress = it
                                                cleanDanmaku()
                                                videoPlayerComponent.mediaPlayer().controls().setPosition(timeProgress)
                                            })
                                    }else{
                                        LinearProgressIndicator(progress = animatedPosition,
                                            modifier = Modifier.align(Alignment.Center).fillMaxWidth().offset(x=0.dp,y= (-20).dp).padding(top = 20.dp))
                                    }
                                }
                                // 暂停、音量、时间、弹幕、设置
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.width(600.dp)){
                                    IconButton(onClick = {
                                        play()
                                        manualPause()
                                    }){
                                        Icon(
                                            if(isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Localized description",
                                            tint = Color.White,
                                        )
                                    }
                                    var volumeOff by remember{ mutableStateOf(false) }
                                    var volumeSliderVisible by remember{ mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .onPointerEvent(PointerEventType.Enter){volumeSliderVisible = true}
                                            .onPointerEvent(PointerEventType.Exit){volumeSliderVisible = false}
                                    ){
                                        IconButton(onClick = {
                                            volumeOff = !volumeOff
                                            videoPlayerComponent.mediaPlayer().audio().isMute = volumeOff
                                        }){
                                            Icon(
                                                if(volumeOff) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        if(volumeSliderVisible){
                                            Slider(
                                                value = volumeProgress,
                                                valueRange = 1f..100f,
                                                onValueChange = {
                                                    volumeProgress = it
                                                    videoPlayerComponent.mediaPlayer().audio().setVolume(volumeProgress.toInt())
                                                },
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .onPointerEvent(PointerEventType.Enter){volumeSliderVisible = true}
                                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            )
                                        }
                                    }


                                    Text(" $timeText ",color = Color.White)
                                    Box{

                                        IconButton(onClick = {settingsExpanded = true }){
                                            Icon(
                                                Icons.Filled.Settings,
                                                contentDescription = "Localized description",
                                                tint = Color.White,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = settingsExpanded,
                                            offset = DpOffset(x = 0.dp,y = (-20).dp),
                                            onDismissRequest = {
                                                settingsExpanded = false
                                                controlBoxVisible = true},
                                            modifier = Modifier
                                                .onPointerEvent(PointerEventType.Enter){
                                                   controlBoxVisible = true
                                                }
                                                .onPointerEvent(PointerEventType.Exit) {
                                                    controlBoxVisible = true
                                                }
                                        ) {

                                            DropdownMenuItem(onClick = {  }) {

                                                Row(verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()){
                                                    Text("快速定位弹幕")
                                                    Switch(checked = playerState.showSequence, onCheckedChange = {
                                                        playerState.showSequence = it
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                            DropdownMenuItem(onClick = {  }) {
                                                Row(verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()){
                                                    Text("弹幕")
                                                    Switch(checked = playerState.danmakuVisible, onCheckedChange = {
                                                        if(playerState.danmakuVisible){
                                                            playerState.danmakuVisible = false
                                                            shouldAddDanmaku.clear()
                                                            showingDanmaku.clear()
                                                            danmakuTimer.stop()
                                                        }else{
                                                            playerState.danmakuVisible = true
                                                            danmakuTimer.restart()
                                                        }
                                                        playerState.savePlayerState()
                                                    })
                                                }
                                            }
                                        }

                                    }
                                    if(playerState.showSequence && playerState.danmakuVisible){

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)))){
                                            fun searchNum(){
                                                if(danmakuNum.isNotEmpty()){
                                                    val num = danmakuNum.toIntOrNull()
                                                    if(num != null){
                                                        val danmakuItem =  showingDanmaku.get(num)
                                                        if(danmakuItem != null){
                                                            danmakuItem.isPause = true
                                                            if(!isManualPause){
                                                                play()
                                                            }
                                                        }
                                                    }

                                                }
                                            }
                                            Box(modifier = Modifier.width(110.dp).padding(start = 5.dp)){
                                                BasicTextField(
                                                    value = danmakuNum,
                                                    singleLine = true,
                                                    onValueChange = {danmakuNum = it},
                                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                                    textStyle = MaterialTheme.typography.h5.copy(
                                                        color = Color.White,
                                                    ),
                                                    modifier = Modifier.onKeyEvent { keyEvent ->
                                                        if((keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && keyEvent.type == KeyEventType.KeyUp){
                                                            searchNum()
                                                            true
                                                        }else false
                                                    }
                                                )
                                                if(danmakuNum.isEmpty()){
                                                    Text("输入弹幕编号",color = Color.White)
                                                }
                                            }
                                            IconButton(onClick = { searchNum() },modifier = Modifier.size(40.dp,40.dp)){
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

                        MessageDialog(
                            show = showMessageDialog,
                            close = {showMessageDialog = false},
                            message = message
                        )
                        LaunchedEffect(Unit){
                            danmakuTimer.start()
                        }
                    }
                }
            }

        }

        // 设置播放器、播放视频
        LaunchedEffect(Unit){
            var lastTime = -1
            var lastMaxLength = 0
            videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object :MediaPlayerEventAdapter(){
                override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {

                    timeProgress = (newTime.toFloat()).div(videoDuration)
                    var startText: String
                    timeProgress.times(videoDuration).toInt().milliseconds.toComponents { hours, minutes, seconds, _ ->
                        startText =  timeFormat(hours, minutes, seconds)
                    }
                    videoDuration.milliseconds.toComponents { hours, minutes, seconds, _ ->
                        val durationText = timeFormat(hours, minutes, seconds)
                        timeText = "$startText / $durationText"
                    }

                    // 单位为秒
                    val startTime = (newTime.milliseconds.inWholeSeconds + widthDuration.div(3000)).toInt()
                    // 每秒执行一次
                    if(playerState.danmakuVisible && startTime != lastTime){
                        val danmakuList = danmakuMap.get(startTime)
                        var offsetY = 20
                        val sequenceWidth = if(playerState.showSequence) counter.toString().length * 12 else 0
                        val offsetX = sequenceWidth + lastMaxLength * 12 + 30
                        var maxLength = 0
                        danmakuList?.forEach { danmakuItem ->
                            if(offsetY > 395) offsetY = 10
                            danmakuItem.position = IntOffset(window.size.width+offsetX,offsetY)
                            offsetY += 35
                            if(danmakuItem.content.length > maxLength) {
                                maxLength =  danmakuItem.content.length
                            }

                            // todo 这里还是有多线程问题，可能会出现：这里刚刚添加，在另一个控制动画的 Timer 里面马上就删除了。
                            danmakuItem.sequence = counter
                            shouldAddDanmaku.put(counter,danmakuItem)
                            counter++
                        }
                        lastMaxLength = maxLength
                        lastTime = startTime
                    }

                }

                private fun timeFormat(hours: Long, minutes: Int, seconds: Int): String {
                    val h = if (hours < 10) "0$hours" else "$hours"
                    val m = if (minutes < 10) "0$minutes" else "$minutes"
                    val s = if (seconds < 10) "0$seconds" else "$seconds"
                    return "$h:$m:$s"
                }

                override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                    mediaPlayer?.audio()?.setVolume(volumeProgress.toInt())
                }

                override fun finished(mediaPlayer: MediaPlayer?) {
                    isPlaying = false
                }
            })
            videoPlayerComponent.mediaPlayer().media()
                .play(videoPath)
            isPlaying = true
        }

        // 同步窗口尺寸
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.size }
                .onEach{
                    videoPlayerComponent.size = Dimension(windowState.size.width.value.toInt(),windowState.size.height.value.toInt() - 40)
                    widthDuration = windowState.size.width.value.div(3).times(30).toInt()
                }
                .launchIn(this)
            snapshotFlow { windowState.placement }
                .onEach{
                    if(windowState.placement == WindowPlacement.Maximized){
                        playerWindow!!.placement = WindowPlacement.Floating
                        controlWindow!!.placement = WindowPlacement.Floating
                        showMessageDialog = true
                        message = "暂时不支持通过快捷键最大化窗口，\n但是可以手动调整窗口到最大。"
                        controlWindow?.requestFocus()
                    }else if(windowState.placement == WindowPlacement.Fullscreen){
                        playerWindow!!.placement = WindowPlacement.Floating
                        controlWindow!!.placement = WindowPlacement.Floating
                        showMessageDialog = true
                        message = "暂时不支持全屏"
                        controlWindow?.requestFocus()
                    }
                }
                .launchIn(this)
        }
    }
}

@Composable
fun DanmakuBox(
    wordState: WordState,
    playerState: PlayerState,
    showingDanmaku: SnapshotStateMap<Int, DanmakuItem>,
    isManualPause:Boolean,
    play:()-> Unit,
    windowHeight:Int
){
    /** 如果手动触发了暂停，就不处理播放函数 */
    val playEvent:()-> Unit = {
        if(!isManualPause){
            play()
        }
    }

    /** 删除单词 */
    val deleteWord:(DanmakuItem) -> Unit = { danmakuItem ->
        if(danmakuItem.word != null){
            val word = danmakuItem.word
            wordState.vocabulary.wordList.remove(word)
            wordState.vocabulary.size = wordState.vocabulary.wordList.size
            wordState.saveCurrentVocabulary()
        }
        showingDanmaku.remove(danmakuItem.sequence)
        playEvent()
    }

    /** 把单词加入到熟悉词库 */
    val addToFamiliar:(DanmakuItem) -> Unit = { danmakuItem ->
        val word = danmakuItem.word
        if(word != null){

            val file = getFamiliarVocabularyFile()
            val familiar =  loadVocabulary(file.absolutePath)
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (wordState.vocabulary.type == VocabularyType.MKV ||
                wordState.vocabulary.type == VocabularyType.SUBTITLES
            ) {
                word.captions.forEach{caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = wordState.vocabulary.relateVideoPath,
                        subtitlesTrackId = wordState.vocabulary.subtitlesTrackId,
                        subtitlesName = wordState.vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    word.externalCaptions.add(externalCaption)
                }
                word.captions.clear()

            }
            if(!familiar.wordList.contains(word)){
                familiar.wordList.add(word)
                familiar.size = familiar.wordList.size
            }
            saveVocabulary(familiar,file.absolutePath)
            deleteWord(danmakuItem)
        }

    }

    /** 等宽字体*/
    val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }

    // 在这个 Box 使用 Modifier.fillMaxSize() 可能会导致 DropdownMenu 显示的位置不准。
    Box{
        showingDanmaku.forEach { (_, danmakuItem) ->
            Danmaku(
                playerState,
                danmakuItem,
                playEvent,
                monospace,
                windowHeight,
                deleteWord = {deleteWord(it)},
                addToFamiliar = {addToFamiliar(it)}
            )
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberPlayerState() = remember{
    val playerSettings = getPlayerSettingsFile()
    if(playerSettings.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys }
            val playerData = decodeFormat.decodeFromString<PlayerData>(playerSettings.readText())
            mutableStateOf(PlayerState(playerData))
        }catch (exception:Exception){
            println("解析视频播放器的设置失败，将使用默认值")
            val playerState = PlayerState(PlayerData())
            mutableStateOf(playerState)
        }
    }else{
        val playerState = PlayerState(PlayerData())
        mutableStateOf(playerState)
    }
}
@ExperimentalSerializationApi
@Serializable
data class PlayerData(
    var showSequence: Boolean = false,
    var danmakuVisible: Boolean = false,
    var autoCopy:Boolean = false,
    var autoSpeak:Boolean = true,
    var preferredChinese:Boolean = true
)
@OptIn(ExperimentalSerializationApi::class)
class PlayerState(playerData: PlayerData){
    var showSequence by mutableStateOf(playerData.showSequence)
    var danmakuVisible by mutableStateOf(playerData.danmakuVisible)
    var autoCopy by mutableStateOf(playerData.autoCopy)
    var autoSpeak by mutableStateOf(playerData.autoSpeak)
    var preferredChinese by mutableStateOf(playerData.preferredChinese)

    fun savePlayerState(){
        val encodeBuilder = Json{
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

private fun getPlayerSettingsFile():File{
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "PlayerSettings.json")
}
package player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import data.Caption
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimplePlayer(
    close: () -> Unit,
    videoPlayerBounds: DialogState,
    volume: Float,
    playTriple: Triple<Caption, String, Int>,
    externalSubtitlesVisible:Boolean = false,
    resetVideoBounds :() ->Unit,
    isVideoBoundsChanged:Boolean = false,
    setIsVideoBoundsChanged:(Boolean) -> Unit = {}
){

    /** 窗口的大小和位置 */
    val windowState = rememberWindowState(
        size = videoPlayerBounds.size,
        position = WindowPosition(Alignment.Center)
    )

    /** 显示视频的窗口 */
    var playerWindow by remember { mutableStateOf<ComposeDialog?>(null) }

    /** VLC 是视频播放组件 */
    val videoPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }

    /** 控制视频显示的窗口，弹幕显示到这个窗口 */
    val controlWindow by remember { mutableStateOf<ComposeDialog?>(null) }
    
    /** 是否全屏，如果使用系统的全屏，播放器窗口会黑屏 */
    var isFullscreen by remember { mutableStateOf(false) }

    /** 全屏之前的位置 */
    var fullscreenBeforePosition by remember { mutableStateOf(WindowPosition(0.dp,0.dp)) }

    /** 全屏之前的尺寸 */
    var fullscreenBeforeSize by remember{ mutableStateOf(videoPlayerBounds.size) }

    /** 是否正在播放视频 */
    var isPlaying by remember { mutableStateOf(false) }

    /** 关闭窗口 */
    val closeWindow: () -> Unit = {
        if(videoPlayerComponent.mediaPlayer().status().isPlaying){
            videoPlayerComponent.mediaPlayer().controls().pause()
        }
        close()
    }

    /** 播放 */
    val play: () -> Unit = {
        if (isPlaying) {
            isPlaying = false
            videoPlayerComponent.mediaPlayer().controls().pause()
        } else {
            isPlaying = true
            videoPlayerComponent.mediaPlayer().controls().play()
        }
    }

    Window(
        title = "播放视频",
        state = windowState,
        icon = painterResource("logo/logo.png"),
        undecorated = true,
        transparent = true,
        resizable = true,
        onCloseRequest = {closeWindow()},
    ){
        /** 全屏 */
        val fullscreen:()-> Unit = {
            if(isFullscreen){
                isFullscreen = false
                videoPlayerBounds.position =  fullscreenBeforePosition
                videoPlayerBounds.size = fullscreenBeforeSize
                controlWindow?.isResizable = true
                playerWindow?.requestFocus()
            }else{
                isFullscreen = true
                fullscreenBeforePosition = WindowPosition(videoPlayerBounds.position.x,videoPlayerBounds.position.y)
                fullscreenBeforeSize =  videoPlayerBounds.size
                videoPlayerBounds.position = WindowPosition((-1).dp, 0.dp)
                val windowSize = Toolkit.getDefaultToolkit().screenSize.size.toComposeSize()
                videoPlayerBounds.size = windowSize.copy(width = windowSize.width + 1.dp)
                controlWindow?.isResizable = false
                playerWindow?.requestFocus()
            }
        }


        VideoLayer(
            windowState = videoPlayerBounds,
            videoPlayerComponent = videoPlayerComponent,
            setPlayerWindow = { playerWindow = it }
        )

        // control layer
        Dialog(
            onCloseRequest = {  },
            transparent = true,
            undecorated = true,
            state = videoPlayerBounds,
            icon = painterResource("logo/logo.png"),
            onPreviewKeyEvent ={ keyEvent ->
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) {
                    play()
                    true
                } else if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown) {
                    if(isFullscreen){
                        fullscreen()
                        true
                    }else false
                }  else if (keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(+5000L)
                    true
                } else if (keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyUp) {
                    videoPlayerComponent.mediaPlayer().controls().skipTime(-5000L)
                    true
                } else false

            }
        ) {
            WindowDraggableArea {
                Box (Modifier.fillMaxSize()){
                    Row(Modifier.align(Alignment.BottomCenter)){
                        if(isVideoBoundsChanged){
                            IconButton(onClick = {resetVideoBounds()}){
                                Icon(
                                    Icons.Filled.FlipToBack,
                                    contentDescription = "Localized description",
                                    tint = Color.White,
                                )
                            }
                        }

                        IconButton(onClick = {play()}){
                            Icon(
                                if(isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Localized description",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = {closeWindow()}){
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Localized description",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            LaunchedEffect(Unit){

                val mediaPlayerEventListener = object: MediaPlayerEventAdapter(){
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                        println("Ready!")
                        mediaPlayer.audio().setVolume((volume).toInt())
                    }
                    override fun finished(mediaPlayer: MediaPlayer) {
                        closeWindow()
                        videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
                    }
                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(mediaPlayerEventListener)

                val caption = playTriple.first
                val relativeVideoPath = playTriple.second
                val trackId = playTriple.third
                val start = parseTime(caption.start)
                val end = parseTime(caption.end)
                // 使用内部字幕轨道,通常是从 MKV 生成的词库
                if(trackId != -1){
                    videoPlayerComponent.mediaPlayer().media()
                        .play(relativeVideoPath, ":sub-track=$trackId", ":start-time=$start", ":stop-time=$end")
                    // 自动加载外部字幕
                }else if(externalSubtitlesVisible){
                    videoPlayerComponent.mediaPlayer().media()
                        .play(relativeVideoPath, ":sub-autodetect-file",":start-time=$start", ":stop-time=$end")
                }else{
                    // 视频有硬字幕，加载了外部字幕会发生重叠。
                    videoPlayerComponent.mediaPlayer().media()
                        .play(relativeVideoPath, ":no-sub-autodetect-file",":start-time=$start", ":stop-time=$end")
                }
                isPlaying = true
            }

        }

        DisposableEffect(Unit){
            onDispose {
                videoPlayerComponent.mediaPlayer().release()
            }
        }

        LaunchedEffect(videoPlayerBounds) {
            snapshotFlow { videoPlayerBounds.size }
                .onEach{
                   windowState.size = videoPlayerBounds.size
                    setIsVideoBoundsChanged(true)
                }
                .launchIn(this)

            snapshotFlow { videoPlayerBounds.position }
                .onEach {
                   windowState.position = videoPlayerBounds.position
                    setIsVideoBoundsChanged(true)
                }
                .launchIn(this)
        }
    }
}

@Composable
private fun VideoLayer(
    windowState: DialogState,
    videoPlayerComponent: Component,
    setPlayerWindow:(ComposeDialog) -> Unit
) {
    Dialog(
        icon = painterResource("logo/logo.png"),
        state = windowState,
        undecorated = true,
        resizable = false,
        onCloseRequest = {  },
    ) {
        setPlayerWindow(window)
        Column(Modifier.fillMaxSize()) {
            Divider(color = Color(0xFF121212), modifier = Modifier.height(1.dp))
            Box(Modifier.fillMaxSize()) {
                val videoSize by remember(windowState.size) {
                    derivedStateOf { Dimension(window.size.width, window.size.height) }
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
}
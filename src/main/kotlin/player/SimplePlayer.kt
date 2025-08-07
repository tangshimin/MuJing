package player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File

/**
 * 执行简单的视频播放，用在搜索框和链接词库界面中,
 * 包含播放按钮和视频播放器和错误提示对话框。
 * @param mediaInfo 视频信息
 * @param vocabularyDir 词库目录
 * @param volume 音量，范围 0.0f 到 1.0
 */
@Composable
fun PlayerBox(
    mediaInfo : MediaInfo,
    vocabularyDir:File,
    volume : Float
){
    Box{
        var visible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        IconButton(
            onClick = {
                val resolvedPath =  resolveMediaPath(
                    mediaInfo.mediaPath,
                    vocabularyDir
                )
                if(resolvedPath != ""){
                    mediaInfo.mediaPath = resolvedPath
                    visible = true
                }else{
                    errorMessage = if(mediaInfo.mediaPath.isEmpty())"视频地址为空" else "视频地址错误:\n${mediaInfo.mediaPath}"
                }

            },
            modifier = Modifier
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Localized description",
                tint = MaterialTheme.colors.primary
            )
        }

        PopupVideoPlayer(
            visible = visible,
            setVisible = { visible = it },
            volume = volume,
            mediaInfo = mediaInfo,
            offset = DpOffset(0.dp, 0.dp)
        )

        if(errorMessage.isNotEmpty()){
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                title = { Text("错误",color = MaterialTheme.colors.error) },
                text = { SelectionContainer { Text(errorMessage)  } },
                confirmButton = {
                    OutlinedButton(onClick = { errorMessage = "" }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Composable
fun PopupVideoPlayer(
    visible:Boolean,
    setVisible:(Boolean) -> Unit = {},
    mediaInfo: MediaInfo,
    volume: Float = 1.0f,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    extSubVisible: Boolean = false,
){
    DropdownMenu(
        expanded = visible,
        offset = offset,
        onDismissRequest = { setVisible(false) },
    ) {
        println("播放视频")
        Box(Modifier.size(540.dp,330.dp)){
            val videoPlayerComponent  = remember { createMediaPlayerComponent2() }
            val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
            val surface = remember {
                SkiaImageVideoSurface().also {
                    videoPlayer.videoSurface().set(it)
                }
            }
            var isPlaying by remember { mutableStateOf(false) }
            val stop = {
                if (videoPlayer.status().isPlaying) {
                    videoPlayer.controls().stop()
                }
                setVisible(false)

            }
            val play = {
                if (videoPlayer.status().isPlaying) {
                    videoPlayer.controls().pause()
                    isPlaying = false
                } else {
                    videoPlayer.controls().play()
                    isPlaying = true
                }
            }
            CustomCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .align(Alignment.Center),
                surface = surface
            )
            // 控制区
            Row(modifier = Modifier.align (Alignment.BottomCenter)) {
                // 这里可以添加控制按钮，比如播放、暂停、停止等
                IconButton(onClick = play){
                    Icon(imageVector = if(isPlaying) Icons.Default.Pause else  Icons.Default.PlayArrow,
                        contentDescription = if(isPlaying) "Pause" else  "Play",
                        tint = Color.White)
                }
                IconButton(onClick = stop){
                    Icon(imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White)
                }
            }


            DisposableEffect(Unit){

                val eventListener = object:MediaPlayerEventAdapter(){
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                        videoPlayerComponent.requestFocusInWindow()
                        mediaPlayer.audio().setVolume((volume).toInt())
                    }
                    override fun finished(mediaPlayer: MediaPlayer) {
                        setVisible(false)
                    }
                }
                videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                val start = convertTimeToSeconds(mediaInfo.caption.start)
                val end = convertTimeToSeconds(mediaInfo.caption.end)
                // 使用内部字幕轨道,通常是从 MKV 生成的词库
                if(mediaInfo.trackId != -1){
                    videoPlayerComponent.mediaPlayer().media()
                        .play(mediaInfo.mediaPath, ":sub-track=$mediaInfo.trackId", ":start-time=$start", ":stop-time=$end")
                    // 自动加载外部字幕
                }else if(extSubVisible){
                    videoPlayerComponent.mediaPlayer().media()
                        .play(mediaInfo.mediaPath, ":sub-autodetect-file",":start-time=$start", ":stop-time=$end")
                }else{
                    // 视频有硬字幕，加载了外部字幕会发生重叠。
                    videoPlayerComponent.mediaPlayer().media()
                        .play(mediaInfo.mediaPath, ":no-sub-autodetect-file",":start-time=$start", ":stop-time=$end")
                }

                isPlaying = true

                onDispose {
                    if(videoPlayer.status().isPlaying) {
                        videoPlayer.controls().stop()
                    }
                    // 释放资源
                    surface.release()
                    videoPlayerComponent.release()
                }
            }
        }
    }

}


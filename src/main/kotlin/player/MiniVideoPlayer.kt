package player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import data.Caption
import kotlinx.coroutines.*
import ui.wordscreen.replaceSeparator
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Component
import java.io.File

/**
 * MiniVideoPlayer 是一个用于在 Compose UI 中显示视频播放器的组件。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MiniVideoPlayer(
    modifier: Modifier,
    size: DpSize,
    isPlaying : Boolean,
    stop: () -> Unit,
    volume: Float,
    mediaInfo: MediaInfo?,
    externalSubtitlesVisible:Boolean = false,
    timeChanged:(Long) -> Unit = {},
    isInPiPMode:Boolean = false
) {

    if(isPlaying && mediaInfo != null) {
        /** VLC 视频播放组件 */
        val videoPlayerComponent  = remember { createMediaPlayerComponent() }
        val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
        val surface = remember {
            SkiaImageVideoSurface().also {
                videoPlayer.videoSurface().set(it)
            }
        }
        val focusRequester = remember { FocusRequester() }
        var isPaused by remember { mutableStateOf(false) }
        var currentTime by remember{mutableStateOf("00:00:00")}
        var videoDuration by remember{mutableStateOf("00:00:00")}
        /** 展开设置菜单 */
        var settingsExpanded by remember { mutableStateOf(false) }

        // 事件监听器
        val eventListener = object : MediaPlayerEventAdapter() {

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                // 更新当前时间
                currentTime = String.format(
                    "%02d:%02d:%02d",
                    (newTime / 3600000).toInt(),
                    (newTime / 60000 % 60).toInt(),
                    (newTime / 1000 % 60).toInt()
                )
            }

            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                videoPlayer.audio().setVolume((volume).toInt())
                val duration = videoPlayer.media().info().duration()
                videoDuration = String.format(
                    "%02d:%02d:%02d",
                    (duration / 3600000).toInt(),
                    (duration / 60000 % 60).toInt(),
                    (duration / 1000 % 60).toInt()
                )
            }

            /**
             * stopped 事件触发时机：
             * 1. 用户手动调用 stop() 方法
             * 2. 播放器被强制停止（如资源不足）
             * 3. 应用程序请求停止播放
             *
             * 特点：
             * - 这是一个"主动停止"事件
             * - 播放器状态被重置到初始状态
             * - 通常是预期的行为，不表示错误
             * - 通常表示用户已经播放完/听完了内容
             *
             */
            override fun stopped(mediaPlayer: MediaPlayer?) {
                videoPlayer.events().removeMediaPlayerEventListener(this)
                // 切换到主线程更新状态
                CoroutineScope(Dispatchers.Main).launch {
                    stop()
                }
            }

            /**
             * finished 事件触发时机：
             * 1. 媒体播放到达文件末尾
             * 2. 播放列表中的最后一个项目播放完成
             * 3. 流媒体播放结束
             *
             * 特点：
             * - 这是一个"自然结束"事件
             * - 表示内容已完整播放
             * - 播放器可能会自动进入停止状态
             * - 通常表示用户已经播放完/听完了内容
             *
             */
            override fun finished(mediaPlayer: MediaPlayer?) {
                videoPlayer.events().removeMediaPlayerEventListener(this)
                // 切换到主线程更新状态
                CoroutineScope(Dispatchers.Main).launch {
                    stop()
                }
            }

            /**
             * error 事件触发时机：
             * 1. 媒体文件损坏或格式不支持
             * 2. 网络流播放时网络中断或连接失败
             * 3. 解码器无法处理媒体内容
             * 4. 磁盘空间不足导致播放失败
             * 5. 权限问题无法访问媒体文件
             * 6. VLC 内部错误或资源不足
             *
             * 特点：
             * - 这是一个"异常停止"事件，播放遇到无法恢复的错误
             * - 通常需要用户干预（如检查文件、网络等）
             * - 应该向用户显示错误信息
             * - 播放器状态不确定，建议重新初始化
             */
            override fun error(mediaPlayer: MediaPlayer?) {
                videoPlayer.events().removeMediaPlayerEventListener(this)
                // 切换到主线程更新状态
                CoroutineScope(Dispatchers.Main).launch {
                    stop()
                }
            }
        }
        val play = {
            if(!isPaused){
                videoPlayer.controls().pause()
                isPaused = true
            }else{
                videoPlayer.controls().play()
                isPaused = false
            }
        }
        Box(modifier.size(size).background(Color.Black).shadow(10.dp, shape = RoundedCornerShape(10.dp))
            .focusRequester(focusRequester)
            .onKeyEvent{ keyEvent ->
                // 处理键盘事件
                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) { // 空格键
                    play()
                    true // 事件已处理
                } else {
                    false // 事件未处理
                }
            }
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .align(Alignment.Center)
            ) {

                surface.image.value?.let{ image ->

                    // 获取真实的画布尺寸（考虑显示器缩放）
                    val canvasWidth = size.width.value
                    val canvasHeight = size.height.value
                    val imageWidth = image.width.toFloat() / density
                    val imageHeight = image.height.toFloat() / density

                    // 计算缩放比例，确保图片适应 Canvas 且保持宽高比
                    val scale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
                    val scaledWidth = imageWidth * scale
                    val scaledHeight = imageHeight * scale

                    // 计算居中位置
                    val xOffset = (canvasWidth - scaledWidth) / 2
                    val yOffset = (canvasHeight - scaledHeight) / 2

                    drawIntoCanvas { canvas ->
                        // 保存当前画布状态
                        canvas.save()

                        // 应用变换：先移动到目标位置，再缩放
                        canvas.translate(xOffset, yOffset)
                        canvas.scale(scale, scale)

                        // 绘制图片
                        canvas.nativeCanvas.drawImage(image, 0f, 0f)

                        // 恢复画布状态
                        canvas.restore()
                    }
                }
            }



            Column(modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally) {

                // 简化的字幕显示 - 直接显示已知的字幕内容
                if (mediaInfo.caption.content.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            text = mediaInfo.caption.content,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                        )
                    }
                }


                // 控制区
                Row(verticalAlignment = Alignment.CenterVertically,){

                    // 当前时间和总时间
                    Text(
                        text = "$currentTime / $videoDuration",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )


                    // 播放/暂停按钮
                    IconButton(onClick = { play() }){
                        Icon(
                            imageVector = if (!isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    // 按钮间隔
                    Spacer(Modifier.width(8.dp))

                    // 停止按钮
                    IconButton(onClick = {
                        if(videoPlayer.status().isPlaying){
                            videoPlayer.controls().pause()
                        }
                        videoPlayer.events().removeMediaPlayerEventListener(eventListener)
                        stop()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    // 设置按钮
                    Box {
                        IconButton(onClick = {settingsExpanded = true}){
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = settingsExpanded,
                            offset = DpOffset(x = (-60).dp, y = (-100).dp),
                            onDismissRequest = {
                                settingsExpanded = false
                                focusRequester.requestFocus()
                            },
                        ) {
                            DropdownMenuItem(onClick = { }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "显示字幕",
                                        color = androidx.compose.material.MaterialTheme.colors.onBackground,
                                    )
                                    Switch(checked = true, onCheckedChange = {

                                    })
                                }
                            }
                        }

                    }


                }
            }
        }


        /**
         *  使用一个 50 毫秒的协程来定时更新字幕。
         *  VLCJ 的 timeChanged 时间间隔很不稳定，
         *  大部分是在 0～ 50 毫秒之间，有时候会到 200 毫秒或 300毫秒。
         *  暂时先设置为 50 毫秒。
         */
        LaunchedEffect( Unit) {
            while (isActive) {
                val time = videoPlayer.status().time()
                timeChanged(time)
                delay(50)
            }
        }

        LaunchedEffect(Unit) {
            // 添加事件监听器
            videoPlayer.events().addMediaPlayerEventListener(eventListener)
            val caption = mediaInfo.caption
            val start = convertTimeToSeconds(caption.start)
            val end = convertTimeToSeconds(caption.end)

            // 使用内部字幕轨道,通常是从 MKV 生成的词库
            if(mediaInfo.trackId != -1){
                println("使用内部字幕轨道: ${mediaInfo.trackId}")
                videoPlayer.media()
                    .play(mediaInfo.mediaPath, ":sub-track=${mediaInfo.trackId}", ":start-time=$start", ":stop-time=$end")
            }else if(externalSubtitlesVisible){
                videoPlayer.media()
                    .play(mediaInfo.mediaPath, ":sub-autodetect-file", ":start-time=$start", ":stop-time=$end")
            }else{
                videoPlayer.media()
                    .play(mediaInfo.mediaPath, ":no-sub-autodetect-file", ":start-time=$start", ":stop-time=$end")
            }

            focusRequester.requestFocus()
        }


        DisposableEffect(Unit) {
            onDispose {
                videoPlayer.release()
            }
        }
    }


}

/**
 *
 * 解析媒体文件路径，优先使用绝对路径，如果文件不存在则在词库目录中查找同名文件
 *
 * @param absPath 媒体文件的绝对路径
 * @param vocabularyDir 词库所在的目录，用作备选查找位置
 * @return 解析后的有效媒体文件路径，如果文件不存在或路径为空则返回空字符串
 */
fun resolveMediaPath(
    absPath: String,
    vocabularyDir: File,
): String {

    // 视频文件的绝对地址
    val absPath = replaceSeparator(absPath)
    val absFile = File(absPath)
    // 如果绝对位置找不到，就在词库所在的文件夹寻找
    val relFile = File(vocabularyDir, absFile.name)
    if (absPath.isNotEmpty() && (absFile.exists() || relFile.exists())) {
        return if (!absFile.exists()) {
            relFile.absolutePath
        } else {
            absFile.absolutePath
        }

    }else{
        val message = if(absPath.isEmpty())"视频地址为空" else "视频地址错误"
        println(message)
        return ""
    }
}


/**
 * 媒体信息数据类，包含视频播放所需的核心信息
 *@property caption 字幕信息，包含字幕内容、开始时间和结束时间
 *@property mediaPath 媒体文件路径，可以是绝对路径或相对路径
 *@property trackId 字幕轨道ID，-1表示使用外部字幕文件，其他值表示使用内部字幕轨道
**/
data class MediaInfo(
    val caption: Caption,
    var mediaPath: String,
    val trackId: Int,
)



fun Component.createMediaPlayer(): EmbeddedMediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}
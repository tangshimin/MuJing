package player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Navigation
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import data.Caption
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import theme.LocalCtrl
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
 *
 * 这是一个基于 VLC 的嵌入式视频播放器，支持画中画(PiP)模式、字幕显示、播放控制等功能。
 * 主要用于字幕浏览器中的视频播放场景，提供完整的视频播放体验。
 *
 * ## 核心功能
 * - 基于 VLC 的视频播放，支持多种视频格式(MP4、MKV等)
 * - 内置播放控制界面(播放/暂停、停止、时间显示)
 * - 支持内部和外部字幕轨道
 * - 双向状态同步，支持外部控制播放/暂停状态
 * - 键盘快捷键支持(空格键播放/暂停)
 * - 可拖拽的画中画窗口
 * - 字幕显示和选择功能
 * - 指定时间段播放(根据字幕时间自动设置开始和结束时间)
 *
 * ## 状态管理
 * - 内部状态：`isPlaying` - 当前播放/暂停状态
 * - 外部状态：`externalPlayingState` - 接收外部状态变化
 * - 状态同步：通过 `onPlayingStateChanged` 回调通知外部状态变化
 * - 自动同步：外部状态变化会自动更新内部状态
 *
 * ## 使用场景
 * 主要用于字幕浏览器的画中画视频播放，配合 `PiPVideoWindow` 使用。
 * 支持单句字幕播放和多行字幕连续播放模式。
 *
 * @param modifier Compose 修饰符，用于设置组件的布局和样式
 * @param size 视频播放器的尺寸，指定宽度和高度
 * @param stop 停止播放的回调函数，会关闭播放器窗口并清理资源
 * @param volume 音量大小，范围 0.0-100.0
 * @param mediaInfo 媒体信息，包含视频路径、字幕信息、轨道ID等，为null时不显示播放器
 * @param externalSubtitlesVisible 是否显示外部字幕文件，默认为 false
 * @param timeChanged 时间变化回调，每50ms触发一次，用于更新播放进度和字幕同步
 * @param onPlayerReady 播放器准备就绪回调，返回 VLC EmbeddedMediaPlayer 实例
 * @param onPlayingStateChanged 播放状态变化回调，用于向外部通知状态变化
 * @param externalPlayingState 外部播放状态，用于接收外部的播放/暂停控制
 * @param showContextButton 是否显示查看语境按钮，默认为 false
 * @param showContext 查看语境的回调函数，点击按钮时触发
 * @param isLooping 是否启用循环播放，默认为 false
 * @param onLoopRestart 循环播放重启回调，
 *
 * @sample
 * ```kotlin
 * MiniVideoPlayer(
 *     modifier = Modifier.fillMaxSize(),
 *     size = DpSize(540.dp, 303.dp),
 *     stop = { /* 关闭播放器并清理资源 */ },
 *     volume = 50f,
 *     mediaInfo = MediaInfo(
 *         caption = Caption("00:00:10,000", "00:00:15,000", "Hello World"),
 *         mediaPath = "/path/to/video.mp4",
 *         trackId = -1
 *     ),
 *     externalSubtitlesVisible = false,
 *     timeChanged = { time -> /* 处理时间变化 */ },
 *     onPlayerReady = { player -> /* 播放器就绪 */ },
 *     onPlayingStateChanged = { isPlaying -> /* 处理状态变化 */ },
 *     externalPlayingState = false
 * )
 * ```
 *
 * ## 播放控件
 * - **播放/暂停按钮**：切换播放状态，支持空格键快捷键
 * - **停止按钮**：停止播放并调用 stop 回调
 * - **时间显示**：显示当前时间/总时长
 * - **设置按钮**：显示字幕相关设置（当前为占位符）
 *
 * ## 字幕支持
 * - **内部字幕**：支持 MKV 等容器格式的内嵌字幕轨道
 * - **外部字幕**：支持 SRT 等外部字幕文件
 * - **字幕显示**：在视频底部显示当前字幕内容
 * - **时间段播放**：根据字幕的开始和结束时间自动播放指定片段
 *
 * ## 技术实现
 * - 使用 VLC 的 EmbeddedMediaPlayer 进行视频播放
 * - 通过 SkiaImageVideoSurface 在 Compose Canvas 中渲染视频帧
 * - 使用 MediaPlayerEventAdapter 监听播放器事件
 * - 支持指定时间段播放(start-time, stop-time)
 * - 自动处理播放器生命周期和资源释放
 * - 50ms 定时器用于时间同步和进度更新
 *
 * ## 事件处理
 * - **stopped**: 用户手动停止或播放器被强制停止时触发
 * - **finished**: 媒体播放到达文件末尾时触发
 * - **error**: 播放遇到错误时触发
 * - **timeChanged**: 播放时间变化时触发
 * - **mediaPlayerReady**: 播放器准备就绪时触发
 *
 * ## 注意事项
 * - 组件内部会自动管理 VLC 播放器的创建和销毁
 * - 只有当 mediaInfo 不为 null 时才会创建播放器
 * - 播放器会在组件销毁时自动释放资源
 * - 支持的视频格式取决于系统中的 VLC 安装
 * - 外部状态变化会立即同步到内部状态
 * - 状态变化通过回调函数通知外部组件
 */

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MiniVideoPlayer(
    modifier: Modifier,
    size: DpSize,
    stop: () -> Unit,
    volume: Float,
    mediaInfo: MediaInfo?,
    externalSubtitlesVisible:Boolean = false,
    timeChanged:(Long) -> Unit = {},
    onPlayerReady: (EmbeddedMediaPlayer) -> Unit = {},
    onPlayingStateChanged: (Boolean) -> Unit = {},
    externalPlayingState: Boolean = false,
    showContextButton:Boolean = false, //
    showContext :() -> Unit= {},
    isLooping: Boolean = false, // 添加循环播放参数
    onLoopRestart: () -> Unit = {} // 添加循环重启回调
) {

    if(mediaInfo != null) {
        /** VLC 视频播放组件 */
        val videoPlayerComponent  = remember { createMediaPlayerComponent2() }
        val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
        val surface = remember {
            SkiaImageVideoSurface().also {
                videoPlayer.videoSurface().set(it)
            }
        }
        val focusRequester = remember { FocusRequester() }
        var isPlaying by remember { mutableStateOf(false) }
        var currentTime by remember{mutableStateOf("00:00:00")}
        var videoDuration by remember{mutableStateOf("00:00:00")}
        /** 展开设置菜单 */
        var settingsExpanded by remember { mutableStateOf(false) }
        /** 是否显示字幕 */
        var showCaption by remember { mutableStateOf(loadShowCaptionState()) }
        // 循环播放相关状态
        val endTimeMillis = remember(mediaInfo.caption.end) { convertTimeToMilliseconds(mediaInfo.caption.end) }
        val startTimeMillis = remember(mediaInfo.caption.start) { convertTimeToMilliseconds(mediaInfo.caption.start) }

        // 监听外部播放状态变化
        LaunchedEffect(externalPlayingState) {
            isPlaying = externalPlayingState
        }

        // 初始化播放状态
        LaunchedEffect(Unit) {
            isPlaying = externalPlayingState
        }

        val play = {
            if(isPlaying){
                videoPlayer.controls().pause()
                isPlaying = false
                onPlayingStateChanged(false) // 通知外部播放状态改变
            }else{
                videoPlayer.controls().play()
                isPlaying = true
                onPlayingStateChanged(true) // 通知外部恢复状态改变
            }
        }
        Box(modifier.size(size).background(Color.Black).shadow(10.dp, shape = RoundedCornerShape(10.dp))
            .focusRequester(focusRequester)
            .onKeyEvent{ keyEvent ->
                // 处理键盘事件
                val isModifierPressed = if(isMacOS()) keyEvent.isMetaPressed else  keyEvent.isCtrlPressed

                if (keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyUp) { // 空格键
                    play()
                    true // 事件已处理
                }else if (isModifierPressed && keyEvent.key == Key.G && keyEvent.type == KeyEventType.KeyUp) { // 空格键
                    showContext() // 显示语境
                    true // 事件已处理
                } else {
                    false // 事件未处理
                }
            }
        ) {

            CustomCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .align(Alignment.Center),
                surface = surface
            )


            Column(modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally) {

                // 简化的字幕显示 - 直接显示已知的字幕内容
                if (mediaInfo.caption.content.isNotEmpty() && showCaption) {
                    SelectionContainer {
                        Text(
                            text = mediaInfo.caption.content,
                            color = Color.White,
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .shadow(4.dp, shape = RoundedCornerShape(8.dp))
                        )
                    }
                }


                // 控制区
                Row(verticalAlignment = Alignment.CenterVertically){

                    // 当前时间和总时间
                    Text(
                        text = "$currentTime / $videoDuration",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.subtitle2
                    )

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                val text = if (isPlaying) "暂停" else "播放"
                                Text(
                                    text = text,
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
                        // 播放/暂停按钮
                        IconButton(onClick = play){
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                    }


                    // 按钮间隔
                    Spacer(Modifier.width(8.dp))

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(
                                    text = "停止",
                                    color =MaterialTheme.colors.onSurface,
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
                        // 停止按钮
                        IconButton(onClick = {
                            if(videoPlayer.status().isPlaying){
                                videoPlayer.controls().pause()
                            }
                            stop()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White
                            )
                        }
                    }

                    // 按钮间隔
                    Spacer(Modifier.width(8.dp))
                    if(showContextButton){
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp,MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    val ctrl = LocalCtrl.current
                                    val shortcut = if (isMacOS()) "$ctrl G" else "$ctrl+G"
                                    Row(modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ){
                                        Text(
                                            text = "查看语境 ",
                                            color =MaterialTheme.colors.onSurface,
                                        )
                                        Text(text =shortcut,
                                            color =MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                        )
                                    }                                }
                            },
                            delayMillis = 100,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.TopCenter,
                                alignment = Alignment.TopCenter,
                                offset = DpOffset.Zero
                            )
                        ) {
                            IconButton(onClick = showContext) {
                                Icon(
                                    imageVector = Icons.Outlined.Navigation,
                                    contentDescription = "导航到字幕的具体语境",
                                    tint = Color.White
                                )
                            }
                        }

                    }


                    Spacer(Modifier.width(8.dp))
                    // 设置按钮
                    Box {

                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp,MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(
                                        text = "设置",
                                        color =MaterialTheme.colors.onSurface,
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
                            IconButton(onClick = {settingsExpanded = true}){
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = settingsExpanded,
                            offset = DpOffset(x = (-60).dp, y = (-10).dp),
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
                                        color =MaterialTheme.colors.onBackground,
                                    )
                                    Switch(checked = showCaption,
                                        onCheckedChange = {
                                            showCaption = it
                                            saveShowCaptionState(it) // 保存状态变化
                                        })
                                }
                            }
                        }

                    }


                }
            }
        }


        /**
         *  使用 50 毫秒的协程来定时更新字幕。
         *  VLCJ 的 timeChanged 时间间隔很不稳定，
         *  大部分是在 0～ 50 毫秒之间，有时候会到 200 毫秒或 300毫秒。
         *  暂时先设置为 50 毫秒。
         */
        LaunchedEffect(Unit) {
            while (isActive) {
                val time = videoPlayer.status().time()
                timeChanged(time)
                delay(50)
            }
        }

        /**
         *  只在需要循环播放时启动 10 毫秒的协程来检测循环播放，
         *  实现基于时间监听的循环播放功能，避免 VLC 内部循环导致的崩溃问题。
         */
        LaunchedEffect(isLooping) {
            if (isLooping) {
                while (isActive) {
                    val time = videoPlayer.status().time()

                    // 检查是否需要循环播放，并且没有正在进行循环重启
                    if (videoPlayer.status().isPlaying && time >= endTimeMillis) {
                        println("循环播放: 基于时间监听 - 当前时间 ${time}ms >= 结束时间 ${endTimeMillis}ms")
                        // 安全地重置到开始时间
                        videoPlayer.controls().setTime(startTimeMillis)
                        println("循环播放: 重置到开始时间 ${startTimeMillis}ms")
                        // 调用循环重启回调
                        onLoopRestart()
                    }

                    delay(10)
                }
            }
        }

        DisposableEffect(Unit) {
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
                    // 通知外部播放器已准备好
                    onPlayerReady(videoPlayer)

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
                 * 注意：循环播放现在通过时间监听实现，避免 VLC 内部循环导致的崩溃问题
                 */
                override fun finished(mediaPlayer: MediaPlayer?) {
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
                    // 输出错误信息
                    println("播放错误: 未知错误")
                    // 切换到主线程更新状态
                    CoroutineScope(Dispatchers.Main).launch {
                        stop()
                    }
                }
            }
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

            onDispose {
                if(videoPlayer.status().isPlaying) {
                    videoPlayer.controls().stop()
                }
                surface.release()
                videoPlayerComponent.release()
                System.gc()
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

@Serializable
data class MiniPlayerSettings(
    val showCaption: Boolean = true
)

/**
 * 保存字幕显示状态到本地文件
 * @param showCaption 是否显示字幕
 */
private fun saveShowCaptionState(showCaption: Boolean) {
    try {
        val settings = MiniPlayerSettings(showCaption = showCaption)
        val json = Json.encodeToString(settings)
        val file = settingsFile()
        file.parentFile?.mkdirs() // 确保目录存在
        file.writeText(json)
    } catch (e: Exception) {
        println("保存字幕显示状态失败: ${e.message}")
    }
}

/**
 * 从本地文件加载字幕显示状态
 * @return 字幕显示状态，默认为true
 */
private fun loadShowCaptionState(): Boolean {
    return try {
        val file = settingsFile()
        if (file.exists()) {
            val json = file.readText()
            if (json.isNotBlank()) {
                val settings = Json.decodeFromString<MiniPlayerSettings>(json)
                settings.showCaption
            } else {
                true // 默认显示字幕
            }
        } else {
            true // 默认显示字幕
        }
    } catch (e: Exception) {
        println("加载字幕显示状态失败: ${e.message}")
        true // 出错时默认显示字幕
    }
}

private fun settingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "MiniPlayerSettings.json")
}
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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * PiPVideoWindow 是一个管理画中画(Picture-in-Picture)视频播放窗口的类。
 *
 * 该类负责创建和管理一个始终置顶的可拖拽窗口，内嵌 MiniVideoPlayer 组件用于视频播放。
 * 主要用于字幕浏览器中的画中画视频播放功能，允许用户在学习字幕时同时观看视频内容。
 *
 * ## 核心功能
 * - 创建可拖拽的画中画窗口，始终置顶显示
 * - 管理视频播放状态的双向同步
 * - 支持窗口位置和大小的动态调整
 * - 提供播放控制接口(播放、暂停、停止)
 * - 自动处理窗口生命周期和资源清理
 * - 支持初始播放状态设置
 *
 * ## 状态管理
 * - `isInPiPMode`: 是否处于画中画模式
 * - `isVideoPlaying`: 当前视频播放状态
 * - 支持外部状态同步，确保与字幕浏览器状态一致
 * - 通过回调函数通知外部组件状态变化
 *
 * ## 窗口特性
 * - **始终置顶**: 窗口保持在其他应用程序之上
 * - **无装饰**: 去除系统标题栏，提供简洁界面
 * - **可调整大小**: 支持用户拖拽调整窗口尺寸
 * - **可拖拽**: 整个窗口区域都支持拖拽移动
 * - **自动保存**: 窗口位置和大小变化会自动通知外部保存
 *
 * ## 使用场景
 * 主要用于字幕学习场景：
 * - 单句字幕播放：播放当前选中的单句字幕对应的视频片段
 * - 多行字幕播放：连续播放多行字幕对应的视频片段
 * - 字幕同步显示：在视频下方显示当前播放的字幕内容
 *
 * @param onClose 窗口关闭时的回调函数，用于通知外部组件清理状态
 *
 * @sample
 * ```kotlin
 * val pipWindow = PiPVideoWindow(onClose = {
 *     // 清理外部状态
 *     isPlaying = false
 * })
 *
 * // 进入画中画模式
 * pipWindow.enterPiPMode(
 *     mediaInfo = MediaInfo(
 *         caption = Caption("00:00:10,000", "00:00:15,000", "Hello World"),
 *         mediaPath = "/path/to/video.mp4",
 *         trackId = -1
 *     ),
 *     volume = 0.5f,
 *     bounds = Rectangle(100, 100, 540, 303),
 *     onPlayingStateChanged = { isPlaying ->
 *         // 同步播放状态到外部
 *     },
 *     initialPlayingState = true
 * )
 *
 * // 控制播放
 * pipWindow.pauseVideo()   // 暂停
 * pipWindow.resumeVideo()  // 恢复
 * pipWindow.stopVideo()    // 停止并关闭
 * ```
 *
 * ## 方法说明
 * - **enterPiPMode()**: 进入画中画模式，创建窗口并开始播放
 * - **pauseVideo()**: 暂停视频播放，保持窗口打开
 * - **resumeVideo()**: 恢复视频播放
 * - **stopVideo()**: 停止播放并完全关闭画中画窗口
 * - **exitPiPMode()**: 退出画中画模式，清理所有资源
 * - **isVideoPlaying()**: 获取当前播放状态
 * - **isInPiPMode()**: 检查是否处于画中画模式
 *
 * ## 技术实现
 * - 使用 ComposeWindow 创建原生窗口
 * - 通过 WindowDraggableArea 实现整窗口拖拽
 * - 使用回调函数实现状态双向同步
 * - 自动处理窗口事件监听和资源清理
 * - 支持显示器缩放和多显示器环境
 *
 * ## 注意事项
 * - 同时只能有一个画中画窗口实例处于活动状态
 * - 窗口关闭时会自动清理所有回调和状态
 * - 状态变化会通过回调函数实时通知外部组件
 * - 支持的视频格式取决于底层 VLC 播放器
 * - 窗口位置会在边界检查后自动调整以确保可见性
 */
class PiPVideoWindow(
    private val onClose: () -> Unit,
) {
    private var pipWindow: ComposeWindow? = null
    private var isInPiPMode = false
    private var isVideoPlaying = false
    private var pauseCallback: (() -> Unit)? = null
    private var resumeCallback: (() -> Unit)? = null
    private var updatePlayingStateCallback: ((Boolean) -> Unit)? = null
    private var updateInternalPlayingState: ((Boolean) -> Unit)? = null

    /**
     * 进入画中画(Picture-in-Picture)模式，创建并显示画中画视频播放窗口。
     *
     * 该方法会创建一个始终置顶的可拖拽窗口，内嵌 MiniVideoPlayer 组件进行视频播放。
     * 如果已经处于画中画模式，则直接返回，不会创建重复的窗口。
     *
     * ## 功能特性
     * - 创建始终置顶的无装饰窗口
     * - 支持整窗口拖拽移动
     * - 支持窗口大小调整
     * - 自动处理窗口生命周期事件
     * - 双向状态同步机制
     * - 支持初始播放状态设置
     *
     * ## 窗口配置
     * - **始终置顶**: 窗口保持在所有应用程序之上
     * - **无装饰**: 移除系统标题栏，提供简洁界面
     * - **可调整大小**: 用户可以拖拽边缘调整窗口尺寸
     * - **可拖拽**: 整个窗口区域都支持拖拽移动
     *
     * ## 状态同步机制
     * - 外部状态变化会立即同步到内部播放器
     * - 内部播放器状态变化会通过回调通知外部
     * - 支持设置初始播放状态
     * - 自动处理播放/暂停/停止状态转换
     *
     * @param mediaInfo 媒体信息，包含视频路径、字幕内容和轨道ID等播放所需的核心信息
     * @param volume 音量大小，范围 0-100.0
     * @param externalSubtitlesVisible 是否显示外部字幕文件，默认 false。
     *   - true: 自动检测并显示外部字幕文件(如 .srt)
     *   - false: 仅显示内部字幕轨道或指定的字幕内容
     * @param timeChanged 时间变化回调函数，每50ms触发一次，用于播放进度同步和字幕更新。
     *   参数为当前播放时间(毫秒)
     * @param bounds 窗口位置和大小，默认为 Rectangle(100, 100, 320, 180)。
     *   格式：Rectangle(x, y, width, height)
     * @param updateBounds 窗口位置或大小变化时的回调函数，用于保存窗口状态。
     *   当用户拖拽移动或调整窗口大小时会自动调用
     * @param onPlayingStateChanged 播放状态变化回调函数，用于向外部同步播放状态。
     *   参数：isPlaying - true表示播放中，false表示已暂停
     * @param initialPlayingState 初始播放状态，默认 true。
     *   - true: 窗口创建后立即开始播放
     *   - false: 窗口创建后处于暂停状态，需要手动播放
     * @param isLooping 是否循环播放，默认 false。
     *   - true: 播放完毕后自动从头开始播放
     *   - false: 播放完毕后停止
     * @param onLoopRestart 循环播放时，视频从头开始播放的回调函数
     *
     * @sample
     * ```kotlin
     * // 基本用法
     * pipWindow.enterPiPMode(
     *     mediaInfo = MediaInfo(
     *         caption = Caption("00:00:10,000", "00:00:15,000", "Hello World"),
     *         mediaPath = "/path/to/video.mp4",
     *         trackId = -1
     *     ),
     *     volume = 50f
     * )
     *
     * // 完整参数用法
     * pipWindow.enterPiPMode(
     *     mediaInfo = mediaInfo,
     *     volume = 50f,
     *     externalSubtitlesVisible = true,
     *     timeChanged = { time ->
     *         // 更新播放进度，同步字幕显示
     *         updateCurrentSubtitle(time)
     *     },
     *     bounds = Rectangle(200, 100, 640, 360),
     *     updateBounds = { newBounds ->
     *         // 保存窗口位置和大小
     *         saveWindowState(newBounds)
     *     },
     *     onPlayingStateChanged = { isPlaying ->
     *         // 同步播放状态到主界面
     *         mainScreenPlayingState = isPlaying
     *     },
     *     initialPlayingState = false // 创建窗口但不自动播放
     * )
     * ```
     *
     * ## 注意事项
     * - 如果已经处于画中画模式，重复调用此方法会直接返回，不会创建新窗口
     * - 窗口创建后会自动获取焦点并开始播放(除非 initialPlayingState 为 false)
     * - 窗口关闭时会自动调用 onClose 回调并清理所有资源
     * - 支持的视频格式取决于系统中安装的 VLC 播放器版本
     * - 窗口位置会根据屏幕边界自动调整以确保完全可见
     * - 状态同步是双向的：外部可以控制播放器，播放器状态变化也会通知外部
     *
     * @see MiniVideoPlayer 内嵌的视频播放器组件
     * @see MediaInfo 媒体信息数据类
     * @see exitPiPMode 退出画中画模式
     * @see pauseVideo 暂停播放
     * @see resumeVideo 恢复播放
     * @see stopVideo 停止播放并关闭窗口
     */
    fun enterPiPMode(
        mediaInfo: MediaInfo,
        volume: Float,
        externalSubtitlesVisible: Boolean = false,
        timeChanged: (Long) -> Unit = {},
        bounds: Rectangle = Rectangle(100, 100, 320, 180),
        updateBounds: (Rectangle) -> Unit = {},
        onPlayingStateChanged: (Boolean) -> Unit = {},
        initialPlayingState: Boolean = true, // 添加初始播放状态参数
        isLooping: Boolean = false, // 添加循环播放参数
        onLoopRestart: () -> Unit = {}, // 添加循环重启回调,
        colors: Colors,
    ) {
        if (isInPiPMode) return

        updatePlayingStateCallback = onPlayingStateChanged

        // 创建 PiP 窗口
        pipWindow = ComposeWindow().apply {
            title = "画中画播放"
            isAlwaysOnTop = true
            isUndecorated = true
            isResizable = true

            // 添加关闭监听器
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    exitPiPMode()
                }
            })

            // 添加窗口移动和大小改变监听器
            addComponentListener(object : java.awt.event.ComponentAdapter() {
                override fun componentMoved(e: java.awt.event.ComponentEvent?) {
                    updateBounds(this@apply.bounds)
                }

                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    updateBounds(this@apply.bounds)
                }
            })

            // 直接设置 Compose 内容
            setContent {
                var videoPlayerRef: EmbeddedMediaPlayer? by remember { mutableStateOf(null) }
                var internalPlayingState by remember { mutableStateOf(false) }

                // 设置内部状态更新回调
                updateInternalPlayingState = { isPlaying ->
                    internalPlayingState = isPlaying
                }

                // 设置暂停和恢复回调
                pauseCallback = {
                    videoPlayerRef?.controls()?.pause()
                    isVideoPlaying = false
                    internalPlayingState = false
                    updatePlayingStateCallback?.invoke(false) // 通知外部播放状态改变
                }

                resumeCallback = {
                    videoPlayerRef?.controls()?.play()
                    isVideoPlaying = true
                    internalPlayingState = true
                    updatePlayingStateCallback?.invoke(true) // 通知外部恢复状态改变
                }
                MaterialTheme(colors = colors){
                    WindowDraggableArea {
                        MiniVideoPlayer(
                            modifier = Modifier.fillMaxSize(),
                            size = DpSize(bounds.width.dp, bounds.height.dp),
                            stop = {
                                exitPiPMode()
                            },
                            timeChanged = timeChanged,
                            volume = volume,
                            mediaInfo = mediaInfo,
                            externalSubtitlesVisible = externalSubtitlesVisible,
                            onPlayerReady = { player ->
                                videoPlayerRef = player
                            },
                            onPlayingStateChanged = { isPlaying ->
                                isVideoPlaying = isPlaying
                                updatePlayingStateCallback?.invoke(isPlaying)
                            },
                            externalPlayingState = internalPlayingState,
                            isLooping = isLooping,
                            onLoopRestart = onLoopRestart
                        )
                    }
                }
            }

            // 设置位置和大小
            setBounds(bounds)
            isVisible = true
        }

        // 根据初始播放状态决定是否播放
        if (initialPlayingState) {
            isVideoPlaying = true
            updateInternalPlayingState?.invoke(true)
        } else {
            isVideoPlaying = false
            updateInternalPlayingState?.invoke(false)
        }

        isInPiPMode = true
    }

    fun pauseVideo() {
        if (isInPiPMode && isVideoPlaying) {
            updateInternalPlayingState?.invoke(false)
            pauseCallback?.invoke()
        }
    }

    fun resumeVideo() {
        if (isInPiPMode && !isVideoPlaying) {
            updateInternalPlayingState?.invoke(true)
            resumeCallback?.invoke()
        }
    }

    fun isVideoPlaying(): Boolean = isVideoPlaying

    fun stopVideo() {
        if (isInPiPMode) {
            // 先暂停视频（如果正在播放）
            if (isVideoPlaying) {
                updateInternalPlayingState?.invoke(false)
                pauseCallback?.invoke()
            }
            // 然后完全退出画中画模式
            exitPiPMode()
        }
    }

    fun exitPiPMode() {
        if (!isInPiPMode) return

        pipWindow?.dispose()
        pipWindow = null
        isInPiPMode = false
        isVideoPlaying = false
        pauseCallback = null
        resumeCallback = null
        updatePlayingStateCallback = null
        updateInternalPlayingState = null
        onClose()
    }

    fun isInPiPMode(): Boolean = isInPiPMode


}
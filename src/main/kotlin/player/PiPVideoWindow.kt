package player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class PiPVideoWindow(
    private val onClose: () -> Unit
) {
    private var pipWindow: ComposeWindow? = null
    private var isInPiPMode = false

    fun enterPiPMode(
        mediaInfo: MediaInfo,
        volume: Float,
        externalSubtitlesVisible: Boolean = false,
        timeChanged: (Long) -> Unit = {},
        bounds: Rectangle = Rectangle(100, 100, 320, 180),
        updateBounds: (Rectangle) -> Unit = {},
    ) {
        if (isInPiPMode) return

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
                    // 窗口位置改变时的回调
                    // this@apply - 指向第40行 ComposeWindow().apply 中的 ComposeWindow 实例
                    // this（默认）- 在 ComponentAdapter 的方法内部，默认指向 ComponentAdapter 实例
                    updateBounds(this@apply.bounds)

                }

                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    // 窗口大小改变时的回调
                    updateBounds(this@apply.bounds)
                }
            })

            // 直接设置 Compose 内容
            setContent {
                var isPlaying by remember { mutableStateOf(true) }

                WindowDraggableArea {
                    MiniVideoPlayer(
                        modifier = Modifier.fillMaxSize(),
                        size = DpSize(bounds.width.dp, bounds.height.dp),
                        isPlaying = isPlaying,
                        stop = {
                            isPlaying = false
                            exitPiPMode()
                        },
                        timeChanged = timeChanged,
                        volume = volume,
                        mediaInfo = mediaInfo,
                        isInPiPMode = true,
                        externalSubtitlesVisible = externalSubtitlesVisible
                    )
                }



            }

            // 设置位置和大小
            setBounds(bounds)
            isVisible = true
        }

        isInPiPMode = true
    }

    fun exitPiPMode() {
        if (!isInPiPMode) return

        pipWindow?.dispose()

        pipWindow = null
        isInPiPMode = false
        onClose()
    }

    fun isInPiPMode(): Boolean = isInPiPMode
}
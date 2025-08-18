package player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import util.monitorMemory

/**
 * 自定义 Canvas 组件，用于渲染视频帧
 *
 * 该组件负责将视频表面（VideoSurface）中的图像帧渲染到 Canvas 上，
 * 并自动处理图像的缩放和居中显示，保持原始宽高比。
 *
 * @param modifier Compose 修饰符，用于定义 Canvas 的布局和样式
 * @param surface 视频表面对象，包含待渲染的视频图像帧
 * @param showFps 是否显示帧率测试信息，默认为 false
 *
 * 功能特性：
 * - 自动适应 Canvas 尺寸，保持视频原始宽高比
 * - 图像居中显示
 * - 使用 Skia 原生 Canvas 进行高性能渲染
 * - 支持实时视频帧更新
 */
@Composable
fun CustomCanvas(
    modifier: Modifier,
    surface: SkiaImageVideoSurface,
    showFps: Boolean = false
){

    val performanceMonitor = if (showFps) remember { PerformanceMonitor() } else null
    var lastImageIdentity by remember { mutableStateOf<Any?>(null) }

    Canvas(
        modifier = modifier,
    ) {

        val startTime = if (showFps) System.nanoTime() else 0

        // 使用 withImage 安全地访问 Image 对象
        surface.withImage { image ->
            image?.let { img ->
                // 检查是否是新的图像帧
                val currentIdentity = if (showFps) System.identityHashCode(img) else null
                val isNewFrame = if (showFps) currentIdentity != lastImageIdentity else false

                // 获取Canvas的实际绘制区域尺寸
                val canvasWidth = size.width
                val canvasHeight = size.height
                val imageWidth = img.width.toFloat()
                val imageHeight = img.height.toFloat()

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
                    canvas.nativeCanvas.drawImage(img, 0f, 0f)

                    // 恢复画布状态
                    canvas.restore()
                }

                // 只在启用帧率测试且为新帧时记录
                if (showFps && isNewFrame && performanceMonitor != null) {
                    val renderTimeMs = (System.nanoTime() - startTime) / 1_000_000
                    performanceMonitor.onFrameRendered(renderTimeMs)
                    lastImageIdentity = currentIdentity
                }
            }
        }
    }
}

class PerformanceMonitor {
    private var frameCount = 0L
    private var dropFrameCount = 0L
    private var lastMonitorTime = System.currentTimeMillis()

    fun onFrameRendered(renderTimeMs: Long) {
        frameCount++

        // 检测丢帧（渲染时间超过33ms，即低于30fps）
        if (renderTimeMs > 33) {
            dropFrameCount++
        }

        val now = System.currentTimeMillis()
        if (now - lastMonitorTime >= 5000) { // 每5秒报告一次
            val fps = frameCount * 1000.0 / (now - lastMonitorTime)
            val dropRate = dropFrameCount.toDouble() / frameCount * 100

            println("FPS: $fps, Drop Rate: ${dropRate}%")
            monitorMemory()

            frameCount = 0
            dropFrameCount = 0
            lastMonitorTime = now
        }
    }
}
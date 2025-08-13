package player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * 自定义 Canvas 组件，用于渲染视频帧
 *
 * 该组件负责将视频表面（VideoSurface）中的图像帧渲染到 Canvas 上，
 * 并自动处理图像的缩放和居中显示，保持原始宽高比。
 *
 * @param modifier Compose 修饰符，用于定义 Canvas 的布局和样式
 * @param surface 视频表面对象，包含待渲染的视频图像帧
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
    surface: SkiaImageVideoSurface
){

    Canvas(
        modifier = modifier,
    ) {
        // 使用 withImage 安全地访问 Image 对象
        surface.withImage { image ->
            image?.let { img ->
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
            }
        }
    }
}
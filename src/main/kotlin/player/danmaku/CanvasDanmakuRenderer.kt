package player.danmaku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 基于 Canvas 的弹幕渲染器
 * 第一阶段：实现基础的从右到左移动的弹幕渲染
 */
@Composable
fun CanvasDanmakuRenderer(
    danmakuItems: List<CanvasDanmakuItem>,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
    fontSize: Int = 18,
    speed: Float = 3f
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = fontSize.sp,
        fontFamily = fontFamily
    )

    // 启动动画循环
    LaunchedEffect(danmakuItems) {
        while (isActive) {
            // 更新所有弹幕的位置
            danmakuItems.forEach { item ->
                if (item.isActive) {
                    item.updatePosition(speed)
                    // 测量文字宽度（如果还未测量）
                    if (item.textWidth == 0f) {
                        val measured = textMeasurer.measure(item.text, textStyle)
                        item.textWidth = measured.size.width.toFloat()
                    }

                    // 检查弹幕是否已移出屏幕（静止弹幕由时间控制生命周期）
                    if (item.type == DanmakuType.SCROLL && item.x + item.textWidth < 0) {
                        item.isActive = false
                    }

                }
            }
            delay(16) // ~60 FPS
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawDanmakuItems(
            danmakuItems = danmakuItems,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
            canvasWidth = size.width
        )
    }
}

/**
 * 在 Canvas 上绘制弹幕项目
 */
private fun DrawScope.drawDanmakuItems(
    danmakuItems: List<CanvasDanmakuItem>,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    canvasWidth: Float
) {
    danmakuItems.forEach { item ->
        if (item.isActive && item.isVisible(canvasWidth)) {
            // 确保文字宽度已经测量
            if (item.textWidth == 0f) {
                val measured = textMeasurer.measure(item.text, textStyle)
                item.textWidth = measured.size.width.toFloat()

                // 对于静止弹幕，重新计算居中位置（仅限顶部和底部弹幕）
                if (item.type == DanmakuType.TOP || item.type == DanmakuType.BOTTOM) {
                    item.x = (canvasWidth - item.textWidth) / 2
                }
                // 标注弹幕保持原始位置，不需要重新计算
            }

            // 绘制完整文字 - 取消 maxLines 限制，让文字在一行内完全显示
            drawText(
                textMeasurer = textMeasurer,
                text = item.text,
                style = textStyle.copy(
                    color = item.color
                ),
                topLeft = androidx.compose.ui.geometry.Offset(item.x, item.y - textStyle.fontSize.toPx()),
                // 移除 maxLines 和 overflow 限制，让文字自然显示
                softWrap = false  // 禁止软换行，强制在一行显示
            )


        }
    }
}

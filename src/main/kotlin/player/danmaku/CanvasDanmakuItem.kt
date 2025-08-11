package player.danmaku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import data.Word

/**
 * Canvas 弹幕项目数据类
 * 用于 Canvas 渲染的弹幕对象
 */
class CanvasDanmakuItem(
    val text: String,
    val word: Word? = null,
    val color: Color = Color.White,
    val type: DanmakuType = DanmakuType.SCROLL,
    val startTime: Float = 0f,
    initialX: Float = 0f,
    initialY: Float = 0f
) {
    // 位置状态
    var x by mutableStateOf(initialX)
    var y by mutableStateOf(initialY)

    // 文字宽度（需要测量后设置）
    var textWidth by mutableStateOf(0f)

    // 是否激活状态
    var isActive by mutableStateOf(true)

    // 是否暂停移动（用于鼠标悬停等交互）
    var isPaused by mutableStateOf(false)

    /**
     * 更新弹幕位置
     */
    fun updatePosition(speed: Float) {
        if (!isPaused && isActive) {
            when (type) {
                DanmakuType.SCROLL -> {
                    x -= speed
                }
                DanmakuType.TOP, DanmakuType.BOTTOM -> {
                    // 顶部和底部弹幕不移动位置
                }
            }
        }
    }

    /**
     * 检查弹幕是否在屏幕可见范围内
     */
    fun isVisible(canvasWidth: Float): Boolean {
        return when (type) {
            DanmakuType.SCROLL -> x + textWidth >= 0 && x <= canvasWidth
            DanmakuType.TOP, DanmakuType.BOTTOM -> true
        }
    }

    /**
     * 测量并设置文字宽度
     */
//    fun measureTextWidth(font: Font) {
//        textWidth = font.measureTextWidth(text)
//    }

    /**
     * 重置弹幕状态（用于对象池）
     */
    fun reset(
        newText: String,
        newWord: Word? = null,
        newColor: Color = Color.White,
        newType: DanmakuType = DanmakuType.SCROLL,
        newStartTime: Float = 0f,
        newX: Float = 0f,
        newY: Float = 0f
    ) {
        // 这里不能直接重新赋值不可变属性，需要在对象池中处理
        x = newX
        y = newY
        isActive = true
        isPaused = false
        textWidth = 0f
    }
}

/**
 * 弹幕类型枚举
 */
enum class DanmakuType {
    SCROLL,  // 滚动弹幕
    TOP,     // 顶部弹幕
    BOTTOM   // 底部弹幕
}

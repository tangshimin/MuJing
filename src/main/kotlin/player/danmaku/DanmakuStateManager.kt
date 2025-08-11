package player.danmaku

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import data.Word
import kotlin.random.Random

/**
 * 弹幕状态管理器
 * 管理弹幕的整个生命周期：创建、调度、移除
 */
class DanmakuStateManager {
    // 当前活跃的弹幕列表
    private val _activeDanmakus = mutableStateListOf<CanvasDanmakuItem>()
    val activeDanmakus: List<CanvasDanmakuItem> = _activeDanmakus

    // 弹幕配置
    var isEnabled by mutableStateOf(true)
    var globalOpacity by mutableStateOf(1f)
    var speed by mutableStateOf(3f)
    var maxDanmakuCount by mutableStateOf(50) // 最大同时显示弹幕数

    // Canvas 尺寸
    private var canvasWidth by mutableStateOf(0f)
    private var canvasHeight by mutableStateOf(0f)

    // 字体相关
    private var lineHeight = 30f

    /**
     * 设置 Canvas 尺寸
     */
    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    /**
     * 设置行高
     */
    fun setLineHeight(lineHeight: Float) {
        this.lineHeight = lineHeight
    }

    /**
     * 添加新弹幕
     */
    fun addDanmaku(
        text: String,
        word: Word? = null,
        color: Color = Color.White,
        type: DanmakuType = DanmakuType.SCROLL
    ) {
        if (!isEnabled || _activeDanmakus.size >= maxDanmakuCount) {
            return
        }

        val startX = canvasWidth
        val startY = when (type) {
            DanmakuType.SCROLL -> getRandomTrackY()
            DanmakuType.TOP -> lineHeight
            DanmakuType.BOTTOM -> canvasHeight - lineHeight
        }

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = type,
            initialX = startX,
            initialY = startY
        )

        _activeDanmakus.add(danmaku)
    }

    /**
     * 清理不活跃的弹幕
     */
    fun cleanup() {
        _activeDanmakus.removeAll { !it.isActive }
    }

    /**
     * 清空所有弹幕
     */
    fun clear() {
        _activeDanmakus.clear()
    }

    /**
     * 暂停所有弹幕
     */
    fun pauseAll() {
        _activeDanmakus.forEach { it.isPaused = true }
    }

    /**
     * 恢复所有弹幕
     */
    fun resumeAll() {
        _activeDanmakus.forEach { it.isPaused = false }
    }

    /**
     * 获取随机轨道Y坐标
     * 简单实现，后续阶段会用轨道管理器替代
     */
    private fun getRandomTrackY(): Float {
        val trackCount = (canvasHeight / lineHeight).toInt()
        val trackIndex = Random.nextInt(0, trackCount.coerceAtLeast(1))
        return (trackIndex + 1) * lineHeight
    }

    /**
     * 根据时间添加弹幕（为后续时间轴同步做准备）
     */
    fun addTimedDanmaku(
        text: String,
        timeMs: Long,
        word: Word? = null,
        color: Color = Color.White
    ) {
        // 这里先简单添加，后续阶段会实现真正的时间轴同步
        addDanmaku(text, word, color)
    }
}

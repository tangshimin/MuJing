package player.danmaku

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import data.Word
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    // 轨道管理器
    private val trackManager = TrackManager()

    // 等待队列：没有找到可用轨道的弹幕
    private val waitingQueue = mutableListOf<PendingDanmaku>()

    // 时间轴同步器（可选）
    private var timelineSynchronizer: TimelineSynchronizer? = null

    /**
     * 等待中的弹幕数据类
     */
    private data class PendingDanmaku(
        val text: String,
        val word: Word?,
        val color: Color,
        val type: DanmakuType,
        val addTime: Long = System.currentTimeMillis()
    )

    /**
     * 初始化时间轴同步器
     */
    fun initializeTimelineSync(mediaTimeFlow: Flow<Long>? = null): TimelineSynchronizer {
        if (timelineSynchronizer == null) {
            timelineSynchronizer = TimelineSynchronizer(this)
        }
        return timelineSynchronizer!!
    }

    /**
     * 获取时间轴同步器
     */
    fun getTimelineSynchronizer(): TimelineSynchronizer? = timelineSynchronizer

    /**
     * 设置 Canvas 尺寸
     */
    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
        trackManager.updateCanvasSize(height, lineHeight)
    }

    /**
     * 设置行高
     */
    fun setLineHeight(lineHeight: Float) {
        this.lineHeight = lineHeight
        trackManager.updateCanvasSize(canvasHeight, lineHeight)
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

        when (type) {
            DanmakuType.SCROLL -> {
                addScrollDanmaku(text, word, color)
            }
            DanmakuType.TOP, DanmakuType.BOTTOM -> {
                addStaticDanmaku(text, word, color, type)
            }
        }
    }

    /**
     * 添加滚动弹幕（使用轨道管理）
     */
    private fun addScrollDanmaku(text: String, word: Word?, color: Color) {
        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = DanmakuType.SCROLL,
            initialX = canvasWidth,
            initialY = 0f // Y坐标会由轨道管理器设置
        )

        // 尝试分配轨道
        val trackIndex = trackManager.assignTrack(danmaku, canvasWidth)

        if (trackIndex >= 0) {
            // 成功分配轨道，添加到活跃弹幕列表
            _activeDanmakus.add(danmaku)
        } else {
            // 没有可用轨道，加入等待队列
            waitingQueue.add(PendingDanmaku(text, word, color, DanmakuType.SCROLL))

            // 限制等待队列大小，避免内存溢出
            if (waitingQueue.size > 20) {
                waitingQueue.removeAt(0) // 移除最老的等待弹幕
            }
        }
    }

    /**
     * 添加静态弹幕（顶部/底部）
     */
    private fun addStaticDanmaku(text: String, word: Word?, color: Color, type: DanmakuType) {
        val startY = when (type) {
            DanmakuType.TOP -> lineHeight
            DanmakuType.BOTTOM -> canvasHeight - lineHeight
            else -> lineHeight
        }

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = type,
            initialX = canvasWidth / 2 - 100f, // 居中显示，粗略计算
            initialY = startY
        )

        _activeDanmakus.add(danmaku)
    }

    /**
     * 清理不活跃的弹幕并尝试处理等待队列
     */
    fun cleanup() {
        // 清理不活跃的弹幕
        val removedDanmakus = _activeDanmakus.filter { !it.isActive }
        _activeDanmakus.removeAll { !it.isActive }

        // 释放轨道
        removedDanmakus.forEach { danmaku ->
            trackManager.releaseTrack(danmaku)
        }

        // 清理轨道管理器
        trackManager.cleanup()

        // 尝试处理等待队列中的弹幕
        processWaitingQueue()
    }

    /**
     * 处理等待队列中的弹幕
     */
    private fun processWaitingQueue() {
        if (waitingQueue.isEmpty()) return

        val iterator = waitingQueue.iterator()
        while (iterator.hasNext() && _activeDanmakus.size < maxDanmakuCount) {
            val pending = iterator.next()

            // 检查是否等待时间过长，如果是则丢弃
            if (System.currentTimeMillis() - pending.addTime > 5000) { // 5秒超时
                iterator.remove()
                continue
            }

            // 尝试创建弹幕并分配轨道
            val danmaku = CanvasDanmakuItem(
                text = pending.text,
                word = pending.word,
                color = pending.color,
                type = pending.type,
                initialX = canvasWidth,
                initialY = 0f
            )

            val trackIndex = trackManager.assignTrack(danmaku, canvasWidth)
            if (trackIndex >= 0) {
                // 成功分配，添加到活跃列表并从等待队列移除
                _activeDanmakus.add(danmaku)
                iterator.remove()
            }
        }
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
     * 根据时间添加弹幕（现在真正实现时间轴同步）
     */
    fun addTimedDanmaku(
        text: String,
        timeMs: Long,
        word: Word? = null,
        color: Color = Color.White,
        type: DanmakuType = DanmakuType.SCROLL
    ) {
        timelineSynchronizer?.addTimedDanmaku(timeMs, text, word, color, type)
            ?: addDanmaku(text, word, color, type) // 如果没有时间轴同步器，直接添加
    }

    /**
     * 加载一批定时弹幕数据
     */
    fun loadTimedDanmakus(danmakus: List<TimelineSynchronizer.TimedDanmakuData>) {
        timelineSynchronizer?.loadTimedDanmakus(danmakus)
    }

    /**
     * 更新媒体播放时间
     */
    fun updateMediaTime(timeMs: Long) {
        timelineSynchronizer?.updateTime(timeMs)
    }

    /**
     * 重置时间轴同步状态
     */
    fun resetTimeline() {
        timelineSynchronizer?.reset()
    }
}

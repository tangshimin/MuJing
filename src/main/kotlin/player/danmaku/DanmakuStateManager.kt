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
        type: DanmakuType = DanmakuType.SCROLL,
        timeMs: Long? = null // 弹幕的时间戳（可选）
    ) {
        if (!isEnabled || _activeDanmakus.size >= maxDanmakuCount) {
            return
        }

        when (type) {
            DanmakuType.SCROLL -> {
                addScrollDanmaku(text, word, color, timeMs)
            }
            DanmakuType.TOP, DanmakuType.BOTTOM -> {
                addStaticDanmaku(text, word, color, type)
            }
            DanmakuType.ANNOTATION -> {
                // 标注弹幕需要具体位置，这里提供默认位置
                addAnnotationDanmaku(text, canvasWidth * 0.5f, canvasHeight * 0.5f, word, color)
            }
        }
    }

    /**
     * 移除指定的弹幕
     */
    fun removeDanmaku(danmaku: CanvasDanmakuItem) {
        // 从活跃弹幕列表中移除
        val removed = _activeDanmakus.remove(danmaku)

        if (removed) {
            // 释放轨道资源
            trackManager.releaseTrack(danmaku)

            // 标记弹幕为不活跃状态
            danmaku.isActive = false

            // 立即尝试处理等待队列中的弹幕
            processWaitingQueue()
        }
    }

    /**
     * 添加滚动弹幕（使用轨道管理）
     */
    private fun addScrollDanmaku(text: String, word: Word?, color: Color, timeMs: Long? = null) {
        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = DanmakuType.SCROLL,
            timeMs = timeMs,
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

        // 计算居中位置
        val estimatedTextWidth = text.length * 12f // 粗略估算，实际会在渲染时精确测量
        val centerX = (canvasWidth - estimatedTextWidth) / 2

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = type,
            initialX = centerX,
            initialY = startY
        )

        // 设置显示时长
        danmaku.setDisplayDuration(3000L) // 默认3秒

        _activeDanmakus.add(danmaku)
    }

    /**
     * 添加顶部静止弹幕
     */
    fun addTopDanmaku(
        text: String,
        word: Word? = null,
        color: Color = Color.White,
        durationMs: Long = 3000L
    ) {
        if (!isEnabled || _activeDanmakus.size >= maxDanmakuCount) {
            return
        }

        val estimatedTextWidth = text.length * 12f
        val centerX = (canvasWidth - estimatedTextWidth) / 2

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = DanmakuType.TOP,
            initialX = centerX,
            initialY = lineHeight
        )

        danmaku.setDisplayDuration(durationMs)
        _activeDanmakus.add(danmaku)
    }

    /**
     * 添加底部静止弹幕
     */
    fun addBottomDanmaku(
        text: String,
        word: Word? = null,
        color: Color = Color.White,
        durationMs: Long = 3000L
    ) {
        if (!isEnabled || _activeDanmakus.size >= maxDanmakuCount) {
            return
        }

        val estimatedTextWidth = text.length * 12f
        val centerX = (canvasWidth - estimatedTextWidth) / 2

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = DanmakuType.BOTTOM,
            initialX = centerX,
            initialY = canvasHeight - lineHeight
        )

        danmaku.setDisplayDuration(durationMs)
        _activeDanmakus.add(danmaku)
    }

    /**
     * 添加自定义位置的静止弹幕（用于视频标注）
     */
    fun addAnnotationDanmaku(
        text: String,
        x: Float,
        y: Float,
        word: Word? = null,
        color: Color = Color.White,
        durationMs: Long = 5000L
    ) {
        if (!isEnabled || _activeDanmakus.size >= maxDanmakuCount) {
            return
        }

        val danmaku = CanvasDanmakuItem(
            text = text,
            word = word,
            color = color,
            type = DanmakuType.ANNOTATION,
            initialX = x,
            initialY = y
        )

        danmaku.setDisplayDuration(durationMs)
        _activeDanmakus.add(danmaku)
    }

    /**
     * 添加相对位置的标注弹幕（基于百分比）
     */
    fun addAnnotationDanmakuRelative(
        text: String,
        xPercent: Float, // 0.0-1.0，表示在屏幕宽度的百分比位置
        yPercent: Float, // 0.0-1.0，表示在屏幕高度的百分比位置
        word: Word? = null,
        color: Color = Color.White,
        durationMs: Long = 5000L
    ) {
        val absoluteX = canvasWidth * xPercent
        val absoluteY = canvasHeight * yPercent
        addAnnotationDanmaku(text, absoluteX, absoluteY, word, color, durationMs)
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
            ?: addDanmaku(text, word, color, type, timeMs) // 如果没有时间轴同步器，直接添加
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

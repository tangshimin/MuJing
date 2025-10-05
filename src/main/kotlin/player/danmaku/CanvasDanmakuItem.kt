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
    val timeMs: Long? = null, // 弹幕的时间戳（毫秒），用于碰撞检测
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

    // 静止弹幕相关属性
    var displayDurationMs by mutableStateOf(3000L) // 默认显示3秒
    var createdTimeMs by mutableStateOf(0L) // 创建时间

    /**
     * 更新弹幕位置
     */
    fun updatePosition(speed: Float) {
        if (!isPaused && isActive) {
            when (type) {
                DanmakuType.SCROLL -> {
                    x -= speed
                }
                DanmakuType.TOP, DanmakuType.BOTTOM, DanmakuType.ANNOTATION -> {
                    // 静止弹幕不移动位置，但需要处理显示时长
                    updateStaticDanmakuLifetime()
                }
            }
        }
    }

    /**
     * 设置静止弹幕的显示时长
     */
    fun setDisplayDuration(durationMs: Long) {
        displayDurationMs = durationMs
        createdTimeMs = System.currentTimeMillis()
    }

    /**
     * 更新静止弹幕的生命周期
     */
    private fun updateStaticDanmakuLifetime() {
        if (type == DanmakuType.TOP || type == DanmakuType.BOTTOM || type == DanmakuType.ANNOTATION) {
            val currentTime = System.currentTimeMillis()
            if (createdTimeMs > 0 && currentTime - createdTimeMs > displayDurationMs) {
                isActive = false // 超时后标记为不活跃
            }
        }
    }

    /**
     * 检查弹幕是否在屏幕可见范围内
     */
    fun isVisible(canvasWidth: Float): Boolean {
        return when (type) {
            DanmakuType.SCROLL -> x + textWidth >= 0 && x <= canvasWidth
            DanmakuType.TOP, DanmakuType.BOTTOM, DanmakuType.ANNOTATION -> {
                // 静止弹幕和标注弹幕在显示时长内都可见
                isActive && (createdTimeMs == 0L ||
                        System.currentTimeMillis() - createdTimeMs <= displayDurationMs)
            }
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
    SCROLL,     // 滚动弹幕
    TOP,        // 顶部弹幕
    BOTTOM,     // 底部弹幕
    ANNOTATION  // 标注弹幕（任意位置）
}

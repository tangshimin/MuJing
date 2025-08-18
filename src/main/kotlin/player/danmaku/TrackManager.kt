package player.danmaku

import androidx.compose.runtime.mutableStateListOf
import kotlin.math.max

/**
 * 弹幕轨道管理器
 * 负责管理弹幕轨道，避免弹幕重叠
 */
class TrackManager {
    // 轨道列表，每个轨道存储最后一条弹幕的信息
    private val tracks = mutableStateListOf<TrackInfo?>()

    // 轨道配置
    private var canvasHeight = 0f
    private var lineHeight = 30f
    private var maxTracks = 0

    /**
     * 轨道信息数据类
     */
    data class TrackInfo(
        val danmaku: CanvasDanmakuItem,
        val assignTime: Long = System.currentTimeMillis()
    )

    /**
     * 更新Canvas尺寸和轨道配置
     */
    fun updateCanvasSize(height: Float, lineHeight: Float) {
        this.canvasHeight = height
        this.lineHeight = lineHeight

        // 计算最大轨道数量
        val newMaxTracks = (height / lineHeight).toInt()

        // 调整轨道列表大小
        if (newMaxTracks != maxTracks) {
            maxTracks = newMaxTracks
            tracks.clear()
            repeat(maxTracks) {
                tracks.add(null)
            }
        }
    }

    /**
     * 为新弹幕分配轨道
     * @param danmaku 要分配轨道的弹幕
     * @param canvasWidth 画布宽度，用于碰撞计算
     * @return 分配的轨道索引，如果没有可用轨道返回-1
     */
    fun assignTrack(danmaku: CanvasDanmakuItem, canvasWidth: Float): Int {
        if (tracks.isEmpty()) return -1

        // 遍历所有轨道，找到可用的轨道
        for (trackIndex in tracks.indices) {
            if (isTrackAvailable(trackIndex, danmaku, canvasWidth)) {
                // 分配轨道
                tracks[trackIndex] = TrackInfo(danmaku)

                // 设置弹幕的Y坐标
                danmaku.y = (trackIndex + 1) * lineHeight

                return trackIndex
            }
        }

        // 没有找到可用轨道
        return -1
    }

    /**
     * 检查轨道是否可用
     */
    private fun isTrackAvailable(trackIndex: Int, newDanmaku: CanvasDanmakuItem, canvasWidth: Float): Boolean {

        // 第一条轨道和标题栏重叠了，不使用
        if (trackIndex == 0) return false

        val trackInfo = tracks[trackIndex] ?: return true // 轨道为空，可用

        val lastDanmaku = trackInfo.danmaku

        // 检查上一条弹幕是否仍然活跃
        if (!lastDanmaku.isActive) {
            tracks[trackIndex] = null // 清理不活跃的弹幕
            return true
        }

        // 检查碰撞：计算新弹幕是否会与轨道上的最后一条弹幕碰撞
        return !willCollide(lastDanmaku, newDanmaku, canvasWidth)
    }

    /**
     * 检查两条弹幕是否会发生碰撞
     */
    private fun willCollide(existingDanmaku: CanvasDanmakuItem, newDanmaku: CanvasDanmakuItem, canvasWidth: Float): Boolean {
        // 如果现有弹幕已经完全进入屏幕（右边缘离开屏幕右侧）
        val existingRightEdge = existingDanmaku.x + existingDanmaku.textWidth
        val newDanmakuSpeed = 3f // 假设弹幕速度，后续可以从参数传入
        val existingSpeed = 3f

        // 如果现有弹幕的速度大于等于新弹幕，且已经完全进入屏幕
        if (existingSpeed >= newDanmakuSpeed && existingRightEdge < canvasWidth) {
            return false // 不会碰撞
        }

        // 简化的碰撞检测：如果现有弹幕的左边缘还没有完全离开屏幕右侧
        if (existingDanmaku.x > canvasWidth * 0.7f) {
            return true // 可能碰撞，等待更多空间
        }

        return false
    }

    /**
     * 释放弹幕占用的轨道
     */
    fun releaseTrack(danmaku: CanvasDanmakuItem) {
        for (i in tracks.indices) {
            val trackInfo = tracks[i]
            if (trackInfo?.danmaku === danmaku) {
                tracks[i] = null
                break
            }
        }
    }

    /**
     * 清理所有不活跃弹幕占用的轨道
     */
    fun cleanup() {
        for (i in tracks.indices) {
            val trackInfo = tracks[i]
            if (trackInfo?.danmaku?.isActive == false) {
                tracks[i] = null
            }
        }
    }

    /**
     * 获取可用轨道数量
     */
    fun getAvailableTrackCount(): Int {
        return tracks.count { it == null }
    }

    /**
     * 获取总轨道数量
     */
    fun getTotalTrackCount(): Int {
        return maxTracks
    }

    /**
     * 清空所有轨道
     */
    fun clear() {
        tracks.fill(null)
    }
}

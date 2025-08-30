package player.danmaku

import androidx.compose.runtime.mutableStateListOf

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
    
    // 弹幕间距配置
    private var minSafeDistance = 50f // 弹幕之间的最小安全距离（像素）

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


        val preferredTrackOrder = generatePreferredTrackOrder()
        
        for (trackIndex in preferredTrackOrder) {
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
     * 生成优先轨道顺序，从顶部开始向下扩展
     */
    private fun generatePreferredTrackOrder(): List<Int> {
        if (tracks.isEmpty()) return emptyList()
        
        val result = mutableListOf<Int>()
        
        // 从顶部开始（跳过第0轨道，因为与标题栏重叠）
        for (i in 1 until tracks.size) {
            result.add(i)
        }
        
        return result
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


        // 特殊检查：如果两条弹幕的时间戳相同或非常接近，说明是同一批处理的
        // 使用 timeMs 直接对比更准确
        if (newDanmaku.timeMs != null && lastDanmaku.timeMs != null) {
            val timeDiff = kotlin.math.abs(newDanmaku.timeMs - lastDanmaku.timeMs)
            if (timeDiff < 100) { // 时间差小于100ms认为是同时处理的
                // 对于同时处理的弹幕，它们不能在同一轨道
                return false
            }
        }

        // 检查碰撞：计算新弹幕是否会与轨道上的最后一条弹幕碰撞
        return !willCollide(lastDanmaku, newDanmaku, canvasWidth)
    }

    /**
     * 检查两条弹幕是否会发生碰撞
     * 使用安全距离来判断两个弹幕是否会重叠
     */
    private fun willCollide(existingDanmaku: CanvasDanmakuItem, newDanmaku: CanvasDanmakuItem, canvasWidth: Float): Boolean {
        // 获取现有弹幕的当前位置和宽度
        val existingX = existingDanmaku.x
        val existingWidth = existingDanmaku.textWidth
        val existingRightEdge = existingX + existingWidth
        
        // 新弹幕从屏幕右侧开始，需要估算其宽度
        val newDanmakuWidth = estimateTextWidth(newDanmaku.text)
        
        // 如果现有弹幕已经完全移出屏幕，不会碰撞
        if (existingRightEdge <= 0) {
            return false
        }
        
        // 使用安全距离进行简单有效的碰撞检测
        // 如果现有弹幕还在屏幕右侧的安全区域内，认为会碰撞
        val safeDistance = minSafeDistance + newDanmakuWidth
        if (existingX > canvasWidth - safeDistance) {
            return true
        }
        
        return false
    }
    
    
    /**
     * 估算文本宽度（更精确的估算）
     */
    private fun estimateTextWidth(text: String): Float {
        // 更精确的字符宽度估算
        var width = 0f
        for (char in text) {
            width += when {
                char.isWhitespace() -> 8f // 空格
                char in "ilI1!|." -> 8f  // 窄字符
                char in "mwMW" -> 16f // 宽字符
                char.code <= 127 -> 12f // 标准ASCII字符
                else -> 20f // 中文字符（比之前更窄）
            }
        }
        
        // 添加一些边距，避免估算过小
        return width + 10f
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

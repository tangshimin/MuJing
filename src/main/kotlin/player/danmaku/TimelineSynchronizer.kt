package player.danmaku

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import data.MutableVocabulary
import data.VocabularyType
import data.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import player.convertTimeToSeconds
import java.io.File

/**
 * 时间轴同步器
 * 负责将弹幕与媒体播放时间轴同步
 */
class TimelineSynchronizer(
    private val danmakuManager: DanmakuStateManager
) {
    // 弹幕数据列表，按时间排序
    private val timedDanmakuList = mutableListOf<TimedDanmakuData>()

    // 当前处理到的数据索引
    private var currentIndex by mutableStateOf(0)

    // 当前媒体时间（毫秒）
    private var currentTimeMs by mutableStateOf(0L)

    /**
     * 定时弹幕数据类
     */
    data class TimedDanmakuData(
        val timeMs: Long,           // 出现时间（毫秒）
        val text: String,          // 弹幕文本
        val word: Word? = null,    // 关联的单词数据
        val color: Color = Color.White,  // 弹幕颜色
        val type: DanmakuType = DanmakuType.SCROLL
    )

    /**
     * 加载弹幕数据并按时间排序
     */
    fun loadTimedDanmakus(danmakus: List<TimedDanmakuData>) {
        timedDanmakuList.clear()
        timedDanmakuList.addAll(danmakus.sortedBy { it.timeMs })
        currentIndex = 0
    }

    /**
     * 添加单个定时弹幕
     */
    fun addTimedDanmaku(
        timeMs: Long,
        text: String,
        word: Word? = null,
        color: Color = Color.White,
        type: DanmakuType = DanmakuType.SCROLL
    ) {
        val danmaku = TimedDanmakuData(timeMs, text, word, color, type)

        // 找到合适的插入位置以保持排序
        val insertIndex = timedDanmakuList.binarySearch { it.timeMs.compareTo(timeMs) }
        val actualIndex = if (insertIndex < 0) -insertIndex - 1 else insertIndex

        timedDanmakuList.add(actualIndex, danmaku)

        // 如果插入位置在当前索引之前，需要调整当前索引
        if (actualIndex <= currentIndex) {
            currentIndex++
        }
    }

    /**
     * 更新当前媒体时间并触发相应的弹幕
     */
    fun updateTime(timeMs: Long) {
        val oldTime = currentTimeMs
        currentTimeMs = timeMs

        // 检测是否在播放途中打开弹幕
        if (currentIndex == 0 && oldTime != 0L) {
            // 更新索引
            currentIndex = findIndexForTime(timeMs)
            processTimedDanmakus(timeMs)
            return
        }

        // 处理时间跳跃（快进、快退、拖拽进度条）
        if (timeMs < oldTime || timeMs - oldTime > 2000) {
            handleTimeJump(timeMs)
            return
        }

        // 正常播放时的弹幕触发
        processTimedDanmakus(timeMs)
    }

    /**
     * 处理时间跳跃（快进、快退等）
     */
    private fun handleTimeJump(newTimeMs: Long) {
        // 重新定位索引
        currentIndex = findIndexForTime(newTimeMs)

        // 触发当前时间点的弹幕
        processTimedDanmakus(newTimeMs)
    }

    /**
     * 查找指定时间对应的数据索引
     */
    private fun findIndexForTime(timeMs: Long): Int {
        // 使用二分查找找到第一个时间大于等于指定时间的弹幕
        var left = 0
        var right = timedDanmakuList.size

        while (left < right) {
            val mid = (left + right) / 2
            if (timedDanmakuList[mid].timeMs < timeMs) {
                left = mid + 1
            } else {
                right = mid
            }
        }

        return left
    }

    /**
     * 处理定时弹幕
     */
    private fun processTimedDanmakus(timeMs: Long) {
        // 遍历从当前索引开始的弹幕，找到所有应该在当前时间显示的弹幕
        while (currentIndex < timedDanmakuList.size) {
            val danmaku = timedDanmakuList[currentIndex]

            // 如果弹幕时间还没到，停止处理
            if (danmaku.timeMs > timeMs) {
                break
            }

            // 如果屏幕上已经有这个单词弹幕了就不添加
            // 使用 A S D 键播放字幕时会触发多次添加弹幕，这里做个简单去重
            // 因为使用 A S D 添加的单词都是在列表的结尾，所以遍历从后往前
            for (i in danmakuManager.activeDanmakus.size - 1 downTo 0) {
                if (danmakuManager.activeDanmakus[i].text == danmaku.text) {
                    currentIndex++
                    return
                }
            }

            // 触发弹幕显示
            danmakuManager.addDanmaku(
                text = danmaku.text,
                word = danmaku.word,
                color = danmaku.color,
                type = danmaku.type,
                timeMs = danmaku.timeMs
            )

            currentIndex++
        }
    }

    /**
     * 重置同步器状态
     */
    fun reset() {
        currentIndex = 0
        currentTimeMs = 0L
    }

    /**
     * 清空所有弹幕数据
     */
    fun clear() {
        timedDanmakuList.clear()
        currentIndex = 0
        currentTimeMs = 0L
    }

    /**
     * 获取总弹幕数量
     */
    fun getTotalCount(): Int = timedDanmakuList.size

    /**
     * 获取已处理的弹幕数量
     */
    fun getProcessedCount(): Int = currentIndex

    /**
     * 获取当前时间
     */
    fun getCurrentTime(): Long = currentTimeMs

    /**
     * 从词库加载定时弹幕数据
     */
    fun loadTimedDanmakusFromVocabulary(
        videoPath: String,
        vocabularyPath: String,
        vocabulary: MutableVocabulary?
    ) {
        timedDanmakuList.clear()
        currentIndex = 0

        if (vocabulary == null) return

        val vocabularyDir = File(vocabularyPath).parentFile
        val tempDanmakuList = mutableListOf<TimedDanmakuData>()

        // 使用字幕和MKV 生成的词库
        if (vocabulary.type == VocabularyType.MKV || vocabulary.type == VocabularyType.SUBTITLES) {
            val absVideoFile = File(videoPath)
            val relVideoFile = File(vocabularyDir, absVideoFile.name)

            // absVideoFile.exists() 为真 视频文件没有移动，还是词库里保存的地址
            // relVideoFile.exists() 为真 视频文件移动了，词库里保存的地址是旧地址
            if ((absVideoFile.exists() && absVideoFile.absolutePath == vocabulary.relateVideoPath) ||
                (relVideoFile.exists() && relVideoFile.name == File(vocabulary.relateVideoPath).name)
            ) {
                vocabulary.wordList.forEach { word ->
                    if (word.captions.isNotEmpty()) {
                        word.captions.forEach { caption ->
                            val startTimeMs = (convertTimeToSeconds(caption.start) * 1000).toLong()
                            tempDanmakuList.add(
                                TimedDanmakuData(
                                    timeMs = startTimeMs,
                                    text = word.value,
                                    word = word,
                                    color = Color.White,
                                    type = DanmakuType.SCROLL
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // 文档词库，或混合词库
            vocabulary.wordList.forEach { word ->
                word.externalCaptions.forEach { externalCaption ->
                    val absVideoFile = File(videoPath)
                    val relVideoFile = File(vocabularyDir, absVideoFile.name)

                    if ((absVideoFile.exists() && absVideoFile.absolutePath == externalCaption.relateVideoPath) ||
                        (relVideoFile.exists() && relVideoFile.name == File(externalCaption.relateVideoPath).name)
                    ) {
                        val startTimeMs = (convertTimeToSeconds(externalCaption.start) * 1000).toLong()
                        tempDanmakuList.add(
                            TimedDanmakuData(
                                timeMs = startTimeMs,
                                text = word.value,
                                word = word,
                                color = Color.White,
                                type = DanmakuType.SCROLL
                            )
                        )
                    }
                }
            }
        }

        // 按时间排序并加载
        timedDanmakuList.addAll(tempDanmakuList.sortedBy { it.timeMs })
    }
}

/**
 * Compose 函数：监听媒体时间变化并同步弹幕
 */
@Composable
fun rememberTimelineSynchronizer(
    danmakuManager: DanmakuStateManager,
    mediaTimeFlow: Flow<Long>? = null
): TimelineSynchronizer {
    val synchronizer = remember(danmakuManager) {
        TimelineSynchronizer(danmakuManager)
    }

    // 监听媒体时间变化
    LaunchedEffect(mediaTimeFlow) {
        mediaTimeFlow?.distinctUntilChanged()?.collect { timeMs ->
            synchronizer.updateTime(timeMs)
        }
    }

    return synchronizer
}

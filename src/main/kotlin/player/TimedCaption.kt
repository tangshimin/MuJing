package player

import androidx.compose.runtime.*
import data.Caption
import kotlinx.serialization.Serializable

/**
 * 字幕显示界面专用的定时字幕管理器
 *
 * 该类负责管理 data.Caption 列表，为字幕显示界面提供高效的字幕查找和状态管理。
 * 与 VideoPlayerTimedCaption 不同，此类专门为字幕浏览和编辑界面设计。
 *
 * 核心功能：
 * - 管��字幕列表和播放状态跟踪
 * - 提供多重字幕查找方法（支持不同参数组合）
 * - 维护前一个、当前和下一个字幕的缓存
 * - 支持字幕索引���找和状态管理
 *
 * 缓存策略：
 * - 使用 timeMap 进行秒级时间索引优化查找
 * - 缓存前一个、当前、下一个字幕的时间范围
 * - 支持双向导航（前进和后退）
 * - 自动更新缓存状态以提高性能
 *
 * @property captionList 字幕列表，使用 mutableStateListOf 支持 Compose 响应式更新
 * @property timeMap 时间映射表，Key 为秒（Int），Value 为该秒内的字幕信息三元组列表
 * @property previousCaptionStart 前一个字幕的开始时间（毫秒）
 * @property previousCaptionEnd 前一个字幕的结束时间（毫秒）
 * @property captionStart 当前字幕的开始时间（毫秒）
 * @property captionEnd 当前字幕的结束时间（毫秒）
 * @property nextCaptionStart 下一个字幕的开始时间（毫秒）
 * @property nextCaptionEnd 下一个字幕的结束时间（毫秒）
 * @property currentIndex 当前字幕的索引位置
 *
 * 数据结构说明：
 * - timeMap: Int -> List<Triple<Double, Double, Int>>
 *   - Key: 时间戳的秒数部分（Int）
 *   - Value: 三元组列表 (开始秒数, 结束秒数, 字幕索引)
 *
 * 与 VideoPlayerTimedCaption 的区别：
 * - TimedCaption: 使用 data.Caption (时间为String) - 用于字幕显示界面
 * - VideoPlayerTimedCaption: 使用 PlayerCaption (时间为Long) - 用于视频播放器
 * - TimedCaption: 支持更复杂的导航和状态管理
 * - VideoPlayerTimedCaption: 专注于实时播放性能优化
 *
 * @see data.Caption 字幕条目数据类（时间为字符串格式）
 * @see VideoPlayerTimedCaption 视频播放器专用的字幕管理器
 * @see PlayerCaption 播放器专用字幕数据类
 *
 * 示例用法：
 * ```kotlin
 * val timedCaption = remember { TimedCaption() }
 *
 * // 设置字幕列表
 * timedCaption.setCaptionList(captionList)
 *
 * // 在播放过程中获取当前字幕（基础用法）
 * val currentTime = 15500L // 15.5秒
 * val caption = timedCaption.getCaption(currentTime)
 *
 * // 带索引的字幕查找（用于精确控制）
 * val captionWithIndex = timedCaption.getCaption(currentTime, currentIndex)
 *
 * // 获取字幕索引（用于UI状态同步）
 * val newIndex = timedCaption.getCaptionIndex(currentTime, currentIndex)
 * ```
 *
 * 注意事项：
 * - 所有时间参数均以毫秒为单位
 * - 支持三种不同的 getCaption() 重载方法
 * - 自动处理字幕间的导航和状态更新
 * - 适用于字幕浏览界面的复杂交互需求
 * - 线程安全：适用于 Compose 单线程模型
 */
class TimedCaption{
    val captionList =  mutableStateListOf<Caption>()
    /** Map 的 Key 为秒，Triple 的第一个参数是开始，第二个参数是结束，第三个参数是字幕索引 */
    private val timeMap =  mutableStateMapOf<Int,MutableList<Triple<Double,Double,Int>>>()

    private var previousCaptionStart: Long by  mutableStateOf(0)
    private var previousCaptionEnd: Long by  mutableStateOf(0)
    private var captionStart: Long by  mutableStateOf(0)
    private var captionEnd: Long by  mutableStateOf(0)
    private var nextCaptionStart: Long by  mutableStateOf(0)
    private var nextCaptionEnd: Long by  mutableStateOf(0)
    private var currentIndex by mutableStateOf(0)

    /**
     * 设置字幕列表并构建时间映射表
     *
     * 该方法会清空现有数据，重新构建字幕列表和时间索引。
     * 与 VideoPlayerTimedCaption 不同，此方法处理的是 data.Caption 类型，
     * 需要将字符串时间格式转换为数值进行索引。
     *
     * @param captions 要设置的字幕列表（data.Caption 类型）
     *
     * 实现细节：
     * - 自动清理旧数据和缓存状态
     * - 将字符串时间格式转换为秒数进行索引
     * - 为每个字幕建立三元组索引 (开始时间, 结束时间, 索引)
     * - 支持同一秒内多个字幕的情况
     */
    fun setCaptionList(captions:List<Caption>){
        clear()
        captions.forEachIndexed { index, caption ->
            captionList.add(caption)
            val start = convertTimeToSeconds(caption.start)
            val end = convertTimeToSeconds(caption.end)
            val startInt = start.toInt()
            val list = timeMap[startInt] ?: mutableListOf()
            list.add(Triple(start,end,index))
            timeMap[startInt] = list
        }
    }

    /**
     * 根据时间和当前索引获取字幕（精确控制版本）
     *
     * 这是最复杂的字幕查找方法，支持精确的状态管理和双向导航。
     * 通过维护前一个、当前、下一个字幕的缓存来优化性能。
     *
     * @param newTime 当前播放时间（毫秒）
     * @param currentIndex 当前字幕的索引位置
     * @return 匹配的字幕对象，如果没有匹配则返回 null
     *
     * 查找策略：
     * 1. 检查当前字幕缓存时间范围
     * 2. 检查下一个字幕缓存时间范围（前进播放）
     * 3. 检查前一个字幕缓存时间范围（后退播放）
     * 4. 通过时间映射表进行全局查找
     *
     * 性能特点：
     * - 顺序播放时使用缓存，时间复杂度 O(1)
     * - 跳跃播放时通过映射表查找，时间复杂度 O(k)
     * - 自动更新前一个、当前、下一个字幕的缓存
     * - 支持双向导航优化
     */
    fun getCaption( newTime:Long,currentIndex: Int): Caption?{
        when (newTime) {
            in captionStart..captionEnd -> {
                return captionList[currentIndex]
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                previousCaptionStart = convertTimeToSeconds(captionList[index - 1].start).times(1000).toLong()
                previousCaptionEnd = convertTimeToSeconds(captionList[index - 1].end).times(1000).toLong()
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    nextCaptionStart = convertTimeToSeconds(captionList[index + 1].start).times(1000).toLong()
                    nextCaptionEnd = convertTimeToSeconds(captionList[index + 1].end).times(1000).toLong()
                }

                return captionList[index]
            }
            in previousCaptionStart..previousCaptionEnd -> {
                val index = currentIndex - 1

                captionStart = previousCaptionStart
                captionEnd = previousCaptionEnd

                previousCaptionStart = convertTimeToSeconds(captionList[index - 1].start).times(1000).toLong()
                previousCaptionEnd = convertTimeToSeconds(captionList[index - 1].end).times(1000).toLong()

                if(index + 1 < captionList.size){
                    nextCaptionStart = convertTimeToSeconds(captionList[index + 1].start).times(1000).toLong()
                    nextCaptionEnd = convertTimeToSeconds(captionList[index + 1].end).times(1000).toLong()
                }

                return captionList[index]
            }
            else -> {
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        previousCaptionStart = convertTimeToSeconds(captionList[it.third - 1].start).times(1000).toLong()
                        previousCaptionEnd = convertTimeToSeconds(captionList[it.third - 1].end).times(1000).toLong()
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            nextCaptionStart = convertTimeToSeconds(captionList[it.third + 1].start).times(1000).toLong()
                            nextCaptionEnd = convertTimeToSeconds(captionList[it.third + 1].end).times(1000).toLong()
                        }
                        return captionList[it.third]
                    }
                }
            }
        }
        return null
    }

    /**
     * 根据时间获取字幕（基础版本）
     *
     * 这是简化版的字幕查找方法，自动管理当前索引状态。
     * 适用于不需要精确索引控制的场景。
     *
     * @param newTime 当前播放时间（毫秒）
     * @return 匹配的字幕对象，如果没有匹配则返回 null
     *
     * 查找策略：
     * 1. 检查当前字幕缓存
     * 2. 检查下一个字幕缓存
     * 3. 通过时间映射表进行全局查找并自动更新状态
     *
     * 调试信息：
     * - 输出 "current" 表示使用当前缓存
     * - 输出 "next" 表示切换到下一个字幕
     */
    fun getCaption( newTime:Long): Caption?{
        // newTime 是毫秒
        when (newTime) {
            in captionStart..captionEnd -> {
                println("current")
                return captionList[currentIndex]
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    val nextCaption = captionList[index + 1]
                    nextCaptionStart = convertTimeToSeconds(nextCaption.start).times(1000).toLong()
                    nextCaptionEnd = convertTimeToSeconds(nextCaption.end).times(1000).toLong()
                }
                currentIndex = index
                println("next")
                return captionList[index]
            }
            else -> {
                // time 是秒
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            val nextCaption = captionList[it.third + 1]
                            nextCaptionStart = convertTimeToSeconds(nextCaption.start).times(1000).toLong()
                            nextCaptionEnd = convertTimeToSeconds(nextCaption.end).times(1000).toLong()
                        }
                        currentIndex = it.third
                        return captionList[it.third]
                    }
                }
            }
        }
        return null
    }

    /**
     * 根据时间和当前字幕获取字幕（优化版本）
     *
     * 这是优化版的字幕查找方法，当时间在当前字幕范围内时直接返回传入的字幕对象，
     * 避免重复查找和对象创建。
     *
     * @param newTime 当前播放时间（毫秒）
     * @param currentCaption 当前正在显示的字幕对象
     * @return 匹配的字幕对象，如果没有匹配则返回 null
     *
     * 优化策略：
     * - 时间在当前范围内时直接返回传入的字幕对象
     * - 只有当时间超出当前范围时才进行查找
     * - 减少不必要的对象查找和状态更新
     */
    fun getCaption( newTime:Long,currentCaption: Caption?): Caption?{

        when (newTime) {
            in captionStart..captionEnd -> {
                return currentCaption
            }
            else ->{
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        previousCaptionStart = convertTimeToSeconds(captionList[it.third - 1].start).times(1000).toLong()
                        previousCaptionEnd = convertTimeToSeconds(captionList[it.third - 1].end).times(1000).toLong()
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            nextCaptionStart = convertTimeToSeconds(captionList[it.third + 1].start).times(1000).toLong()
                            nextCaptionEnd = convertTimeToSeconds(captionList[it.third + 1].end).times(1000).toLong()
                        }
                        return captionList[it.third]
                    }
                }
            }
        }

        return null
    }

    /**
     * 根据时间和当前索引获取字幕索引
     *
     * 该方法用于获取指定时间对应的字幕索引，主要用于UI状态同步。
     * 与 getCaption 方法类似，但只返回索引而不返回字幕对象。
     *
     * @param newTime 当前播放时间（毫秒）
     * @param currentIndex 当前字幕的索引位置
     * @return 匹配的字幕索引，如果没有匹配则返回传入的 currentIndex
     *
     * 用途：
     * - UI 状态同步（如高亮当前字幕行）
     * - 字幕导航控制
     * - 播放进度与字幕位置的同步
     *
     * 查找策略：
     * - 与 getCaption(Long, Int) 方法采用相同的查找逻辑
     * - 自动更新缓存状态
     * - 优先使用缓存以提高性能
     */
    fun getCaptionIndex(
        newTime:Long,
        currentIndex: Int,
    ):Int{
        when (newTime) {
            in captionStart..captionEnd -> {
                return currentIndex
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    val nextCaption = captionList[index + 1]
                    nextCaptionStart = convertTimeToSeconds(nextCaption.start).times(1000).toLong()
                    nextCaptionEnd = convertTimeToSeconds(nextCaption.end).times(1000).toLong()
                }
                return index
            }
            else -> {
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    val index = it.third
                    if(newTime in start..end){
                        captionStart = start
                        captionEnd = end
                        if(index + 1 < captionList.size){
                            val nextCaption = captionList[index + 1]
                            nextCaptionStart = convertTimeToSeconds(nextCaption.start).times(1000).toLong()
                            nextCaptionEnd = convertTimeToSeconds(nextCaption.end).times(1000).toLong()
                        }
                        return index
                    }
                }
            }
        }

        return currentIndex
    }

    /**
     * 检查字幕列表是否为空
     * @return true 如果没有字幕，false 否则
     */
    fun isEmpty():Boolean{
        return captionList.isEmpty()
    }

    /**
     * 检查字幕列表是否不为空
     * @return true 如果有字幕，false 否则
     */
    fun isNotEmpty():Boolean{
        return captionList.isNotEmpty()
    }

    /**
     * 清空所有字幕数据和缓存状态
     *
     * 该方法会重置所有状态和缓存：
     * - 清空字幕列表
     * - 清空时间映射表
     * - 重置所有时间缓存（前一个、当前、下一个）
     * - 重置索引位置
     *
     * 适用场景：
     * - 切换字幕文件时
     * - 内存清理时
     * - 重新加载字幕时
     * - 界面重置时
     */
    fun clear(){
        timeMap.clear()
        captionList.clear()
        captionStart = 0
        captionEnd = 0
        nextCaptionStart = 0
        nextCaptionEnd = 0
    }
}

@Composable
fun rememberTimedCaption():TimedCaption = remember{
    TimedCaption()
}


/**
 * 播放器专用的字幕条目数据类
 *
 * 与 data.Caption 不同，PlayerCaption 专门为视频播放器设计，
 * 使用毫秒级的时间戳以便于与播放器时间精确同步。
 *
 * @property start 字幕开始时间（毫秒）
 * @property end 字幕结束时间（毫秒）
 * @property content 字幕文本内容
 *
 * 时间格式说明：
 * - 时间以毫秒为单位存储（Long类型）
 * - 便于与播放器的时间进度直接比较
 * - 避免了字符串时间格式的转换开销
 *
 * 与 data.Caption 的区别：
 * - PlayerCaption: 时间为 Long (毫秒) - 用于播放器内部计算
 * - data.Caption: 时间为 String ("hh:mm:ss,ms") - 用于UI显示
 *
 * @see data.Caption UI显示用的字幕数据类
 * @see VideoPlayerTimedCaption 管理PlayerCaption列表的容器类
 *
 * 示例用法：
 * ```kotlin
 * val caption = PlayerCaption(
 *     start = 5000L,  // 5秒开始
 *     end = 8000L,    // 8秒结束
 *     content = "Hello World"
 * )
 *
 * // 检查当前时间是否在字幕时间范围内
 * val currentTime = 6500L
 * if (currentTime in caption.start..caption.end) {
 *     println(caption.content) // 输出: Hello World
 * }
 * ```
 */
@Serializable
data class PlayerCaption(var start: Long, var end: Long, var content: String) {
    override fun toString(): String {
        return content
    }
}

/**
 * 视频播放器专用的定时字幕管理器
 *
 * 该类负责管理 PlayerCaption 列表，提供高效的字幕查找和缓存机制。
 * 专门为视频播放器设计，优化了实时字幕显示的性能。
 *
 * 核心功能：
 * - 管理字幕列表和当前播放状态
 * - 提供基于时间的快速字幕查找
 * - 维护当前和下一个字幕的缓存以提高性能
 * - 使用时间映射表优化查找效率
 *
 * 性能优化策略：
 * - 使用 timeMap 进行 O(1) 时间复杂度的快速查找
 * - 缓存当前和下一个字幕以减少重复计算
 * - 支持 Compose 的响应式更新机制
 *
 * @property captionList 字幕列表，使用 mutableStateListOf 支持 Compose 响应式更新
 * @property currentIndex 当前字幕的索引位置
 * @property timeMap 时间映射表，Key 为秒（Long），Value 为该秒内的字幕列表
 * @property currentCaption 当前正在显示的字幕缓存
 * @property nextCaption 下一个即将显示的字幕缓存
 *
 * 数据结构说明：
 * - timeMap: Long -> List<Pair<PlayerCaption, Int>>
 *   - Key: 时间戳的秒数部分
 *   - Value: 该秒内开始的字幕和其索引的配对列表
 *
 * @see PlayerCaption 字幕条目数据类
 * @see TimedCaption 另一个字幕管理器（使用 data.Caption）
 *
 * 示例用法：
 * ```kotlin
 * val timedCaption = remember { VideoPlayerTimedCaption() }
 *
 * // 设置字幕列表
 * timedCaption.setCaptionList(captionList)
 *
 * // 在播放过程中获取当前字幕
 * val currentTime = 15500L // 15.5秒
 * val subtitleText = timedCaption.getCaption(currentTime)
 * if (subtitleText.isNotEmpty()) {
 *     Text(text = subtitleText)
 * }
 * ```
 *
 * 注意事项：
 * - 所有时间参数均以毫秒为单位
 * - getCaption() 方法具有 260ms 的已知延迟问题（TODO待修复）
 * - 线程安全：适用于 Compose 单线程模型
 * - 内存管理：大量字幕时注意及时调用 clear() 清理资源
 */
class VideoPlayerTimedCaption{
    val captionList =  mutableStateListOf<PlayerCaption>()
    var currentIndex by mutableStateOf(0)
    /** Map 的 Key 为秒，Pair 第一个参数是字幕，第二个参数是字幕索引 */
    private val timeMap =  mutableStateMapOf<Long,MutableList<Pair<PlayerCaption,Int>>>()
    private var currentCaption: PlayerCaption by  mutableStateOf(PlayerCaption(0,0,""))
    private var nextCaption: PlayerCaption by  mutableStateOf(PlayerCaption(0,0,""))


    /**
     * 设置字幕列表并构建时间映射表
     *
     * 该方法会清空现有数据，重新构建字幕列表和时间索引。
     * 时间映射表以秒为单位进行索引，提高查找效率。
     *
     * @param captions 要设置的字幕列表
     *
     * 实现细节：
     * - 自动清理旧数据
     * - 为每个字幕建立时间索引（按开始时间的秒数）
     * - 支持同一秒内多个字幕的情况
     */
    fun setCaptionList(captions:List<PlayerCaption>){
        clear()
        captions.forEachIndexed { index, caption ->
            captionList.add(caption)
            val start = caption.start.toDouble().div(1000)
            val startInt = start.toLong()
            val list = timeMap[startInt] ?: mutableListOf()
            list.add(Pair(caption,index))
            timeMap[startInt] = list
        }
    }

    /**
     * 根据当前播放时间获取对应的字幕内容
     *
     * 该方法使用三级查找策略优化性能：
     * 1. 首先检查缓存的当前字幕
     * 2. 然后检查缓存的下一个字幕
     * 3. 最后通过时间映射表进行全局查找
     *
     * @param newTime 当前播放时间（毫秒）
     * @return 当前时间对应的字幕内容，如果没有字幕则返回空字符串
     *
     * 性能特点：
     * - 大部分情况下使用缓存，时间复杂度 O(1)
     * - 跳跃播放时通过时间映射表查找，时间复杂度 O(k)，k为同一秒内的字幕数量
     * - 自动更新当前和下一个字幕的缓存
     *
     * 已知问题：
     * - TODO: 存在 260 毫秒的延迟，原因待查
     *
     */
    fun getCaption( newTime:Long): String{
        // newTime 是毫秒
        when (newTime) {
            // TODO  有 260 毫秒的延迟，还没有找到原因
            in currentCaption.start..currentCaption.end -> {
                return currentCaption.content
            }
            in nextCaption.start..nextCaption.end -> {
                currentCaption.start = nextCaption.start
                currentCaption.end = nextCaption.end
                currentCaption.content = nextCaption.content

                currentIndex += 1

                val nextIndex = currentIndex + 1
                if (nextIndex < captionList.size) {
                    nextCaption = captionList[nextIndex]
                }

                return currentCaption.content
            }
            else -> {
                // time 是秒
                val time = newTime / 1000
                val list = timeMap[time]
                list?.forEach {
                    val start = it.first.start
                    val end = it.first.end
                    if(newTime in start..end){
                        currentIndex = it.second
                        currentCaption.start = it.first.start
                        currentCaption.end = it.first.end
                        currentCaption.content = it.first.content

                        if(it.second + 1 < captionList.size){
                            val next = captionList[it.second + 1]
                            nextCaption = next
                        }
                        return currentCaption.content
                    }
                }
            }
        }
        return ""
    }
    fun getPreviousCaptionTime(): Long {
        if (currentIndex <= 0 || captionList.isEmpty()) return -1
        val previousIndex = currentIndex - 1
        return if (previousIndex >= 0) captionList[previousIndex].start else -1
    }

    fun getNextCaptionTime(): Long {
        if (currentIndex >= captionList.size - 1 || captionList.isEmpty()) return -1
        val nextIndex = currentIndex + 1
        return if (nextIndex < captionList.size) captionList[nextIndex].start else -1
    }

    /**
     * 检查字幕列表是否为空
     * @return true 如果没有字幕，false 否则
     */
    fun isEmpty():Boolean{
        return captionList.isEmpty()
    }

    /**
     * 检查字幕列表是否不为空
     * @return true 如果有字幕，false 否则
     */
    fun isNotEmpty():Boolean{
        return captionList.isNotEmpty()
    }

    /**
     * 清空所有字幕数据和缓存
     *
     * 该方法会重置所有状态：
     * - 清空字幕列表
     * - 清空时间映射表
     * - 重置当前和下一个字幕缓存
     * - 重置索引位置
     *
     * 适用场景：
     * - 切换视频文件时
     * - 内存清理时
     * - 重新加载字幕时
     */
    fun clear(){
        timeMap.clear()
        captionList.clear()
        currentCaption = PlayerCaption(0,0,"")
        nextCaption = PlayerCaption(0,0,"")
    }
}

@Composable
fun rememberPlayerTimedCaption():VideoPlayerTimedCaption = remember{
    VideoPlayerTimedCaption()
}
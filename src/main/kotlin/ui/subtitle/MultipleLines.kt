package ui.subtitle

import androidx.compose.runtime.*

/**
 * 跟读的时候，播放多条字幕
 */
class MultipleLines{

    /** 启动 */
    var enabled by mutableStateOf(false)

    /** 开始索引 */
    var startIndex by mutableStateOf(0)

    /** 结束索引 */
    var endIndex by mutableStateOf(0)

    /** 开始时间 */
    var startTime by mutableStateOf("")

    /** 结束时间 */
    var endTime by mutableStateOf("")

    /** 播放器的位置是否向上偏移,
     * 播放多条字幕要选择两个索引
     * 如果后选择的是开始索引，就向上偏移
     * 如果后现在的是结束索引，就向下偏移 */
    var isUp by mutableStateOf(false)
}

@Composable
fun rememberMultipleLines(): MultipleLines = remember{
    MultipleLines()
}
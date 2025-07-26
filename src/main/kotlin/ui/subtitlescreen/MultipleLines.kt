package ui.subtitlescreen

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
}

@Composable
fun rememberMultipleLines(): MultipleLines = remember{
    MultipleLines()
}
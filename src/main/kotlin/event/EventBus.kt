package event


import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventBus {
    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    suspend fun post(event: Any) = _events.emit(event)
}

/**
 * 播放器的键盘事件类型
 */
enum class PlayerEventType {
    PLAY, // 播放
    ESC, // 退出全屏
    FULL_SCREEN, // 全屏
    CLOSE_PLAYER, // 关闭播放器
    DIRECTION_LEFT, // 左方向键
    DIRECTION_RIGHT, // 右方向键
    DIRECTION_UP, // 上方向键
    DIRECTION_DOWN, // 下方向键
    PREVIOUS_CAPTION, // 上一句字幕
    NEXT_CAPTION, // 下一句字幕
    REPEAT_CAPTION,// 重复字幕
}
package event


import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventBus {
    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    suspend fun post(event: Any) = _events.emit(event)
}

/**
 * 播放器事件类型
 */
enum class PlayerEventType {
    PLAY, // 播放
    ESC, // 退出
}
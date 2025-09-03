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
    AUTO_PAUSE, // 自动暂停
    OPEN_SEARCH,// 打开搜索
    TOGGLE_FIRST_CAPTION, // 隐藏-显示第一语言字幕
    TOGGLE_SECOND_CAPTION,// 隐藏-显示第二语言字幕
}

/**
 * 记忆单词界面的键盘事件类型
 */
enum class WordScreenEventType {
    NEXT_WORD, // 下一个单词
    PREVIOUS_WORD, // 上一个单词
    OPEN_SIDEBAR, // 打开侧边栏
    SHOW_WORD, // 显示单词
    SHOW_PRONUNCIATION, // 显示音标
    SHOW_LEMMA, // 显示词形
    SHOW_DEFINITION, // 显示英文释义
    SHOW_TRANSLATION, // 显示中文释义
    SHOW_SENTENCES, // 显示例句
    SHOW_SUBTITLES, // 显示字幕
    PLAY_AUDIO, // 播放音频
    OPEN_SEARCH, // 打开搜索
    OPEN_VOCABULARY, // 打开词库
    DELETE_WORD, // 删除单词
    ADD_TO_FAMILIAR, // 加入熟词库
    ADD_TO_DIFFICULT, // 加入困难词库
    COPY_WORD, // 复制单词
    PLAY_FIRST_CAPTION, // 播放第一句字幕
    PLAY_SECOND_CAPTION, // 播放第二句字幕
    PLAY_THIRD_CAPTION, // 播放第三句字幕
    FOCUS_ON_WORD, // 聚焦到单词
}
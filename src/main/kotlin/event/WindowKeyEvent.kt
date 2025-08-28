package event

import androidx.compose.ui.input.key.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.PlayerState
import player.isMacOS
import state.AppState
import state.ScreenType

/**
 * 处理窗口的键盘事件
 */
@OptIn(ExperimentalSerializationApi::class)
fun handleWindowKeyEvent(
    it: KeyEvent,
    eventBus: EventBus,
    appState: AppState,
    playerState: PlayerState,
    scope: CoroutineScope,
): Boolean {

    val isModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
    if (isModifierPressed && it.key == Key.Comma && it.type == KeyEventType.KeyUp) {
        appState.openSettings = true
        true
    }

    // 处理视频播放器的快捷键
    if(playerState.visible){
        when {
            it.key == Key.Spacebar && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.PLAY)
                }
                true
            }
            it.key == Key.Escape && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.ESC)
                }
                true
            }
            isModifierPressed && it.key == Key.F && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.OPEN_SEARCH)
                }
                true
            }
            it.key == Key.F && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.FULL_SCREEN)
                }

                true
            }
            isModifierPressed && it.key == Key.W && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.CLOSE_PLAYER)
                }
                true
            }
            it.key == Key.DirectionLeft && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.DIRECTION_LEFT)
                }
                true
            }
            it.key == Key.DirectionRight && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.DIRECTION_RIGHT)
                }
                true
            }
            it.key == Key.A && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.PREVIOUS_CAPTION)
                }
                true
            }
            it.key == Key.S && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.REPEAT_CAPTION)
                }
                true
            }
            it.key == Key.D && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.NEXT_CAPTION)
                }
                true
            }
            it.key == Key.P && it.type == KeyEventType.KeyUp -> {
                scope.launch {
                    eventBus.post(PlayerEventType.AUTO_PAUSE)
                }
                true
            }
        }
        false

    }else if(appState.global.type == ScreenType.WORD){
        // 处理记忆单词界面的快捷键
        val isModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
        val isDeleteModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isShiftPressed
        val deleteKey = if(isMacOS()) Key.Backspace else Key.Delete
        when {
            (isModifierPressed && it.isShiftPressed && it.key == Key.A && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.FOCUS_ON_WORD)
                }
                true
            }
            (isModifierPressed && it.key == Key.C && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.COPY_WORD)
                }
                true
            }
            (isModifierPressed && it.key == Key.E && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_DEFINITION)
                }
                true
            }
            (isModifierPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.OPEN_SEARCH)
                }
                true
            }
            (isModifierPressed && it.key == Key.O && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.OPEN_VOCABULARY)
                }
                true
            }
            (isModifierPressed && it.key == Key.P && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_PRONUNCIATION)
                }
                true
            }
            (isModifierPressed && it.key == Key.L && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_LEMMA)
                }
                true
            }
            (isModifierPressed && (if(isMacOS()) it.key == Key.R else it.key == Key.H) && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_SENTENCES)
                }
                true
            }
            (isModifierPressed && it.key == Key.K && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_TRANSLATION)
                }
                true
            }
            (isModifierPressed && it.key == Key.V && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_WORD)
                }
                true
            }
            (isModifierPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PLAY_AUDIO)
                }
                true
            }
            ((isModifierPressed &&  (if(isMacOS()) it.isCtrlPressed else it.isAltPressed )) && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.OPEN_SIDEBAR)
                }
                true
            }
            (isModifierPressed && it.key == Key.S && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.SHOW_SUBTITLES)
                }
                true
            }
            (isModifierPressed && it.key == Key.I && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.ADD_TO_DIFFICULT)
                }
                true
            }
            (isModifierPressed && it.key == Key.Y && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.ADD_TO_FAMILIAR)
                }
                true
            }
            (isDeleteModifierPressed && it.key == deleteKey && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.DELETE_WORD)
                }
                true
            }
            (isModifierPressed &&  it.key == Key.One && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PLAY_FIRST_CAPTION)
                }
                true
            }
            (isModifierPressed && it.key == Key.Two && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PLAY_SECOND_CAPTION)
                }
                true
            }
            (isModifierPressed &&  it.key == Key.Three && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PLAY_THIRD_CAPTION)
                }
                true
            }
            (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PREVIOUS_WORD)
                }
                true
            }
            ((it.key == Key.DirectionRight || it.key == Key.NumPadEnter || it.key == Key.PageDown) && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.NEXT_WORD)
                }
                true
            }
            ((it.key == Key.PageUp || it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                scope.launch {
                    eventBus.post(WordScreenEventType.PREVIOUS_WORD)
                }

                true
            }
            else -> false
        }
    }else{
        false
    }


    return false
}


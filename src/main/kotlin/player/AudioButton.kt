package player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import theme.LocalCtrl
import tts.AzureTTS
import ui.wordscreen.WordScreenState
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

val LocalAudioPlayerComponent = staticCompositionLocalOf<AudioPlayerComponent> {
    error("LocalMediaPlayerComponent isn't provided")
}

@Composable
fun rememberAudioPlayerComponent(): AudioPlayerComponent = remember {
    // 防止字幕描述在 Windows 乱码
    System.setProperty("native.encoding", "UTF-8")
    embeddedVLCDiscovery()
    AudioPlayerComponent()
}

/** 记忆单词界面的播放按钮
 * @param volume 音量
 * @param isPlaying 是否正在播放单词发音
 * @param setIsPlaying 设置是否正在播放单词发音
 * @param pronunciation 音音 或 美音
 * @param paddingTop 顶部填充
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioButton(
    audioSet:Set<String>,
    addToAudioSet:(String) -> Unit,
    word:String,
    volume: Float,
    isPlaying: Boolean,
    setIsPlaying: (Boolean) -> Unit,
    pronunciation: String,
    azureTTS: AzureTTS,
    playTimes: Int,
    paddingTop: Dp,
) {
    if (playTimes != 0) {
        val scope = rememberCoroutineScope()
        val audioPlayerComponent = LocalAudioPlayerComponent.current

        // 添加防抖状态
        var lastAutoPlayTime by remember { mutableStateOf(0L) }

        val playAudio = {
            val audioPath = getAudioPath(
                word = word,
                audioSet = audioSet,
                addToAudioSet = addToAudioSet,
                pronunciation = pronunciation,
                azureTTS = azureTTS
            )
            playAudio(
                word,
                audioPath,
                pronunciation = pronunciation,
                volume,
                audioPlayerComponent,
                changePlayerState = setIsPlaying,
                )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Min)){
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shortcutText = if (isMacOS()) "$ctrl J" else "$ctrl+J"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "朗读发音  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText)
                            }
                        }
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.CenterEnd,
                    alignment = Alignment.CenterEnd,
                    offset = DpOffset.Zero
                ),
            ) {
                val tint by animateColorAsState(if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground)
                IconToggleButton(
                    checked = isPlaying,
                     modifier = Modifier.padding(top = paddingTop),
                    onCheckedChange = {
                        if (!isPlaying) {
                            scope.launch (Dispatchers.IO){
                                playAudio()
                            }
                        }
                    }) {
                    Crossfade(isPlaying) { isPlaying ->
                        if (isPlaying) {
                            Icon(
                                Icons.Filled.VolumeUp,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        } else {
                            Icon(
                                Icons.Filled.VolumeDown,
                                contentDescription = "Localized description",
                                tint = tint
                            )
                        }
                    }

                }
            }
        }

        LaunchedEffect(word) {
            val currentTime = System.currentTimeMillis()
            // 防抖：500ms内不自动播放
            if (!isPlaying && currentTime - lastAutoPlayTime > 500) {
                lastAutoPlayTime = currentTime
                scope.launch(Dispatchers.IO) {
                    playAudio()
                }
            }
        }
    }

}

/**
 * 搜索界面的播放按钮
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSerializationApi::class)
@Composable
fun AudioButton(
    word: Word,
    state:AppState,
    wordScreenState: WordScreenState,
    volume: Float,
    pronunciation: String,
    azureTTS: AzureTTS,
) {
    val scope = rememberCoroutineScope()
    val audioPlayerComponent = LocalAudioPlayerComponent.current
    var isPlaying by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val playAudio = {
        val currentTime = System.currentTimeMillis()
        // 防抖，500ms内不重复执行
        if(currentTime - lastClickTime> 500){
            val audioPath = getAudioPath(
                word = word.value,
                audioSet = state.localAudioSet,
                addToAudioSet = {state.localAudioSet.add(it)},
                pronunciation = wordScreenState.pronunciation,
                azureTTS = azureTTS
            )
            playAudio(
                word.value,
                audioPath,
                pronunciation = pronunciation,
                volume,
                audioPlayerComponent,
                changePlayerState = { isPlaying = it },
            )
        }

    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(48.dp)
            .width(IntrinsicSize.Max)
    ) {
        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    val ctrl = LocalCtrl.current
                    val shortcutText = if (isMacOS()) "$ctrl J" else "$ctrl+J"
                    Row(modifier = Modifier.padding(10.dp)){
                        Text(text = "朗读发音  " )
                        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                            Text(text = shortcutText)
                        }
                    }
                }
            },
            delayMillis = 300,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.CenterEnd,
                alignment = Alignment.CenterEnd,
                offset = DpOffset.Zero
            ),
        ) {
            val tint by animateColorAsState(if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground)
            IconToggleButton(
                checked = isPlaying,
                onCheckedChange = {
                    if (!isPlaying) {
                        scope.launch (Dispatchers.IO){
                            playAudio()
                        }
                    }
                }) {
                Crossfade(isPlaying) { isPlaying ->
                    if (isPlaying) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Localized description",
                            tint = tint
                        )
                    } else {
                        Icon(
                            Icons.Filled.VolumeDown,
                            contentDescription = "Localized description",
                            tint = tint
                        )
                    }
                }

            }
        }
    }

}





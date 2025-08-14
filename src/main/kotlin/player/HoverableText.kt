package player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.Dictionary
import player.danmaku.DisplayMode
import player.danmaku.WordDetail

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HoverableText(
    text: String,
    playerState: PlayerState,
    playAudio:(String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isInPopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(modifier = Modifier) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.h4,
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) {
                    expanded = true
                }
                .onPointerEvent(PointerEventType.Exit) {
                    if (!isInPopup) {
                        expanded = false
                    }
                }
        )

        if (expanded) {
            val dictWord = Dictionary.query(text.lowercase().trim())

            Popup(
                alignment = Alignment.TopCenter,
                offset = with(density) { IntOffset(0, -350.dp.toPx().toInt()) },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Column( modifier = Modifier
                    .size(400.dp, 353.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        isInPopup = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        isInPopup = false
                        expanded = false
                    }){
                    Box(modifier = Modifier.size(400.dp, 350.dp),) {
                        if(dictWord != null){
                            WordDetail(
                                word =dictWord ,
                                displayMode = DisplayMode.DICT,
                                playerState = playerState,
                                pointerExit = {},
                                height = 350.dp,
                                deleteWord = {},
                                addToFamiliar = {},
                                playAudio =playAudio ,
                            )
                        }else{
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colors.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "内置词典里面没有 \"$text\"",
                                    style = MaterialTheme.typography.body1,
                                    color = Color.White
                                )
                            }
                        }

                    }
                    Box(Modifier.height(3.dp).width(400.dp).background(Color.Transparent))
                }
            }
        }
    }
}


@Composable
fun HoverableCaption(
    caption: String,
    playAudio: (String) -> Unit,
    playerState: PlayerState,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        caption.split("\n").forEach { line ->
            Row {
                line.split(" ").forEachIndexed { index, word ->
                    val cleanWord = word.filter { it.isLetter() && it.code < 128 } // 只保留ASCII字母
                    val otherChars = word.filter { !(it.isLetter() && it.code < 128) }

                    if (cleanWord.isNotEmpty()) {
                        HoverableText(
                            text = cleanWord,
                            playAudio = playAudio,
                            playerState = playerState,
                            modifier = Modifier
                        )
                    }
                    if (otherChars.isNotEmpty()) {
                        Text(otherChars,  color = Color.White,style = MaterialTheme.typography.h4)
                    }
                    if (index < line.split(" ").size - 1) {
                        Text(" ",  color = Color.White,style = MaterialTheme.typography.h4)
                    }
                }
            }
        }
    }
}

package ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import player.isMacOS

/**
 *
 * 关闭按钮，关闭打开的词库，字幕和文本
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RemoveButton(
    toolTip:String,
    onClick: () -> Unit
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(text = toolTip, modifier = Modifier.padding(10.dp))
            }
        },
        delayMillis = 50,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.BottomCenter,
            alignment = Alignment.BottomCenter,
            offset = DpOffset.Zero
        )
    ) {
        val color = MaterialTheme.colors.onBackground
        var tint by remember(color){ mutableStateOf(color) }
        IconButton(
            onClick = onClick,
            modifier = Modifier.padding(top = if (isMacOS()) 30.dp else 0.dp)
                .onPointerEvent(PointerEventType.Enter){
                    tint = Color.Red
                }
                .onPointerEvent(PointerEventType.Exit){
                    tint = color
                }
        ) {
            Icon(
                Icons.Filled.HighlightOff,
                contentDescription = "Localized description",
                tint = tint
            )
        }
    }
}
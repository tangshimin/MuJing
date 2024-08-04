package ui.subtitleScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Caption
import java.awt.Point
import androidx.compose.ui.geometry.Rect
import java.awt.Rectangle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayButton(
    caption: Caption,
    isPlaying: Boolean,
    playCaption:(Caption) ->Unit,
    textFieldRequester: FocusRequester,
    isVideoBoundsChanged: Boolean,
    videoPlayerBounds: Rectangle,
    textRect: Rect,
    window: ComposeWindow,
    playerPoint1: Point,
    adjustPosition: (Float, Rectangle) -> Unit,
    mediaType: String,
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                ),
                shape = RectangleShape
            ) {
                Row(modifier = Modifier.padding(10.dp)){
                    Text(text = "播放" )
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " Tab")
                    }
                }

            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        val density = LocalDensity.current.density
        IconButton(onClick = {
            playCaption(caption)
            textFieldRequester.requestFocus()
        },
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val rect = coordinates.boundsInWindow()
                    if(!isVideoBoundsChanged){
                        if(!rect.isEmpty){
                            // 视频播放按钮没有被遮挡
                            videoPlayerBounds.x = window.x + rect.left.toInt() + (48 * density).toInt()
                            videoPlayerBounds.y = window.y + rect.top.toInt() - (100 * density).toInt()
                        }else{
                            // 视频播放按钮被遮挡
                            videoPlayerBounds.x = window.x + textRect.right.toInt()
                            videoPlayerBounds.y = window.y + textRect.top.toInt() - (100 * density).toInt()
                        }
                        // 根据一些特殊情况调整播放器的位置， 比如显示器缩放，播放器的位置超出屏幕边界。
                        adjustPosition(density, videoPlayerBounds)
                    }else{
                        if(!rect.isEmpty){
                            // 视频播放按钮没有被遮挡
                            playerPoint1.x = window.x + rect.left.toInt() + (48 * density).toInt()
                            playerPoint1.y = window.y + rect.top.toInt() - (100 * density).toInt()
                        }else{
                            // 视频播放按钮被遮挡
                            playerPoint1.x = window.x + textRect.right.toInt()
                            playerPoint1.y = window.y + textRect.top.toInt() - (100 * density).toInt()
                        }
                    }

                }
        ) {
            val icon = if(mediaType=="audio" && !isPlaying) {
                Icons.Filled.VolumeDown
            } else if(mediaType=="audio"){
                Icons.Filled.VolumeUp
            }else Icons.Filled.PlayArrow

            Icon(
                icon,
                contentDescription = "播放按钮",
                tint = if(isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
            )
        }

    }
}
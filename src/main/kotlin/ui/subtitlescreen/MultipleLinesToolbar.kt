package ui.subtitlescreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Caption
import java.awt.Point
import java.awt.Rectangle

/**
 * 多行字幕工具栏,有一个固定的宽度，包含退出和播放按钮，点击数字按钮后启用
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultipleLinesToolbar(
    index: Int,
    isPlaying: Boolean,
    playIconIndex: Int,
    multipleLines: MultipleLines,
    mediaType: String,
    cancel:() -> Unit,
    selectAll:() -> Unit,
    playCaption:(Caption) ->Unit,
    playerPoint2: Point,
    window: ComposeWindow,
    isVideoBoundsChanged: Boolean,
    videoPlayerBounds: Rectangle,
    adjustPosition: (Float, Rectangle) -> Unit,
) {
    Row(Modifier.width(144.dp)){
        if (multipleLines.enabled && playIconIndex == index) {
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
                            Text(text = "退出" )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = " Esc")
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
                IconButton(onClick = cancel) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
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

                        Text(text = "全选",modifier = Modifier.padding(10.dp))
                    }
                },
                delayMillis = 300,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = {
                    selectAll()
                }) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }

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
                                Text(text = " Tab ")
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
                    val playItem = Caption(multipleLines.startTime ,multipleLines.endTime ,"")
                    playCaption(playItem)
                },
                    modifier = Modifier
                        .onGloballyPositioned{coordinates ->
                            val rect = coordinates.boundsInWindow()
                            if (multipleLines.isUp) {
                                playerPoint2.y =
                                    window.y + rect.top.toInt() - ((303 - 48) * density).toInt()
                            } else {
                                playerPoint2.y =
                                    window.y + rect.top.toInt() + (100 * density).toInt()
                            }
                            playerPoint2.x =
                                window.x + rect.left.toInt() - (270 * density).toInt()
                            if(!isVideoBoundsChanged){
                                // 播放按钮可以显示
                                if(!rect.isEmpty){
                                    videoPlayerBounds.location = playerPoint2
                                    adjustPosition(density, videoPlayerBounds)
                                    playerPoint2.x = videoPlayerBounds.location.x
                                    playerPoint2.y = videoPlayerBounds.location.y
                                }
                            }


                        }
                ) {
                    val icon = if (mediaType == "audio" && !isPlaying) {
                        Icons.Filled.VolumeDown
                    } else if (mediaType == "audio") {
                        Icons.Filled.VolumeUp
                    } else Icons.Filled.PlayArrow

                    Icon(
                        icon,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }


        }
    }
}
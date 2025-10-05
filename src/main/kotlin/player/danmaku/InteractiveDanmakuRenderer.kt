/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package player.danmaku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import player.PlayerState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InteractiveDanmakuRenderer(
    danmakuItems: List<CanvasDanmakuItem>,
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
    fontSize: Int = 18,
    speed: Float = 3f,
    isPaused: Boolean = false,
    deleteWord: (CanvasDanmakuItem) -> Unit = {},
    addToFamiliar: (CanvasDanmakuItem) -> Unit = {},
    playAudio: (String) -> Unit ={},
    onHoverChanged: (Boolean) -> Unit = {}
) {

    val density = LocalDensity.current
    var hoveredItem by remember { mutableStateOf<CanvasDanmakuItem?>(null) }
    var popupOffset by remember { mutableStateOf(IntOffset.Zero) }

   // 循环动画
    LaunchedEffect(isPaused) {
        while (true) {
            if (!isPaused) {
                danmakuItems.forEach { item ->
                    if (item.isActive && item != hoveredItem) {  // 只有非悬停的弹幕才移动
                        item.x -= speed
                        if (item.x + item.textWidth < 0) {
                            item.isActive = false
                        }
                    }
                }
            }
            delay(16)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        danmakuItems.forEach { item ->
            if (item.isActive && item != hoveredItem) {
                Text(
                    text = item.text,
                    fontSize = fontSize.sp,
                    color = item.color,
                    fontFamily = fontFamily,
                    modifier = Modifier
                        .offset(
                            x = item.x.dp / LocalDensity.current.density,
                            y = (item.y - fontSize * LocalDensity.current.density).dp / LocalDensity.current.density
                        )
                        .onPointerEvent(PointerEventType.Enter) { event ->
                            hoveredItem = item
                            onHoverChanged(true)
                            // 悬停时固定弹窗位置
                            popupOffset = IntOffset(
                                x = (item.x - 200 * density.density).toInt(), // 向左偏移200dp
                                y = (item.y - fontSize * density.density - 50).toInt()
                            )


                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            hoveredItem = null
                            onHoverChanged(false)
                        }
                )
            }
        }

        if(hoveredItem != null){
            Popup(
                offset = popupOffset,
                onDismissRequest = { hoveredItem = null },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {

                WordDetail(
                    word = hoveredItem?.word!!,
                    playerState =playerState,
                    pointerExit = {hoveredItem = null},
                    height = 350.dp,
                    deleteWord = {
                        deleteWord(hoveredItem!!)
                        hoveredItem = null
                    },
                    addToFamiliar = {
                        addToFamiliar(hoveredItem!!)
                        hoveredItem = null
                    },
                    playAudio = playAudio
                )

            }


        }
    }
}


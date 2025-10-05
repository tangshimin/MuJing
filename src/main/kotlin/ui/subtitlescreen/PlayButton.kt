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

package ui.subtitlescreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Caption

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayButton(
    caption: Caption,
    isPlaying: Boolean,
    playCaption:(Caption) ->Unit,
    textFieldRequester: FocusRequester,
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
        IconButton(onClick = {
            playCaption(caption)
            textFieldRequester.requestFocus()
        }
        ) {
            val icon = if(mediaType=="audio" && !isPlaying) {
                Icons.Filled.VolumeDown
            } else if(mediaType=="audio"){
                Icons.Filled.VolumeUp
            }else if(isPlaying) {
                Icons.Filled.Pause
            }else Icons.Filled.PlayArrow

            Icon(
                icon,
                contentDescription = "播放按钮",
                tint = if(isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
            )
        }

    }
}
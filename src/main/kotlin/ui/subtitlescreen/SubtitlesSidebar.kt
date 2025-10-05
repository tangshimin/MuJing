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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import player.isMacOS

/**
 * 字幕浏览器的侧边栏
 */
@Composable
fun SubtitlesSidebar(
    isOpen: Boolean,
    transcriptionCaption: Boolean,
    setTranscriptionCaption:(Boolean) -> Unit,
    currentCaptionVisible: Boolean,
    setCurrentCaptionVisible:(Boolean) -> Unit,
    notWroteCaptionVisible: Boolean,
    setNotWroteCaptionVisible:(Boolean) -> Unit,
    externalSubtitlesVisible: Boolean,
    setExternalSubtitlesVisible:(Boolean) -> Unit,
    isPlayKeystrokeSound: Boolean,
    setIsPlayKeystrokeSound: (Boolean) -> Unit,
    trackSize: Int,
    selectTrack: () -> Unit,
    resetVideoBounds:() -> Unit,
) {
    if (isOpen) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .padding(top = if(isMacOS()) 44.dp else 0.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.fillMaxWidth().height(48.dp))
            Divider()
            val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground

            if (trackSize > 1) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { selectTrack() }
                        .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("选择字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                    )
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {

                Text("抄写字幕", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = transcriptionCaption,
                    onCheckedChange = { setTranscriptionCaption(!transcriptionCaption) },
                )
            }
            if(transcriptionCaption){
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {
                    Text("当前字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))

                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = currentCaptionVisible,
                        onCheckedChange = { setCurrentCaptionVisible(!currentCaptionVisible) },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
                ) {

                    Text("未写字幕", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(15.dp))

                    Switch(
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                        checked = notWroteCaptionVisible,
                        onCheckedChange = {setNotWroteCaptionVisible(!notWroteCaptionVisible) },
                    )
                }
            }
            Divider()


            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Text("击键音效", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isPlayKeystrokeSound,
                    onCheckedChange = { setIsPlayKeystrokeSound(it) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("显示外部字幕", color = MaterialTheme.colors.onBackground)

                }

                Spacer(Modifier.width(15.dp))

                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = externalSubtitlesVisible,
                    onCheckedChange = {setExternalSubtitlesVisible(!externalSubtitlesVisible) },
                )
            }

            var playExpanded by remember { mutableStateOf(false) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clickable {  playExpanded = true }
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Text("播放设置", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(15.dp))

                CursorDropdownMenu(
                    expanded = playExpanded,
                    onDismissRequest = { playExpanded = false },
                ) {
                    Surface(
                        elevation = 4.dp,
                        shape = RectangleShape,
                    ) {
                        Row(Modifier.width(280.dp).height(48.dp)){
                            DropdownMenuItem(
                                onClick = resetVideoBounds,
                                modifier = Modifier.width(280.dp).height(48.dp)
                            ) {
                                Text("恢复播放器的默认大小和位置",modifier = Modifier.padding(end = 10.dp))
                                Icon(
                                    imageVector = Icons.Filled.FlipToBack,
                                    contentDescription = "",
                                    tint = MaterialTheme.colors.onBackground,
                                    modifier = Modifier.size(40.dp, 40.dp)
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )

            }

        }
    }
}
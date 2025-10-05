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

package player

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import icons.SwapVert
import icons.TextSelectStart
import theme.LocalCtrl

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun CaptionToolbar(
    subtitleDescription: String,
    primaryCaptionVisible: Boolean,
    secondaryCaptionVisible: Boolean,
    isSwap: Boolean,
    isSelectionActivated: Boolean,
    onPrimaryCaptionToggle: () -> Unit,
    onSecondaryCaptionToggle: () -> Unit,
    onSwapToggle: () -> Unit,
    onSelectionToggle: () -> Unit,
    onFocusClear: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (subtitleDescription.contains("&")) {
            var parts = subtitleDescription
                .split("&")
                .map { removeSuffix(it) }
                .filter { it.isNotEmpty() && it != "&" }
            if(isSwap) {
                // 交换主次字幕
                parts = parts.reversed()
            }

            // 这里只处理双语字幕
            if (parts.size == 2) {
                for ((i, lang) in parts.withIndex()) {
                    val selected = (primaryCaptionVisible && i == 0) ||
                            (secondaryCaptionVisible && i == 1)

                    val order = if(i == 0) "第一" else "第二"
                    val tooltip =if (selected) "隐藏${order}语言 " else "显示${order}语言 "
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                val ctrl = LocalCtrl.current
                                val letter = if (i == 0) "1" else "2"
                                val shortcut = if (isMacOS()) "$ctrl $letter" else "$ctrl+$letter"
                                Row(modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ){
                                    Text(text = tooltip,color = MaterialTheme.colors.onSurface)
                                    Text(text =shortcut,color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                }

                            }
                        },
                        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )

                    ) {
                        LabelButton(
                            onClick = {
                                if (i == 0) {
                                    onPrimaryCaptionToggle()
                                } else {
                                    onSecondaryCaptionToggle()
                                }
                                onFocusClear()
                            },
                            enabled = selected,
                            lang = lang
                        )
                    }

                }
                // 切换主次字幕按钮
                SwapButton(
                    isActive = isSwap,
                    onClick = {
                        onSwapToggle()
                        onFocusClear()
                    }
                )
            }
        } else {
            if (subtitleDescription.isNotEmpty()) {
                val selected = primaryCaptionVisible
                val tooltip =if (selected) "隐藏第一语言 " else "显示第一语言 "
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            val ctrl = LocalCtrl.current
                            val letter =  "1"
                            val shortcut = if (isMacOS()) "$ctrl $letter" else "$ctrl+$letter"
                            Row(modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ){
                                Text(text = tooltip,color = MaterialTheme.colors.onSurface)
                                Text(text =shortcut,color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            }

                        }
                    },
                    delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    LabelButton(
                        lang = removeSuffix(subtitleDescription),
                        onClick = {
                            onPrimaryCaptionToggle()
                            onFocusClear()
                        },
                        enabled = primaryCaptionVisible
                    )
                }

            }
        }

        SelectionButton(
            isActive = isSelectionActivated,
            onClick = {
                onSelectionToggle()
                onFocusClear()
            }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwapButton(
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier
){

    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(text = "交换字幕",color = MaterialTheme.colors.onSurface)
                }
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(height = 32.dp,width = 44.dp)
                .padding(end = 8.dp)
                .background(
                    if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                )
                .clickable{ onClick() }
        ){
            Icon(
                SwapVert,
                contentDescription = "交换字幕",
                tint = if (isActive) MaterialTheme.colors.primary else Color.White.copy(alpha = 0.7f),
                modifier = modifier.size(18.dp)
            )
        }
    }

}

@Composable
fun LabelButton(
    onClick: () -> Unit,
    enabled: Boolean,
    lang: String
){
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(
                if (enabled) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
            )
            .clickable{ onClick() }

    ){
        Text(
            text = lang,
            color = if (enabled) MaterialTheme.colors.primary else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectionButton(
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier
){
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(text = "选择文本",color = MaterialTheme.colors.onSurface)
                }
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(height = 32.dp,width = 36.dp)
                .background(
                    if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                )
                .clickable{ onClick() }
        ){
            Icon(
                TextSelectStart,
                contentDescription = "选择字幕文本",
                tint = if (isActive) MaterialTheme.colors.primary else Color.White.copy(alpha = 0.7f),
                modifier = modifier.size(18.dp)
            )
        }
    }

}


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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Caption

/**
 * 多行字幕工具栏组件
 *
 * 这是一个用于字幕播放界面的工具栏组件，当用户选择多行字幕进行跟读时显示。
 * 工具栏具有固定的宽度（144dp），包含三个主要功能按钮：退出、全选和播放。
 * 组件具有平滑的弹出动画效果和专业的背景样式，提升用户体验。
 *
 * ## 功能特性
 * - **条件显示**: 只在多行字幕模式启用且当前索引匹配时显示
 * - **快捷键支持**: 提供键盘快捷键操作（Esc退出、Tab播放）
 * - **智能播放图标**: 根据媒体类型和播放状态动态切换图标
 * - **工具提示**: 为每个按钮提供详细的操作说明
 * - **弹出动画**: 平滑的淡入淡出和缩放效果，提升用户体验
 * - **专业背景**: 使用 Surface 组件提供阴影、圆角和边框效果
 * - **复制功能**: 复制所选字幕区域的文本内容
 *
 * ## 按钮功能
 * 1. **退出按钮** (Close): 退出多行字幕模式，快捷键 Esc
 * 2. **循环按钮** (AB): 开启/关闭循环播放模式
 * 3. **复制按钮** (ContentCopy): 复制所选字幕区域的文本内容
 * 4. **全选按钮** (Checklist): 选择当前显示的所有字幕行
 * 5. **播放按钮** (Play/Pause/Volume): 播放选中的字幕片段，快捷键 Tab
 *
 * ## 播放图标逻辑
 * - 音频媒体 + 未播放: VolumeDown 图标
 * - 音频媒体 + 播放中: VolumeUp 图标
 * - 视频媒体 + 播放中: Pause 图标
 * - 视频媒体 + 未播放: PlayArrow 图标
 *
 * ## 背景样式
 * - **Surface 容器**: 使用 Material Design 的 Surface 组件
 * - **背景颜色**: 使用主题的 surface 颜色，自动适配明暗主题
 * - **阴影效果**: 8dp 的高度提供立体感和浮动效果
 * - **圆角边框**: 使用主题的 medium 形状，提供现代化外观
 * - **边框装饰**: 半透明边框增强视觉层次
 *
 * @param modifier 修饰符，用于自定义布局和样式
 * @param index 当前字幕索引，用于判断是否显示工具栏
 * @param isPlaying 当前是否正在播放状态
 * @param toolbarDisplayIndex 工具栏显示的位置索引，用于判断是否显示工具栏
 * @param multipleLines 多行字幕状态对象，包含启用状态、时间范围等信息
 * @param mediaType 媒体类型，"audio" 或 "video"，影响播放图标的显示
 * @param captionList 字幕列表，用于获取选择的字幕文本
 * @param cancel 退出多行字幕模式的回调函数
 * @param selectAll 全选当前字幕的回调函数
 * @param playCaption 播放字幕片段的回调函数，接收 Caption 对象作为参数
 * @param copySelectedText 复制所选字幕文本的回调函数，接收复制的文本作为参数
 *
 * @see MultipleLines 多行字幕状态管理类
 * @see Caption 字幕数据类
 *
 * ## 使用示例
 * ```kotlin
 * MultipleLinesToolbar(
 *     modifier = Modifier.padding(8.dp),
 *     index = currentIndex,
 *     isPlaying = playbackState.isPlaying,
 *     toolbarDisplayIndex = selectedIndex,
 *     multipleLines = multipleLines,
 *     mediaType = "video",
 *     captionList = captionList,
 *     cancel = { multipleLines.enabled = false },
 *     selectAll = { selectAllSubtitles() },
 *     playCaption = { caption -> playSubtitleRange(caption) },
 *     copySelectedText = { text -> copyToClipboard(text) }
 * )
 * ```
 *
 * ## 注意事项
 * - 工具栏只在 `multipleLines.enabled == true` 且 `toolbarDisplayIndex == index` 时显示
 * - 播放功能会根据 `multipleLines.startTime` 和 `multipleLines.endTime` 创建 Caption 对象
 * - 所有按钮都包含工具提示，提供用户友好的操作指导
 * - 播放按钮的颜色会根据播放状态变化（播放时显示主题色，停止时显示默认色）
 * - 动画效果会在组件显示/隐藏时自动触发
 * - 背景样式会自动适配系统的明暗主题
 *
 * @since 2.6.12
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultipleLinesToolbar(
    modifier: Modifier,
    index: Int,
    isPlaying: Boolean,
    toolbarDisplayIndex: Int,
    multipleLines: MultipleLines,
    mediaType: String,
    captionList: List<Caption>,
    cancel:() -> Unit,
    selectAll:() -> Unit,
    playCaption:(Caption) ->Unit,
    copySelectedText: (String) -> Unit,
) {
    // 复制成功提示状态
    var copySuccess by remember { mutableStateOf(false) }
    
    // 自动隐藏复制成功提示
    LaunchedEffect(copySuccess) {
        if (copySuccess) {
            kotlinx.coroutines.delay(2000)
            copySuccess = false
        }
    }
    
    // 添加动画过渡效果
    AnimatedVisibility(
        visible = multipleLines.enabled && toolbarDisplayIndex == index,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 150,  // 更快的淡入，类似 tooltip
                delayMillis = 50,      // 添加短暂延迟
                easing = LinearOutSlowInEasing
            )
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 150,
                delayMillis = 50,
                easing = LinearOutSlowInEasing
            ),
            initialScale = 0.92f,  // 更小的缩放变化，更subtle
            transformOrigin = TransformOrigin.Center
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 100,  // 更快的淡出
                easing = FastOutLinearInEasing
            )
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutLinearInEasing
            ),
            targetScale = 0.92f,
            transformOrigin = TransformOrigin.Center
        )
    ) {
        // 为工具栏添加背景和阴影效果
        Surface(
            modifier = modifier.width(240.dp),
            elevation = 8.dp,  // 提供阴影效果，增强浮动感
            color = MaterialTheme.colors.surface,  // 使用主题的 surface 颜色
            shape = MaterialTheme.shapes.medium,  // 使用圆角形状
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)  // 添加边框
            )
        ) {
            val iconColor = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
            Row {
                // 播放按钮
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
                    IconButton(
                        onClick = {
                            val playItem = Caption(multipleLines.startTime, multipleLines.endTime, "")
                            playCaption(playItem)
                        }
                    ) {
                        val icon = if (mediaType == "audio" && !isPlaying) {
                            Icons.Filled.VolumeDown
                        } else if (mediaType == "audio") {
                            Icons.Filled.VolumeUp
                        } else if (isPlaying) {
                            Icons.Filled.Pause
                        } else Icons.Filled.PlayArrow

                        Icon(
                            icon,
                            contentDescription = "播放字幕片段",
                            tint = if(isPlaying) MaterialTheme.colors.primary else iconColor
                        )
                    }
                }


                // 循环按钮
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
                            Text(
                                text = if (multipleLines.isLooping) "关闭循环播放" else "开启循环播放",
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconToggleButton(
                        checked = multipleLines.isLooping,
                        onCheckedChange = { multipleLines.isLooping = it }
                    ){
                        Text(
                            text = "AB",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (multipleLines.isLooping) MaterialTheme.colors.primary else iconColor,
                            modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                        )
                    }

                }


                // 复制按钮
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
                            Text(text = "复制", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(
                        onClick = {
                            // 获取所选字幕区域的文本
                            val selectedText = getSelectedCaptionsText(captionList, multipleLines)
                            if (selectedText.isNotEmpty()) {
                                copySelectedText(selectedText)
                                // 显示成功提示
                                copySuccess = true
                            }
                        }
                    ) {
                        Icon(
                            if(copySuccess)Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            tint = iconColor
                        )
                    }
                }


                // 全选按钮
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
                            Text(text = "全选", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopCenter,
                        alignment = Alignment.TopCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = selectAll) {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = "全选字幕",
                            tint = iconColor
                        )
                    }
                }


                // 退出按钮
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
                            contentDescription = "退出多行字幕模式",
                            tint = iconColor
                        )
                    }
                }
            }

        }
    }
}

/**
 * 获取所选字幕区域的文本内容
 * 
 * 根据多行字幕模式的选择范围，从字幕列表中提取对应的文本内容
 * 
 * @param captionList 完整的字幕列表
 * @param multipleLines 多行字幕状态对象，包含选择范围信息
 * @return 拼接后的字幕文本，用换行符分隔
 */
fun getSelectedCaptionsText(captionList: List<Caption>, multipleLines: MultipleLines): String {
    val startIndex = multipleLines.startIndex
    val endIndex = multipleLines.endIndex
    
    // 确保索引在有效范围内
    if (startIndex < 0 || endIndex >= captionList.size || startIndex > endIndex) {
        return ""
    }
    
    // 提取所选范围内的字幕文本
    val selectedCaptions = captionList.subList(startIndex, endIndex + 1)
    
    // 拼接文本，用换行符分隔
    return selectedCaptions.joinToString("\n") { it.content }
}
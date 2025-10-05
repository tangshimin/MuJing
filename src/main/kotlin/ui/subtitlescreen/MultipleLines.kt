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

import androidx.compose.runtime.*

/**
 * 跟读的时候，播放多条字幕
 */
class MultipleLines{

    /** 启动 */
    var enabled by mutableStateOf(false)

    /** 开始索引 */
    var startIndex by mutableStateOf(0)

    /** 结束索引 */
    var endIndex by mutableStateOf(0)

    /** 开始时间 */
    var startTime by mutableStateOf("")

    /** 结束时间 */
    var endTime by mutableStateOf("")

    /** AB循环播放状态 */
    var isLooping by mutableStateOf(false)
}

@Composable
fun rememberMultipleLines(): MultipleLines = remember{
    MultipleLines()
}
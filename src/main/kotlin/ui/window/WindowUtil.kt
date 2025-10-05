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

package ui.window

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.WindowScope

/**
 * A hack to work around the window flashing its background color when closed
 * (https://github.com/JetBrains/compose-multiplatform/issues/3790).
 */
@Composable
fun WindowScope.windowBackgroundFlashingOnCloseFixHack() {
    val backgroundColor = MaterialTheme.colors.background
    LaunchedEffect(window, backgroundColor) {
        window.background = java.awt.Color(backgroundColor.toArgb())
    }
}
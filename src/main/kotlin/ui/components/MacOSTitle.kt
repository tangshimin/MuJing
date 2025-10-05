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

package ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.formdev.flatlaf.FlatClientProperties


@Composable
fun MacOSTitle(
    title: String,
    window: ComposeWindow,
    modifier: Modifier = Modifier.height(44.dp)
) {

    Text(
        text = title,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier.padding(top = 8.dp).fillMaxWidth()
    )
    Divider()
    LaunchedEffect(Unit){
        window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
//        System.setProperty("apple.awt.application.appearance", "system")
        //加了这一行，全屏的时候工具栏的背景颜色才会跟随主题一起变化
        window.rootPane.putClientProperty(
            FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING,
            FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING_MEDIUM
        )

    }

}


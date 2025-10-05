/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 *
 * This file contains vector graphics generated from Material Symbols icons
 * from Google Fonts (https://fonts.google.com/icons)
 * Original icons Copyright by Google LLC
 * Original icons licensed under Apache License 2.0
 *
 * The original Apache License 2.0 text:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val DockToRight: ImageVector
    get() {
        if (_DockToRight != null) {
            return _DockToRight!!
        }
        _DockToRight = ImageVector.Builder(
            name = "DockToRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 120f)
                horizontalLineToRelative(560f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                verticalLineToRelative(560f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(760f, 840f)
                lineTo(200f, 840f)
                close()
                moveTo(320f, 760f)
                verticalLineToRelative(-560f)
                lineTo(200f, 200f)
                verticalLineToRelative(560f)
                horizontalLineToRelative(120f)
                close()
                moveTo(400f, 760f)
                horizontalLineToRelative(360f)
                verticalLineToRelative(-560f)
                lineTo(400f, 200f)
                verticalLineToRelative(560f)
                close()
                moveTo(320f, 760f)
                lineTo(200f, 760f)
                horizontalLineToRelative(120f)
                close()
            }
        }.build()

        return _DockToRight!!
    }

@Suppress("ObjectPropertyName")
private var _DockToRight: ImageVector? = null

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

val SwapVert: ImageVector
    get() {
        if (_SwapVert != null) {
            return _SwapVert!!
        }
        _SwapVert = ImageVector.Builder(
            name = "SwapVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(320f, 520f)
                verticalLineToRelative(-287f)
                lineTo(217f, 336f)
                lineToRelative(-57f, -56f)
                lineToRelative(200f, -200f)
                lineToRelative(200f, 200f)
                lineToRelative(-57f, 56f)
                lineToRelative(-103f, -103f)
                verticalLineToRelative(287f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(600f, 880f)
                lineTo(400f, 680f)
                lineToRelative(57f, -56f)
                lineToRelative(103f, 103f)
                verticalLineToRelative(-287f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(287f)
                lineToRelative(103f, -103f)
                lineToRelative(57f, 56f)
                lineTo(600f, 880f)
                close()
            }
        }.build()

        return _SwapVert!!
    }

@Suppress("ObjectPropertyName")
private var _SwapVert: ImageVector? = null

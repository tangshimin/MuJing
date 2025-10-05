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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.ArrowDown: ImageVector
    get() {
        if (_arrowDown != null) {
            return _arrowDown!!
        }
        _arrowDown = materialIcon(name = "Filled.ArrowDown") {
            materialPath {
                moveTo(17.77f, 6.23f)
                lineToRelative(1.77f, 1.77f)
                lineToRelative(-8.23f, 8.23f)
                lineToRelative(-8.23f, -8.23f)
                lineToRelative(1.77f, -1.77f)
                lineToRelative(6.46f, 6.46f)
                close()
            }
        }
        return _arrowDown!!
    }

private var _arrowDown: ImageVector? = null


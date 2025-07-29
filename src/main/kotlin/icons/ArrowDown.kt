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


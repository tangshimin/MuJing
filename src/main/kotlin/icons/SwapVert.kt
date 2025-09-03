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

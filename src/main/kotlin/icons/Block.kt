package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Block: ImageVector
    get() {
        if (_Block != null) {
            return _Block!!
        }
        _Block = ImageVector.Builder(
            name = "Block",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 880f)
                quadToRelative(-83f, 0f, -156f, -31.5f)
                reflectiveQuadTo(197f, 763f)
                quadToRelative(-54f, -54f, -85.5f, -127f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -83f, 31.5f, -156f)
                reflectiveQuadTo(197f, 197f)
                quadToRelative(54f, -54f, 127f, -85.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(83f, 0f, 156f, 31.5f)
                reflectiveQuadTo(763f, 197f)
                quadToRelative(54f, 54f, 85.5f, 127f)
                reflectiveQuadTo(880f, 480f)
                quadToRelative(0f, 83f, -31.5f, 156f)
                reflectiveQuadTo(763f, 763f)
                quadToRelative(-54f, 54f, -127f, 85.5f)
                reflectiveQuadTo(480f, 880f)
                close()
                moveTo(480f, 800f)
                quadToRelative(54f, 0f, 104f, -17.5f)
                reflectiveQuadToRelative(92f, -50.5f)
                lineTo(228f, 284f)
                quadToRelative(-33f, 42f, -50.5f, 92f)
                reflectiveQuadTo(160f, 480f)
                quadToRelative(0f, 134f, 93f, 227f)
                reflectiveQuadToRelative(227f, 93f)
                close()
                moveTo(732f, 676f)
                quadToRelative(33f, -42f, 50.5f, -92f)
                reflectiveQuadTo(800f, 480f)
                quadToRelative(0f, -134f, -93f, -227f)
                reflectiveQuadToRelative(-227f, -93f)
                quadToRelative(-54f, 0f, -104f, 17.5f)
                reflectiveQuadTo(284f, 228f)
                lineToRelative(448f, 448f)
                close()
            }
        }.build()

        return _Block!!
    }

@Suppress("ObjectPropertyName")
private var _Block: ImageVector? = null

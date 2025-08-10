package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AutopauseDisabled: ImageVector
    get() {
        if (_AutopauseDisabled != null) {
            return _AutopauseDisabled!!
        }
        _AutopauseDisabled = ImageVector.Builder(
            name = "AutopauseDisabled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFFE3E3E3))) {
                moveTo(360f, 600f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(240f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(520f, 600f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(240f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(480f, 920f)
                quadToRelative(-108f, 0f, -202.5f, -49.5f)
                reflectiveQuadTo(120f, 732f)
                verticalLineToRelative(108f)
                lineTo(40f, 840f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(-98f)
                quadToRelative(51f, 75f, 129.5f, 117.5f)
                reflectiveQuadTo(480f, 840f)
                quadToRelative(115f, 0f, 208.5f, -66f)
                reflectiveQuadTo(820f, 599f)
                lineToRelative(78f, 18f)
                quadToRelative(-45f, 136f, -160f, 219.5f)
                reflectiveQuadTo(480f, 920f)
                close()
                moveTo(42f, 440f)
                quadToRelative(7f, -67f, 32f, -128.5f)
                reflectiveQuadTo(143f, 198f)
                lineToRelative(57f, 57f)
                quadToRelative(-32f, 41f, -52f, 87.5f)
                reflectiveQuadTo(123f, 440f)
                lineTo(42f, 440f)
                close()
                moveTo(256f, 199f)
                lineTo(199f, 142f)
                quadToRelative(53f, -44f, 114f, -69.5f)
                reflectiveQuadTo(440f, 42f)
                verticalLineToRelative(80f)
                quadToRelative(-51f, 5f, -97f, 25f)
                reflectiveQuadToRelative(-87f, 52f)
                close()
                moveTo(705f, 199f)
                quadToRelative(-41f, -32f, -87.5f, -52f)
                reflectiveQuadTo(520f, 122f)
                verticalLineToRelative(-80f)
                quadToRelative(67f, 6f, 128.5f, 31f)
                reflectiveQuadTo(762f, 142f)
                lineToRelative(-57f, 57f)
                close()
                moveTo(838f, 440f)
                quadToRelative(-5f, -51f, -25f, -97.5f)
                reflectiveQuadTo(761f, 255f)
                lineToRelative(57f, -57f)
                quadToRelative(44f, 52f, 69f, 113.5f)
                reflectiveQuadTo(918f, 440f)
                horizontalLineToRelative(-80f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color(0xFFE3E3E3)),
                strokeLineWidth = 75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(100f, 100f)
                lineTo(860f, 860f)
            }
        }.build()

        return _AutopauseDisabled!!
    }

@Suppress("ObjectPropertyName")
private var _AutopauseDisabled: ImageVector? = null

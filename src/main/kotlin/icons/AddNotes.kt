package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AddNotes: ImageVector
    get() {
        if (_AddNotes != null) {
            return _AddNotes!!
        }
        _AddNotes = ImageVector.Builder(
            name = "AddNotes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 120f)
                horizontalLineToRelative(560f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                verticalLineToRelative(268f)
                quadToRelative(-19f, -9f, -39f, -15.5f)
                reflectiveQuadToRelative(-41f, -9.5f)
                verticalLineToRelative(-243f)
                lineTo(200f, 200f)
                verticalLineToRelative(560f)
                horizontalLineToRelative(242f)
                quadToRelative(3f, 22f, 9.5f, 42f)
                reflectiveQuadToRelative(15.5f, 38f)
                lineTo(200f, 840f)
                close()
                moveTo(200f, 720f)
                verticalLineToRelative(40f)
                verticalLineToRelative(-560f)
                verticalLineToRelative(243f)
                verticalLineToRelative(-3f)
                verticalLineToRelative(280f)
                close()
                moveTo(280f, 680f)
                horizontalLineToRelative(163f)
                quadToRelative(3f, -21f, 9.5f, -41f)
                reflectiveQuadToRelative(14.5f, -39f)
                lineTo(280f, 600f)
                verticalLineToRelative(80f)
                close()
                moveTo(280f, 520f)
                horizontalLineToRelative(244f)
                quadToRelative(32f, -30f, 71.5f, -50f)
                reflectiveQuadToRelative(84.5f, -27f)
                verticalLineToRelative(-3f)
                lineTo(280f, 440f)
                verticalLineToRelative(80f)
                close()
                moveTo(280f, 360f)
                horizontalLineToRelative(400f)
                verticalLineToRelative(-80f)
                lineTo(280f, 280f)
                verticalLineToRelative(80f)
                close()
                moveTo(720f, 920f)
                quadToRelative(-83f, 0f, -141.5f, -58.5f)
                reflectiveQuadTo(520f, 720f)
                quadToRelative(0f, -83f, 58.5f, -141.5f)
                reflectiveQuadTo(720f, 520f)
                quadToRelative(83f, 0f, 141.5f, 58.5f)
                reflectiveQuadTo(920f, 720f)
                quadToRelative(0f, 83f, -58.5f, 141.5f)
                reflectiveQuadTo(720f, 920f)
                close()
                moveTo(700f, 840f)
                horizontalLineToRelative(40f)
                verticalLineToRelative(-100f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-40f)
                lineTo(740f, 700f)
                verticalLineToRelative(-100f)
                horizontalLineToRelative(-40f)
                verticalLineToRelative(100f)
                lineTo(600f, 700f)
                verticalLineToRelative(40f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(100f)
                close()
            }
        }.build()

        return _AddNotes!!
    }

@Suppress("ObjectPropertyName")
private var _AddNotes: ImageVector? = null

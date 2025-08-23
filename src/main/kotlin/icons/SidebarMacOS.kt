package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * macOS 侧边栏图标
 * 从 SF Symbols 中获取 svg 并使用 Valkyrie 转换为 Compose 的 ImageVector
 */
val SidebarMacOS: ImageVector
    get() {
        if (_Sidebar != null) {
            return _Sidebar!!
        }
        _Sidebar = ImageVector.Builder(
            name = "Sidebar",
            defaultWidth = 23.389.dp,
            defaultHeight = 17.979.dp,
            viewportWidth = 23.389f,
            viewportHeight = 17.979f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0f,
                strokeAlpha = 0f
            ) {
                moveTo(0f, 0f)
                horizontalLineToRelative(23.389f)
                verticalLineToRelative(17.979f)
                horizontalLineToRelative(-23.389f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.85f
            ) {
                moveTo(7.441f, 16.719f)
                lineTo(8.975f, 16.719f)
                lineTo(8.975f, 1.289f)
                lineTo(7.441f, 1.289f)
                close()
                moveTo(4.121f, 17.979f)
                lineTo(19.15f, 17.979f)
                curveTo(21.611f, 17.979f, 23.027f, 16.494f, 23.027f, 13.857f)
                lineTo(23.027f, 4.131f)
                curveTo(23.027f, 1.494f, 21.611f, 0f, 19.15f, 0f)
                lineTo(4.121f, 0f)
                curveTo(1.494f, 0f, 0f, 1.494f, 0f, 4.131f)
                lineTo(0f, 13.857f)
                curveTo(0f, 16.494f, 1.494f, 17.979f, 4.121f, 17.979f)
                close()
                moveTo(4.131f, 16.406f)
                curveTo(2.51f, 16.406f, 1.572f, 15.479f, 1.572f, 13.857f)
                lineTo(1.572f, 4.131f)
                curveTo(1.572f, 2.51f, 2.51f, 1.572f, 4.131f, 1.572f)
                lineTo(18.896f, 1.572f)
                curveTo(20.518f, 1.572f, 21.455f, 2.51f, 21.455f, 4.131f)
                lineTo(21.455f, 13.857f)
                curveTo(21.455f, 15.479f, 20.518f, 16.406f, 18.896f, 16.406f)
                close()
                moveTo(5.566f, 5.205f)
                curveTo(5.859f, 5.205f, 6.123f, 4.941f, 6.123f, 4.658f)
                curveTo(6.123f, 4.365f, 5.859f, 4.111f, 5.566f, 4.111f)
                lineTo(3.467f, 4.111f)
                curveTo(3.174f, 4.111f, 2.92f, 4.365f, 2.92f, 4.658f)
                curveTo(2.92f, 4.941f, 3.174f, 5.205f, 3.467f, 5.205f)
                close()
                moveTo(5.566f, 7.734f)
                curveTo(5.859f, 7.734f, 6.123f, 7.471f, 6.123f, 7.178f)
                curveTo(6.123f, 6.885f, 5.859f, 6.641f, 5.566f, 6.641f)
                lineTo(3.467f, 6.641f)
                curveTo(3.174f, 6.641f, 2.92f, 6.885f, 2.92f, 7.178f)
                curveTo(2.92f, 7.471f, 3.174f, 7.734f, 3.467f, 7.734f)
                close()
                moveTo(5.566f, 10.254f)
                curveTo(5.859f, 10.254f, 6.123f, 10.01f, 6.123f, 9.717f)
                curveTo(6.123f, 9.424f, 5.859f, 9.17f, 5.566f, 9.17f)
                lineTo(3.467f, 9.17f)
                curveTo(3.174f, 9.17f, 2.92f, 9.424f, 2.92f, 9.717f)
                curveTo(2.92f, 10.01f, 3.174f, 10.254f, 3.467f, 10.254f)
                close()
            }
        }.build()

        return _Sidebar!!
    }

@Suppress("ObjectPropertyName")
private var _Sidebar: ImageVector? = null

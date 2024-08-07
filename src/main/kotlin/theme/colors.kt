package theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color


val IDEADarkThemeOnBackground = Color(133, 144, 151)
fun createColors(
    isDarkTheme: Boolean,
    primary: Color,
    background:Color,
    onBackground:Color
): Colors {
    return if (isDarkTheme) {
        darkColors(
            primary = primary,
            onBackground = IDEADarkThemeOnBackground
        )
    } else {
        lightColors(
            primary = primary,
            background = background,
            surface = background,
            onBackground = onBackground
        )
    }
}

fun java.awt.Color.toCompose(): Color {
    return Color(red, green, blue)
}

fun Color.toAwt(): java.awt.Color {
    return java.awt.Color(red, green, blue)
}
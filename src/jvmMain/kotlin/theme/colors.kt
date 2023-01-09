package theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color


val green = Color(0xFF09af00)// Color(46, 125, 50)
val IDEADarkThemeOnBackground = Color(133, 144, 151)
fun createColors(
    isDarkTheme: Boolean,
    primary: Color
): Colors {
    return if (isDarkTheme) {
        darkColors(
            primary = primary,
            onBackground = IDEADarkThemeOnBackground
        )
    } else {
        lightColors(
            primary = primary,
        )
    }
}
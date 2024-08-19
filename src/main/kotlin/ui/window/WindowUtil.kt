package ui.window

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.WindowScope

/**
 * A hack to work around the window flashing its background color when closed
 * (https://github.com/JetBrains/compose-multiplatform/issues/3790).
 */
@Composable
fun WindowScope.windowBackgroundFlashingOnCloseFixHack() {
    val backgroundColor = MaterialTheme.colors.background
    LaunchedEffect(window, backgroundColor) {
        window.background = java.awt.Color(backgroundColor.toArgb())
    }
}
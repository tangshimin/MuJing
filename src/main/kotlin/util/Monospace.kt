package util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Monospace 字体
 */
@Composable
fun rememberMonospace() : FontFamily {
    val monospace by remember {
        mutableStateOf(
            FontFamily(
                Font(
                    "font/Inconsolata-Regular.ttf",
                    FontWeight.Normal,
                    FontStyle.Normal
                )
            )
        )
    }
    return monospace
}
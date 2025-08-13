package theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import player.LocalAudioPlayerComponent
import player.isMacOS
import player.rememberAudioPlayerComponent


@Composable
fun CustomLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
        LocalCtrl provides rememberCtrl(),
        LocalScrollbarStyle provides scrollbarStyle(),
        content = content
    )
}
@Composable
fun PlayerLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
//        LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
        LocalCtrl provides rememberCtrl(),
        LocalScrollbarStyle provides scrollbarStyle(),
        content = content
    )
}
/** 本地的 Ctrl 键,
 *  在 Windows 使用 Ctrl 键，
 *  在 macOS 使用 Command 键
 **/
val LocalCtrl = staticCompositionLocalOf<String> {
    error("LocalCtrl isn't provided")
}

/** 本地的 Ctrl 键,
 *  在 Windows 使用 Ctrl 键，
 *  在 macOS 使用 Command 键
 *  */
@Composable
fun rememberCtrl(): String = remember {
    if (isMacOS()) "⌘" else "Ctrl"
}


@Composable
fun scrollbarStyle(): ScrollbarStyle {
    val shape = if(isMacOS()) RoundedCornerShape(4.dp) else  RectangleShape
    return ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 8.dp,
        shape =shape,
        hoverDurationMillis = 300,
        unhoverColor = if(MaterialTheme.colors.isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f),
        hoverColor = if(MaterialTheme.colors.isLight) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.50f)
    )
}


/**
 * 自定义的文本选择颜色
 * 主要用于文本选择时的背景和手柄颜色
 * 根据主题自动调整颜色
 */
@Composable
fun rememberCustomSelectionColors(): TextSelectionColors {
    val isLight = MaterialTheme.colors.isLight
    val primaryColor = MaterialTheme.colors.primary
    return remember(isLight) {
        val backgroundColor = if (isLight) {
            // 浅色主题使用经典的亮蓝色
            if (isMacOS()) Color(0xFFACCEF7) else Color(0xFF3390FF)
        } else {
            // 深色主题使用更暗、饱和度更低的蓝色
            Color(0xFF29417F) // RGB(41, 65, 127)
        }
        TextSelectionColors(
            handleColor = primaryColor, // 通常设置为主色调
            backgroundColor = backgroundColor
        )
    }
}

/**
 * 播放器一直使用的深色主题选择颜色
 * 主要用于播放器的文本选择
 */
@Composable
fun rememberDarkThemeSelectionColors(): TextSelectionColors {
   val primaryColor = MaterialTheme.colors.primary
    return remember {
        TextSelectionColors(
            handleColor = primaryColor, // 通常设置为主色调
            backgroundColor = Color(0xFF29417F)
        )
    }
}
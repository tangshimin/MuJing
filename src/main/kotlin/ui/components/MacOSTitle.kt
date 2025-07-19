package ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import com.formdev.flatlaf.FlatClientProperties


@Composable
fun MacOSTitle(
    title: String,
    window: ComposeWindow,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = MaterialTheme.colors.onBackground,
        modifier = modifier
    )

    LaunchedEffect(Unit){
        window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
//        System.setProperty("apple.awt.application.appearance", "system")
        //加了这一行，全屏的时候工具栏的背景颜色才会跟随主题一起变化
        window.rootPane.putClientProperty(
            FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING,
            FlatClientProperties.MACOS_WINDOW_BUTTONS_SPACING_LARGE
        )

    }

}


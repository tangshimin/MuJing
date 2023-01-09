package ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow


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
    }

}


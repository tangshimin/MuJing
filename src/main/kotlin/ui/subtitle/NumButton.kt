package ui.subtitle

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * 显示字幕索引，点击后就可以选择多行字幕
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumButton(
    index:Int,
    indexColor: Color,
    onClick:()->Unit,
) {
    val ctrl = LocalCtrl.current
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                ),
                shape = RectangleShape
            ) {
                Row(modifier = Modifier.padding(10.dp)) {
                    Text(text = "选择多行字幕")
                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                        Text(text = " $ctrl + N ")
                    }
                }

            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        Text(
            modifier = Modifier.clickable { onClick() },
            text = androidx.compose.ui.text.buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = indexColor,
                        fontSize = MaterialTheme.typography.h5.fontSize,
                        letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                        fontFamily = MaterialTheme.typography.h5.fontFamily,
                    )
                ) {
                    append("${index + 1}")
                }
            },
        )

    }
}
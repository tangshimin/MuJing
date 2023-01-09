package ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkText(
    text:String,
    url:String
){
    val uriHandler = LocalUriHandler.current
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(style = SpanStyle(color = blueColor)) {
            append(text)
        }
        pop()
    }
    ClickableText(text = annotatedString,
        style = MaterialTheme.typography.body1,
        modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let {
                uriHandler.openUri(it.item)
            }
        })
}
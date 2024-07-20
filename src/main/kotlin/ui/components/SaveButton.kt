package ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SaveButtonPreview() {
    PreviewLayout {
        SaveButton(
            saveClick = {},
            otherClick = {}
        )
    }
}


@Composable
fun SaveButton(
    enabled: Boolean = true,
    saveClick: () -> Unit,
    otherClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
    ){
        val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(36.dp)
                .border(
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                )
        ) {
            var expanded by remember { mutableStateOf(false) }
            val buttonColor = if(enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
            Box(Modifier.width(IntrinsicSize.Max)) {
                Text(
                    text = "保存",
                    color = buttonColor,
                    modifier = Modifier
                        .clickable(
                            enabled = enabled,
                            onClick = {  saveClick() })
                        .padding(start = 16.dp, end = 4.dp, top = 5.dp, bottom = 5.dp
                        )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(130.dp).height(40.dp)
                        .background(MaterialTheme.colors.background)
                        .border(border = border)
                        .clickable(
                            onClick = {
                                expanded = false
                                otherClick()
                            }
                        )
                ) {
                    Text(
                        text = "保存其他格式",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(130.dp).height(40.dp)
                    )
                }
            }

            Divider(Modifier.fillMaxHeight().width(1.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = buttonColor,
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        onClick = { expanded = true })
                    .padding(start = 4.dp, end = 8.dp,
                        top = 5.dp, bottom = 5.dp
                    )

            )
        }
    }
}


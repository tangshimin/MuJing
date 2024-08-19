package ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ui.window.windowBackgroundFlashingOnCloseFixHack

@Composable
fun MessageDialog(
    show:Boolean,
    close:()->Unit,
    message:String
){
    if(show){
        Dialog(
            title = "消息",
            icon = painterResource("logo/logo.png"),
            onCloseRequest = { close() },
            resizable = true,
            state = rememberDialogState(
                position = WindowPosition(Alignment.Center),
                size = DpSize(400.dp, 400.dp)
            ),
        ) {
            windowBackgroundFlashingOnCloseFixHack()
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                Box(Modifier.fillMaxSize()){
                    Text(message,modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
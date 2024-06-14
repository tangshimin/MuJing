package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState

/**
 * 确认对话框
 * @param message 要显示的消息
 * @param confirm 点击确认之后调用的函数
 * @param close 点击取消之后调用的函数
 */
@ExperimentalComposeUiApi
@Composable
fun ConfirmDialog(message: String, confirm: () -> Unit, close: () -> Unit) {
    Dialog(
        title = "删除",
        onCloseRequest = { close() },
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(400.dp, 300.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        ) {
            val focusRequester = remember { FocusRequester() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if(keyEvent.key == Key.Y && keyEvent.type == KeyEventType.KeyUp){
                            confirm()
                            true
                        }else if((keyEvent.key == Key.N || keyEvent.key == Key.Escape )&& keyEvent.type == KeyEventType.KeyUp){
                            close()
                            true
                        }else false
                    }
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
                ) {
                    Text(message)
                }

                Spacer(Modifier.height(20.dp))
                Row {
                    OutlinedButton(onClick = { confirm() }) {
                        Text("确定(Y)")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("取消(N)")
                    }
                }
            }
        }
    }
}
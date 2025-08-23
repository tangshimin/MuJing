package ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ui.window.windowBackgroundFlashingOnCloseFixHack

@Composable
fun DonateDialog(close: () -> Unit) {
    DialogWindow(
        title = "捐赠",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(645.dp, 650.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Divider()
                var state by remember { mutableStateOf(0) }
                TabRow(
                    selectedTabIndex = state,
                    backgroundColor = Color.Transparent
                ) {
                    Tab(
                        text = { Text("微信支付") },
                        selected = state == 0,
                        onClick = { state = 0 }
                    )

                }

                when (state) {
                    0 -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 75.dp)
                        ) {
                            Image(
                                painter = painterResource("donate/WeChat Payment.png"),
                                contentDescription = "donate",
                                modifier = Modifier.width(400.dp).height(400.dp)
                            )
                        }
                    }

                }
            }
        }
    }
}
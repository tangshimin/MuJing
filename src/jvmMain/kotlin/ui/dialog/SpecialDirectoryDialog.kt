package ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.OutlinedButton
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
import player.isMacOS
import state.getAudioDirectory
import state.getResourcesFile
import state.getSettingsDirectory
import java.awt.Desktop
import java.io.File


@Composable
fun SpecialDirectoryDialog(close: () -> Unit) {
    Dialog(
        title = "特殊文件夹",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(400.dp, 400.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getSettingsDirectory())
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("软件设置文件夹")
                }
                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getAudioDirectory())
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("单词发音文件夹")
                }

                OutlinedButton(onClick = {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(getResourcesFile("vocabulary"))
                    }
                },
                    modifier = Modifier.width(140.dp)){
                    Text("内置词库文件夹")
                }

                if (!isMacOS()) {
                    OutlinedButton(
                        onClick = {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                    .isSupported(Desktop.Action.OPEN)
                            ) {
                                Desktop.getDesktop().open(File("."))
                            }
                        },
                        modifier = Modifier.width(140.dp)
                    ) {
                        Text("安装目录")
                    }
                }


            }
        }
    }
}
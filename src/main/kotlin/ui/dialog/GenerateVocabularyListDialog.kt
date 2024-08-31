package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import ui.window.windowBackgroundFlashingOnCloseFixHack

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun GenerateVocabularyListDialog(
    show: Boolean,
    close: () -> Unit,
    appState: AppState
) {
    if (show) {
        DialogWindow(
            title = "生成词库",
            icon = painterResource("logo/logo.png"),
            onCloseRequest = { close() },
            resizable = false,
            state = rememberDialogState(
                position = WindowPosition(Alignment.Center),
                size = DpSize(940.dp, 700.dp)
            ),
        ) {
            windowBackgroundFlashingOnCloseFixHack()
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                        .background(color = MaterialTheme.colors.background)

                ) {
                    Text(
                        text = "用文档生成词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {
                            appState.generateVocabularyFromDocument = true
                            close()
                        })
                            .width(165.dp)
                            .padding(top = 5.dp, bottom = 5.dp, start = 20.dp,)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "用字幕生成词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {
                            appState.generateVocabularyFromSubtitles = true
                            close()
                        })
                            .width(165.dp)
                            .padding(top = 5.dp, bottom = 5.dp, start = 20.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "用视频生成词库",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.clickable(onClick = {
                            appState.generateVocabularyFromVideo = true
                            close()
                        })
                            .width(165.dp)
                            .padding(top = 5.dp, bottom = 5.dp, start = 20.dp)
                            .onGloballyPositioned { coordinates ->
                                println("onGloballyPositioned Size: ${coordinates.size}")
                            }
                    )
                }
            }

        }
    }

}

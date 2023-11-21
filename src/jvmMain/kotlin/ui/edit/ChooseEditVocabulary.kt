package ui.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.RecentItem
import player.isMacOS
import player.isWindows
import state.rememberWordState
import java.io.File

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseEditVocabulary(
    close: () -> Unit,
    recentList: List<RecentItem>,
    openEditVocabulary: (String) -> Unit,
    colors: Colors,
) {

    Window(
        title = "选择要编辑词库",
        icon = painterResource("logo/logo.png"),
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
        ),
        onCloseRequest = close,
    ) {
        MaterialTheme(colors = colors) {
            Surface {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val wordState = rememberWordState()
                    if (recentList.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 10.dp)
                        ) { Text("最近词库") }
                        Box(
                            Modifier.fillMaxWidth().height(400.dp).padding(10.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                        ) {
                            val stateVertical = rememberScrollState(0)
                            Column(Modifier.verticalScroll(stateVertical)) {
                                if(wordState.vocabularyName.isNotEmpty()){
                                    ListItem(
                                        text = {
                                            Text(
                                                wordState.vocabularyName,
                                                color = MaterialTheme.colors.onBackground
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            if(wordState.vocabularyPath.isNotEmpty()){
                                                openEditVocabulary(wordState.vocabularyPath)
                                            }
                                        },
                                        trailing = {
                                            Text("当前词库    ", color = MaterialTheme.colors.primary)
                                        }
                                    )
                                }

                                recentList.forEach { item ->
                                    if (wordState.vocabularyName != item.name && File(item.path).exists()) {
                                        ListItem(
                                            text = { Text(item.name, color = MaterialTheme.colors.onBackground) },
                                            modifier = Modifier.clickable {
                                                openEditVocabulary(item.path)
                                            }
                                        )
                                    }

                                }
                            }
                            VerticalScrollbar(
                                style = LocalScrollbarStyle.current.copy(
                                    shape = if (isWindows()) RectangleShape else RoundedCornerShape(
                                        4.dp
                                    )
                                ),
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(stateVertical)
                            )
                        }
                    }
                    var showFilePicker by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showFilePicker = true }) {
                        Text(
                            text = "选择词库",
                        )
                    }


                    val extensions = if (isMacOS()) listOf("public.json") else listOf("json")
                    FilePicker(
                        show = showFilePicker,
                        fileExtensions = extensions,
                        initialDirectory = ""
                    ) { pickFile ->
                        if (pickFile != null) {
                            openEditVocabulary(pickFile.path)
                        }

                        showFilePicker = false
                    }
                }

            }
        }


    }
}


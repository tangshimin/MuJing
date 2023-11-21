package ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import data.MutableVocabulary
import data.Vocabulary
import data.VocabularyType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import state.getResourcesFile
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

fun vocabularyInfoWindow(
    vocabulary: Vocabulary,
    vocabularyPath: String,
    close: () -> Unit,
    saveVideoPath: (String) -> Unit,
    colors: Colors,
) {
    val window = JFrame("基本信息")
    window.size = Dimension(650, 300)
    val iconFile = getResourcesFile("logo/logo.svg")
    val iconImages = FlatSVGUtils.createWindowIconImages(iconFile.toURI().toURL())
    window.iconImages = iconImages
    window.setLocationRelativeTo(null)
    window.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            close()
            window.dispose()
        }
    })

    val composePanel = ComposePanel()
    composePanel.setContent {
        VocabularyInfo(
            vocabulary = MutableVocabulary(vocabulary),
            vocabularyPath = vocabularyPath,
            parentWindow = window,
            colors = colors,
            close = {
                close()
                window.dispose()
            },
            saveVideoPath = { saveVideoPath(it) }
        )
    }

    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true
}

@Composable
fun VocabularyInfo(
    vocabulary: MutableVocabulary,
    vocabularyPath: String,
    parentWindow:JFrame,
    close: () -> Unit,
    saveVideoPath:(String) -> Unit,
    colors: Colors
){
    val windowState = rememberDialogState(
        size = DpSize(650.dp, 300.dp),
        position = WindowPosition(parentWindow.location.x.dp,parentWindow.location.y.dp)
    )

    MaterialTheme(colors = colors) {
        Dialog(
            title = "基本信息",
            onCloseRequest = close,
            state = windowState,
        ) {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                window.isAlwaysOnTop = true
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()){
                    var videoPath by remember{ mutableStateOf(TextFieldValue(vocabulary.relateVideoPath))}

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp,bottom = 10.dp)){
                            SelectionContainer {
                                Text("数量 ${vocabulary.wordList.size} 个",Modifier.padding(end = 25.dp))
                            }
                            SelectionContainer {
                                Text("语言 english",Modifier.padding(end = 25.dp))
                            }
                            SelectionContainer {
                                Text("类型 ${vocabulary.type.name}",Modifier.padding(end = 25.dp))
                            }
                            SelectionContainer {
                                Text("字幕轨道 ${vocabulary.subtitlesTrackId}",Modifier.padding(end = 25.dp))
                            }

                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, bottom = 10.dp)
                    ) {
                        Text("词库地址：",)
                        BasicTextField(
                            value = vocabularyPath,
                            onValueChange = { },
                            readOnly = true,
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                lineHeight = 29.sp,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onBackground
                            ),
                            modifier = Modifier
                                .width(windowState.size.width - 100.dp)
                                .padding(start = 8.dp, end = 8.dp)
                                .height(35.dp)
                                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                        )
                    }



                    if(vocabulary.type == VocabularyType.SUBTITLES){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()){
                            Text("视频地址：", modifier = Modifier.padding(start = 10.dp))
                            if(vocabulary.relateVideoPath.isNotEmpty()){
                                BasicTextField(
                                    value = videoPath,
                                    onValueChange = { videoPath = it},
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    textStyle = TextStyle(
                                        lineHeight = 29.sp,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    modifier = Modifier
                                        .width(windowState.size.width - 100.dp)
                                        .padding(start = 8.dp, end = 8.dp)
                                        .height(35.dp)
                                        .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                                )


                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()){
                        if(videoPath.text != vocabulary.relateVideoPath){
                            OutlinedButton(onClick = {
                                vocabulary.relateVideoPath = videoPath.text
                                saveVideoPath(vocabulary.relateVideoPath)
                                close()
                            }){
                                Text("保存")
                            }
                        }
                    }


                }
            }

            LaunchedEffect(Unit){
                parentWindow.addWindowFocusListener(object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) {
                        window.requestFocus()
                    }
                    override fun windowLostFocus(e: WindowEvent?) {}

                })
            }
            LaunchedEffect(windowState){
                snapshotFlow { windowState.size }
                    .onEach {
                        // 同步窗口和对话框的大小
                        parentWindow.size = windowState.size.toAwtSize()
                    }
                    .launchIn(this)

                snapshotFlow { windowState.position }
                    .onEach {
                        // 同步窗口和对话框的位置
                        parentWindow.location = windowState.position.toPoint()
                    }
                    .launchIn(this)



            }

        }

    }
}
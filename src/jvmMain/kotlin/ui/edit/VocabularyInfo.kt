package ui.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import data.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.isWindows
import state.getResourcesFile
import ui.dialog.ConfirmDialog
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import javax.swing.JFrame

fun vocabularyInfoWindow(
    vocabulary: Vocabulary,
    vocabularyPath: String,
    close: () -> Unit,
    saveVideoPath: (String) -> Unit,
    colors: Colors,
) {
    val window = JFrame("信息")
    window.size = Dimension(650, 520)
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
        size = DpSize(650.dp, 520.dp),
        position = WindowPosition(parentWindow.location.x.dp,parentWindow.location.y.dp)
    )

    MaterialTheme(colors = colors) {
        Dialog(
            title = "信息",
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
                    var activeSave by remember{ mutableStateOf(false)}

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

                    // 如果是文档词库，可能会链接字幕词库。
                    if(vocabulary.type == VocabularyType.DOCUMENT){
                        LinkedFile(
                            vocabulary = vocabulary,
                            activeSave = { activeSave = true },
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
                        LaunchedEffect(videoPath.text){
                            activeSave = videoPath.text != vocabulary.relateVideoPath
                        }
                        OutlinedButton(
                            enabled = activeSave,
                            onClick = {
                                vocabulary.relateVideoPath = videoPath.text
                                saveVideoPath(vocabulary.relateVideoPath)
                                close()
                            }){
                            Text("保存")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = {close()}){
                            Text("关闭")
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


/**
 * activeSave 是一个回调函数，删除里一个字幕词库链接后，激活保存按钮
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkedFile(
    vocabulary: MutableVocabulary,
    activeSave: () -> Unit
){
    // key : 代表的是一个文件，用来批量删除和这个文件链接的所有字幕。可以是视频地址或字幕名称，有字幕名称是因为使用字幕生成的词库可以没有对应的视频。
    // value: 和当前词库链接的字幕数量
    val externalNameMap = remember { mutableStateMapOf<String,Int>() }
    var deleted by remember{ mutableStateOf(false)}

    LaunchedEffect(Unit){
        computeNameMap(vocabulary.wordList, externalNameMap)
    }
    LaunchedEffect(deleted){
        if(deleted){
            externalNameMap.clear()
            computeNameMap(vocabulary.wordList, externalNameMap)
            deleted = false
        }
    }
    Column(Modifier.width(IntrinsicSize.Max).padding(start = 10.dp,bottom = 20.dp)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) { Text("已链接的视频或字幕") }
        if (externalNameMap.isNotEmpty()) {
            val boxHeight by remember(externalNameMap.size){
                    derivedStateOf {
                        val size = externalNameMap.size
                        if(size <=5){
                            externalNameMap.size * 48.dp
                        }else{
                            240.dp
                        }
                    }
            }
            Box(Modifier
                .height(boxHeight)
                .fillMaxWidth()
                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            ){
                val stateVertical = rememberScrollState(0)
                Column (Modifier
                    .padding(start = 20.dp)
                    .verticalScroll(stateVertical)
                    ){
                    externalNameMap.forEach { (path, count) ->
                        var showConfirmationDialog by remember { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable{}) {
                            val name = File(path).nameWithoutExtension
                            if (showConfirmationDialog) {
                                ConfirmDialog(
                                    message = "确定要删除 $name 的所有字幕吗?",
                                    confirm = {
                                        vocabulary.wordList.forEach { word ->
                                            val tempList = mutableListOf<ExternalCaption>()
                                            word.externalCaptions.forEach { externalCaption ->
                                                if (externalCaption.relateVideoPath == path || externalCaption.subtitlesName == path) {
                                                    tempList.add(externalCaption)
                                                }
                                            }
                                            word.externalCaptions.removeAll(tempList)
                                        }
                                        deleted = true
                                        showConfirmationDialog = false
                                        activeSave()
                                    },
                                    close = { showConfirmationDialog = false }
                                )
                            }
                            Text(
                                text = name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(450.dp).padding(start = 10.dp,end = 10.dp)
                            )
                            Text("$count", modifier = Modifier.width(60.dp))
                            IconButton(onClick = { showConfirmationDialog = true },modifier = Modifier.padding(end = 10.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "",
                                    tint = MaterialTheme.colors.onBackground
                                )
                            }


                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(stateVertical),
                )
            }

        }

    }
}

fun computeNameMap(
    wordList: SnapshotStateList<Word>,
    externalNameMap: SnapshotStateMap<String, Int>
) {
    wordList.forEach { word ->
        word.externalCaptions.forEach { externalCaption ->

            // 视频词库,可以播放字幕对应的视频。
            if (externalCaption.relateVideoPath.isNotEmpty()) {
                var counter = externalNameMap[externalCaption.relateVideoPath]
                if (counter == null) {
                    externalNameMap[externalCaption.relateVideoPath] = 1
                } else {
                    counter++
                    externalNameMap[externalCaption.relateVideoPath] = counter
                }
                // 字幕词库，使用字幕生成的词库，可以没有对应的视频，所以使用字幕的名称。
            } else if (externalCaption.subtitlesName.isNotEmpty()) {
                var counter = externalNameMap[externalCaption.subtitlesName]
                if (counter == null) {
                    externalNameMap[externalCaption.subtitlesName] = 1
                } else {
                    counter++
                    externalNameMap[externalCaption.subtitlesName] = counter
                }
            }

        }
    }
}

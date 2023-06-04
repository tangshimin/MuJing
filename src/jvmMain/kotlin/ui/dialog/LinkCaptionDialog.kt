package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.*
import kotlinx.coroutines.launch
import player.play
import state.AppState
import state.TypingWordState
import java.awt.Point
import java.awt.Rectangle
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

/**
 * 链接一条字幕到一个单词
 * @param word 当前正在链接的单词
 * @param state 应用程序的状态
 * @param setLinkSize 由于 word 是不可观察的，增加或删除链接后需要更新链接的数量。
 * @param close 关闭当前窗口
 */
@OptIn(ExperimentalComposeUiApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@Composable
fun LinkCaptionDialog(
    word: Word,
    state: AppState,
    typingWordState: TypingWordState,
    setLinkSize: (Int) -> Unit,
    close: () -> Unit
) {
    Dialog(
        title = "链接字幕",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(610.dp, 700.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                val scope = rememberCoroutineScope()
                val wordList = remember { mutableStateListOf<Word>() }
                var subtitleVocabularyPath by remember { mutableStateOf("") }
                var relateVideoPath by remember { mutableStateOf("") }
                var subtitlesTrackId by remember { mutableStateOf(0) }
                var subtitlesName by remember { mutableStateOf("") }
                var selectedCaptionContent by remember { mutableStateOf("") }
                var selectedCaption by remember { mutableStateOf<Caption?>(null) }
                Column(Modifier.width(IntrinsicSize.Max).align(Alignment.Center)) {
                    EditingCaptions(
                        state = state,
                        typingWordState = typingWordState,
                        setLinkSize = { setLinkSize(it) },
                        word = word
                    )
                    Divider(Modifier.padding(start = 10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp)
                    ) {
                        Text("选择字幕词库：")
                        BasicTextField(
                            value = subtitleVocabularyPath,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = TextStyle(
                                lineHeight = 26.sp,
                                fontSize = 16.sp,
                                color = MaterialTheme.colors.onBackground
                            ),
                            modifier = Modifier
                                .width(275.dp)
                                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                        )
                        IconButton(onClick = {
                            Thread {
                                state.loadingFileChooserVisible = true
                                val fileChooser = state.futureFileChooser.get()
                                fileChooser.dialogTitle = "选择字幕词库"
                                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                                fileChooser.isAcceptAllFileFilterUsed = false
                                fileChooser.selectedFile = null
                                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    subtitleVocabularyPath = file.absolutePath
                                    wordList.clear()
                                    val vocabulary = loadVocabulary(file.absolutePath)
                                    wordList.addAll(vocabulary.wordList)
                                    relateVideoPath = vocabulary.relateVideoPath
                                    if (vocabulary.type == VocabularyType.SUBTITLES) {
                                        subtitlesName = vocabulary.name
                                    }
                                    subtitlesTrackId = vocabulary.subtitlesTrackId
                                    fileChooser.selectedFile = File("")
                                }
                                state.loadingFileChooserVisible = false
                            }.start()

                        }) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = "",
                                tint = MaterialTheme.colors.onBackground
                            )
                        }
                    }


                    if (wordList.isNotEmpty()) {
                        val index = wordList.indexOf(word)
                        if (index != -1) {
                            val subtitleWord = wordList[index]
                            if (subtitleWord.captions.size > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp)
                                ) {
                                    Text(
                                        "选择字幕：",
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.width(116.dp)
                                    )
                                    Box(Modifier.width(IntrinsicSize.Max)) {
                                        var expanded by remember { mutableStateOf(false) }
                                        OutlinedButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.width(IntrinsicSize.Max)
                                                .background(Color.Transparent)
                                                .border(1.dp, Color.Transparent)
                                        ) {
                                            Text(
                                                text = selectedCaptionContent.ifEmpty { "" },
                                                color = MaterialTheme.colors.onBackground
                                            )
                                            Icon(
                                                Icons.Default.ExpandMore,
                                                contentDescription = "",
                                                modifier = Modifier.size(20.dp, 20.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.width(500.dp).height(140.dp)
                                        ) {
                                            subtitleWord.captions.forEachIndexed { _, caption ->
                                                DropdownMenuItem(
                                                    onClick = {
                                                        selectedCaptionContent = caption.content
                                                        selectedCaption = caption
                                                        expanded = false
                                                    },
                                                    modifier = Modifier.width(500.dp).height(40.dp)
                                                ) {
                                                    Text(
                                                        text = caption.content,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.width(IntrinsicSize.Max)
                                                    )
                                                    val playTriple =
                                                        Triple(caption, relateVideoPath, subtitlesTrackId)
                                                    val playerBounds by remember {
                                                        mutableStateOf(
                                                            Rectangle(
                                                                0,
                                                                0,
                                                                540,
                                                                303
                                                            )
                                                        )
                                                    }
                                                    val mousePoint by remember{ mutableStateOf(Point(0,0)) }
                                                    var isVideoBoundsChanged by remember{mutableStateOf(false)}
                                                    val resetVideoBounds:() -> Rectangle = {
                                                        isVideoBoundsChanged = false
                                                        Rectangle(mousePoint.x, mousePoint.y, 540, 303)
                                                    }
                                                    var isPlaying by remember { mutableStateOf(false) }
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier
                                                            .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                                                                val location =
                                                                    pointerEvent.awtEventOrNull?.locationOnScreen
                                                                if (location != null && !isPlaying) {
                                                                    if (isVideoBoundsChanged) {
                                                                        mousePoint.x = location.x - 270 + 24
                                                                        mousePoint.y = location.y - 320
                                                                    } else {
                                                                        playerBounds.x = location.x - 270 + 24
                                                                        playerBounds.y = location.y - 320
                                                                    }
                                                                    isPlaying = true
                                                                    val file = File(relateVideoPath)
                                                                    if (file.exists()) {
                                                                        scope.launch {
                                                                            play(
                                                                                window = state.videoPlayerWindow,
                                                                                setIsPlaying = { isPlaying = it },
                                                                                volume = state.global.videoVolume,
                                                                                playTriple = playTriple,
                                                                                videoPlayerComponent = state.videoPlayerComponent,
                                                                                bounds = playerBounds,
                                                                                resetVideoBounds = resetVideoBounds,
                                                                                isVideoBoundsChanged = isVideoBoundsChanged,
                                                                                setIsVideoBoundsChanged = {
                                                                                    isVideoBoundsChanged = it
                                                                                }
                                                                            )
                                                                        }

                                                                    }
                                                                }
                                                            }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.PlayArrow,
                                                            contentDescription = "Localized description",
                                                            tint = MaterialTheme.colors.primary
                                                        )
                                                    }
                                                }
                                            }

                                        }
                                    }

                                    OutlinedButton(onClick = {
                                        if (subtitleVocabularyPath.isNotEmpty() && selectedCaptionContent.isNotEmpty()) {
                                            if (selectedCaption != null) {
                                                val externalCaption = ExternalCaption(
                                                    relateVideoPath,
                                                    subtitlesTrackId,
                                                    subtitlesName,
                                                    selectedCaption!!.start,
                                                    selectedCaption!!.end,
                                                    selectedCaption!!.content
                                                )

                                                if (word.externalCaptions.size < 3 && !word.externalCaptions.contains(
                                                        externalCaption
                                                    )
                                                ) {
                                                    word.externalCaptions.add(externalCaption)
                                                }
                                            }
                                            setLinkSize(word.externalCaptions.size)
                                            typingWordState.vocabulary.wordList.removeAt(index)
                                            typingWordState.vocabulary.wordList.add(index, word)
                                        }
                                    }, modifier = Modifier.padding(start = 10.dp)) {
                                        Text("添加")
                                    }

                                }
                            }
                        } else {
                            Text("所选择的词库没有与 ${word.value} 相等的单词，请重新选择字幕词库", color = Color.Red)
                        }

                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                ) {
                    OutlinedButton(onClick = {

                        close()
                    }) {
                        Text("确定")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("取消")
                    }
                }
            }
        }
    }
}
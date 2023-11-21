package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import player.play
import state.AppState
import ui.getPlayTripleMap
import java.awt.Point
import java.awt.Rectangle
import java.io.File

/**
 * 链接一条字幕到一个单词
 * @param word 当前正在链接的单词
 * @param appState 应用程序的状态
 * @param close 关闭当前窗口
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class, ExperimentalMaterialApi::class)
@Composable
fun LinkCaptionDialog(
    word: Word,
    appState: AppState,
    vocabulary: MutableVocabulary,
    vocabularyDir: File,
    save:(MutableList<ExternalCaption>) -> Unit,
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
                /** 添加或删除了字幕后触发重组，tempWord 的 externalCaptions 增加或删除了字幕不会触发重组 */
                var externalCaptionSize by remember { mutableStateOf(word.externalCaptions.size) }
                val tempWord by remember { mutableStateOf(word.deepCopy()) }
                val scope = rememberCoroutineScope()
                var subtitleVocabularyPath by remember { mutableStateOf("") }
                var relateVideoPath by remember { mutableStateOf("") }
                var subtitlesTrackId by remember { mutableStateOf(0) }
                var subtitlesName by remember { mutableStateOf("") }
                var selectedCaption by remember { mutableStateOf<Caption?>(null) }
                val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                var isPlaying by remember { mutableStateOf(false) }
                var chosenWord by remember { mutableStateOf<Word?>(null) }
                var chosenVocabularyType by remember { mutableStateOf(VocabularyType.SUBTITLES) }
                var chosenVocabularyDir by remember { mutableStateOf(File("")) }
                Column(Modifier.width(IntrinsicSize.Max).align(Alignment.TopCenter)) {
                    if(externalCaptionSize > 0){
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp)){
                            Text("已链接的字幕：")
                        }

                        Column(Modifier.border(border)){
                            val playTripleMap = getPlayTripleMap(vocabulary.type,subtitlesTrackId,relateVideoPath, tempWord)
                            playTripleMap.forEach { (_, playTriple) ->
                                ListItem(
                                    text = {
                                        Text(playTriple.first.content, color = MaterialTheme.colors.onBackground)
                                    },
                                    modifier = Modifier.clickable {},
                                    trailing = {
                                        Row {
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
                                            val mousePoint by remember { mutableStateOf(Point(0, 0)) }
                                            var isVideoBoundsChanged by remember { mutableStateOf(false) }
                                            val resetVideoBounds: () -> Rectangle = {
                                                isVideoBoundsChanged = false
                                                Rectangle(mousePoint.x, mousePoint.y, 540, 303)
                                            }

                                            IconButton(onClick = {},
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

                                                            val absFile = File(playTriple.second)
                                                            val relFile = File(vocabularyDir,absFile.name)
                                                            if (absFile.exists() || relFile.exists()) {
                                                                val playParams = if(!absFile.exists()){
                                                                    Triple(playTriple.first,relFile.absolutePath,playTriple.third)
                                                                }else {
                                                                    playTriple
                                                                }
                                                                isPlaying = true
                                                                scope.launch {
                                                                    play(
                                                                        window = appState.videoPlayerWindow,
                                                                        setIsPlaying = { isPlaying = it },
                                                                        volume = appState.global.videoVolume,
                                                                        playTriple = playParams,
                                                                        videoPlayerComponent = appState.videoPlayerComponent,
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
                                            IconButton(
                                                onClick = {
                                                    var removeIndex = -1
                                                    tempWord.externalCaptions.forEachIndexed { index, externalCaption ->

                                                        if (externalCaption.content == playTriple.first.content) {
                                                            removeIndex = index

                                                        }
                                                    }

                                                    if(removeIndex != -1){
                                                        tempWord.externalCaptions.removeAt(removeIndex)
                                                        // 重新触发重组
                                                        externalCaptionSize = tempWord.externalCaptions.size
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    Icons.Filled.Remove,
                                                    contentDescription = "Localized description",
                                                    tint = MaterialTheme.colors.primary
                                                )
                                            }
                                        }
                                    }
                                )

                            }

                        }

                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 30.dp,bottom = 30.dp)
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
                        var showFilePicker by remember {mutableStateOf(false)}
                        val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
                        FilePicker(
                            show = showFilePicker,
                            fileExtensions = extensions,
                            initialDirectory = ""){pickFile ->
                            if(pickFile != null){
                                if(pickFile.path.isNotEmpty()){
                                    val file = File(pickFile.path)
                                    subtitleVocabularyPath = file.absolutePath
                                    chosenVocabularyDir = file.parentFile
                                    val pickedVocabulary = loadVocabulary(file.absolutePath)
                                   pickedVocabulary.wordList.indexOf(tempWord).let {
                                       if(it != -1){
                                           chosenWord = pickedVocabulary.wordList[it]
                                           chosenVocabularyType = pickedVocabulary.type
                                           relateVideoPath = pickedVocabulary.relateVideoPath
                                           if (pickedVocabulary.type == VocabularyType.SUBTITLES) {
                                               subtitlesName = pickedVocabulary.name
                                           }
                                           subtitlesTrackId = pickedVocabulary.subtitlesTrackId
                                       }

                                }
                            }

                            showFilePicker = false
                        }

                    }
                        IconButton(onClick = {showFilePicker = true}) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = "",
                                tint = MaterialTheme.colors.onBackground
                            )
                        }
                    }
                    if (chosenWord != null) {
                        val playItems = getPlayTripleMap(chosenVocabularyType,subtitlesTrackId,relateVideoPath, chosenWord!!)
                        if (playItems.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("选择要链接的字幕：")
                            }

                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().border(border).padding(start = 10.dp)
                            ) {
                                playItems.forEach { (_,playTriple) ->
                                    val caption = playTriple.first
                                    ListItem(
                                        text = {
                                            Text(caption.content, color = MaterialTheme.colors.onBackground)
                                        },
                                        modifier = Modifier.clickable {},
                                        trailing = {
                                            Row {
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
                                                val mousePoint by remember { mutableStateOf(Point(0, 0)) }
                                                var isVideoBoundsChanged by remember { mutableStateOf(false) }
                                                val resetVideoBounds: () -> Rectangle = {
                                                    isVideoBoundsChanged = false
                                                    Rectangle(mousePoint.x, mousePoint.y, 540, 303)
                                                }
                                                IconButton(onClick = {},
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
                                                                val absFile = File(playTriple.second)
                                                                val relFile = File(vocabularyDir,absFile.name)
                                                                if (absFile.exists() || relFile.exists()) {
                                                                    val playParams = if (!absFile.exists()) {
                                                                        Triple(
                                                                            playTriple.first,
                                                                            relFile.absolutePath,
                                                                            playTriple.third
                                                                        )
                                                                    } else {
                                                                        playTriple
                                                                    }
                                                                    scope.launch {
                                                                        play(
                                                                            window = appState.videoPlayerWindow,
                                                                            setIsPlaying = { isPlaying = it },
                                                                            volume = appState.global.videoVolume,
                                                                            playTriple = playParams,
                                                                            videoPlayerComponent = appState.videoPlayerComponent,
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
                                                IconButton(
                                                    onClick = {
                                                        selectedCaption = caption
                                                        if (subtitleVocabularyPath.isNotEmpty() && caption.content.isNotEmpty()) {
                                                            if (selectedCaption != null) {
                                                                val externalCaption = ExternalCaption(
                                                                    playTriple.second,
                                                                    subtitlesTrackId,
                                                                    subtitlesName,
                                                                    selectedCaption!!.start,
                                                                    selectedCaption!!.end,
                                                                    selectedCaption!!.content
                                                                )

                                                                if (tempWord.externalCaptions.size < 3 && !tempWord.externalCaptions.contains(
                                                                        externalCaption
                                                                    )
                                                                ) {
                                                                    tempWord.externalCaptions.add(externalCaption)
                                                                    // 重新触发重组
                                                                    externalCaptionSize = tempWord.externalCaptions.size
                                                                }
                                                            }
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = "Localized description",
                                                        tint = MaterialTheme.colors.primary
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }


                        } else {
                            Text(
                                "所选择的词库没有与 ${tempWord.value} 相等的单词，请重新选择字幕词库",
                                color = Color.Red
                            )
                        }

                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                ) {
                    OutlinedButton(onClick = {
                        save(tempWord.externalCaptions)
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
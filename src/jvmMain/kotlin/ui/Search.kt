package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import data.*
import kotlinx.coroutines.launch
import player.*
import state.AppState
import java.awt.Rectangle
import java.io.File
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.serialization.ExperimentalSerializationApi
import state.TypingWordState
import java.awt.Point

@OptIn(ExperimentalComposeUiApi::class, ExperimentalSerializationApi::class)
@Composable
fun Search(
    appState: AppState,
    typingWordState: TypingWordState,
    vocabularyDir : File,
    vocabulary:  MutableVocabulary
){

    var searchResult by remember{ mutableStateOf<Word?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    /** 等宽字体*/
    val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }
    val onDismissRequest :() -> Unit = {
        appState.searching = false
    }
    val audioPlayer = LocalAudioPlayerComponent.current
    val keyEvent: (KeyEvent) -> Boolean = {
        if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
            onDismissRequest()
            true
        }else if (it.isCtrlPressed && it.key == Key.J && it.type == KeyEventType.KeyUp) {
            if (!isPlayingAudio && searchResult != null && searchResult!!.value.isNotEmpty()) {
                val audioPath = getAudioPath(
                    word = searchResult!!.value,
                    audioSet = appState.localAudioSet,
                    addToAudioSet = {audioPath -> appState.localAudioSet.add(audioPath)},
                    pronunciation = typingWordState.pronunciation
                )
                playAudio(
                    word = searchResult!!.value,
                    audioPath = audioPath,
                    pronunciation =  typingWordState.pronunciation,
                    volume = appState.global.audioVolume,
                    audioPlayerComponent = audioPlayer,
                    changePlayerState = { isPlaying -> isPlayingAudio = isPlaying },
                )

            }
            true
        } else if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
            onDismissRequest()
            true
        } else false
    }

    Popup(
        alignment = Alignment.Center,
        focusable = true,
        onDismissRequest = {onDismissRequest()},
        onKeyEvent = {keyEvent(it)}
    ) {
        val scope = rememberCoroutineScope()

        val focusRequester = remember { FocusRequester() }
        var input by remember { mutableStateOf("") }

        /** 熟悉词库 */
        val familiarVocabulary = remember{ loadMutableVocabularyByName("FamiliarVocabulary") }

        val search:(String) -> Unit = {
                input = it
                if(searchResult != null) {
                    searchResult!!.value = ""
                }

                val inputWord = Word(value = input.lowercase())
                // 先搜索当前词库
                val index = vocabulary.wordList.indexOf(inputWord)
                if(index != -1){
                    searchResult = vocabulary.wordList.get(index).deepCopy()
                }
                // 如果当前词库没有，或者当前词库的单词没有字幕，再搜索熟悉词库。
                if((searchResult == null) || searchResult!!.value.isEmpty() ||
                    (searchResult!!.captions.isEmpty() && searchResult!!.externalCaptions.isEmpty())){
                    val indexOf = familiarVocabulary.wordList.indexOf(inputWord)
                    if(indexOf != -1){
                        val familiar = familiarVocabulary.wordList.get(indexOf).deepCopy()
                        searchResult = familiar
                    }
                }

                // 如果词库里面没有，就搜索内置词典
                if((searchResult == null) || searchResult!!.value.isEmpty()){
                    val dictWord = Dictionary.query(input.lowercase())
                    if(dictWord != null){
                        searchResult = dictWord.deepCopy()
                    }
                }

        }
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
            modifier = Modifier
                .width(600.dp)
                .height(500.dp)
                .background(MaterialTheme.colors.background),
        ) {
            Box(Modifier.fillMaxSize()) {
                val stateVertical = rememberScrollState(0)
                Column(Modifier.verticalScroll(stateVertical)) {
                    Row(Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Localized description",
                            tint = if (MaterialTheme.colors.isLight) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                        )

                        BasicTextField(
                            value = input,
                            onValueChange = { search(it) },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                            textStyle = MaterialTheme.typography.h5.copy(
                                color = MaterialTheme.colors.onBackground,
                                fontFamily = monospace
                            ),
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 5.dp, bottom = 5.dp)
                                .focusRequester(focusRequester)
                        )

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                    }
                    Divider()
                    if (searchResult != null && searchResult!!.value.isNotEmpty()) {

                        SearchResultInfo(
                            word = searchResult!!,
                            appState = appState,
                            typingState = typingWordState
                        )

                        if (searchResult!!.captions.isNotEmpty()) {
                            searchResult!!.captions.forEachIndexed { index, caption ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${index + 1}. ${caption.content}",
                                        modifier = Modifier.padding(5.dp)
                                    )

                                    val playTriple =
                                        Triple(caption, vocabulary.relateVideoPath, vocabulary.subtitlesTrackId)
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
                                                    if(isVideoBoundsChanged){
                                                        mousePoint.x = location.x - 270 + 24
                                                        mousePoint.y = location.y - 320
                                                    }else{
                                                        playerBounds.x = location.x - 270 + 24
                                                        playerBounds.y = location.y - 320
                                                    }

                                                    isPlaying = true
                                                    val absFile = File(vocabulary.relateVideoPath)
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
                                                                setIsPlaying = {
                                                                    isPlaying = it
                                                                },
                                                                volume = appState.global.videoVolume,
                                                                playTriple = playParams,
                                                                videoPlayerComponent = appState.videoPlayerComponent,
                                                                bounds = playerBounds,
                                                                resetVideoBounds = resetVideoBounds,
                                                                isVideoBoundsChanged = isVideoBoundsChanged,
                                                                setIsVideoBoundsChanged = {isVideoBoundsChanged = it}
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
                        if (searchResult!!.externalCaptions.isNotEmpty()) {
                            searchResult!!.externalCaptions.forEachIndexed { index, externalCaption ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${index + 1}. ${externalCaption.content}",
                                        modifier = Modifier.padding(5.dp)
                                    )
                                    val caption =
                                        Caption(externalCaption.start, externalCaption.end, externalCaption.content)
                                    val playTriple =
                                        Triple(
                                            caption,
                                            externalCaption.relateVideoPath,
                                            externalCaption.subtitlesTrackId
                                        )
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
                                                    if(isVideoBoundsChanged){
                                                        mousePoint.x = location.x - 270 + 24
                                                        mousePoint.y = location.y - 320
                                                    }else{
                                                        playerBounds.x = location.x - 270 + 24
                                                        playerBounds.y = location.y - 320
                                                    }
                                                    isPlaying = true
                                                    val absFile = File(externalCaption.relateVideoPath)
                                                    val relFile = File(vocabularyDir,absFile.name)

                                                    if (absFile.exists() || relFile.exists()) {
                                                        val playParams = if(!absFile.exists()){
                                                            Triple(playTriple.first,relFile.absolutePath,playTriple.third)
                                                        }else {
                                                            playTriple
                                                        }
                                                        scope.launch {
                                                            play(
                                                                window = appState.videoPlayerWindow,
                                                                setIsPlaying = {
                                                                    isPlaying = it
                                                                },
                                                                volume = appState.global.videoVolume,
                                                                playTriple = playParams,
                                                                videoPlayerComponent = appState.videoPlayerComponent,
                                                                bounds = playerBounds,
                                                                resetVideoBounds = resetVideoBounds,
                                                                isVideoBoundsChanged = isVideoBoundsChanged,
                                                                setIsVideoBoundsChanged = {isVideoBoundsChanged = it}
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

                    }else if(input.isNotEmpty()){
                        Text("没有找到相关单词")
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

    }

}
package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.isMacOS
import player.isWindows
import player.play
import state.AppState
import state.getResourcesFile
import ui.edit.displayExchange
import ui.edit.toAwtSize
import ui.edit.toPoint
import ui.getPlayTripleMap
import ui.secondsToString
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.JFrame

/**
 * 编辑当前单词
 * 这个组件和别的组件不同，它会从记忆单词界面调用，也可以从编辑词库界面调用。
 * 记忆单词界面是一个 Compose 界面，而编辑词库界面是一个 Swing 界面。
 *
 * @param parentWindow 父窗口，只要在编辑词库界面调用时才设置。
 * @param word 当前单词
 * @param appState 应用程序的状态
 * @param save 点击保存之后调用的回调
 * @param close 点击取消之后调用的回调
 */

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun EditWordDialog(
    parentWindow: JFrame? = null,
    word: Word,
    title:String,
    appState: AppState,
    vocabulary: MutableVocabulary,
    vocabularyDir:File,
    save: (Word) -> Unit,
    close: () -> Unit
) {
    val windowState =  rememberDialogState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(840.dp, 810.dp)
    )
    MaterialTheme(colors = appState.colors) {
        Dialog(
            title = title,
            onCloseRequest = { close() },
            resizable = true,
            state = windowState,
        ) {
            EditWordComposeContent(
                word = word,
                title = title,
                appState = appState,
                vocabulary = vocabulary,
                vocabularyDir = vocabularyDir,
                save = {
                    save(it)
                },
                close = close
            )
            LaunchedEffect(Unit){
                if(parentWindow != null){
                    window.isAlwaysOnTop = true
                    parentWindow.addWindowFocusListener(object : WindowFocusListener {
                        override fun windowGainedFocus(e: WindowEvent?) {
                            window.requestFocus()
                        }
                        override fun windowLostFocus(e: WindowEvent?) {}

                    })
                }

            }

            LaunchedEffect(windowState){
                if(parentWindow != null){
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

}

/**
 *  编辑当前单词,从编辑词库界面调用
 *  在 Swing 中使用 Compose 如果是 Dark 主题，会出现白色闪光，
 *  相关 Issue https://github.com/JetBrains/compose-multiplatform/issues/1800
 *  这里这么写是为了解决这个问题。
 */
@OptIn(ExperimentalSerializationApi::class)
fun editWordSwing(
    word: Word,
    title:String,
    appState: AppState,
    vocabulary: Vocabulary,
    vocabularyDir:File,
    save: (Word) -> Unit,
    close: () -> Unit
) {
    val window = JFrame(title)
    window.setSize(840, 810)
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
        EditWordDialog(
            parentWindow = window,
            word = word,
            title = title,
            appState = appState,
            vocabulary = MutableVocabulary(vocabulary),
            vocabularyDir = vocabularyDir,
            save = {
                save(it)
                if(title =="编辑单词"){
                    window.dispose()
                }
            },
            close = {
                close()
                window.dispose()
            }
        )
    }

    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true

}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSerializationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun EditWordComposeContent(
    word: Word,
    title:String,
    appState: AppState,
    vocabulary: MutableVocabulary,
    vocabularyDir: File,
    save: (Word) -> Unit,
    close: () -> Unit
) {
    /** 一个临时单词 */
    var tempWord by remember { mutableStateOf(word.deepCopy()) }
    var inputWordStr by remember { mutableStateOf(TextFieldValue(tempWord.value)) }
    var usphone by remember{ mutableStateOf(TextFieldValue(tempWord.usphone)) }
    var ukphone by remember{ mutableStateOf(TextFieldValue(tempWord.ukphone)) }
    var definitionFieldValue by remember { mutableStateOf(TextFieldValue(tempWord.definition)) }
    var translationFieldValue by remember { mutableStateOf(TextFieldValue(tempWord.translation)) }
    var exchange by remember { mutableStateOf(tempWord.exchange) }
    var addSuccess by remember { mutableStateOf(false) }

    val save = {
        tempWord.value = inputWordStr.text
        tempWord.usphone = usphone.text
        tempWord.ukphone = ukphone.text
        tempWord.translation = translationFieldValue.text
        tempWord.definition = definitionFieldValue.text
        tempWord.exchange = exchange
        save(tempWord)
        if(title =="添加单词"){
            addSuccess = true
            tempWord = Word("")
            inputWordStr = TextFieldValue("")
            usphone = TextFieldValue("")
            ukphone = TextFieldValue("")
            translationFieldValue = TextFieldValue("")
            definitionFieldValue = TextFieldValue("")
            exchange = ""
        }
    }

    Box (Modifier.onKeyEvent {
        if(it.key == Key.Escape && it.type == KeyEventType.KeyUp){
            close()
            true
        }else if(it.key == Key.S && it.isCtrlPressed && it.type == KeyEventType.KeyUp){
            save()
            true

        }else false
    }){
        Column(Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .background(MaterialTheme.colors.background)

        ) {
            val textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colors.onBackground
            )
            val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

            Divider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                var queryFailed by remember { mutableStateOf(false) }
                val query = {
                    Thread {
                        val resultWord = Dictionary.query(inputWordStr.text)
                        if (resultWord != null) {
                            tempWord = resultWord
                            usphone = TextFieldValue(resultWord.usphone)
                            ukphone = TextFieldValue(resultWord.ukphone)
                            translationFieldValue = TextFieldValue(resultWord.translation)
                            definitionFieldValue = TextFieldValue(resultWord.definition)
                            exchange = resultWord.exchange
                            queryFailed = false
                        } else {
                            queryFailed = true
                        }
                    }.start()
                }
                Text("单词：", color = MaterialTheme.colors.onBackground)
                Spacer(Modifier.width(20.dp))
                BasicTextField(
                    value = inputWordStr,
                    onValueChange = { inputWordStr = it },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    modifier = Modifier
                        .border(border = border)
                        .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                        .onKeyEvent {
                            if(it.key == Key.Enter && it.type == KeyEventType.KeyUp){
                                query()
                                true
                            }else{
                                false
                            }
                        }
                )
                Spacer(Modifier.width(10.dp))


                Box{
                    OutlinedButton(onClick = { query() }) {
                        Text("查询")
                    }

                    DropdownMenu(
                        expanded = queryFailed,
                        onDismissRequest = { queryFailed = false },
                    ) {
                        Surface(
                            elevation = 5.dp,
                            shape = RectangleShape,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.width(250.dp).height(60.dp)
                            ) {
                                Text("本地词典没有找到 ${inputWordStr.text} ", color = MaterialTheme.colors.onBackground)
                            }
                            LaunchedEffect(Unit){
                                delay(2000)
                                queryFailed = false
                            }
                        }

                    }
                }

                Spacer(Modifier.width(52.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ){
                Text("英：" ,color = MaterialTheme.colors.onBackground)
                BasicTextField(
                    value = ukphone,
                    onValueChange = {
                        ukphone = it
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    modifier = Modifier.border(border = border).padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                )
                Text("美：" ,color = MaterialTheme.colors.onBackground,modifier = Modifier.padding(start = 10.dp))
                BasicTextField(
                    value = usphone,
                    onValueChange = {
                        usphone = it
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    modifier = Modifier.border(border = border).padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                )
            }
            Box{
                var editExchange by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    var displayExchange = displayExchange(exchange)
                    if(displayExchange.isEmpty()){
                        displayExchange = "没有词形"
                    }
                    SelectionContainer {
                        Text(
                            text = displayExchange,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.widthIn(min = 75.dp,max = 500.dp)
                        )
                    }

                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Text(text = "编辑词形", modifier = Modifier.padding(10.dp))
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.CenterEnd,
                            alignment = Alignment.CenterEnd,
                            offset = DpOffset.Zero
                        ),
                    ) {
                        IconButton(onClick = { editExchange = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "",
                                tint = MaterialTheme.colors.onBackground
                            )
                        }
                    }

                }

                DropdownMenu(
                    expanded = editExchange,
                    offset = DpOffset(270.dp, 0.dp),
                    onDismissRequest = { editExchange = false },
                ) {

                    var lemma by remember { mutableStateOf("") }
                    var plural by remember { mutableStateOf("") }
                    var preterite by remember { mutableStateOf("") }
                    var pastParticiple by remember { mutableStateOf("") }
                    var presentParticiple by remember { mutableStateOf("") }
                    var third by remember { mutableStateOf("") }
                    var er by remember { mutableStateOf("") }
                    var est by remember { mutableStateOf("") }

                    LaunchedEffect(exchange){
                        val exchanges = exchange.split("/")
                        exchanges.forEach { exchange ->
                            val pair = exchange.split(":")
                            when (pair[0]) {
                                "p" -> {
                                    preterite = pair[1]
                                }

                                "d" -> {
                                    pastParticiple = pair[1]
                                }

                                "i" -> {
                                    presentParticiple = pair[1]
                                }

                                "3" -> {
                                    third = pair[1]
                                }

                                "r" -> {
                                    er = pair[1]
                                }

                                "t" -> {
                                    est = pair[1]
                                }

                                "s" -> {
                                    plural = pair[1]
                                }

                                "0" -> {
                                    lemma = pair[1]
                                }

                            }
                        }
                    }

                    val getExchangeStr :() -> String = {
                        var exchangeStr =""
                        if(preterite.isNotEmpty()){
                            exchangeStr += "/p:$preterite"
                        }
                        if(pastParticiple.isNotEmpty()){
                            exchangeStr += "/d:$pastParticiple"
                        }
                        if(presentParticiple.isNotEmpty()){
                            exchangeStr += "/i:$presentParticiple"
                        }
                        if(third.isNotEmpty()){
                            exchangeStr += "/3:$third"
                        }
                        if(er.isNotEmpty()){
                            exchangeStr += "/r:$er"
                        }
                        if(est.isNotEmpty()){
                            exchangeStr += "/t:$est"
                        }
                        if(plural.isNotEmpty()){
                            exchangeStr += "/s:$plural"
                        }
                        if(lemma.isNotEmpty()){
                            exchangeStr += "/0:$lemma"
                        }
                        exchangeStr
                    }

                    LaunchedEffect(lemma, plural, preterite, pastParticiple, presentParticiple, third, er, est){
                        exchange = getExchangeStr()
                    }

                    Surface(
                        elevation = 5.dp,
                        shape = RectangleShape,
                    ) {
                        Column(Modifier.width(300.dp).height(400.dp).padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("原型",modifier = Modifier.padding(end = 77.dp))
                                BasicTextField(
                                    value = lemma,
                                    onValueChange = {
                                        lemma = it
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("复数",modifier = Modifier.padding(end = 77.dp))
                                BasicTextField(
                                    value = plural,
                                    onValueChange = { plural = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("过去式",modifier = Modifier.padding(end = 60.dp))
                                BasicTextField(
                                    value = preterite,
                                    onValueChange = { preterite = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("过去分词",modifier = Modifier.padding(end = 44.dp))
                                BasicTextField(
                                    value = pastParticiple,
                                    onValueChange = { pastParticiple = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("现在分词",modifier = Modifier.padding(end = 44.dp))
                                BasicTextField(
                                    value = presentParticiple,
                                    onValueChange = { presentParticiple = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("第三人称单数",modifier = Modifier.padding(end = 10.dp))
                                BasicTextField(
                                    value = third,
                                    onValueChange = { third = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("形容词比较级",modifier = Modifier.padding(end = 10.dp))
                                BasicTextField(
                                    value = er,
                                    onValueChange = { er = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text("形容词最高级",modifier = Modifier.padding(end = 10.dp))

                                BasicTextField(
                                    value = est,
                                    onValueChange = { est = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    modifier = Modifier.border(border = border)
                                        .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                                )
                            }
                        }
                    }

                }
            }


            val isWindows = isWindows()
            val scrollbarStyle = LocalScrollbarStyle.current.copy(shape = if(isWindows) RectangleShape else RoundedCornerShape(4.dp))
            val modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            Column(modifier = modifier) {
                Text("中文释义：",color = MaterialTheme.colors.onBackground)
                Box(modifier = Modifier.fillMaxWidth()
                    .height(160.dp)
                    .border(border = border)) {
                    val stateVertical = rememberScrollState(0)
                    BasicTextField(
                        value = translationFieldValue,
                        onValueChange = {
                            translationFieldValue = it
                        },
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier.verticalScroll(stateVertical)
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical),
                        style = scrollbarStyle,
                    )
                }

            }

            Column(modifier = modifier) {
                Text("英语释义：",color = MaterialTheme.colors.onBackground)
                Box(modifier = Modifier.fillMaxWidth()
                    .height(160.dp)
                    .border(border = border)) {
                    val stateVertical = rememberScrollState(0)
                    BasicTextField(
                        value = definitionFieldValue,
                        onValueChange = {
                            definitionFieldValue = it
                        },
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = textStyle,
                        modifier = Modifier
                            .verticalScroll(stateVertical)
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical),
                        style = scrollbarStyle,
                    )
                }
            }
            var linkSize by remember { mutableStateOf(tempWord.externalCaptions.size) }
            EditingCaptions(
                videoVolume = appState.global.videoVolume,
                playerWindow = appState.videoPlayerWindow,
                videoPlayerComponent = appState.videoPlayerComponent,
                vocabularyDir = vocabularyDir,
                vocabularyType = vocabulary.type,
                subtitlesTrackId = vocabulary.subtitlesTrackId,
                relateVideoPath = vocabulary.relateVideoPath,
                setLinkSize = { linkSize = it },
                word = tempWord
            )

            if (vocabulary.type == VocabularyType.DOCUMENT && linkSize <= 3) {
                var isLink by remember { mutableStateOf(false) }
                if (isLink) {
                    LinkCaptionDialog(
                        word = tempWord.deepCopy(),
                        appState = appState,
                        vocabulary = vocabulary,
                        vocabularyDir = vocabularyDir,
                        save = {
                            tempWord.externalCaptions = it
                            linkSize = it.size
                            isLink = false
                        },
                        close = { isLink = false }
                    )
                }

                if (!isLink && linkSize < 3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { isLink = true },
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text("链接字幕", modifier = Modifier.padding(end = 10.dp))
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "",
                                tint = MaterialTheme.colors.primary,
                            )
                        }

                    }
                }
            }
        }

        Surface (Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colors.background)

            ) {
                Divider()
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp,bottom = 10.dp)
                ) {
                    TooltipArea(
                        tooltip = {
                            Surface(
                                elevation = 4.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                shape = RectangleShape
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    val ctrl = if (isMacOS()) "⌃" else "Ctrl"
                                    Text(text = "保存单词")
                                    CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                        Text(text = " $ctrl + S ")
                                    }
                                }
                            }
                        },
                        delayMillis = 300,
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopCenter,
                            alignment = Alignment.TopCenter,
                            offset = DpOffset.Zero
                        )
                    ) {
                        OutlinedButton(onClick = { save() }, enabled = inputWordStr.text.isNotEmpty()) {
                            Text("保存")
                        }
                    }

                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { close() }) {
                        Text("关闭")
                    }
                }
            }
        }

        if(addSuccess){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.Center)
                    .border(BorderStroke(1.dp,MaterialTheme.colors.onSurface.copy(0.12f)))){
                Surface(
                    elevation = 5.dp,
                    shape = RectangleShape,
                ) {
                    Text("添加成功" ,color = MaterialTheme.colors.primary,modifier = Modifier.padding(20.dp))
                }
                LaunchedEffect(addSuccess){
                    delay(2000)
                    addSuccess = false
                }
            }
        }

    }
}


@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
)
@Composable
fun EditingCaptions(
    videoVolume: Float,
    playerWindow: JFrame,
    videoPlayerComponent: Component,
    relateVideoPath: String,
    subtitlesTrackId: Int,
    vocabularyType: VocabularyType,
    vocabularyDir: File,
    setLinkSize: (Int) -> Unit,
    word: Word
) {
    val scope = rememberCoroutineScope()
    val playTripleMap = getPlayTripleMap(vocabularyType,subtitlesTrackId,relateVideoPath, word)
    playTripleMap.forEach { (index, playTriple) ->
        val captionContent = playTriple.first.content
        val relativeVideoPath = playTriple.second

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp)
        ) {
            var isEdit by remember{ mutableStateOf(false) }
            var editCaptionContent by remember{ mutableStateOf(TextFieldValue(captionContent)) }
            Box(Modifier.padding(bottom = if(isEdit) 10.dp else 0.dp)){
                if(!isEdit){
                    Text(text = captionContent, color = MaterialTheme.colors.onBackground,modifier = Modifier.padding(start = 10.dp))
                }else{
                    BasicTextField(
                        value = editCaptionContent,
                        onValueChange = { editCaptionContent = it },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            color = MaterialTheme.colors.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier
                            .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                            .padding(10.dp)
                    )
                }
            }

            Row {

                IconButton(onClick = {
                    isEdit = !isEdit
                    scope.launch {
                        if (vocabularyType == VocabularyType.DOCUMENT) {
                            word.externalCaptions[index].content = editCaptionContent.text
                        } else {
                            word.captions[index].content = editCaptionContent.text
                        }
                    }
                },modifier = Modifier){
                    Icon(
                        imageVector = if(isEdit) Icons.Filled.Save else Icons.Filled.Edit,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground
                    )
                }

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
                var playFailed by remember{mutableStateOf(false)}
                Box{
                    IconButton(onClick = {},
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                                val location =
                                    pointerEvent.awtEventOrNull?.locationOnScreen
                                if (location != null) {
                                    if(isVideoBoundsChanged){
                                        mousePoint.x = location.x - 270 + 24
                                        mousePoint.y = location.y - 320
                                    }else{
                                        playerBounds.x = location.x - 270 + 24
                                        playerBounds.y = location.y - 320
                                    }

                                    if (!isPlaying) {
                                        isPlaying = true
                                        // 使用绝对地址
                                        val absFile = File(relativeVideoPath)
                                        // 如果在绝对地址找不到，就在词库所在的文件夹寻找
                                        val relFile = File(vocabularyDir,absFile.name)
                                        if (absFile.exists() || relFile.exists()) {
                                            val playParameter = if(absFile.exists()){
                                                playTriple
                                            }else{
                                                Triple(playTriple.first,relFile.absolutePath,playTriple.third)
                                            }
                                            scope.launch {
                                                play(
                                                    window = playerWindow,
                                                    setIsPlaying = { isPlaying = it },
                                                    volume = videoVolume,
                                                    playTriple = playParameter,
                                                    videoPlayerComponent = videoPlayerComponent,
                                                    bounds = playerBounds,
                                                    resetVideoBounds = resetVideoBounds,
                                                    isVideoBoundsChanged = isVideoBoundsChanged,
                                                    setIsVideoBoundsChanged = { isVideoBoundsChanged = it }
                                                )
                                            }
                                        } else {
                                            playFailed = true
                                            isPlaying = false
                                        }
                                    }
                                }
                            }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    DropdownMenu(
                        expanded = playFailed,
                        onDismissRequest = { playFailed = false },
                        modifier = Modifier.width(210.dp).height(40.dp)
                    ) {
                        Surface(
                            elevation = 5.dp,
                            shape = RectangleShape,
                        ) {
                            Column(Modifier.width(210.dp).height(40.dp)) {
                                Text("播放失败，视频地址错误", color = MaterialTheme.colors.onBackground,modifier = Modifier.padding(start = 10.dp))
                            }
                            LaunchedEffect(Unit){
                                delay(2000)
                                playFailed = false
                            }
                        }
                    }
                }


                var showSettingTimeLineDialog by remember { mutableStateOf(false) }
                if (showSettingTimeLineDialog) {
                    SettingTimeLine(
                        index = index,
                        videoVolume = videoVolume,
                        playerWindow = playerWindow,
                        playTriple = playTriple,
                        mediaPlayerComponent = videoPlayerComponent,
                        confirm = { (index, start, end) ->
                            scope.launch {
                                if (vocabularyType == VocabularyType.DOCUMENT) {
                                    word.externalCaptions[index].start = secondsToString(start)
                                    word.externalCaptions[index].end = secondsToString(end)
                                } else {
                                    word.captions[index].start = secondsToString(start)
                                    word.captions[index].end = secondsToString(end)
                                }
                            }
                        },
                        close = { showSettingTimeLineDialog = false }
                    )
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "调整时间", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        showSettingTimeLineDialog = true
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.SpaceBar,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
                var showConfirmationDialog by remember { mutableStateOf(false) }
                if (showConfirmationDialog) {
                    ConfirmDialog(
                        message = "确定要删除 $captionContent 吗？",
                        confirm = {
                            scope.launch {
                                if (vocabularyType == VocabularyType.DOCUMENT) {
                                    word.externalCaptions.removeAt(index)
                                } else {
                                    word.captions.removeAt(index)
                                }
                                playTripleMap.remove(index)
                                setLinkSize(playTripleMap.size)
                                showConfirmationDialog = false
                            }
                        },
                        close = { showConfirmationDialog = false }
                    )
                }
                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            Text(text = "删除", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 300,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset.Zero
                    )
                ) {
                    IconButton(onClick = {
                        showConfirmationDialog = true

                    }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "",
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
            }

        }
    }
}

/**
 * 调整字幕时间轴
 * @param index 字幕的索引
 * @param close 点击取消后调用的回调
 * @param confirm 点击确定后调用的回调
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 */
@ExperimentalComposeUiApi
@Composable
fun SettingTimeLine(
    index: Int,
    videoVolume: Float,
    playerWindow: JFrame,
    close: () -> Unit,
    confirm: (Triple<Int, Double, Double>) -> Unit,
    playTriple: Triple<Caption, String, Int>,
    mediaPlayerComponent: Component,
) {
    Dialog(
        title = "调整时间轴",
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(700.dp, 700.dp)
        ),
    ) {
        /** 协程构建器 */
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        /** 视频地址 */
        val relativeVideoPath = playTriple.second
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
        var isSPressed by remember { mutableStateOf(false)}

        var isEPressed by remember { mutableStateOf(false)}
        var isPlaying by remember { mutableStateOf(false) }
        var isSAndDirectionPressed by remember { mutableStateOf(false)}
        var isEAndDirectionPressed by remember { mutableStateOf(false)}

        /** 字幕内容 */
        val caption = playTriple.first
        /** 当前字幕的开始时间，单位是秒 */
        var start by remember {
            mutableStateOf(
                LocalTime.parse(caption.start, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                    .toNanoOfDay().toDouble().div(1000_000_000)
            )
        }
        /** 当前字幕的结束时间，单位是秒 */
        var end by remember {
            mutableStateOf(
                LocalTime.parse(caption.end, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                    .toNanoOfDay().toDouble().div(1000_000_000)
            )
        }
        /** 调整时间轴的精度 */
        var precise by remember { mutableStateOf(1f) }

        val oldStart = caption.start
        val oldEnd = caption.end

        val play = {
            isPlaying = true
            val file = File( relativeVideoPath)
            if (file.exists()) {
                scope.launch {
                    playTriple.first.start = secondsToString(start)
                    playTriple.first.end = secondsToString(end)
                    play(
                        window = playerWindow,
                        setIsPlaying = { isPlaying = it },
                        volume = videoVolume,
                        playTriple = playTriple,
                        videoPlayerComponent = mediaPlayerComponent,
                        bounds = playerBounds,
                        resetVideoBounds = resetVideoBounds,
                        isVideoBoundsChanged = isVideoBoundsChanged,
                        setIsVideoBoundsChanged = { isVideoBoundsChanged = it }
                    )
                }

            } else {
                println("视频地址错误")
            }
        }

        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Box(modifier = Modifier.fillMaxSize()
                .focusable()
                .focusRequester(focusRequester)
                .onKeyEvent {
                    if (it.key == Key.Spacebar && it.type == KeyEventType.KeyUp) {
                        play()
                        true
                    }   else if (it.key == Key.S && it.type == KeyEventType.KeyDown) {
                        isSPressed = true
                        true
                    } else if (it.key == Key.E && it.type == KeyEventType.KeyDown) {
                        isEPressed = true
                        true
                    } else if (it.key == Key.E && it.type == KeyEventType.KeyUp) {
                        isEPressed = false
                        if (isEAndDirectionPressed) {
                            play()
                            isEAndDirectionPressed = false
                        }
                        true
                    } else if (it.key == Key.S && it.type == KeyEventType.KeyUp) {
                        isSPressed = false
                        if (isSAndDirectionPressed) {
                            play()
                            isSAndDirectionPressed = false
                        }
                        true

                    } else if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyUp) {
                        if (isSPressed) {
                            isSAndDirectionPressed = true
                            start -= precise
                        } else if (isEPressed) {
                            isEAndDirectionPressed = true
                            end -= precise
                        }
                        true
                    } else if (it.key == Key.DirectionUp && it.type == KeyEventType.KeyDown) {
                        if (isSPressed) {
                            isSAndDirectionPressed = true
                            start += precise
                        } else if (isEPressed) {
                            isEAndDirectionPressed = true
                            end += precise
                        }

                        true
                    } else false
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    Divider()
                    Box(
                        modifier = Modifier.width(540.dp).height(303.dp)
                        .background(Color.Black)
                        .onGloballyPositioned { coordinates ->
                            val rect = coordinates.boundsInWindow()
                            if(isVideoBoundsChanged){
                                mousePoint.x = window.x + rect.left.toInt()
                                mousePoint.y = window.y + rect.top.toInt()
                            }else{
                                playerBounds.x = window.x + rect.left.toInt() + 10
                                playerBounds.y = window.y + rect.top.toInt()+35
                            }

                        })
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(top = 40.dp)
                    ) {

                        Text("开始:")
                        TimeControl(
                            type= "start",
                            time = start,
                            addTime = { start += it },
                            minusTime = { start -= it },
                            precise = precise,
                        )
                        Spacer(Modifier.width(30.dp))
                        Text("结束:")
                        TimeControl(
                            type= "end",
                            time = end,
                            addTime = { end += it },
                            minusTime = { end -= it },
                            precise = precise,
                        )
                        Spacer(Modifier.width(30.dp))
                        var expanded by remember { mutableStateOf(false) }
                        Text("精度:")
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .width(93.dp)
                                    .background(Color.Transparent)
                                    .border(1.dp, Color.Transparent)
                            ) {
                                Text(text = "${precise}S")
                                Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                            }
                            val menuItemModifier = Modifier.width(93.dp).height(30.dp)
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.width(93.dp)
                                    .height(190.dp)
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        precise = 1f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("1S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = 0.5f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.5S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = 0.15f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.1S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = 0.05f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.05S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise = 0.15f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.01S")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        precise =0.01f
                                        expanded = false
                                    },
                                    modifier = menuItemModifier
                                ) {
                                    Text("0.001S")
                                }
                            }

                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.width(20.dp))
                        OutlinedButton(onClick = {
                            if (oldStart != secondsToString(start) ||
                                oldEnd != secondsToString(end)
                            ) {
                                confirm(Triple(index, start, end))
                            }
                            close()
                        }) {
                            Text("确定")
                        }

                        OutlinedButton(onClick = {},
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                                    val location =
                                        pointerEvent.awtEventOrNull?.locationOnScreen
                                    if (location != null && !isPlaying) {
                                        play()
                                    }
                                }
                        ) {
                            Text("预览")
                        }


                        OutlinedButton(onClick = { close() }, modifier = Modifier.padding(start = 20.dp)) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 调整时间轴的开始或结束时间
 * @param time 时间
 * @param addTime 点击增加按钮后调用的回调
 * @param minusTime 点击减少按钮后调用的回调
 * @param precise 调整精度
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeControl(
    type:String,
    time: Double,
    addTime: (Float) -> Unit,
    minusTime: (Float) -> Unit,
    precise: Float,
) {
    Text(text = secondsToString(time))
    Column {

        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(text = "快捷键 ")
                        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                            val shortcut = if(type == "start") "S + " else "E + "
                            Text(text = shortcut)
                            Text(text = "↑",
                                fontWeight = FontWeight.Black,
                                style = TextStyle(fontSize = 17.sp)
                                ,modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }

                }
            },
            delayMillis = 300,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.TopCenter,
                alignment = Alignment.TopCenter,
                offset = DpOffset.Zero
            ),
        ) {

            Icon(Icons.Filled.Add,
                contentDescription = "",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.clickable {
                    addTime(precise)
                })
        }


        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(text = "快捷键 ",modifier = Modifier.padding(top = 5.dp))
                        CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                            val shortcut = if(type == "start") "S + " else "E + "
                            Text(text = shortcut, modifier = Modifier.padding(top = 5.dp))
                            Text(text = "↓",
                                fontWeight = FontWeight.Black,
                                style = TextStyle(fontSize = 17.sp))
                        }
                    }
                    }

            },
            delayMillis = 300,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.TopCenter,
                alignment = Alignment.TopCenter,
                offset = DpOffset.Zero
            ),
        ) {

            Icon(Icons.Filled.Remove,
                contentDescription = "",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.clickable {
                    minusTime(precise)
                })
        }

    }
}
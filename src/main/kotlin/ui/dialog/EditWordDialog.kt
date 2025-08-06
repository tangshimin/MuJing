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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.formdev.flatlaf.extras.FlatSVGUtils
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.AppState
import state.getResourcesFile
import ui.edit.displayExchange
import ui.edit.toAwtSize
import ui.edit.toPoint
import ui.window.windowBackgroundFlashingOnCloseFixHack
import ui.wordscreen.getPlayTripleMap
import ui.wordscreen.secondsToString
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
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
    val position = if(parentWindow != null) {
        WindowPosition(parentWindow.location.x.dp, parentWindow.location.y.dp)
    }else{
        WindowPosition(Alignment.Center)
        }

    val height = if (java.awt.Toolkit.getDefaultToolkit().screenSize.height > 720) 984.dp else 600.dp
    val windowState =  rememberDialogState(
        position = position,
        size = DpSize(840.dp, height)
    )
    MaterialTheme(colors = appState.colors) {
        DialogWindow(
            title = title,
            onCloseRequest = { close() },
            resizable = true,
            state = windowState,
        ) {
            windowBackgroundFlashingOnCloseFixHack()
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
    val height = if (java.awt.Toolkit.getDefaultToolkit().screenSize.height > 720) 984 else 600
    window.setSize(840, height)
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
    var sentencesFieldValue by remember { mutableStateOf(TextFieldValue(tempWord.pos)) }
    var exchange by remember { mutableStateOf(tempWord.exchange) }
    var saveEnable by remember { mutableStateOf(false) }
    var captionsChanged by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(inputWordStr, usphone, ukphone, translationFieldValue, sentencesFieldValue, definitionFieldValue, exchange,captionsChanged,tempWord.captions.size,tempWord.externalCaptions.size) {
        // 单词不为空，并且任何一个字段发生变化，就激活保存按钮
        saveEnable = (
                inputWordStr.text.isNotEmpty() &&
                        (
                                (inputWordStr.text != word.value)
                                        || (usphone.text != word.usphone)
                                        || (ukphone.text != word.ukphone)
                                        || (definitionFieldValue.text != word.definition)
                                        || (translationFieldValue.text != word.translation)
                                        || (sentencesFieldValue.text != word.pos)
                                        || (exchange != word.exchange)
                                        || (tempWord.captions.size != word.captions.size)
                                        || (tempWord.externalCaptions.size != word.externalCaptions.size)
                                        || captionsChanged
                                )
                )
    }
    val saveWord = {
        if(saveEnable){
            tempWord.value = inputWordStr.text
            tempWord.usphone = usphone.text
            tempWord.ukphone = ukphone.text
            tempWord.translation = translationFieldValue.text
            tempWord.pos = sentencesFieldValue.text
            tempWord.definition = definitionFieldValue.text
            tempWord.exchange = exchange
            save(tempWord)
            if(title =="添加单词"){
                tempWord = Word("")
                inputWordStr = TextFieldValue("")
                usphone = TextFieldValue("")
                ukphone = TextFieldValue("")
                translationFieldValue = TextFieldValue("")
                sentencesFieldValue = TextFieldValue("")
                definitionFieldValue = TextFieldValue("")
                exchange = ""
            }
        }
    }

    Box (Modifier
        .fillMaxSize()
        .onKeyEvent {
        if(it.key == Key.Escape && it.type == KeyEventType.KeyUp){
            close()
            true
        }else if(it.key == Key.S && it.isCtrlPressed && it.type == KeyEventType.KeyUp){
            saveWord()
            true

        }else false
    }){
        val stateVertical = rememberScrollState(0)
        val scrollbarStyle =  LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp))
        Column(Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .background(MaterialTheme.colors.background)
            .verticalScroll(stateVertical)
            .padding(bottom = 75.dp)
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
                    scope.launch(Dispatchers.Default){
                            val resultWord = Dictionary.query(inputWordStr.text)
                            if (resultWord != null) {
                                tempWord = resultWord
                                usphone = TextFieldValue(resultWord.usphone)
                                ukphone = TextFieldValue(resultWord.ukphone)
                                translationFieldValue = TextFieldValue(resultWord.translation)
                                sentencesFieldValue = TextFieldValue(resultWord.pos)
                                definitionFieldValue = TextFieldValue(resultWord.definition)
                                exchange = resultWord.exchange
                                queryFailed = false
                            } else {
                                queryFailed = true
                            }
                    }

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


            val modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            Column(modifier = modifier) {
                Text("中文释义：",color = MaterialTheme.colors.onBackground)
                Box(modifier = Modifier.fillMaxWidth()
                    .height(160.dp)
                    .border(border = border)) {
                    val scrollState = rememberScrollState(0)
                    BasicTextField(
                        value = translationFieldValue,
                        onValueChange = {
                            translationFieldValue = it
                        },
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState),
                        style = scrollbarStyle
                    )
                }

            }

            Column(modifier = modifier) {
                Text("英语释义：",color = MaterialTheme.colors.onBackground)
                Box(modifier = Modifier.fillMaxWidth()
                    .height(160.dp)
                    .border(border = border)) {
                    val scrollState = rememberScrollState(0)
                    BasicTextField(
                        value = definitionFieldValue,
                        onValueChange = {
                            definitionFieldValue = it
                        },
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = textStyle,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState),
                        style = scrollbarStyle
                    )
                }
            }
            Column(modifier = modifier) {
                Text("例句：",color = MaterialTheme.colors.onBackground)
                Box(modifier = Modifier.fillMaxWidth()
                    .height(140.dp)
                    .border(border = border)) {
                    val scrollState = rememberScrollState(0)
                    BasicTextField(
                        value = sentencesFieldValue,
                        onValueChange = {
                            sentencesFieldValue = it
                        },
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        textStyle = textStyle,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState),
                        style = scrollbarStyle
                    )
                }
            }
            var linkSize by remember { mutableStateOf(tempWord.externalCaptions.size) }
            EditingCaptions(
                vocabularyDir = vocabularyDir,
                vocabularyType = vocabulary.type,
                subtitlesTrackId = vocabulary.subtitlesTrackId,
                relateVideoPath = vocabulary.relateVideoPath,
                setLinkSize = { linkSize = it },
                captionsChanged = { captionsChanged = it },
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

                        OutlinedButton(onClick = { saveWord() }, enabled = saveEnable) {
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

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical),
            style = scrollbarStyle,
            )
    }
}


@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class
)
@Composable
fun EditingCaptions(
    relateVideoPath: String,
    subtitlesTrackId: Int,
    vocabularyType: VocabularyType,
    vocabularyDir: File,
    setLinkSize: (Int) -> Unit,
    captionsChanged: (Boolean) -> Unit,
    word: Word
) {
    val scope = rememberCoroutineScope()
    val playTripleMap = getPlayTripleMap(vocabularyType,subtitlesTrackId,relateVideoPath, word)
    playTripleMap.forEach { (index, playTriple) ->
        val captionContent = playTriple.first.content

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
                            if(word.externalCaptions[index].content != editCaptionContent.text){
                                word.externalCaptions[index].content = editCaptionContent.text
                                captionsChanged(true)
                            }
                        } else {
                            if(word.captions[index].content != editCaptionContent.text){
                                word.captions[index].content = editCaptionContent.text
                                captionsChanged(true)
                            }
                        }
                    }
                },modifier = Modifier){
                    Icon(
                        imageVector = if(isEdit) Icons.Filled.Save else Icons.Filled.Edit,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                var showSettingTimeLineDialog by remember { mutableStateOf(false) }
                if (showSettingTimeLineDialog) {
                    SettingTimeLine(
                        index = index,
                        vocabularyDir = vocabularyDir,
                        mediaInfo = MediaInfo(
                            mediaPath = playTriple.second,
                            caption = playTriple.first,
                            trackId = playTriple.third
                        ),
                        confirm = { (index, start, end) ->
                            scope.launch {
                                if (vocabularyType == VocabularyType.DOCUMENT) {
                                    if(word.externalCaptions[index].start != secondsToString(start) || word.externalCaptions[index].end != secondsToString(end)){
                                        word.externalCaptions[index].start = secondsToString(start)
                                        word.externalCaptions[index].end = secondsToString(end)
                                        captionsChanged(true)
                                    }
                                } else {
                                    if(word.captions[index].start != secondsToString(start) || word.captions[index].end != secondsToString(end)){
                                        word.captions[index].start = secondsToString(start)
                                        word.captions[index].end = secondsToString(end)
                                        captionsChanged(true)
                                    }
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
 * @param vocabularyDir 词库目录
 * @param mediaInfo 视频信息，包含视频路径、字幕开始时间、结束时间和字幕轨道ID
 */
@ExperimentalComposeUiApi
@Composable
fun SettingTimeLine(
    index: Int,
    close: () -> Unit,
    vocabularyDir:File,
    confirm: (Triple<Int, Double, Double>) -> Unit,
    mediaInfo: MediaInfo,
) {
    DialogWindow(
        title = "调整时间轴",
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(700.dp, 700.dp)
        ),
    ) {

        val videoPlayerComponent  = remember { createMediaPlayerComponent2() }
        val videoPlayer = remember { videoPlayerComponent.createMediaPlayer() }
        val surface = remember {
            SkiaImageVideoSurface().also {
                videoPlayer.videoSurface().set(it)
            }
        }
        /** 协程构建器 */
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        var isSPressed by remember { mutableStateOf(false)}
        var isEPressed by remember { mutableStateOf(false)}
        var isSAndDirectionPressed by remember { mutableStateOf(false)}
        var isEAndDirectionPressed by remember { mutableStateOf(false)}
        var errorMessage by remember { mutableStateOf("") }


        /** 当前字幕的开始时间，单位是秒 */
        var start by remember {
            mutableStateOf(
                convertTimeToSeconds(mediaInfo.caption.start)
            )
        }
        /** 当前字幕的结束时间，单位是秒 */
        var end by remember {
            mutableStateOf(
                convertTimeToSeconds(mediaInfo.caption.end)
            )
        }
        /** 调整时间轴的精度 */
        var precise by remember { mutableStateOf(1f) }

        val oldStart = mediaInfo.caption.start
        val oldEnd = mediaInfo.caption.end

        val play = {
            scope.launch {
                if(!videoPlayer.status().isPlaying){
                    // 验证视频文件的路径
                    val resolvedPath =  resolveMediaPath(mediaInfo.mediaPath, vocabularyDir)
                    if(resolvedPath != ""){
                        mediaInfo.mediaPath = resolvedPath
                    }else{
                       errorMessage =  if(mediaInfo.mediaPath.isEmpty())"视频地址为空" else "文件不存在:\n${mediaInfo.mediaPath}"
                      println(errorMessage)
                    }

                    videoPlayer.media().play(mediaInfo.mediaPath,":start-time=$start", ":stop-time=$end")
                }
            }

        }

        DisposableEffect(Unit) {
            onDispose {
                surface.release()
                videoPlayerComponent.release()
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
                    Box(modifier = Modifier.width(540.dp).height(303.dp)){
                        CustomCanvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .align(Alignment.Center),
                            surface = surface
                        )
                    }
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

                        OutlinedButton(onClick = { play() },
                            modifier = Modifier.padding(start = 20.dp)

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

       // 错误提示对话框
        if(errorMessage.isNotEmpty()){
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                title = { Text("错误",color = MaterialTheme.colors.error) },
                text = {
                    SelectionContainer { Text(errorMessage) }
                },
                confirmButton = {
                    OutlinedButton(onClick = { errorMessage = "" }) {
                        Text("确定")
                    }
                },
                modifier = Modifier.width(500.dp).height(250.dp)
            )
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
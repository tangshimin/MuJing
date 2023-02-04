package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.North
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import player.isMacOS
import player.isWindows
import state.GlobalState
import state.TextState
import ui.dialog.FormatDialog
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TypingText(
    title: String,
    window: ComposeWindow,
    globalState: GlobalState,
    saveGlobalState: () -> Unit,
    textState: TextState,
    saveTextState: () -> Unit,
    isOpenSettings: Boolean,
    setIsOpenSettings: (Boolean) -> Unit,
    setIsDarkTheme: (Boolean) -> Unit,
    futureFileChooser: FutureTask<JFileChooser>,
    openLoadingDialog: () -> Unit,
    closeLoadingDialog: () -> Unit,
    openSearch: () -> Unit,
    showPlayer :(Boolean) -> Unit,
){
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var pgUp by remember { mutableStateOf(false) }
    val lines = remember { readAllLines(textState.textPath) }
    val lastModified by remember { mutableStateOf(File(textState.textPath).lastModified()) }
    val monospace by remember { mutableStateOf(FontFamily(Font("font/Inconsolata-Regular.ttf", FontWeight.Normal, FontStyle.Normal))) }
    /** 汉语使用输入法输入文字时，可以一次输入多个汉字，可能会超出正在抄写的那一行的文本 */
    var remainWords by remember{ mutableStateOf("") }
    /** 正在抄写的下一行 */
    var nextRowFull by remember{ mutableStateOf(false) }

    /** 显示格式化对话框 */
    var showFormatDialog by remember{ mutableStateOf(false) }
    /** 这一行的字母超过了 75 个字母*/
    var row by remember { mutableStateOf( -1) }
    var formatPath by remember{ mutableStateOf("") }


    /** 播放按键音效 */
    val playKeySound = {
        if (globalState.isPlayKeystrokeSound) {
            playSound("audio/keystroke.wav", globalState.keystrokeVolume)
        }
    }


    /** 改变文本路径 */
    val changeTextPath :(File) -> Unit = { file ->
        textState.textPath = file.absolutePath
        // 清除 focus 后，当前正在抄写的文本数据会被清除
        focusManager.clearFocus()
        textState.currentIndex = 0
        textState.firstVisibleItemIndex = 0
        lines.clear()
        lines.addAll(readAllLines(textState.textPath))
        saveTextState()
    }

    if(showFormatDialog){
        FormatDialog(
            close = {showFormatDialog = false},
            row = row,
            formatPath = formatPath,
            futureFileChooser = futureFileChooser,
            changeTextPath = {changeTextPath(it)}
        )
    }

    /** 解析打开的文件 */
    val parseImportFile: (List<File>, OpenMode) -> Unit = { files, openMode ->
        val file = files.first()
        scope.launch {
            Thread(Runnable {
                if(file.extension == "txt"){
                    // 拖放的文件和已有的文件不一样，或者文件路径一样，但是后面又修改了。
                    if(textState.textPath != file.absolutePath || lastModified == file.lastModified()){
                        val result = isGreaterThan75(file)
                        if(!result.first){
                            changeTextPath(file)
                        }else{
                            formatPath = file.absolutePath
                            row = result.second + 1
                            showFormatDialog = true
                        }

                    }else {
                        JOptionPane.showMessageDialog(window, "文件已打开")
                    }

                }else{
                    JOptionPane.showMessageDialog(window, "格式不支持")
                }
            }).start()
        }
    }

    /** 打开文件对话框 */
    val openFileChooser: () -> Unit = {
        // 打开 windows 的文件选择器很慢，有时候会等待超过2秒
        openLoadingDialog()
        Thread(Runnable {
            val fileChooser = futureFileChooser.get()
            fileChooser.dialogTitle = "打开文本"
            fileChooser.fileSystemView = FileSystemView.getFileSystemView()
            fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.isAcceptAllFileFilterUsed = false
            val fileFilter = FileNameExtensionFilter(" ","txt")
            fileChooser.addChoosableFileFilter(fileFilter)
            fileChooser.selectedFile = null
            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                parseImportFile(listOf(file),OpenMode.Open)
                closeLoadingDialog()
            } else {
                closeLoadingDialog()
            }
            fileChooser.selectedFile = null
            fileChooser.isMultiSelectionEnabled = false
            fileChooser.removeChoosableFileFilter(fileFilter)
        }).start()

    }
    /** 当前界面的快捷键 */
    val boxKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            (keyEvent.isCtrlPressed && keyEvent.key == Key.O && keyEvent.type == KeyEventType.KeyUp) -> {
                openFileChooser()
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.F && keyEvent.type == KeyEventType.KeyUp) -> {
                scope.launch {openSearch() }
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.D && keyEvent.type == KeyEventType.KeyUp) -> {
                setIsDarkTheme(!globalState.isDarkTheme)
                true
            }
            else -> false
        }
    }

    //设置窗口的拖放处理函数
    LaunchedEffect(Unit){
        val transferHandler = createTransferHandler(
            singleFile = true,
            showWrongMessage = { message ->
                JOptionPane.showMessageDialog(window, message)
            },
            parseImportFile = { parseImportFile(it,OpenMode.Drag) }
        )
        window.transferHandler = transferHandler
    }

    Box(Modifier.fillMaxSize()
        .background(MaterialTheme.colors.background)
        .focusRequester(focusRequester)
        .onKeyEvent(boxKeyEvent)
        .focusable()){

        LaunchedEffect(Unit){
            focusRequester.requestFocus()
        }

        Row(Modifier.fillMaxSize()){
            TypingTextSidebar(
                isOpen = isOpenSettings,
                isDarkTheme = globalState.isDarkTheme,
                setIsDarkTheme = {setIsDarkTheme(it)},
                openFileChooser = { openFileChooser() },
            )
            val topPadding = if (isMacOS()) 30.dp else 0.dp
            if (isOpenSettings) {
                Divider(Modifier.fillMaxHeight().width(1.dp).padding(top = topPadding))
            }
            Box(Modifier.fillMaxSize().padding(top = topPadding)) {
                if(lines.isNotEmpty()){
                    val listState = rememberLazyListState(textState.firstVisibleItemIndex)
                    val stateHorizontal = rememberScrollState(0)
                    val isAtTop by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                        }
                    }

                    LazyColumn(
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1200.dp)
                            .fillMaxHeight()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                            .horizontalScroll(stateHorizontal),
                    ) {
                        itemsIndexed(lines) {index,item ->
                            val line = item.ifEmpty { " " }
                            // 当 384 行的 BasicTextField 失去焦点时自动清理 typingResult 和 textFieldValue
                            val typingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }
                            var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
                            var selectable by remember { mutableStateOf(false) }
                            val selectRequester = remember { FocusRequester() }
                            val textFieldRequester = remember { FocusRequester() }

                            val next :() -> Unit = {
                                scope.launch {
                                    val end =
                                        listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size - 2
                                    if (index >= end) {
                                        listState.scrollToItem(index)
                                    }
                                    if(index+1 != lines.size){
                                        textState.currentIndex = textState.currentIndex + 1
                                    }
                                }
                            }
                            val previous :() -> Unit = {
                                scope.launch {
                                    // 向上翻页
                                    if(index == listState.firstVisibleItemIndex+1){
                                        var top = index - listState.layoutInfo.visibleItemsInfo.size
                                        if(top < 0) top = 0
                                        listState.scrollToItem(top)
                                        textState.currentIndex = index-1
                                        pgUp = true
                                    }else if(textState.currentIndex > 0){
                                        textState.currentIndex = textState.currentIndex - 1
                                    }

                                }
                            }
                            if(remainWords.isNotEmpty() && textState.currentIndex == index ){
                                // 最多只能跨两行,如果上一行剩余的单词超过了这一行，就只截取这一行的内容，剩下的丢弃
                               if(remainWords.length >= line.length){
                                    remainWords = remainWords.substring(0,line.length)
                                }
                                textFieldValue = TextFieldValue(remainWords, TextRange(remainWords.length))
                                val inputChars = remainWords.toMutableList()
                                for (i in inputChars.indices) {
                                    val inputChar = inputChars[i]
                                    val char = line[i]
                                    if (inputChar == char) {
                                        typingResult.add(Pair(inputChar, true))
                                        // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                                    } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                        typingResult.add(Pair(char, true))
                                        // 音乐符号不好输入，所以可以使用空格替换
                                    }else if (inputChar == ' ' && (char == '♪')) {
                                        typingResult.add(Pair(char, true))
                                        // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                                        inputChars.add(i,'♪')
                                        inputChars.removeAt(i+1)
                                        textFieldValue =  TextFieldValue(String(inputChars.toCharArray()),TextRange(inputChars.size))
                                    } else {
                                        typingResult.add(Pair(inputChar, false))
                                    }
                                }


                                if(textFieldValue.text.length == line.length){
                                    nextRowFull = true
                                }
                                remainWords = ""

                            }
                            /** 检查输入的回调函数 */
                            val checkTyping: (String) -> Unit = { input ->
                                    if (input.length > line.length) {
                                        if(nextRowFull){
                                            nextRowFull = false
                                            next()
                                        }else if (index+1 != lines.size){
                                            remainWords = input.substring(line.length)
                                        }

                                    } else {
                                        textFieldValue =  TextFieldValue(input,TextRange(input.length))
                                        typingResult.clear()
                                        val inputChars = input.toMutableList()
                                        for (i in inputChars.indices) {
                                            val inputChar = inputChars[i]
                                            val char = line[i]
                                            if (inputChar == char) {
                                                typingResult.add(Pair(inputChar, true))
                                                // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                                            } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                                                typingResult.add(Pair(char, true))
                                                // 音乐符号不好输入，所以可以使用空格替换
                                            }else if (inputChar == ' ' && (char == '♪')) {
                                                typingResult.add(Pair(char, true))
                                                // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                                                inputChars.add(i,'♪')
                                                inputChars.removeAt(i+1)
                                                textFieldValue = TextFieldValue(String(inputChars.toCharArray()),TextRange(inputChars.size))
                                            } else {
                                                typingResult.add(Pair(inputChar, false))
                                            }
                                        }
                                        if(input.length == line.length){
                                            next()
                                        }

                                    }
                            }

                            val textFieldKeyEvent: (KeyEvent) -> Boolean = { it: KeyEvent ->
                                when {
                                    ((it.key != Key.ShiftLeft && it.key != Key.ShiftRight) && it.type == KeyEventType.KeyDown) -> {
                                        playKeySound()
                                        true
                                    }
                                    ((it.key == Key.Enter ||it.key == Key.NumPadEnter || it.key == Key.DirectionDown) && it.type == KeyEventType.KeyUp) -> {
                                        next()
                                        true
                                    }

                                    ((it.key == Key.DirectionUp) && it.type == KeyEventType.KeyUp) -> {
                                        previous()
                                        true
                                    }
                                    ((it.key == Key.DirectionLeft) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current-20)
                                        }
                                        true
                                    }
                                    ((it.key == Key.DirectionRight) && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch {
                                            val current = stateHorizontal.value
                                            stateHorizontal.scrollTo(current+20)
                                        }
                                        true
                                    }
                                    (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
                                        scope.launch { selectable = !selectable }
                                        true
                                    }
                                    else -> false
                                }

                            }

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(900.dp)
                            ) {
                                val alpha = if(textState.currentIndex == index) ContentAlpha.high else ContentAlpha.medium
                                val textColor =  if(index <  textState.currentIndex){
                                    MaterialTheme.colors.primary.copy(alpha = if(MaterialTheme.colors.isLight) ContentAlpha.high else ContentAlpha.medium)
                                }else if(textState.currentIndex == index){
                                    MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                }else{
                                    MaterialTheme.colors.onBackground.copy(alpha = alpha)
                                }

                                Box(Modifier.width(IntrinsicSize.Max)) {

                                    if (textState.currentIndex == index) {
                                        Divider(
                                            Modifier.align(Alignment.BottomCenter)
                                                .background(MaterialTheme.colors.primary)
                                        )
                                    }

                                    BasicTextField(
                                        value = textFieldValue,
                                        onValueChange = { checkTyping(it.text) },
                                        singleLine = true,
                                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                        textStyle = MaterialTheme.typography.h5.copy(
                                            color = Color.Transparent,
                                            fontFamily = monospace
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp)
                                            .align(Alignment.CenterStart)
                                            .focusable()
                                            .onKeyEvent { textFieldKeyEvent(it) }
                                            .focusRequester(textFieldRequester)
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    scope.launch {
                                                        textState.currentIndex = index
                                                        textState.firstVisibleItemIndex =
                                                            listState.firstVisibleItemIndex
                                                        saveTextState()
                                                    }
                                                } else if (textFieldValue.text.isNotEmpty()) {
                                                    typingResult.clear()
                                                    textFieldValue = TextFieldValue()
                                                }
                                            }
                                    )
                                    if(pgUp){
                                        SideEffect {
                                            if(textState.currentIndex == index){
                                                textFieldRequester.requestFocus()
                                                pgUp = false
                                            }
                                        }
                                    }
                                    SideEffect {
                                        if (textState.currentIndex == index) {
                                            textFieldRequester.requestFocus()
                                        }
                                    }
                                    Text(
                                        text = buildAnnotatedString {

                                            typingResult.forEach { (char, correct) ->
                                                if (correct) {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = MaterialTheme.colors.primary.copy(alpha = alpha),
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = monospace,
                                                        )
                                                    ) {
                                                        append(char)
                                                    }
                                                } else {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = Color.Red,
                                                            fontSize = MaterialTheme.typography.h5.fontSize,
                                                            letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                            fontFamily = monospace,
                                                        )
                                                    ) {
                                                        if (char == ' ') {
                                                            append("_")
                                                        } else {
                                                            append(char)
                                                        }

                                                    }
                                                }
                                            }
                                            var remainChars = line.substring(typingResult.size)


                                            withStyle(
                                                style = SpanStyle(
                                                    color = textColor,
                                                    fontSize = MaterialTheme.typography.h5.fontSize,
                                                    letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                                                    fontFamily = monospace,
                                                )
                                            ) {
                                                append(remainChars)
                                            }
                                        },
                                        textAlign = TextAlign.Start,
                                        color = MaterialTheme.colors.onBackground,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(bottom = 5.dp)
                                    )

                                    DropdownMenu(
                                        expanded = selectable,
                                        focusable = true,
                                        onDismissRequest = {
                                            selectable = false
                                        },
                                        offset = DpOffset(0.dp, (-50).dp)
                                    ) {
                                        BasicTextField(
                                            value = line,
                                            onValueChange = {},
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                            textStyle = MaterialTheme.typography.h5.copy(
                                                fontFamily = monospace,
                                                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
                                            ),
                                            modifier = Modifier.focusable()
                                                .padding(top = 2.5.dp,bottom = 2.5.dp)
                                                .focusRequester(selectRequester)
                                                .onKeyEvent {
                                                    if (it.isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                                                        scope.launch { selectable = !selectable }
                                                        true
                                                    }else if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyUp) {
                                                        scope.launch {openSearch() }
                                                        true
                                                    } else false
                                                }
                                        )
                                        LaunchedEffect(Unit) {
                                            selectRequester.requestFocus()
                                        }

                                    }
                                }

                            }

                        }
                    }

                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                    HorizontalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        adapter = rememberScrollbarAdapter(stateHorizontal)
                    )
                    if (!isAtTop) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(0)
                                    textState.currentIndex = 0
                                    textState.firstVisibleItemIndex = 0
                                    focusManager.clearFocus()
                                    saveTextState()
                                }
                            },
                            backgroundColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 100.dp, bottom = 100.dp)
                        ) {
                            Icon(
                                Icons.Filled.North,
                                contentDescription = "Localized description",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                }else{
                    Text(
                        text = "可以拖放 TXT 文本到这里",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.align(Alignment.Center)
                        )
                }

            }
        }

        if (isMacOS()) {
            MacOSTitle(
                title = title,
                window = window,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp)
            )
        }
        Toolbar(
            isOpen = isOpenSettings,
            setIsOpen = setIsOpenSettings,
            modifier = Modifier.align(Alignment.TopStart),
            globalState = globalState,
            saveGlobalState = saveGlobalState,
            showPlayer = showPlayer
        )
    }

}
 fun readAllLines(textPath:String):SnapshotStateList<String>{
     val list = mutableStateListOf<String>()
     try{
         val lines = Files.readAllLines(Paths.get(textPath))
         list.addAll(lines)
     }catch (exception:Exception){
         println(exception.message)
     }

    return list
}


@Composable
fun TypingTextSidebar(
    isOpen:Boolean,
    isDarkTheme:Boolean,
    setIsDarkTheme:(Boolean) -> Unit,
    openFileChooser: () -> Unit,
){
    if(isOpen){
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
                ){
            Spacer(Modifier.fillMaxWidth().height(if (isMacOS()) 78.dp else 48.dp))
            Divider()
            val ctrl = LocalCtrl.current
            val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { openFileChooser() }
                    .fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("打开文件", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+O",
                        color = MaterialTheme.colors.onBackground
                    )
                }
                Spacer(Modifier.width(15.dp))
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { }.padding(start = 16.dp, end = 8.dp)
            ) {
                Row {
                    Text("深色模式", color = MaterialTheme.colors.onBackground)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "$ctrl+D",
                        color = MaterialTheme.colors.onBackground
                    )
                }

                Spacer(Modifier.width(15.dp))
                Switch(
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                    checked = isDarkTheme,
                    onCheckedChange = { setIsDarkTheme(it) },
                )
            }


        }
    }
}


/** 检测文本的每一行长度，是否超过 75 个字母 */
private fun isGreaterThan75(file:File):Pair<Boolean,Int> {
    file.useLines { lines ->
        lines.forEachIndexed { index,line ->
            if(line.length > 75){
                return Pair(true,index)
            }
        }
    }

    return Pair(false,-1)
}
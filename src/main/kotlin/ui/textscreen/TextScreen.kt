package ui.textscreen

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.isMacOS
import player.isWindows
import state.GlobalState
import theme.LocalCtrl
import ui.components.MacOSTitle
import ui.components.RemoveButton
import ui.components.Toolbar
import ui.dialog.FormatDialog
import ui.subtitlescreen.OpenMode
import ui.subtitlescreen.videoFormatList
import ui.wordscreen.playSound
import util.createDragAndDropTarget
import util.rememberMonospace
import util.shouldStartDragAndDrop
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JOptionPane

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TextScreen(
    title: String,
    window: ComposeWindow,
    globalState: GlobalState,
    saveGlobalState: () -> Unit,
    textState: TextState,
    saveTextState: () -> Unit,
    isOpenSettings: Boolean,
    setIsOpenSettings: (Boolean) -> Unit,
    openSearch: () -> Unit,
    showVideoPlayer :(Boolean) -> Unit,
    setVideoPath:(String) -> Unit,
){
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var pgUp by remember { mutableStateOf(false) }
    val lines = remember { readAllLines(textState.textPath) }
    val lastModified by remember { mutableStateOf(File(textState.textPath).lastModified()) }
    val monospace  = rememberMonospace()
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

    /** 关闭当前文本 */
    val removeText:() -> Unit = {
        textState.textPath = ""
        focusManager.clearFocus()
        textState.currentIndex = 0
        textState.firstVisibleItemIndex = 0
        lines.clear()
        saveTextState()
    }

    if(showFormatDialog){
        FormatDialog(
            close = {showFormatDialog = false},
            row = row,
            formatPath = formatPath,
            changeTextPath = {changeTextPath(it)}
        )
    }

    /** 解析打开的文件 */
    val parseImportFile: (List<File>, OpenMode) -> Unit = { files, openMode ->
        val file = files.first()
        scope.launch(Dispatchers.Default) {
                val extension = file.extension
                if (extension == "txt") {
                    // 拖放的文件和已有的文件不一样，或者文件路径一样，但是后面又修改了。
                    if (textState.textPath != file.absolutePath || lastModified == file.lastModified()) {
                        // 检查一行是否超过 75个字符
                        val result = isGreaterThan75(file)
                        // 如果没有超过 75 个字符，马上就可以开始抄写
                        if (!result.first) {
                            changeTextPath(file)

                        // 如果是需要格式化再抄写
                        } else {
                            formatPath = file.absolutePath
                            row = result.second + 1
                            showFormatDialog = true
                        }

                    } else {
                        JOptionPane.showMessageDialog(window, "文件已打开")
                    }

                }else if(videoFormatList.contains(extension)){
                    showVideoPlayer(true)
                    setVideoPath(file.absolutePath)
                } else {
                    JOptionPane.showMessageDialog(window, "格式不支持")
                }
        }
    }

    val singleLauncher = rememberFilePickerLauncher(
        title = "打开文本",
        type = FileKitType.File(extensions = listOf("txt")),
        mode = FileKitMode.Single,
    ) { file ->
        scope.launch (Dispatchers.IO){
            file?.let {
                parseImportFile(listOf(file.file), OpenMode.Open)
            }
        }

    }

    /** 当前界面的快捷键 */
    val boxKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            (keyEvent.isCtrlPressed && keyEvent.key == Key.O && keyEvent.type == KeyEventType.KeyUp) -> {
                singleLauncher.launch()
                true
            }
            (keyEvent.isCtrlPressed && keyEvent.key == Key.F && keyEvent.type == KeyEventType.KeyUp) -> {
                scope.launch {openSearch() }
                true
            }

            else -> false
        }
    }


    // 拖放处理函数
    val dropTarget = remember {
        createDragAndDropTarget { files ->
            parseImportFile(files, OpenMode.Drag)
        }
    }

    Box(Modifier.fillMaxSize()
        .background(MaterialTheme.colors.background)
        .dragAndDropTarget(
            shouldStartDragAndDrop =shouldStartDragAndDrop,
            target = dropTarget
        )
        .focusRequester(focusRequester)
        .onKeyEvent(boxKeyEvent)
        .focusable()){

        LaunchedEffect(Unit){
            focusRequester.requestFocus()
        }

        Row(Modifier.fillMaxSize()){
            TextSidebar(
                isOpen = isOpenSettings,
                openFileChooser = {  singleLauncher.launch() },
            )
            val topPadding = if (isMacOS()) 78.dp else 40.dp
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
                                // 组合键，MacOS 使用 Meta 键，Windows 和 Linux 使用 Ctrl 键
                                val isModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed

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
                                    (isModifierPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) -> {
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
                                            // 使用中文输入法可能会超出当前行的字符限制
                                            val remainLength = if(typingResult.size <= line.length) typingResult.size else line.length
                                            val remainChars = line.substring(remainLength)


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
                                                    // 组合键，MacOS 使用 Meta 键，Windows 和 Linux 使用 Ctrl 键
                                                    val isModifierPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                                                    when {
                                                        isModifierPressed && it.key == Key.B && it.type == KeyEventType.KeyUp -> {
                                                            scope.launch { selectable = !selectable }
                                                            true
                                                        }
                                                        isModifierPressed && it.key == Key.F && it.type == KeyEventType.KeyUp -> {
                                                            scope.launch {openSearch() }
                                                            true
                                                        }
                                                        else -> false
                                                    }
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
                        color = MaterialTheme.colors.onBackground,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.Center)
                        )
                }

            }
        }

        if (isMacOS()) {
            Column(Modifier.align(Alignment.TopCenter)){
                MacOSTitle(
                    title = title,
                    window = window,
                    modifier = Modifier.height(44.dp).fillMaxWidth()
                )
            }
        }
        Row(modifier = Modifier.align(Alignment.TopStart)){
            Toolbar(
                isOpen = isOpenSettings,
                setIsOpen = setIsOpenSettings,
                modifier = Modifier,
                globalState = globalState,
                saveGlobalState = saveGlobalState,
                showPlayer = showVideoPlayer,
                openSearch = openSearch,
            )


            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val shortcutText = if (isMacOS()) "$ctrl O" else "$ctrl+O"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "打开文本文件  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = shortcutText)
                            }
                        }
                    }
                },
                delayMillis = 50,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = { singleLauncher.launch() },
                    modifier = Modifier.padding(top = if (isMacOS()) 44.dp else 0.dp)) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            RemoveButton( onClick = {removeText()},toolTip = "关闭当前文本")

        }

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
fun TextSidebar(
    isOpen:Boolean,
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
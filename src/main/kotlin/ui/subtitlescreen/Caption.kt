package ui.subtitlescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import data.Caption
import kotlinx.coroutines.launch
import player.isMacOS
import util.rememberMonospace


/**
 * 字幕浏览器的字幕组件
 * @param caption 字幕
 * @param showUnderline 是否显示下划线
 * @param isTranscribe 是否抄写字幕
 * @param notWroteCaptionVisible 是否显示未写字幕
 * @param index 字幕的索引
 * @param currentIndex 当前选中的字幕索引
 * @param currentIndexChanged 当前选中的字幕索引改变时
 * @param visible 是否显示当前字幕
 * @param multipleLines 是否启用多行字幕功能
 * @param updateCaptionBounds 更新字幕边界
 * @param alpha 透明度 0.0f - 1.0f 之间 越小颜色越透明，越大颜色越深。
 * if currentIndex == index then 1.0f else 0.74f
 * @param keyEvent 快捷键事件处理
 * @param focusRequester 焦点请求器
 * @param selectable 是否可以选择文本
 * @param exitSelection 退出选择
 */
@Composable
fun Caption(
    caption: Caption,
    showUnderline: Boolean = false,
    isTranscribe: Boolean = false,
    notWroteCaptionVisible: Boolean = true,
    index: Int,
    currentIndex: Int,
    currentIndexChanged: (Int) -> Unit,
    visible: Boolean = true,
    multipleLines: MultipleLines,
    next: () -> Unit,
    updateCaptionBounds: (Rect) -> Unit,
    alpha: Float,
    keyEvent: (KeyEvent) -> Boolean,
    focusRequester: FocusRequester,
    selectable: Boolean = false,
    exitSelection: () -> Unit,
) {
    Box(Modifier.width(IntrinsicSize.Max)) {

        val scope = rememberCoroutineScope()
        val monospace  = rememberMonospace()
        val captionContent = caption.content
        // 当 BasicTextField 失去焦点时自动清理 typingResult 和 textFieldValue
        val typingResult = remember { mutableStateListOf<Pair<Char, Boolean>>() }
        var textFieldValue by remember { mutableStateOf("") }
        val selectRequester = remember { FocusRequester() }
        val contentColor = setColor(index, currentIndex, multipleLines, isTranscribe, visible, alpha, notWroteCaptionVisible)


        /** 检查输入的回调函数 */
        val handleInput: (String) -> Unit = { input ->
            if (textFieldValue.length > captionContent.length) {
                typingResult.clear()
                textFieldValue = ""

            } else if (input.length <= captionContent.length) {
                textFieldValue = input
                typingResult.clear()
                val inputChars = input.toMutableList()
                for (i in inputChars.indices) {
                    val inputChar = inputChars[i]
                    val char = captionContent[i]
                    if (inputChar == char) {
                        typingResult.add(Pair(inputChar, true))
                        // 方括号的语义很弱，又不好输入，所以可以使用空格替换
                    } else if (inputChar == ' ' && (char == '[' || char == ']')) {
                        typingResult.add(Pair(char, true))
                        // 音乐符号不好输入，所以可以使用空格替换
                    } else if (inputChar == ' ' && (char == '♪')) {
                        typingResult.add(Pair(char, true))
                        // 音乐符号占用两个空格，所以插入♪ 再删除一个空格
                        inputChars.add(i, '♪')
                        inputChars.removeAt(i + 1)
                        textFieldValue = String(inputChars.toCharArray())
                    } else {
                        typingResult.add(Pair(inputChar, false))
                    }
                }
                if (input.length == captionContent.length) {
                    next()
                }

            }
        }

        BasicTextField(
            value = if (isTranscribe) textFieldValue else captionContent,
            onValueChange = { handleInput(it) },
            singleLine = true,
            readOnly = !isTranscribe,
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            textStyle = MaterialTheme.typography.h5.copy(
                color = if (!isTranscribe) contentColor else Color.Transparent,
                fontFamily = monospace
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .align(Alignment.CenterStart)
                .focusable(isTranscribe)
                .onKeyEvent { keyEvent(it) }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        scope.launch {
                            if (!multipleLines.enabled) {
                                currentIndexChanged(index)
                            }

                        }
                    } else if (textFieldValue.isNotEmpty()) {
                        typingResult.clear()
                        textFieldValue = ""
                    }
                }
        )
        // 抄写字幕
        if (isTranscribe) {
            Text(
                text = buildAnnotatedString(captionContent, typingResult, monospace, alpha, contentColor),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.onBackground,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(bottom = 5.dp)
                    .onGloballyPositioned { coordinates ->
                        if (currentIndex == index) {
                            // 如果视频播放按钮被遮挡，就使用这个位置计算出视频播放器的位置
                            updateCaptionBounds(coordinates.boundsInWindow())
                        }

                    }
            )
        }

        // 快捷键 Ctrl + B 打开选择框
        SelectableText(
            content = captionContent,
            selectable = selectable,
            onDismissRequest = { exitSelection() },
            selectRequester = selectRequester,
            monospace = monospace,
            openSearch = {}
        )
        // 下划线
        if (showUnderline) {
            Divider(Modifier.align(Alignment.BottomCenter).background(MaterialTheme.colors.primary))
        }

        SideEffect {
            if (currentIndex == index) {
                focusRequester.requestFocus()
            }
        }
    }
}


/**
 * 设置字幕颜色
 */
@Composable
private fun setColor(
    index: Int,
    currentIndex: Int,
    multipleLines: MultipleLines,
    isTranscribe: Boolean,
    visible: Boolean,
    alpha: Float,
    notWroteCaptionVisible: Boolean
): Color {
    val lineColor =  if(index <  currentIndex){
        MaterialTheme.colors.primary.copy(alpha = if(MaterialTheme.colors.isLight) ContentAlpha.high else ContentAlpha.medium)
    }else if(currentIndex == index){
        if(multipleLines.enabled || !isTranscribe) {
            MaterialTheme.colors.primary.copy(alpha = if(MaterialTheme.colors.isLight) ContentAlpha.high else ContentAlpha.medium)
        }else if(visible){
            MaterialTheme.colors.onBackground.copy(alpha = alpha)
        }else{
            Color.Transparent
        }
    }else{
        if(notWroteCaptionVisible || !isTranscribe){
            MaterialTheme.colors.onBackground.copy(alpha = alpha)
        }else{
            Color.Transparent
        }
    }
    return lineColor
}

@Composable
private fun buildAnnotatedString(
    captionContent: String,
    typingResult: MutableList<Pair<Char, Boolean>>,
    monospace: FontFamily,
    alpha: Float,
    remainCharsColor: Color
) : AnnotatedString {
    return buildAnnotatedString {
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
        val remainChars = captionContent.substring(typingResult.size)

        withStyle(
            style = SpanStyle(
                color = remainCharsColor,
                fontSize = MaterialTheme.typography.h5.fontSize,
                letterSpacing = MaterialTheme.typography.h5.letterSpacing,
                fontFamily = monospace,
            )
        ) {
            append(remainChars)
        }
    }
}

/**
 * 选择文本
 */
@Composable
fun SelectableText(
    content: String,
    selectable: Boolean,
    onDismissRequest: () -> Unit,
    selectRequester: FocusRequester,
    monospace: FontFamily,
    openSearch: () -> Unit
){
    DropdownMenu(
        expanded = selectable,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(0.dp, (-50).dp)
    ) {
        val scope = rememberCoroutineScope()
        BasicTextField(
            value = content,
            onValueChange = {},
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            textStyle = MaterialTheme.typography.h5.copy(
                fontFamily = monospace,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.high),
            ),
            modifier = Modifier.focusable()
                .height(32.dp)
                .focusRequester(selectRequester)
                .onKeyEvent {
                    val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                    if (isCtrlPressed && it.key == Key.B && it.type == KeyEventType.KeyUp) {
                        onDismissRequest()
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

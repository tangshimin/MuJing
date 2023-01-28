package player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import data.MutableVocabulary
import data.Word

class DanmakuItem(
    content: String,
    show:Boolean,
    sequence:Int = 0,
    startTime:Int,
    isPause: Boolean = false,
    position: IntOffset,
    word:Word? = null,
){
    val content by mutableStateOf(content)
    var show by mutableStateOf(show)
    val startTime by mutableStateOf(startTime)
    var sequence by mutableStateOf(sequence)
    var isPause by mutableStateOf(isPause)
    var position by mutableStateOf(position)
    val word by mutableStateOf(word)

    override fun equals(other: Any?): Boolean {
        val otherItem = other as DanmakuItem
        return (this.content == otherItem.content && this.startTime == otherItem.startTime)
    }
    override fun hashCode(): Int {
        return content.hashCode() + startTime.hashCode()
    }
}


@Composable
fun rememberDanmakuMap(
    videoPath:String,
    vocabulary: MutableVocabulary
) = remember{
    // Key 为秒 > 这一秒出现的单词列表
    val timeMap = mutableMapOf<Int,MutableList<DanmakuItem>>()
    if(vocabulary.relateVideoPath == videoPath){
       vocabulary.wordList.forEach { word ->
            if(word.captions.isNotEmpty()){
                word.captions.forEach { caption ->

                    val startTime = Math.floor(parseTime(caption.start)).toInt()
                    val dList = timeMap.get(startTime)
                    val item = DanmakuItem(word.value, true, startTime, 0,false, IntOffset(0, 0), word)
                    if(dList == null){
                        val newList = mutableListOf(item)
                        timeMap.put(startTime,newList)
                    }else{
                        dList.add(item)
                    }
                }
            }else{
                word.externalCaptions.forEach { externalCaption ->
                    val startTime = Math.floor(parseTime(externalCaption.start)).toInt()
                    val dList = timeMap.get(startTime)
                    val item = DanmakuItem(word.value, true, startTime, 0,false, IntOffset(0, 0), word)
                    if(dList == null){
                        val newList = mutableListOf(item)
                        timeMap.put(startTime,newList)
                    }else{
                        dList.add(item)
                    }
                }
            }
        }
    }
    timeMap
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Danmaku(
    showSequence:Boolean,
    danmakuItem: DanmakuItem,
    playEvent: () -> Unit,
    fontFamily:FontFamily
) {
    if(danmakuItem.show){
        val text = if(showSequence) {
            "${danmakuItem.sequence} ${danmakuItem.content}"
        }else{
            danmakuItem.content
        }
        fun enter(){
            // 如果已经由⌈快速定位弹幕⌋暂停，就不执行。
            if(!danmakuItem.isPause){
                danmakuItem.isPause =  true
                playEvent()
            }
        }
        fun exit(){
            danmakuItem.isPause = false
            playEvent()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.h5,
            fontFamily = fontFamily,
            color = Color.White,
            modifier = Modifier.offset { danmakuItem.position }
                .onPointerEvent(PointerEventType.Enter){ enter() }
                .onPointerEvent(PointerEventType.Exit) { exit() }
        )
        var offsetX = (danmakuItem.position.x - 200 + (danmakuItem.content.length * 10).div(2)).dp
        if(offsetX<0.dp) offsetX = 0.dp
        DropdownMenu(
            expanded =danmakuItem.isPause,
            onDismissRequest = {
                danmakuItem.isPause = false
                playEvent()
            },
            offset = DpOffset(offsetX,(danmakuItem.position.y).dp),
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter){ enter() }
                .onPointerEvent(PointerEventType.Exit) { exit() }
                .onKeyEvent { keyEvent ->
                    if(keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp){
                        exit()
                        true
                    }else false
                }
        ) {
            Surface(
                elevation = 4.dp,
                shape = RectangleShape,
            ) {
                Box(Modifier.width(400.dp).height(180.dp)) {
                    val stateVertical = rememberScrollState(0)
                    Box(Modifier.verticalScroll(stateVertical)){
                        SelectionContainer {
                            Text(
                                text = danmakuItem.word?.translation ?: "",
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }
                    VerticalScrollbar(
                        style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )

                }
            }
        }


    }
}
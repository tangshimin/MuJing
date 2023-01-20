package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import player.isWindows
import java.util.*
import kotlin.concurrent.schedule

@Composable
fun DocumentDialog(close: () -> Unit) {
    Dialog(
        title = "文档",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1140.dp, 700.dp)
        ),
    ) {
        Surface {
            Column (Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
                Divider()
                Row{
                    var currentPage by remember{ mutableStateOf("") }
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.width(200.dp).fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "document" }) {
                            Text("用文档生成词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "document"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {  currentPage = "subtitles" }) {
                            Text("用字幕生成词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "subtitles"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {  currentPage = "matroska"}) {
                            Text("用 MKV 视频生成词库", modifier = Modifier.padding(start = 16.dp))
                            if( currentPage == "matroska"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "youtube" }) {
                            Text("YouTube 视频下载", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "youtube"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { currentPage = "linkVocabulary" }) {
                            Text("链接字幕词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkVocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())

                    when(currentPage){
                        "document" -> {
                            DocumentPage()
                        }
                        "subtitles" -> {
                            SubtitlesPage()
                        }
                        "matroska" -> {
                            MatroskaPage()
                        }
                        "youtube" -> {
                            YouTubeDownloadPage()
                        }
                        "linkVocabulary" -> {
                            LinkVocabularyPage()
                        }
                    }
                }
            }

        }
    }
}

const val frequencyText = "\n英国国家语料库(BNC) 和当代语料库(COCA)里的词频顺序介绍\n" +
        "BNC 词频统计的是最近几百年的历史各类英文资料，而当代语料库只统计了最近 20 年的，为什么两者都要提供呢？\n" +
        "很简单，quay（码头）这个词在当代语料库(COCA)里排两万以外，你可能觉得是个没必要掌握的生僻词，而 BNC \n" +
        "里面却排在第 8906 名，基本算是一个高频词，为啥呢？可以想象过去航海还是一个重要的交通工具，所以以往的各类\n" +
        "文字资料对这个词提的比较多，你要看懂 19 世纪即以前的各类名著，你会发现 BNC 的词频很管用。而你要阅读各类\n" +
        "现代杂志，当代语料库的作用就体现出来了，比如 Taliban（塔利班），在 BNC 词频里基本就没收录（没进前 20 万\n" +
        "词汇），而在当代语料库里，它已经冒到 6089 号了，高频中的高频。BNC 较为全面和传统，针对性学习能帮助你阅读\n" +
        "各类国外帝王将相的文学名著，当代语料库较为现代和实时，以和科技紧密相关。所以两者搭配，干活不累。[2]\n"
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrequencyRelatedLink(){

    val uriHandler = LocalUriHandler.current
    val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom =10.dp)){
        Text("[2] ")
        val annotatedString1 = buildAnnotatedString {
            pushStringAnnotation(tag = "android", annotation = "https://github.com/skywind3000/ECDICT#单词标注")
            withStyle(style = SpanStyle(color = blueColor)) {
                append("https://github.com/skywind3000/ECDICT#单词标注")
            }
            pop()
        }
        ClickableText(
            text = annotatedString1,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .pointerHoverIcon(PointerIconDefaults.Hand),
            onClick = { offset ->
                annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                    uriHandler.openUri(it.item)
                }
            })
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"

            Text("\n1. 打开从文档生成词库窗口，然后选择文档，可以拖放文档到窗口快速打开，\n" +
                    "    我这里选择的是一个 android 开发英文文档，有 1300 页。点击开始按钮。[1]\n")
            Image(
                painter = painterResource("screenshot/document-$theme/document-1.png"),
                contentDescription = "document-step-1",
                modifier = Modifier.width(640.dp).height(150.dp).padding(start = 20.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            val uriHandler = LocalUriHandler.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Row (verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 30.dp)){
                Text("[1]演示文档 AndroidNotesForProfessionals 来源于：")
                val annotatedString1 = buildAnnotatedString {
                    pushStringAnnotation(tag = "android", annotation = "https://goalkicker.com/AndroidBook/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("https://goalkicker.com/AndroidBook/")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString1,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()

        }
        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@Composable
fun SameSteps(){
    val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
    Text(frequencyText,modifier = Modifier.padding(start = 20.dp))
    Text("\n2. 在左边的预览区可以看到程序生成的单词。你可以点击左边的过滤词频顺序为0的词，\n" +
            "    词频为 0 的词包括简单的字母和数字还有一些没有收录进词频顺序的生僻词。\n")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-2.png"),
            contentDescription = "step-2",
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n3. 可以勾选【过滤 COCA 词频前 1000 的单词】或【过滤 BNC 词频前 1000 的单词】，\n" +
            "    过滤最常见的 1000 词，这个值可以改成 2000，或 3000.")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-7.png"),
            contentDescription = "step-3",
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }
    Text("\n4. 还可以把所有的派生词替换为原型词。")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-3.png"),
            contentDescription = "step-4",
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n5. 如果有数字还可用过滤数字。")
    Column {

        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-4.png"),
            contentDescription = "step-5",
            modifier = Modifier.width(405.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(405.dp).padding(start = 20.dp))
    }

    Text("\n6. 经过前面的过滤之后，还是有你很熟悉的词，比如你已经过了很熟悉牛津核心5000词了，\n" +
            "    点击左边的内置词库，然后选择：牛津核心词 -> The_Oxford_5000，选择之后的单词是不是少了很多。")
    Column {
        Image(
            painter = painterResource("screenshot/mkv-$theme/MKV-5.png"),
            contentDescription = "step-6",
            modifier = Modifier.width(475.dp).height(636.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(475.dp).padding(start = 20.dp))
    }

    Text("\n7. 如果还有你熟悉的词，可以先把排序改成【按 COCA 词频排序】或【按 BNC 词频排序】，\n" +
            "    这样熟悉的单词就会出现在最前面。再使用鼠标单击单词的右上角的删除按钮，删除的单词会添加到熟悉词库。\n")

    Image(
        painter = painterResource("screenshot/mkv-$theme/MKV-6.png"),
        contentDescription = "step-7",
        modifier = Modifier.width(890.dp).height(400.dp).padding(start = 20.dp)
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
    )
    Text("\n8. 也可以在记忆单词的时候删除熟悉的词，把鼠标移动到正在记忆的单词，会弹出一个菜单，可以从这里删除单词。\n" +
            "    可以直接使用快捷键 Delete 删除单词。\n")
    Image(
        painter = painterResource("screenshot/document-$theme/document-7.png"),
        contentDescription = "step-8",
        modifier = Modifier.width(620.dp).height(371.dp).padding(start = 20.dp,bottom = 10.dp)
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
    )
    Text("\n9. 词库不要保存到应用程序的安装目录，升级的时候要先卸载软件，卸载的时候会把安装目录删除。\n" +
            "    如果你想把内置词库和生成的词库放到一起，可以把内置的词库复制出来。\n",
        color = Color.Red)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubtitlesPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("从字幕生成的词库，每个单词最多匹配三条字幕。\n")
            Text("\n1. 打开从字幕生成词库窗口,然后选择 SRT 字幕，也可以拖放文件到窗口快速打开，\n" +
                    "    如果有对应的视频，就选择对应的视频，然后点击分析按钮。[1]\n")
            Image(
                painter = painterResource("screenshot/subtitles-$theme/Subtitles-1.png"),
                contentDescription = "subtitles-step-1",
                modifier = Modifier.width(633.dp).height(199.dp).padding(start = 20.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("[1]演示字幕来源于")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "blender", annotation = "https://durian.blender.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "blender", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()
        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MatroskaPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("从 MKV 生成的词库，每个单词最多匹配三条字幕。\n")
            Text("\n1. 打开从 MKV 生成词库窗口,然后选择 MKV 视频，也可以拖放文件到窗口快速打开，（最新版支持批量生成词库）\n" +
                    "    然后点击开始按钮。[1]\n")
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-1.png"),
                contentDescription = "mkv-step-1",
                modifier = Modifier.width(588.dp).height(192.dp).padding(start = 20.dp)
                    .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
            )
            SameSteps()
            Row{
                val uriHandler = LocalUriHandler.current
                val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
                Text("[1]演示视频来源于")
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "Sintel", annotation = "https://www.youtube.com/watch?v=eRsGyueVLvQ")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Sintel")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "Sintel", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()
        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YouTubeDownloadPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val uriHandler = LocalUriHandler.current
            val clipboard = LocalClipboardManager.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Row(verticalAlignment = Alignment.CenterVertically){

                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "youtube-dl", annotation = "https://github.com/ytdl-org/youtube-dl")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("youtube-dl")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "youtube-dl", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 非常强大的视频下载程序，可以下载 1000+ 视频网站的视频，")
                Text("下载英语字幕和视频的命令：")
            }
            val command = "youtube-dl.exe  --proxy \"URL\" --sub-lang en --convert-subs srt --write-sub URL"
            Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(if(MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35))){
                SelectionContainer {
                    Text("    $command")
                }

                Box{
                    var copyed by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                    clipboard.setText(AnnotatedString(command))
                    copyed = true
                    Timer("恢复状态", false).schedule(2000) {
                        copyed = false
                    }
                }){
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                    DropdownMenu(
                        expanded = copyed,
                        onDismissRequest = {copyed = false}
                    ){
                        Text("已复制")
                    }
                }


            }

            Row{
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "downloader", annotation = "https://jely2002.github.io/youtube-dl-gui/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("Open Video Downloader")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "downloader", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 基于 youtube-dl 的视频下载工具, UI 非常简洁。")
            }
            val text = if(isWindows()) "最好选择 Microsoft Store 版本，因为其他版本在某些 Windows 电脑上可能无法使用。\n" else ""
           Text("$text")

            val annotatedString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDYouTube%E8%A7%86%E9%A2%91")
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow：如何下载YouTube视频")
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                })
        }
        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkVocabularyPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("提示：不要把链接后的词库保存到应用程序的安装目录\n")
            Text("1. 字幕 > 链接字幕词库(L) 打开链接字幕对话框，然后选择一个词库，也可以拖放一个词库到窗口。")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-1.png"),
                contentDescription = "mkv-1",
                modifier = Modifier.width(590.dp).height(436.dp).padding(start = 20.dp)
            )
            Text("\n2. 这里以四级词库作为例。")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-2.png"),
                contentDescription = "mkv-2",
                modifier = Modifier.width(590.dp).height(436.dp).padding(start = 20.dp)
            )
            Text("\n3. 再选择一个有字幕的词库。选择后可以预览视频片段，然后点击链接，有字幕的词库就链接到了没有字幕的词库。")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-3.png"),
                contentDescription = "mkv-3",
                modifier = Modifier.width(590.dp).height(650.dp).padding(start = 20.dp)
            )
            Text("\n4. 点击链接后返回到链接字幕的主界面，还可以链接多个有字幕的词库。也可以删除已经链接的字幕。不想链接了就点击保存，最后注意不要把词库保存到应用程序的安装目录")
            Image(
                painter = painterResource("screenshot/link-vocabulary-$theme/Link-Vocabulary-4.png"),
                contentDescription = "mkv-4",
                modifier = Modifier.width(580.dp).height(436.dp).padding(start = 20.dp)
            )

        }

        VerticalScrollbar(
            style = LocalScrollbarStyle.current.copy(shape = if(isWindows()) RectangleShape else RoundedCornerShape(4.dp)),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

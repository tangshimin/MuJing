package ui.dialog

import LocalCtrl
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.util.*
import kotlin.concurrent.schedule

@Composable
fun DocumentWindow(
    close: () -> Unit,
    currentPage:String,
    setCurrentPage:(String) -> Unit
) {
    Window(
        title = "文档",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1170.dp, 720.dp)
        ),
    ) {
        Surface {
            Column (Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
                Divider()
                Row{
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.width(200.dp).fillMaxHeight()
                    ) {
                        val selectedColor = if(MaterialTheme.colors.isLight) Color(245, 245, 245) else Color(41, 42, 43)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "features")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("features" )}) {
                            Text("主要功能", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "features"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "vocabulary")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("vocabulary" )}) {
                            Text("词库介绍", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "vocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "tips")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("tips" )}) {
                            Text("使用技巧", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "tips"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "document")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("document") }) {
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
                                .background( if(currentPage == "subtitles")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("subtitles") }) {
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
                                .background( if(currentPage == "matroska")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("matroska")}) {
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
                                .background( if(currentPage == "learnEnglish")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("learnEnglish") }) {
                            Text("如何使用美剧学习英语", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "learnEnglish"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "Danmaku")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("Danmaku") }) {
                            Text("如何打开单词弹幕", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "Danmaku"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "linkVocabulary")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("linkVocabulary") }) {
                            Text("链接字幕词库", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkVocabulary"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "linkCaptions")selectedColor else MaterialTheme.colors.background )
                                .clickable {  setCurrentPage("linkCaptions") }) {
                            Text("链接字幕", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "linkCaptions"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background( if(currentPage == "download")selectedColor else MaterialTheme.colors.background )
                                .clickable { setCurrentPage("download") }) {
                            Text("视频资源下载", modifier = Modifier.padding(start = 16.dp))
                            if(currentPage == "download"){
                                Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                            }
                        }

                    }
                    Divider(Modifier.width(1.dp).fillMaxHeight())

                    when(currentPage){
                        "features" -> {
                            FeaturesPage()
                        }
                        "vocabulary" -> {
                            VocabularyPage()
                        }
                        "tips" -> {
                            Tips()
                        }
                        "document" -> {
                            DocumentPage()
                        }
                        "subtitles" -> {
                            SubtitlesPage()
                        }
                        "matroska" -> {
                            MatroskaPage()
                        }
                        "download" -> {
                            DownloadPage()
                        }
                        "learnEnglish" -> {
                            LearnEnglishPage()
                        }
                        "Danmaku" -> {
                            DanmakuPage()
                        }
                        "linkVocabulary" -> {
                            LinkVocabularyPage()
                        }
                        "linkCaptions" -> {
                            LinkCaptionsPage()
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
                .pointerHoverIcon(Hand),
            onClick = { offset ->
                annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                    uriHandler.openUri(it.item)
                }
            })
    }
}


@Composable
fun FeaturesPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"

            Image(
                painter = painterResource("screenshot/features-$theme/features-word.png"),
                contentDescription = "features-word",
                modifier = Modifier.width(350.dp).height(90.dp)
            )
            Text("记忆单词的时候，会自动播放单词的读音，然后用键盘打字练习拼写，每个单词都可以输入多次，直到记住为止。从 MKV 生成的词库(单词本)，可以抄写单词对应的字幕，播放单词对应的视频片段。默认使用 Enter 键切换下一个单词。\n\n")


            Image(
                painter = painterResource("screenshot/features-$theme/features-subtitle.png"),
                contentDescription = "features-subtitle",
                modifier = Modifier.width(350.dp).height(90.dp)
            )
            Text("字幕浏览器，可以浏览字幕，练习跟读美剧、电影、TED演讲，可以选择性的播放一条或多条字幕，如果要播放多行字幕，点击左边的数字就可以开启，点击 5 和 10 再点击左边的播放按钮，就会从第5行开始播放，到第10行结束。还可以抄写字幕。\n\n")


            Image(
                painter = painterResource("screenshot/features-$theme/features-player.png"),
                contentDescription = "features-player",
                modifier = Modifier.width(350.dp).height(90.dp)
            )
            Text("以弹幕的形式复习单词。播放电影时，添加用电影生成的词库到播放器，单词会以弹幕的形式出现。要查看某个单词的中文解释，只需要输入单词或对应的数字就可以查看。\n" +
                    "打开弹幕的快捷方式：如果正在记忆某个由视频或字幕生成的词库，把视频拖放到记忆单词界面，就可以快速的打开视频和弹幕。\n\n")

            Image(
                painter = painterResource("screenshot/features-$theme/features-text.png"),
                contentDescription = "features-text",
                modifier = Modifier.width(350.dp).height(90.dp)
            )
            Text("抄写文本，可以抄写 txt 格式的文本")
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }


}
@Composable
fun VocabularyPage(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        Text("词库可以分为两类：\n" +
                "     • 文档词库，软件内置的词库就是文档词库，另外使用文档生成的词库也是文档词库。\n" +
                "     • 字幕词库，字幕词库也分为两类：\n" +
                "         • MKV 词库，使用mkv视频内置字幕生成的词库。\n" +
                "         • SUBTITLES 词库，使用外部字幕生成的词库。\n\n" +
                "字幕词库和 MKV 词库里的字幕可以链接到文档词库里的单词。\n" +
                "建议把字幕词库和相关视频文件放到一个文件夹，这样就可以把字幕词库和视频一起分享给朋友了。生成字幕词库后不要修改关联视频的名称。\n"
        )

        Text("熟悉词库：非常熟悉，不要再记忆的单词。\n" +
                "记忆单词的时候，觉得一个单词非常熟悉，不用再记忆了，就可以使用快捷键 Ctrl + Y 把这个单词加入到熟悉词库。\n" +
                "生成词库的时候，在左边的过滤区选择熟悉词库，就可以批量的过滤熟悉词库。\n")

        Text("困难词库：很难拼写的单词，比如发音不规则的单词或者比较长的单词，可以使用快捷键 Ctrl + I 把这个单词添加到困难词库。\n")
    }
}

@Composable
fun Tips(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        val background = if (MaterialTheme.colors.isLight) Color.LightGray else Color(35, 35, 35)
        val ctrl = LocalCtrl.current
        Row(Modifier.fillMaxWidth()){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("复制单词")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append("，如果要复制正在抄写的字幕或文本，可以先把光标定位到要复制的行，然后按 ")
                }

                withStyle(style = SpanStyle(color =  MaterialTheme.colors.primary,background = background)) {
                    append("$ctrl + B")
                }

                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append("，就可以复制单词了。 ")
                }
            }
            Text(annotatedString)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("快速打开视频弹幕")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append("，如果正在记忆某个由视频生成的词库，把视频拖放到记忆单词界面，就可以快速的打开视频和弹幕。")
                }
            }
            Text(annotatedString)
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)){
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold,color = MaterialTheme.colors.onBackground)) {
                    append("播放多行字幕")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colors.onBackground)) {
                    append(", 在字幕浏览器界面，如果要播放多行字幕，点击左边的数字就可以开启，点击 5 和 10 再点击左边的播放按钮，" +
                            "就会从第5行开始播放，到第10行结束。快捷键 ")
                }
                withStyle(style = SpanStyle(color =  MaterialTheme.colors.primary,background = background)) {
                    append("$ctrl + N ")
                }
            }
            Text(annotatedString)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"

            Text("\n1. 把鼠标移动到屏幕顶部的菜单栏 > 点击词库 > 再点击 用文档生成词库，然后选择文档，可以拖放文档到窗口快速打开，\n" +
                    "    我这里选择的是一个 android 开发英文文档[1]，有 1300 页。点击开始按钮。\n")
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
                        .pointerHoverIcon(Hand),
                    onClick = { offset ->
                        annotatedString1.getStringAnnotations(tag = "android", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()

        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@Composable
fun SameSteps(){
    val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
    Text("\n在右边的预览区可以看到程序生成的单词。如果你不想删除任何单词就可以直接点击右下角的保存按钮，\n" +
            "如果有很多数字，或者很多熟悉的单词不想再记忆了，就选择左边的过滤选项过滤掉不需要的单词。\n",
        modifier = Modifier.padding(start = 20.dp)
        )

    Text("\n2. 你可以点击左边的过滤词频顺序为0的词，词频为 0 的词包括简单的字母和数字还有一些没有收录进词频顺序的生僻词。")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
        Spacer(Modifier.width(3.dp).height(180.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
        Text(frequencyText, modifier = Modifier.padding(start = 10.dp, bottom = 5.dp))
    }

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
            modifier = Modifier.width(406.dp).height(450.dp).padding(start = 20.dp)
        )
        Divider(Modifier.width(406.dp).padding(start = 20.dp))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(130.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Column (Modifier.padding(start = 10.dp)){
                    Row(){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " 使用字幕生成的词库，每个单词最多匹配三条字幕。", fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " 生成词库并没有切割视频，生成词库后不要重命名视频，如果重命名了视频，播放视频时会发生错误，只能重新再生成一次。",
                            fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = " 建议把生成的词库保存到相关视频文件所在的文件夹，这样就可以把词库和视频一起分享给朋友了，放在一起后，如果移动了整个文件夹，播放视频时不会出现视频地址错误。",
                            fontWeight = FontWeight.Bold)
                    }


                }
            }

            Text("\n1. 把鼠标移动到屏幕顶部的菜单栏 > 点击词库 > 再点击 用字幕生成词库，然后选择 SRT 字幕，\n    也可以拖放文件到窗口快速打开，" +
                    "如果有对应的视频，就选择对应的视频，然后点击开始按钮。[1]\n")
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
                    modifier = Modifier.pointerHoverIcon(Hand),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "blender", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
            }
            FrequencyRelatedLink()
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@Composable
fun MatroskaPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Row(  verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(130.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Column (Modifier.padding(start = 10.dp)){
                    Row{
                        Text(text = "•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " 使用 MKV 视频生成的词库，每个单词最多匹配三条字幕。",
                            fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold)
                        Text(text = " 生成词库并没有切割视频，生成词库后不要重命名视频，如果重命名了视频，播放视频时会发生错误，只能重新再生成一次。",
                            fontWeight = FontWeight.Bold,)
                    }
                    Row(modifier = Modifier.padding(top = 5.dp)){
                        Text("•",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = " 建议把生成的词库保存到 MKV 视频所在的文件夹，这样就可以把词库和视频一起分享给朋友了。放在一起后，如果移动了整个文件夹，播放视频时不会出现视频地址错误。",
                            fontWeight = FontWeight.Bold)
                    }

                }
            }



            Text("\n1. 把鼠标移动到屏幕顶部的菜单栏 > 点击词库 > 再点击 用 MKV 视频生成词库，然后选择 MKV 视频，\n    也可以拖放文件到窗口快速打开，然后点击开始按钮。[1]\n")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(start = 20.dp)){
                Spacer(Modifier.width(3.dp).height(60.dp).background(if(MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray))
                Text(text = "最新版支持拖放多个视频，你可以拖放多个视频到窗口，使用多个视频生成一个词库要保证每个视频里都有" +
                        "一个 English 字幕轨道，幕境会提取每个视频里的 English 轨道的字幕，然后把所有的字幕合并到一个词库里。",
                    modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
                )
            }
            Image(
                painter = painterResource("screenshot/mkv-$theme/MKV-1.png"),
                contentDescription = "mkv-step-1",
                modifier = Modifier.width(685.dp).height(192.dp).padding(start = 20.dp,top = 10.dp)
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
                        .pointerHoverIcon(Hand)
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
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DownloadPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val uriHandler = LocalUriHandler.current
            val clipboard = LocalClipboardManager.current
            val blueColor = if (MaterialTheme.colors.isLight) Color.Blue else Color(41, 98, 255)
            Text("Youbute 视频下载：\n", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "yt-dlp", annotation = "https://github.com/yt-dlp/yt-dlp")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("yt-dlp")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "youtube-dl", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Text(" 非常强大的视频下载程序，可以下载 1000+ 视频网站的视频，")
                Text("下载英语字幕和视频的命令：")
            }
            val command = "yt-dlp.exe  --proxy \"URL\" --sub-lang en --convert-subs srt --write-sub URL"
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

            val annotatedString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDYouTube%E8%A7%86%E9%A2%91")
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow：使用5种方法下载YouTube视频")
                }
                pop()
            }
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(Hand).padding(start = 16.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                })

            Text("\nBT下载：\n", fontWeight = FontWeight.Bold)
            val btString = buildAnnotatedString {
                pushStringAnnotation(tag = "howto", annotation = "https://zh.wikihow.com/%E4%B8%8B%E8%BD%BDBT%E7%A7%8D%E5%AD%90%E6%96%87%E4%BB%B6")
                withStyle(style = SpanStyle(color = blueColor)) {
                    append("wikiHow：如何下载BT种子文件")
                }
                pop()
            }
            ClickableText(
                text = btString,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.pointerHoverIcon(Hand).padding(start = 16.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "howto", start = offset, end = offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }
                })

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                Text("BT 客户端推荐：")
                val qbittorrentString = buildAnnotatedString {
                    pushStringAnnotation(tag = "qbittorrent", annotation = "https://www.qbittorrent.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("qbittorrent")
                    }
                    pop()
                }
                ClickableText(
                    text = qbittorrentString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        qbittorrentString.getStringAnnotations(tag = "qbittorrent", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Spacer(Modifier.width(10.dp))
                val xunleiString = buildAnnotatedString {
                    pushStringAnnotation(tag = "xunlei", annotation = "https://www.xunlei.com/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("迅雷")
                    }
                    pop()
                }
                ClickableText(
                    text = xunleiString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        xunleiString.getStringAnnotations(tag = "xunlei", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })


            }

            Text("\n字幕下载：\n", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)){
                Text("双语字幕 ")
                val subHDString = buildAnnotatedString {
                    pushStringAnnotation(tag = "SubHD", annotation = "https://subhd.tv/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("SubHD")
                    }
                    pop()
                }
                ClickableText(
                    text = subHDString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        subHDString.getStringAnnotations(tag = "SubHD", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })
                Spacer(Modifier.width(10.dp))
                Text("英语字幕 ")
                val opensubtitlesString = buildAnnotatedString {
                    pushStringAnnotation(tag = "opensubtitles", annotation = "https://www.opensubtitles.org/")
                    withStyle(style = SpanStyle(color = blueColor)) {
                        append("OpenSubtitles")
                    }
                    pop()
                }
                ClickableText(
                    text = opensubtitlesString,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .pointerHoverIcon(Hand)
                    ,
                    onClick = { offset ->
                        opensubtitlesString.getStringAnnotations(tag = "opensubtitles", start = offset, end = offset).firstOrNull()?.let {
                            uriHandler.openUri(it.item)
                        }
                    })


            }

        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}


@Composable
fun LearnEnglishPage(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        Text(
           "我用美剧学习英语的流程是：\n"+
            "  1. 先用英语字幕看一遍。\n"+
            "  2. 然后用英语字幕生成一个词库，把所有的陌生单词学完。\n"+
            "  3. 学完再把视频拖放到记忆单词界面，打开单词弹幕，再看一遍。\n"+
            "  4. 如果有时间会使用字幕浏览器跟读或抄写英语字幕。"
        )
    }
}
@Composable
fun DanmakuPage(){
    Column (Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp,end = 16.dp)){
        Text(
           """
              单词弹幕里的单词就是字幕词库里的单词，如果要打开单词弹幕要先使用字幕或mkv视频里的内置字幕生成词库。
              如果已经生成了字幕词库，打开视频播放器 > 打开视频 > 添加词库，就可以打开单词弹幕了。
              
              还有一种快捷打开单词弹幕的方法，如果正在记忆某个由视频生成的词库，把视频拖放到记忆单词界面，就可以快速的打开视频和弹幕。
           """.trimIndent()
        )
    }
}

@Composable
fun LinkVocabularyPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text("如果你正在记忆四级单词，又使用字幕或mkv视频生成了一个字幕词库，这个词库里有一些单词是四级单词，\n" +
                    "就可以使用链接字幕词库功能，把这些字幕链接到四级文档词库。链接后修改或删除字幕词库不会影响文档词库。\n")
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
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

@Composable
fun LinkCaptionsPage(){
    Box(Modifier.fillMaxSize()){
        val stateVertical = rememberScrollState(0)
        Column (Modifier.padding(start = 16.dp, top = 16.dp,end = 16.dp).verticalScroll(stateVertical)){
            val theme = if(MaterialTheme.colors.isLight) "light" else "dark"
            Text(
                "如果你正在记忆四级单词，又使用字幕或mkv视频生成了一个字幕词库，四级词库里有一个单词比如 Dragon, 字幕词库里也有一个 Dragon," +
                        "如果你只想链接字幕词库里的一个单词而不是整个词库，就可以使用链接字幕功能。\n\n"
            )

            Text(
                "1. 这个功能在编辑单词界面，有两个方式打开编辑单词：\n" +
                        "一个在记忆单词界面，把鼠标移动到正在记忆单词，会弹出一个菜单，然后选择编辑单词。\n" +
                        "另一个在编辑词库界面，选择了一个单词双击鼠标左键，就会打开编辑单词界面。\n"

            )
            Row(Modifier.padding(start = 155.dp)){
                Image(
                    painter = painterResource("screenshot/link-captions-$theme/edit word button.png"),
                    contentDescription = "edit word button",
                    modifier = Modifier.width(520.dp).height(250.dp).padding(start = 20.dp)

                )
            }
            Text(
                "\n\n2. 打开编辑单词后，如果当前单词的字幕数少于 3 个，在底部就会出现链接字幕功能。\n"
            )
            Image(
                painter = painterResource("screenshot/link-captions-$theme/edit word.png"),
                contentDescription = "edit word",
                modifier = Modifier.width(850.dp).height(807.dp).padding(start = 20.dp)
            )
            Text(
                "\n\n3. 打开后选择一个字幕词库，如果字幕词库里有和当前词库匹配的单词，就会出现一个字幕列表\n" +
                        "然后选择对应的字幕即可。\n"
            )
            Row(Modifier.padding(start = 115.dp,bottom = 20.dp)){
                Image(
                    painter = painterResource("screenshot/link-captions-$theme/link caption.png"),
                    contentDescription = "link caption",
                    modifier = Modifier.width(621.dp).height(697.dp).padding(start = 20.dp)
                )
            }

        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(stateVertical)
        )
    }
}

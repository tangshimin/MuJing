package util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.matthewn4444.ebml.subtitles.SSASubtitles
import data.Caption
import data.Dictionary
import data.ExternalCaption
import data.Word
import opennlp.tools.langdetect.LanguageDetector
import opennlp.tools.langdetect.LanguageDetectorME
import opennlp.tools.langdetect.LanguageDetectorModel
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.mozilla.universalchardet.UniversalDetector
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import javax.swing.JOptionPane

@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseDocument(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val file = File(pathName)
    var text = ""
    val extension = file.extension
    val otherExtensions = listOf("txt", "java","md","cs", "cpp", "c", "kt", "js", "py", "ts")

    try{
        if (extension == "pdf") {
            setProgressText("正在加载文档")
            val document: PDDocument = PDDocument.load(file)
            //Instantiate PDFTextStripper class
            val pdfStripper = PDFTextStripper()
            text = pdfStripper.getText(document)
            document.close()
        } else if (otherExtensions.contains(extension)) {
            text = file.readText()
        }
    }catch (exception: InvalidPasswordException){
        JOptionPane.showMessageDialog(null,exception.message)
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }


    val set: MutableSet<String> = HashSet()
    val list = mutableListOf<String>()
    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        val model = TokenizerModel(inputStream)
        setProgressText("正在分词")
        val tokenizer: Tokenizer = TokenizerME(model)
        val tokenize = tokenizer.tokenize(text)
        setProgressText("正在处理特殊分隔符")
        tokenize.forEach { word ->
            val lowercase = word.lowercase(Locale.getDefault())
            // 在代码片段里的关键字之间用.符号分隔
            if (lowercase.contains(".")) {
                val split = lowercase.split("\\.").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            // 还有一些关键字之间用 _ 符号分隔
            if (lowercase.matches(Regex("_"))) {
                val split = lowercase.split("_").toTypedArray()
                for (str in split) {
                    if (!set.contains(str)) {
                        list.add(str)
                        set.add(str)
                    }
                }
                set.addAll(split.toList())
            }
            if (!set.contains(lowercase)) {
                list.add(lowercase)
                set.add(lowercase)
            }
            set.add(lowercase)
        }

    }

    setProgressText("从文档提取出 ${set.size} 个单词，正在批量查询单词，如果词典里没有的就丢弃")
    val validList = Dictionary.queryList(list)
    setProgressText("${validList.size} 个有效单词")
    setProgressText("")
    return validList
}



// 提取 srt 字幕 ffmpeg -i input.mkv -map "0:2" output.eng.srt
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseSRT(
    pathName: String,
    setProgressText: (String) -> Unit
): List<Word> {
    val map: MutableMap<String, MutableList<Caption>> = HashMap()
    // 保存顺序
    val orderList = mutableListOf<String>()
    try{
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { input ->
            val model = TokenizerModel(input)
            val tokenizer: Tokenizer = TokenizerME(model)
            val formatSRT = FormatSRT()
            val file = File(pathName)
            val encoding = UniversalDetector.detectCharset(file)
            val charset =  if(encoding != null){
                Charset.forName(encoding)
            }else{
                Charset.defaultCharset()
            }
            val inputStream: InputStream = FileInputStream(file)

            setProgressText("正在解析字幕文件")
            val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream,charset)

            val captions: TreeMap<Int, subtitleFile.Caption> = timedTextObject.captions
            val captionList: Collection<subtitleFile.Caption> = captions.values
            setProgressText("正在分词")
            for (caption in captionList) {
                var content = replaceSpecialCharacter(caption.content)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    // getTime(format) 返回的时间不能播放
                    start = caption.start.getTime("hh:mm:ss,ms"),
                    end = caption.end.getTime("hh:mm:ss,ms"),
                    content = content
                )
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    val lowercase = word.lowercase(Locale.getDefault())
                    if (!map.containsKey(lowercase)) {
                        val list = mutableListOf(dataCaption)
                        map[lowercase] = list
                        orderList.add(lowercase)
                    } else {
                        if (map[lowercase]!!.size < 3 && !map[lowercase]!!.contains(dataCaption)) {
                            map[lowercase]?.add(dataCaption)
                        }
                    }
                }
            }
        }
        setProgressText("从字幕文件中提取出 ${orderList.size} 个单词，正在批量查询单词，如果词典里没有就丢弃")
        val validList = Dictionary.queryList(orderList)
        setProgressText("${validList.size} 个有效单词")
        validList.forEach { word ->
            if (map[word.value] != null) {
                word.captions = map[word.value]!!
            }
        }
        setProgressText("")
        return validList
    }catch (exception: IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }
    return listOf()
}


@OptIn(ExperimentalComposeUiApi::class)
fun parseMKV(
    pathName: String,
    trackId: Int,
    setProgressText: (String) -> Unit,
): List<Word> {
    val map: MutableMap<String, ArrayList<Caption>> = HashMap()
    val orderList = mutableListOf<String>()
    var reader: EBMLReader? = null
    try {
        reader = EBMLReader(pathName)

        setProgressText("正在解析 MKV 文件")

        /**
         * Check to see if this is a valid MKV file
         * The header contains information for where all the segments are located
         */
        if (!reader.readHeader()) {
            println("This is not an mkv file!")
            return listOf()
        }

        /**
         * Read the tracks. This contains the details of video, audio and subtitles
         * in this file
         */
        reader.readTracks()

        /**
         * Check if there are any subtitles in this file
         */
        val numSubtitles: Int = reader.subtitles.size
        if (numSubtitles == 0) {
            return listOf()
        }

        /**
         * You need this to find the clusters scattered across the file to find
         * video, audio and subtitle data
         */
        reader.readCues()


        /**
         *   OPTIONAL: You can read the header of the subtitle if it is ASS/SSA format
         *       for (int i = 0; i < reader.getSubtitles().size(); i++) {
         *         if (reader.getSubtitles().get(i) instanceof SSASubtitles) {
         *           SSASubtitles subs = (SSASubtitles) reader.getSubtitles().get(i);
         *           System.out.println(subs.getHeader());
         *         }
         *       }
         *
         *
         *  Read all the subtitles from the file each from cue index.
         *  Once a cue is parsed, it is cached, so if you read the same cue again,
         *  it will not waste time.
         *  Performance-wise, this will take some time because it needs to read
         *  most of the file.
         */
        for (i in 0 until reader.cuesCount) {
            reader.readSubtitlesInCueFrame(i)
        }
        setProgressText("正在分词")
        ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            val model = TokenizerModel(inputStream)
            val tokenizer: Tokenizer = TokenizerME(model)
            val subtitle = reader.subtitles[trackId]
            var isASS = false
            if (subtitle is SSASubtitles) {
                isASS = true
            }

            val captionList = subtitle.readUnreadSubtitles()
            for (caption in captionList) {
                val captionContent =  if(isASS){
                    caption.formattedVTT.replace("\\N","\n")
                }else{
                    caption.stringData
                }

                var content = replaceSpecialCharacter(captionContent)
                content = removeLocationInfo(content)
                val dataCaption = Caption(
                    start = caption.startTime.format().toString(),
                    end = caption.endTime.format(),
                    content = content
                )

                content = content.lowercase(Locale.getDefault())
                val tokenize = tokenizer.tokenize(content)
                for (word in tokenize) {
                    if (!map.containsKey(word)) {
                        val list = ArrayList<Caption>()
                        list.add(dataCaption)
                        map[word] = list
                        orderList.add(word)
                    } else {
                        if (map[word]!!.size < 3 && !map[word]!!.contains(dataCaption)) {
                            map[word]!!.add(dataCaption)
                        }
                    }
                }
            }
        }
    } catch (e: IOException) {
        JOptionPane.showMessageDialog(null,e.message)
        e.printStackTrace()
    } finally {
        try {
            // Remember to close this!
            reader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    setProgressText("从视频中提取出${orderList.size}个单词，正在批量查询单词，如果词典里没有就丢弃")
    val validList = Dictionary.queryList(orderList)
    setProgressText("${validList.size}个有效单词")
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.captions = map[word.value]!!
        }
    }
    setProgressText("")
    return validList
}


/**
 * 批量读取 MKV
 */
@OptIn(ExperimentalComposeUiApi::class)
fun batchReadMKV(
    language:String,
    selectedFileList:(List<File>),
    setCurrentTask:(File?) -> Unit,
    setErrorMessages:(Map<File,String>) -> Unit,
    updateTaskState:(Pair<File,Boolean>) -> Unit
):List<Word>{
    val errorMessage = mutableMapOf<File,String>()
    val map: MutableMap<String, ArrayList<ExternalCaption>> = HashMap()
    val orderList = mutableListOf<String>()


    ResourceLoader.Default.load("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { tokensInputStream ->
        ResourceLoader.Default.load("opennlp/langdetect-183.bin").use { langdetectInputStream ->
            // 训练分词器
            val tokensModel = TokenizerModel(tokensInputStream)
            val tokenizer: Tokenizer = TokenizerME(tokensModel)

            // 训练语言检测器
            val langModel = LanguageDetectorModel(langdetectInputStream)
            val languageDetector: LanguageDetector = LanguageDetectorME(langModel)

            val englishIetfList = listOf("en","en-US","en-GB")
            val english = listOf("en","eng")
            for(file in selectedFileList){
                setCurrentTask(file)
                var reader: EBMLReader? = null
                try {
                    reader = EBMLReader(file.absolutePath)
                    if (!reader.readHeader()) {
                        println("This is not an mkv file!")
                        errorMessage[file] = "不是 MKV 文件"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                        continue
                    }

                    reader.readTracks()
                    val numSubtitles: Int = reader.subtitles.size
                    if (numSubtitles == 0) {
                        errorMessage[file] = "没有字幕"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                        continue
                    }
                    reader.readCues()
                    for (i in 0 until reader.cuesCount) {
                        reader.readSubtitlesInCueFrame(i)
                    }

                    var trackID = -1

                    for(i in 0 until reader.subtitles.size){
                        val subtitles = reader.subtitles[i]
                        if (englishIetfList.contains(subtitles.languageIetf) || english.contains(subtitles.language)) {
                            trackID = i
                            break
                        } else {
                            // 使用 OpenNLP 的语言检测工具检测字幕的语言
                            var content = ""
                            val subList = subtitles.readUnreadSubtitles().subList(0,10)
                            subList.forEach { caption ->
                                content += caption.stringData
                            }
                            val lang  = languageDetector.predictLanguage(content)
                            if(lang.lang == "eng"){
                                trackID = i
                                break
                            }
                        }
                    }

                    if (trackID != -1) {
                        val subtitle = reader.subtitles[trackID]
                        var isASS = false
                        if (subtitle is SSASubtitles) {
                            isASS = true
                        }
                        val captionList = subtitle.allReadCaptions
                        if(captionList.isEmpty()){
                            captionList.addAll(subtitle.readUnreadSubtitles())
                        }
                        for (caption in captionList) {
                            val captionContent =  if(isASS){
                                caption.formattedVTT.replace("\\N","\n")
                            }else{
                                caption.stringData
                            }
                            var content = replaceSpecialCharacter(captionContent)
                            content = removeLocationInfo(content)
                            val externalCaption = ExternalCaption(
                                relateVideoPath = file.absolutePath,
                                subtitlesTrackId = trackID,
                                subtitlesName = file.nameWithoutExtension,
                                start = caption.startTime.format().toString(),
                                end = caption.endTime.format(),
                                content = content
                            )

                            content = content.lowercase(Locale.getDefault())
                            val tokenize = tokenizer.tokenize(content)
                            for (word in tokenize) {
                                if (!map.containsKey(word)) {
                                    val list = ArrayList<ExternalCaption>()
                                    list.add(externalCaption)
                                    map[word] = list
                                    orderList.add(word)
                                } else {
                                    if (map[word]!!.size < 3 && !map[word]!!.contains(externalCaption)) {
                                        map[word]!!.add(externalCaption)
                                    }
                                }
                            }
                        }
                        updateTaskState(Pair(file, true))
                    } else {
                        errorMessage[file] = "没有找到英语字幕"
                        updateTaskState(Pair(file, false))
                        setCurrentTask(null)
                        continue
                    }

                } catch (exception: IOException) {
                    updateTaskState(Pair(file, false))
                    setCurrentTask(null)
                    if(exception.message != null){
                        errorMessage[file] = exception.message.orEmpty()
                    } else{
                        errorMessage[file] =  "IO 异常"
                    }
                    exception.printStackTrace()
                    continue
                }catch (exception: UnSupportSubtitlesException){
                    updateTaskState(Pair(file, false))
                    if(exception.message != null){
                        errorMessage[file] = exception.message.orEmpty()
                    } else {
                        errorMessage[file] = "字幕格式不支持"
                    }
                    exception.printStackTrace()
                    setCurrentTask(null)
                    continue
                } catch (exception:NullPointerException){
                    updateTaskState(Pair(file, false))
                    errorMessage[file] = "空指针异常"
                    exception.printStackTrace()
                    setCurrentTask(null)
                    continue
                }finally {
                    try {
                        reader?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }


    val validList = Dictionary.queryList(orderList)
    validList.forEach { word ->
        if (map[word.value] != null) {
            word.externalCaptions = map[word.value]!!
        }
    }

    setErrorMessages(errorMessage)
    return validList
}


/**
 * 替换一些特殊字符
 */
fun replaceSpecialCharacter(captionContent: String): String {
    var content = captionContent
    if (content.startsWith("-")) content = content.substring(1)
    if (content.contains("<i>")) {
        content = content.replace("<i>", "")
    }
    if (content.contains("</i>")) {
        content = content.replace("</i>", "")
    }
    if (content.contains("<br />")) {
        content = content.replace("<br />", "\n")
    }
    content = removeLocationInfo(content)
    return content
}

/** 有一些字幕并不是在一个的固定位置，而是标注在人物旁边，这个函数删除位置信息 */
fun removeLocationInfo(content: String): String {
    val pattern = Pattern.compile("\\{.*\\}")
    val matcher = pattern.matcher(content)
    return matcher.replaceAll("")
}
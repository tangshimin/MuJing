package util

import androidx.compose.ui.ExperimentalComposeUiApi
import com.matthewn4444.ebml.EBMLReader
import com.matthewn4444.ebml.UnSupportSubtitlesException
import com.matthewn4444.ebml.subtitles.SSASubtitles
import data.Caption
import data.Dictionary
import data.MutableVocabulary
import data.Vocabulary
import data.VocabularyType
import data.Word
import data.loadVocabulary
import data.saveVocabulary
import ffmpeg.convertToSrt
import ffmpeg.extractSubtitles
import ffmpeg.hasRichText
import ffmpeg.removeRichText
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.langdetect.LanguageDetector
import opennlp.tools.langdetect.LanguageDetectorME
import opennlp.tools.langdetect.LanguageDetectorModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.Tokenizer
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.LoggerFactory
import player.PlayerState
import state.getSettingsDirectory
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import ui.dialog.getWordLemma
import ui.wordscreen.loadWordScreenVocabulary
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import javax.swing.JOptionPane




/**
 * 解析文档
 * @param pathName 文件路径
 * @param sentenceLength 单词所在句子的最大单词数
 * @param setProgressText 设置进度文本
 */
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseDocument(
    pathName: String,
    enablePhrases: Boolean,
    sentenceLength:Int = 25,
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
            // 移除 windows text 文件的 BOM
            if (extension =="txt" && text.isNotEmpty() && text[0].code == 65279) {
                text = text.substring(1)
            }
        }
    }catch (exception: InvalidPasswordException){
        JOptionPane.showMessageDialog(null,exception.message)
    }catch (exception:IOException){
        JOptionPane.showMessageDialog(null,exception.message)
    }

    // 单词 -> 句子映射，用于保存单词在文档中的位置
    val map = mutableMapOf<String, MutableList<String>>()

    // 加载分词模型
    val tokenModel = loadModelResource("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
        TokenizerModel(inputStream)
    }
    val tokenizer = TokenizerME(tokenModel)
    // 加载词性标注模型
    val posModel = loadModelResource("opennlp/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin").use { inputStream ->
        POSModel(inputStream)
    }
    val posTagger = POSTaggerME(posModel)
    // 加载分块模型
    val chunkerModel = loadModelResource("opennlp/en-chunker.bin").use { inputStream ->
        ChunkerModel(inputStream)
    }
    val chunker = ChunkerME(chunkerModel)

    setProgressText("正在断句")
    val sentences = sentenceDetect(text)
    setProgressText("正在分词")
    sentences.forEach { sentence ->
        val wordList = if(enablePhrases){
            tokenizeAndChunkText(sentence, tokenizer, posTagger, chunker)
        }else{
            tokenizeText(sentence, tokenizer)
        }
        wordList.forEach { word ->
            val clippedSentence = clipSentence(word, tokenizer, sentence, sentenceLength)
            val formatSentence = clippedSentence.replace("\r\n", " ").replace("\n", " ")
            val lowercase = word.lowercase(Locale.getDefault())
            // 在代码片段里的关键字之间用 . 或 _ 符号分隔
            val delimiters = listOf(".", "_")
            delimiters.forEach { delimiter ->
                if (lowercase.contains(delimiter)) {
                    val split = lowercase.split(delimiter).toTypedArray()
                    for (str in split) {
                        if (!map.contains(str)) {
                            val list = mutableListOf(formatSentence)
                            map[str] = list
                        } else {
                            // 如果单词的位置列表小于 3，就添加
                            if (map[str]!!.size < 3) {
                                map[str]?.add(formatSentence)
                            }
                        }
                    }
                }
            }

            if (!map.contains(lowercase)) {
                val list = mutableListOf(formatSentence)
                map[lowercase] = list
            } else {
                // 如果单词的位置列表小于 3，就添加
                if (map[lowercase]!!.size < 3) {
                    map[lowercase]?.add(formatSentence)
                }
            }

        }

    }

    setProgressText("从文档提取出 ${map.size} 个单词，正在批量查询单词，如果词典里没有的就丢弃")
    val validList = Dictionary.queryList(map.keys.toList())

    val filterList = listOf(
        ".", "!", "?", ";", ":",  ")",  "}",  "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/", ">", ",", "，", "。", "、", "；", "：", "？", "！",
        "）","【", "】", "｛", "…",  "》", "”", "’"
    )
    validList.forEach { word ->
        if (map[word.value] != null) {
            var pos = ""
            map[word.value]!!.forEach { sentence ->
                // 丢弃句子开始的标点符号
                pos = if(filterList.contains(sentence[0].toString())){
                    sentence.substring(1) + "\n"
                }else{
                    sentence + "\n"
                }
            }
            word.pos =pos.trim()
        }
    }
    setProgressText("${validList.size} 个有效单词")
    setProgressText("")
    return validList
}

/**
 * 剪裁句子
 */
fun clipSentence(
    word: String,
    tokenizer: Tokenizer,
    sentences: String,
    sentenceLength: Int
): String {
    val tokenList = tokenizer.tokenize(sentences).toList()
    if(tokenList.size > sentenceLength){
        val index = tokenList.indexOf(word)
        if(index != -1){
            val start = if(index - sentenceLength/2 < 0) 0 else index - sentenceLength/2
            val end = if(index + sentenceLength/2 > tokenList.size) tokenList.size else index + sentenceLength/2
            var clipSentence = ""
            for(i in start until end){
                clipSentence += "${tokenList[i]} "
            }
            return clipSentence
        }else{
            // 单词是短语
            val formatSentence = sentences.replace("\r\n", " ").replace("\n", " ")
           val strIndex = formatSentence.indexOf(word)
            if(strIndex == -1){
                return formatSentence
            }
            // 以 strIndex 为中心，向前找到 sentenceLength/2 空格确定开始，如果开始位置小于 0，就从 0 开始
            // 向后找到 sentenceLength/2 空格确定结束，如果结束位置大于 sentences 的长度，就以 sentences 的长度为结束
            var start = strIndex
            var end = strIndex
            var spaceCount = 0
            while (spaceCount < sentenceLength/2){
                if(start == 0){
                    break
                }
                start--
                if(formatSentence[start] == ' '){
                    spaceCount++
                }

            }
            spaceCount = 0
            while (spaceCount < sentenceLength/2){
                if(end == formatSentence.length){
                    break
                }
                if(formatSentence[end] == ' '){
                    spaceCount++
                }
                end++
            }
            return formatSentence.substring(start,end)

        }

    }else{
        return sentences
    }

}

/**
 * 使用 OpenNLP 的 SentenceDetectorME 模型来检测句子
 */
@OptIn(ExperimentalComposeUiApi::class)
fun sentenceDetect(text: String): List<String> {
    val sentences = mutableListOf<String>()
    loadModelResource("opennlp/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin").use { modelIn ->
        val model = SentenceModel(modelIn)
        val sentenceDetector = SentenceDetectorME(model)
        sentenceDetector.sentDetect(text).forEach { sentence ->
            sentences.add(sentence)
        }
    }
    return sentences
}

/**
 * 使用词性标注和分块分词，分割单词和短语
 */
fun tokenizeAndChunkText(
    text: String,
    tokenizer: Tokenizer,
    posTagger: POSTaggerME,
    chunker: ChunkerME
): MutableSet<String> {
    val logger = LoggerFactory.getLogger("tokenizeAndChunkText")
    // 进行分词
    val tokens = tokenizer.tokenize(text)
    // 进行词性标注
    val posTags = posTagger.tag(tokens)
    // 进行分块
    val chunks = chunker.chunkAsSpans(tokens, posTags)

    // 过滤掉常用的标点符号
    // .!?;:(){}[]\-—'"`~@#$%^&*+=|\/<>,，。、；：？！（）【】｛｝—…《》“”‘’,
    val filterList = listOf(
        ".", "!", "?", ";", ":", "(", ")", "{", "}", "[", "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/","<", ">", ",", "，", "。", "、", "；", "：", "？", "！", "（",
        "）","【", "】", "｛", "｝", "…", "《", "》", "“", "”", "‘", "’"
    )

    val wordList =tokens.toMutableList()
    for (chunk in chunks) {
        var word = ""
        for(i in chunk.start until chunk.end){
           word += "${tokens[i]} "
        }
        // 丢弃单词首尾的空格
        word = word.trim()
        // 如果单词的第一个字符是标点符号，就丢弃标点
        if (word.length>1 && filterList.contains(word[0].toString())) {
            logger.info("$word 丢弃开始标点")
            word = word.substring(1)
        }
        // 如果单词的最后一个字符是标点符号，就丢弃标点
        if (word.length>1 && filterList.contains(word[word.length - 1].toString())) {
            logger.info("$word 丢弃结束标点")
            word = word.substring(0, word.length - 1)
        }
        // 再一次丢弃单词首尾的空格
        word = word.trim()
        wordList.add(word)
    }
    val result = mutableSetOf<String>()
    // 过滤掉常用的标点符号
    wordList.forEach { word ->
        if (!filterList.contains(word)) {
            result.add(word)
        }
    }
    return result
}

/**
 * 分割单词
 */
fun tokenizeText(
    text: String,
    tokenizer: Tokenizer,
): MutableSet<String> {
    // 进行分词
    val tokens = tokenizer.tokenize(text)
    // 过滤掉常用的标点符号
    // .!?;:(){}[]\-—'"`~@#$%^&*+=|\/<>,，。、；：？！（）【】｛｝—…《》“”‘’,
    val filterList = listOf(
        ".", "!", "?", ";", ":", "(", ")", "{", "}", "[", "]", "-", "—",
        "'", "`", "~", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|",
        "/","<", ">", ",", "，", "。", "、", "；", "：", "？", "！", "（",
        "）","【", "】", "｛", "｝", "…", "《", "》", "“", "”", "‘", "’"
    )

    val wordList =tokens.toMutableList()
    val result = mutableSetOf<String>()
    // 过滤掉常用的标点符号
    wordList.forEach { word ->
        if (!filterList.contains(word)) {
            var tempWord = word
            // 丢弃单个字符和连字符的组合里的连字符
            // 例如 -y,-d, y-, d-, 不是常用单词，但是内置词典里面有
            if(tempWord.startsWith("-") && tempWord.length == 2){
                tempWord  =  tempWord.substring(1)
                println(tempWord)
            }else if(tempWord.endsWith("-") && tempWord.length == 2){
                tempWord =  tempWord.substring(0,word.length-1)
                println(tempWord)
            }
            result.add(tempWord)
        }
    }

    // 处理特殊单词 gonna
    if (text.contains(" gonna ")) {
        var needRemoveGon = false
        var needRemoveNa = false
        result.forEach { word ->
            // 分词分出 gon 但实际上文本里没有 gon
            if (word == "gon" && !Regex("""\bgon\b""").containsMatchIn(text)) {
                needRemoveGon = true
            }
            // 分词分出 na 但实际上文本里没有 na
            if (word == "na" && !Regex("""\bna\b""").containsMatchIn(text)) {
                needRemoveNa = true
            }
        }
        result.add("gonna")
        if (needRemoveGon) result.remove("gon")
        if (needRemoveNa) result.remove("na")
    }


    return result
}

/**
 * 解析 SRT 字幕文件
 */
@OptIn(ExperimentalComposeUiApi::class)
@Throws(IOException::class)
fun parseSRT(
    pathName: String,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit
): List<Word> {
    val srtFile = File(pathName)
    val hasRichText = hasRichText(srtFile)
    if(hasRichText){
        setProgressText("字幕有富文本标签，先移除富文本标签")
        removeRichText(srtFile)
    }

    val map: MutableMap<String, MutableList<Caption>> = HashMap()
    // 保存顺序
    val orderList = mutableListOf<String>()
    try {
        // 加载分词模型
        val tokenModel = loadModelResource("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            TokenizerModel(inputStream)
        }
        val tokenizer = TokenizerME(tokenModel)
        // 加载词性标注模型
        val posModel = loadModelResource("opennlp/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin").use { inputStream ->
            POSModel(inputStream)
        }
        val posTagger = POSTaggerME(posModel)
        // 加载分块模型
        val chunkerModel = loadModelResource("opennlp/en-chunker.bin").use { inputStream ->
            ChunkerModel(inputStream)
        }
        val chunker = ChunkerME(chunkerModel)

        val formatSRT = FormatSRT()
        val file = File(pathName)
        val encoding = UniversalDetector.detectCharset(file)
        val charset = if (encoding != null) {
            Charset.forName(encoding)
        } else {
            Charset.defaultCharset()
        }
        val inputStream: InputStream = FileInputStream(file)

        setProgressText("正在解析字幕文件")
        val timedTextObject: TimedTextObject = formatSRT.parseFile(file.name, inputStream, charset)

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
            val tokenize = if(enablePhrases){
                tokenizeAndChunkText(content, tokenizer, posTagger, chunker)
            }else{
                tokenizeText(content, tokenizer)
            }
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
    } catch (exception: IOException) {
        JOptionPane.showMessageDialog(null, exception.message)
    }
    return listOf()
}

/**
 * 解析 ASS 字幕文件
 */
@Throws(IOException::class)
fun parseASS(
    pathName: String,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit
): List<Word> {
    val applicationDir = getSettingsDirectory()
    val assFile = File(pathName)
    val srtFile = File("$applicationDir/temp.srt")
    setProgressText("开始转换字幕")
    val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
    if(result == "finished"){
        setProgressText("字幕转换完成")
        val list =  parseSRT(srtFile.absolutePath,enablePhrases,setProgressText)
        srtFile.delete()
        return list
    }else{
        setProgressText("字幕转换失败")
        srtFile.delete()
        return emptyList()
    }
}

/**
 * 使用 FFmpeg 解析视频文件
 */
fun parseVideo(
    pathName: String,
    trackId: Int,
    enablePhrases: Boolean,
    setProgressText: (String) -> Unit,
): List<Word> {
    val applicationDir = getSettingsDirectory()
    setProgressText("正在提取字幕")
    val result =  extractSubtitles(pathName, trackId, "$applicationDir/temp.srt")
    setProgressText("提取字幕完成")
    if(result == "finished"){
        val list = parseSRT("$applicationDir/temp.srt",enablePhrases,setProgressText)
        File("$applicationDir/temp.srt").delete()
        return list
    }
    return emptyList()
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
        loadModelResource("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
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
    enablePhrases: Boolean,
    selectedFileList:(List<File>),
    setCurrentTask:(File?) -> Unit,
    setErrorMessages:(Map<File,String>) -> Unit,
    updateTaskState:(Pair<File,Boolean>) -> Unit
):List<Word>{
    val errorMessage = mutableMapOf<File, String>()
    val orderList = mutableListOf<Word>()
    val logger = LoggerFactory.getLogger("batchReadMKV")
    // 加载语言检测模型
    val langModel = loadModelResource("opennlp/langdetect-183.bin").use { inputStream ->
        LanguageDetectorModel(inputStream)
    }
    val languageDetector: LanguageDetector = LanguageDetectorME(langModel)

    val englishIetfList = listOf("en", "en-US", "en-GB")
    val english = listOf("en", "eng")
    for (file in selectedFileList) {
        setCurrentTask(file)
        var reader: EBMLReader? = null
        try {
            reader = EBMLReader(file.absolutePath)
            if (!reader.readHeader()) {
                logger.error("这个视频不是 MKV 标准的文件")
                errorMessage[file] = "不是 MKV 文件"
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }

            reader.readTracks()
            val numSubtitles: Int = reader.subtitles.size
            if (numSubtitles == 0) {
                errorMessage[file] = "没有字幕"
                logger.error("${file.nameWithoutExtension} 没有字幕")
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }
            reader.readCues()
            for (i in 0 until reader.cuesCount) {
                reader.readSubtitlesInCueFrame(i)
            }

            var trackID = -1
            // 轨道名称和轨道 ID 的映射,可能有多个英语字幕
            val trackMap = mutableMapOf<String,Int>()
            for (i in 0 until reader.subtitles.size) {
                val subtitles = reader.subtitles[i]
                if (englishIetfList.contains(subtitles.languageIetf) || english.contains(subtitles.language)) {
                    val name = if(subtitles.name.isNullOrEmpty()) "English" else subtitles.name
                    trackMap[name] = i
                } else {
                    // 提取一小部分字幕，使用 OpenNLP 的语言检测工具检测字幕的语言
                    val captionSize = subtitles.allReadCaptions.size
                    val subList = if(captionSize>10){
                        subtitles.readUnreadSubtitles().subList(0, 10)
                    }else if(captionSize> 5){
                        subtitles.readUnreadSubtitles().subList(0, 5)
                    }else{
                        subtitles.readUnreadSubtitles()
                    }

                    var content = ""
                    subList.forEach { caption ->
                        content += caption.stringData
                    }
                    val lang = languageDetector.predictLanguage(content)
                    if (lang.lang == "eng") {
                        val name = if(subtitles.name.isNullOrEmpty()) "English" else subtitles.name
                        trackMap[name] = i
                    }
                }
            }

            // 优先选择 SDH 字幕
            for ((name, id) in trackMap) {
                if (name.contains("SDH", ignoreCase = true)) {
                    trackID = id
                    logger.info("$name 字幕，TrackID: $id")
                    break
                }
            }
            if(trackID == -1){
                trackID = trackMap.values.first()
                logger.info("English 字幕，TrackID: $trackID")
            }

            if (trackID != -1) {
                val words = parseVideo(
                    pathName = file.absolutePath,
                    enablePhrases = enablePhrases,
                    trackId = trackID,
                    setProgressText = { }
                )
                orderList.addAll(words)
                updateTaskState(Pair(file, true))
            } else {
                errorMessage[file] = "没有找到英语字幕"
                logger.error("${file.nameWithoutExtension} 没有找到英语字幕")
                updateTaskState(Pair(file, false))
                setCurrentTask(null)
                continue
            }

        } catch (exception: IOException) {
            updateTaskState(Pair(file, false))
            setCurrentTask(null)
            if (exception.message != null) {
                errorMessage[file] = exception.message.orEmpty()
                logger.error("${file.nameWithoutExtension} ${exception.message.orEmpty()}")
            } else {
                errorMessage[file] = "IO 异常"
                logger.error("${file.nameWithoutExtension} IO 异常\n ${exception.printStackTrace()}")
            }
            continue
        } catch (exception: UnSupportSubtitlesException) {
            updateTaskState(Pair(file, false))
            if (exception.message != null) {
                errorMessage[file] = exception.message.orEmpty()
                logger.error("${file.nameWithoutExtension} ${exception.message.orEmpty()}")
            } else {
                errorMessage[file] = "字幕格式不支持"
                logger.error("${file.nameWithoutExtension} 字幕格式不支持")
            }

            logger.error("${file.nameWithoutExtension} 字幕格式不支持\n ${exception.printStackTrace()}")
            setCurrentTask(null)
            continue
        } catch (exception: NullPointerException) {
            updateTaskState(Pair(file, false))
            errorMessage[file] = "空指针异常"
            logger.error("${file.nameWithoutExtension} 空指针异常\n ${ exception.printStackTrace()}")
            setCurrentTask(null)
            continue
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                logger.error("${file.nameWithoutExtension}:\n ${ e.printStackTrace()}")
            }

        }
    }
    setErrorMessages(errorMessage)
    return orderList.toList()
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


/**
 * 对比两个词汇表，返回它们的交集词汇表。
 *
 * @param baseline 基准词汇表，作为对比的基础。
 * @param comparison 需要与基准词汇表对比的词汇表。
 * @param matchLemma 是否按词元（lemma）进行匹配。如果为 true，则以词元为单位进行对比，否则以单词本身为单位。
 * @return 包含交集单词的新词汇表，词汇表类型、语言等信息继承自基准词汇表。
 */
fun matchVocabulary(
    baseline: Vocabulary,
    comparison: Vocabulary,
    matchLemma: Boolean,
): Vocabulary {

    val result = Vocabulary(
        name = "",
        type = baseline.type,
        language = baseline.language,
        size = 0,
        relateVideoPath = baseline.relateVideoPath,
        subtitlesTrackId = baseline.subtitlesTrackId,
        wordList = mutableListOf()
    )

    if (matchLemma) {
        val baselineLemma = mutableMapOf<String,Word>()
        val comparisonLemma = mutableListOf<String>()
        baseline.wordList.forEach { word ->
            val lemma = getWordLemma(word)
            baselineLemma[lemma] = word
        }

        comparison.wordList.forEach { word ->
            val lemma = getWordLemma(word)
            if (!comparisonLemma.contains(lemma)) {
                comparisonLemma.add(lemma)
            }
        }

        comparisonLemma.forEach { lemma ->
            if (baselineLemma.contains(lemma)) {
                // 基准词库有，对比词库也有
                // 这里把基准词库里的单词加入结果
                val word = baselineLemma[lemma]
                word?.let {
                    result.wordList.add(it)
                }
            }
        }
    } else {
        baseline.wordList.forEach { word ->
            if (comparison.wordList.contains(word)) {
                result.wordList.add(word)
            }
        }
    }
    result.size = result.wordList.size
    return result
}


/**
 * 使用字幕生成一个和正在记忆的单词相匹配的词库
 *
 * 该函数会根据提供的视频路径和字幕路径，解析字幕文件并生成词汇表。如果缓存的词汇表已存在且与当前视频一致，
 * 则直接加载缓存的词汇表；否则重新解析字幕并生成新的词汇表。最后，将生成的词汇表与当前词汇屏幕的词汇表进行匹配，
 * 并保存匹配结果。
 *
 * @param videoPath 当前视频的路径。
 * @param subPath 当前视频对应的字幕文件路径。
 * @param trackId 当前选择的字幕轨道 ID。
 * @param state 当前播放器的状态对象，用于存储生成的词汇表路径和内容。
 */
fun generateMatchedVocabulary(
    videoPath: String,
    subPath: String,
    trackId: Int,
    state: PlayerState
) {
    val applicationDir = getSettingsDirectory()
    val cachedVocabulary = File("$applicationDir/VideoPlayer/VideoVocabulary.json")
    if (subPath.isNotEmpty() && File(subPath).exists()) {
        if (!cachedVocabulary.exists() || subIsChanged(videoPath, cachedVocabulary.absolutePath,trackId)) {
            val words = parseSRT(
                pathName = subPath,
                setProgressText = { println(it) },
                enablePhrases = true
            )
            val newVocabulary = Vocabulary(
                name = "VideoPlayerVocabulary",
                type = VocabularyType.SUBTITLES,
                language = "",
                size = words.size,
                relateVideoPath = videoPath,
                subtitlesTrackId = trackId,
                wordList = words.toMutableList()
            )
            saveVocabulary(newVocabulary, cachedVocabulary.absolutePath)
        }
    }

    if (cachedVocabulary.exists() && state.wordScreenVocabularyPath.isNotEmpty()) {
        val baseline = loadVocabulary(cachedVocabulary.absolutePath)
        val comparison = loadWordScreenVocabulary()
        comparison?.let {
            val matched = matchVocabulary(
                baseline = baseline,
                comparison = comparison,
                matchLemma = true
            )
            val matchedPath = "$applicationDir/VideoPlayer/MatchedVocabulary.json"
            saveVocabulary(matched, matchedPath)
            state.vocabularyPath = matchedPath
            state.vocabulary = MutableVocabulary(matched)
        }
    }
}

/**
 * 缓存词库对应的视频路径是否和正在播放视频路径一致
 * @param videoPath 当前视频路径
 * @param vocabularyPath 缓存词库路径
 */
fun subIsChanged(videoPath:String,
                 vocabularyPath:String,
                 trackId: Int):Boolean{
    val cachedVocabulary = loadVocabulary(vocabularyPath)

    return cachedVocabulary.relateVideoPath != videoPath || cachedVocabulary.subtitlesTrackId != trackId
}

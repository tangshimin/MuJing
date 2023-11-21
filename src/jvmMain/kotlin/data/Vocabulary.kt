package data

import androidx.compose.runtime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getResourcesFile
import state.getSettingsDirectory
import java.io.File
import java.util.*
import javax.swing.JOptionPane

/**
 * 词库
 */
@Serializable
data class Vocabulary(
    var name: String = "",
    val type: VocabularyType = VocabularyType.DOCUMENT,
    val language: String,
    var size: Int,
    var relateVideoPath: String = "",
    val subtitlesTrackId: Int = 0,
    var wordList: MutableList<Word> = mutableListOf(),
)

/**
 * 可观察的词库
 */
class MutableVocabulary(vocabulary: Vocabulary) {
    var name by mutableStateOf(vocabulary.name)
    var type by mutableStateOf(vocabulary.type)
    var language by mutableStateOf(vocabulary.language)
    var size by mutableStateOf(vocabulary.size)
    var relateVideoPath by mutableStateOf(vocabulary.relateVideoPath)
    var subtitlesTrackId by mutableStateOf(vocabulary.subtitlesTrackId)
    var wordList = vocabulary.wordList.toMutableStateList()

    /**
     * 用于持久化
     */
    val serializeVocabulary
        get() = Vocabulary(name, type, language, size, relateVideoPath, subtitlesTrackId, wordList)

}


/**
links 存储字幕链接,格式：(subtitlePath)[videoPath][subtitleTrackId][index]
captions 字幕列表
 */
@Serializable
data class Word(
    var value: String,
    var usphone: String = "",
    var ukphone: String = "",
    var definition: String = "",
    var translation: String = "",
    var pos: String = "",
    var collins: Int = 0,
    var oxford: Boolean = false,
    var tag: String = "",
    var bnc: Int? = 0,
    var frq: Int? = 0,
    var exchange: String = "",
    var externalCaptions: MutableList<ExternalCaption> = mutableListOf(),
    var captions: MutableList<Caption> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        val otherWord = other as Word
        return this.value.lowercase() == otherWord.value.lowercase()
    }

    override fun hashCode(): Int {
        return value.lowercase().hashCode()
    }
}
fun Word.deepCopy():Word{
    val newWord =  Word(
        value, usphone, ukphone, definition, translation, pos, collins, oxford, tag, bnc, frq, exchange
    )
    externalCaptions.forEach { externalCaption ->
        newWord.externalCaptions.add(externalCaption)
    }
    captions.forEach { caption ->
        newWord.captions.add(caption)
    }
    return newWord
}


@Serializable
data class Caption(var start: String, var end: String, var content: String) {
    override fun toString(): String {
        return content
    }
}

/**
 * @param relateVideoPath 视频地址
 * @param subtitlesTrackId 字幕轨道 ID
 * @param subtitlesName 字幕名称，链接字幕词库时设置。在链接字幕窗口，用来删除这个字幕文件的所有字幕
 * @param start 开始
 * @param end 结束
 * @param content 字幕内容
 */
@Serializable
data class ExternalCaption(
    val relateVideoPath: String,
    val subtitlesTrackId: Int,
    var subtitlesName: String,
    var start: String,
    var end: String,
    var content: String
) {
    override fun toString(): String {
        return content
    }
}




fun loadMutableVocabulary(path: String): MutableVocabulary {
    val file = getResourcesFile(path)
    // 如果当前词库被删除，重启之后又没有选择新的词库，再次重启时才会调用，
    // 主要是为了避免再次重启是出现”找不到词库"对话框
    return if(path.isEmpty()){
        val vocabulary = Vocabulary(
            name = "",
            type = VocabularyType.DOCUMENT,
            language = "",
            size = 0,
            relateVideoPath = "",
            subtitlesTrackId = 0,
            wordList = mutableListOf()
        )
        MutableVocabulary(vocabulary)
    }else if (file.exists()) {

        try {
            val vocabulary = Json.decodeFromString<Vocabulary>(file.readText())
            if(vocabulary.size != vocabulary.wordList.size){
                vocabulary.size = vocabulary.wordList.size
            }
            MutableVocabulary(vocabulary)
        } catch (exception: Exception) {
            exception.printStackTrace()
            val vocabulary = Vocabulary(
                name = "",
                type = VocabularyType.DOCUMENT,
                language = "",
                size = 0,
                relateVideoPath = "",
                subtitlesTrackId = 0,
                wordList = mutableListOf()
            )
            JOptionPane.showMessageDialog(null, "词库解析错误：\n地址：$path\n" + exception.message)
            if(vocabulary.size != vocabulary.wordList.size){
                vocabulary.size = vocabulary.wordList.size
            }
            MutableVocabulary(vocabulary)
        }

    } else {
        val vocabulary = Vocabulary(
            name = "",
            type = VocabularyType.DOCUMENT,
            language = "",
            size = 0,
            relateVideoPath = "",
            subtitlesTrackId = 0,
            wordList = mutableListOf()
        )
        JOptionPane.showMessageDialog(null, "找不到词库：\n$path")
        MutableVocabulary(vocabulary)
    }

}


fun loadVocabulary(path: String): Vocabulary {
    val file = getResourcesFile(path)
    if (file.exists()) {
        return try {
            //  TODO 链接字幕词库时选取了一个错误文件，导致程序卡死。定位到这里  error:  Required array length 2147483638 + 16142 is too large
            Json.decodeFromString(file.readText())
        } catch (exception: Exception) {
            JOptionPane.showMessageDialog(null, "词库解析错误：\n地址：$path\n" + exception.message)
            Vocabulary(
                name = "",
                type = VocabularyType.DOCUMENT,
                language = "",
                size = 0,
                relateVideoPath = "",
                subtitlesTrackId = 0,
                wordList = mutableListOf()
            )
        }
    } else {

        return Vocabulary(
            name = "",
            type = VocabularyType.DOCUMENT,
            language = "",
            size = 0,
            relateVideoPath = "",
            subtitlesTrackId = 0,
            wordList = mutableListOf()
        )
    }

}
/** 主要加载困难词库和熟悉词库 */
fun loadMutableVocabularyByName(name: String):MutableVocabulary{
    val file = if (name == "FamiliarVocabulary") {
        getFamiliarVocabularyFile()
    } else getHardVocabularyFile()

    return if (file.exists()) {
        try {
            val vocabulary = Json.decodeFromString<Vocabulary>(file.readText())
            MutableVocabulary(vocabulary)
        } catch (exception: Exception) {
            exception.printStackTrace()
            val vocabulary = Vocabulary(
                name = name,
                type = VocabularyType.DOCUMENT,
                language = "",
                size = 0,
                relateVideoPath = "",
                subtitlesTrackId = 0,
                wordList = mutableListOf()
            )
            JOptionPane.showMessageDialog(null, "词库解析错误：\n地址：${file.absoluteFile}\n" + exception.message)
            MutableVocabulary(vocabulary)
        }

    } else {
        val vocabulary = Vocabulary(
            name = name,
            type = VocabularyType.DOCUMENT,
            language = "",
            size = 0,
            relateVideoPath = "",
            subtitlesTrackId = 0,
            wordList = mutableListOf()
        )
        MutableVocabulary(vocabulary)
}}


/** 返回熟悉词库文件 */
fun getFamiliarVocabularyFile():File{
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "FamiliarVocabulary.json")
}
/** 返回困难词库文件 */
fun getHardVocabularyFile():File{
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "HardVocabulary.json")
}

fun saveVocabularyToTempDirectory(vocabulary: Vocabulary, directory: String) {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    val json = format.encodeToString(vocabulary)
    val file = File("src/main/resources/temp/$directory/${vocabulary.name}.json")
    if (!file.parentFile.exists()) {
        file.parentFile.mkdirs()
    }
    File("src/main/resources/temp/$directory/${vocabulary.name}.json").writeText(json)
}

fun saveVocabulary(vocabulary: Vocabulary, path: String) {
    val format = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    val json = format.encodeToString(vocabulary)
    val file = getResourcesFile(path)
    file.writeText(json)
}

fun main() {

}

/**
 * 主要用于批量转换词库，
 * 调用方式：convertWord(File("D:\\MovContext\\resources\\common\\vocabulary"))
 */
fun convertWord(dir: File) {

    dir.listFiles().forEach { file ->

        if (file.isDirectory) {
            convertWord(file)
        } else {

            val oldVocabulary = Json.decodeFromString<OldVocabulary>(file.readText())
            var newList = mutableListOf<Word>()

            for (oldWord in oldVocabulary.wordList) {
                val word = Word(
                    value = oldWord.value,
                    usphone = oldWord.usphone,
                    ukphone = oldWord.ukphone,
                    definition = oldWord.definition,
                    translation = oldWord.translation,
                    pos = oldWord.pos,
                    collins = oldWord.collins,
                    oxford = oldWord.oxford,
                    tag = oldWord.tag,
                    bnc = oldWord.bnc,
                    frq = oldWord.frq,
                    exchange = oldWord.exchange,
                    externalCaptions = oldWord.links,
                    captions = oldWord.captions
                )
                newList.add(word)
            }


            val vocabulary = Vocabulary(
                name = file.nameWithoutExtension,
                language = oldVocabulary.language,
                size = oldVocabulary.size,
                wordList = newList
            )

            val directory = file.parent.split("\\").last()
            saveVocabularyToTempDirectory(vocabulary, directory)
        }
    }
}

@Serializable
data class OldWord(
    var value: String,
    var usphone: String = "",
    var ukphone: String = "",
    var definition: String = "",
    var translation: String = "",
    var pos: String = "",
    var collins: Int = 0,
    var oxford: Boolean = false,
    var tag: String = "",
    var bnc: Int? = 0,
    var frq: Int? = 0,
    var exchange: String = "",
    var links: MutableList<ExternalCaption> = mutableListOf(),
    var captions: MutableList<Caption> = mutableListOf()
)

@Serializable
data class OldVocabulary(
    var name: String = "",
    val type: VocabularyType = VocabularyType.DOCUMENT,
    val language: String,
    var size: Int,
    val relateVideoPath: String = "",
    val subtitlesTrackId: Int = 0,
    var wordList: MutableList<OldWord> = mutableListOf(),
)

fun loadOldVocabulary(pathname: String): OldVocabulary {
    return Json.decodeFromString(File(pathname).readText())
}

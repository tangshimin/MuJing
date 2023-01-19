package state

import androidx.compose.runtime.*
import com.formdev.flatlaf.FlatLightLaf
import data.Word
import data.loadMutableVocabulary
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.JOptionPane

/** 记忆单词的数据类 */
@ExperimentalSerializationApi
@Serializable
data class DataWordState(
    val wordVisible: Boolean = true,
    val phoneticVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val translationVisible: Boolean = true,
    val subtitlesVisible: Boolean = true,
    val isPlaySoundTips: Boolean = true,
    val soundTipsVolume: Float = 0.6F,
    val pronunciation: String = "us",
    val isAuto: Boolean = false,
    val index: Int = 0,
    val hardVocabularyIndex: Int = 0,
    var vocabularyName: String = "",
    var vocabularyPath: String = "",
    var externalSubtitlesVisible: Boolean = true,
)

/** 记忆单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class WordState(dataWordState: DataWordState) {

    // 可持久化的状态 开始
    /**
     * 单词组件的可见性
     */
    var wordVisible by mutableStateOf(dataWordState.wordVisible)

    /**
     * 音标组件的可见性
     */
    var phoneticVisible by mutableStateOf(dataWordState.phoneticVisible)

    /**
     * 词型组件的可见性
     */
    var morphologyVisible by mutableStateOf(dataWordState.morphologyVisible)

    /**
     * 定义组件的可见性
     */
    var definitionVisible by mutableStateOf(dataWordState.definitionVisible)

    /**
     * 翻译组件的可见性
     */
    var translationVisible by mutableStateOf(dataWordState.translationVisible)

    /**
     * 字幕组件的可见性
     */
    var subtitlesVisible by mutableStateOf(dataWordState.subtitlesVisible)

    /**
     * 是否播放提示音
     */
    var isPlaySoundTips by mutableStateOf(dataWordState.isPlaySoundTips)

    /**
     * 提示音音量
     */
    var soundTipsVolume by mutableStateOf(dataWordState.soundTipsVolume)

    /**
     * 选择发音，有英音、美音、日语
     */
    var pronunciation by mutableStateOf(dataWordState.pronunciation)

    /**
     * 是否是自动切换
     */
    var isAuto by mutableStateOf(dataWordState.isAuto)

    /**
     * 当前单词的索引，从0开始，在标题栏显示的时候 +1
     */
    var index by mutableStateOf(dataWordState.index)

    /**
     * 困难词库的索引，从0开始，在标题栏显示的时候 +1
     */
    var hardVocabularyIndex by mutableStateOf(dataWordState.hardVocabularyIndex)

    /**
     * 当前单词的章节，从1开始
     */
    var chapter by mutableStateOf((dataWordState.index / 20) + 1)

    /**
     * 词库的名称
     */
    var vocabularyName by mutableStateOf(dataWordState.vocabularyName)

    /**
     * 当前正在学习的词库的路径
     */
    var vocabularyPath by mutableStateOf(dataWordState.vocabularyPath)

    /** 外部字幕的可见性 */
    var externalSubtitlesVisible by mutableStateOf(dataWordState.externalSubtitlesVisible)

    // 可持久化的状态 结束

    /** 当前正在学习的词库 */
    var vocabulary = loadMutableVocabulary(vocabularyPath)

    /** 记忆单词界面的记忆策略 */
    var memoryStrategy by mutableStateOf(MemoryStrategy.Normal)

    /** 要听写的单词 */
    val dictationWords = mutableStateListOf<Word>()

    /** 听写单词时的索引 */
    var dictationIndex by mutableStateOf(0)

    /** 要听写复习的单词 */
    val reviewWords = mutableStateListOf<Word>()

    /** 听写错误的单词 */
    val wrongWords = mutableStateListOf<Word>()

    /** 进入听写模式之前需要保存变量 `typing` 的一些状态,退出听写模式后恢复 */
    private val typingWordStateMap = mutableStateMapOf<String, Boolean>()

    /** 获得当前单词 */
    fun getCurrentWord(): Word {

        return when (memoryStrategy){

            MemoryStrategy.Normal -> getWord(index)

            MemoryStrategy.Dictation -> dictationWords[dictationIndex]

            MemoryStrategy.Review -> reviewWords[dictationIndex]

            MemoryStrategy.NormalReviewWrong -> wrongWords[dictationIndex]

            MemoryStrategy.DictationReviewWrong -> wrongWords[dictationIndex]
        }

    }

    /** 根据索引返回单词 */
    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0 until size) {
            vocabulary.wordList[index]
        } else {
            // 如果用户使用编辑器修改了索引，并且不在单词列表的范围以内，就把索引改成0。
            this.index = 0
            saveTypingWordState()
            vocabulary.wordList[0]
        }

    }


    /**
     * 为听写模式创建一个随机词汇表
    - 伪代码
    - 1 -> 0,19
    - 2 -> 20,39
    - 3 -> 40,59
    - if chapter == 2
    - start = 2 * 20 -20, end = 2 * 20  -1
    - if chapter == 3
    - start = 3 * 20 -20, end = 3 * 20 - 1
     */
    fun generateDictationWords(currentWord: String): List<Word> {
        val start = chapter * 20 - 20
        var end = chapter * 20
        if(end > vocabulary.wordList.size){
            end = vocabulary.wordList.size
        }
        var list = vocabulary.wordList.subList(start, end).shuffled()
        // 如果打乱顺序的列表的第一个单词，和当前章节的最后一个词相等，就不会触发重组
        while (list[0].value == currentWord) {
            list = vocabulary.wordList.subList(start, end).shuffled()
        }
        return list
    }

    /** 进入听写模式，进入听写模式要保存好当前的状态，退出听写模式后再恢复 */
    fun hiddenInfo() {
        // 先保存状态
        typingWordStateMap["isAuto"] = isAuto
        typingWordStateMap["wordVisible"] = wordVisible
        typingWordStateMap["phoneticVisible"] = phoneticVisible
        typingWordStateMap["definitionVisible"] = definitionVisible
        typingWordStateMap["morphologyVisible"] = morphologyVisible
        typingWordStateMap["translationVisible"] = translationVisible
        typingWordStateMap["subtitlesVisible"] = subtitlesVisible
        // 再改变状态
        isAuto = true
        wordVisible = false
        phoneticVisible = false
        definitionVisible = false
        morphologyVisible = false
        translationVisible = false
        subtitlesVisible = false

    }

    /** 退出听写模式，恢复应用状态 */
    fun showInfo(clear:Boolean = true) {
        // 恢复状态
        isAuto = typingWordStateMap["isAuto"]!!
        wordVisible = typingWordStateMap["wordVisible"]!!
        phoneticVisible = typingWordStateMap["phoneticVisible"]!!
        definitionVisible = typingWordStateMap["definitionVisible"]!!
        morphologyVisible = typingWordStateMap["morphologyVisible"]!!
        translationVisible = typingWordStateMap["translationVisible"]!!
        subtitlesVisible = typingWordStateMap["subtitlesVisible"]!!

        if(clear){
            dictationWords.clear()
        }

    }


    /** 保存当前的词库 */
    fun saveCurrentVocabulary() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        runBlocking {
            launch {
                val json = encodeBuilder.encodeToString(vocabulary.serializeVocabulary)
                val file = getResourcesFile(vocabularyPath)
                file.writeText(json)
            }
        }
    }


    /** 保存记忆单词的设置信息 */
    fun saveTypingWordState() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        // 只有在正常记忆单词和复习错误单词时的状态改变才需要持久化
        if (memoryStrategy != MemoryStrategy.Dictation && memoryStrategy != MemoryStrategy.Review) {
            runBlocking {
                launch {
                    val dataWordState = DataWordState(
                        wordVisible,
                        phoneticVisible,
                        morphologyVisible,
                        definitionVisible,
                        translationVisible,
                        subtitlesVisible,
                        isPlaySoundTips,
                        soundTipsVolume,
                        pronunciation,
                        isAuto,
                        index,
                        hardVocabularyIndex,
                        vocabularyName,
                        vocabularyPath,
                        externalSubtitlesVisible,
                    )

                    val json = encodeBuilder.encodeToString(dataWordState)
                    val settings = getWordSettingsFile()
                    settings.writeText(json)
                }
            }
        }

    }
}

@Composable
fun rememberWordState():WordState = remember{
    loadWordState()
}

/** 加载应用记忆单词界面的设置信息 */
@OptIn(ExperimentalSerializationApi::class)
private fun loadWordState(): WordState {
    val typingWordSettings = getWordSettingsFile()
    return if (typingWordSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val dataWordState = decodeFormat.decodeFromString<DataWordState>(typingWordSettings.readText())
            val wordState = WordState(dataWordState)
            // 主要是为了避免再次重启是出现”找不到词库"对话框
            if(wordState.vocabulary.name.isEmpty() &&
                wordState.vocabulary.relateVideoPath.isEmpty() &&
                wordState.vocabulary.wordList.isEmpty()){
                wordState.vocabularyName = ""
                wordState.vocabularyPath = ""
                wordState.saveTypingWordState()
            }
            wordState
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$typingWordSettings")
            WordState(DataWordState())
        }

    } else {
        WordState(DataWordState())
    }
}

/** 获取记忆单词的配置文件 */
private fun getWordSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingWordSettings.json")
}

package ui.word

import androidx.compose.runtime.*
import com.formdev.flatlaf.FlatLightLaf
import data.Word
import data.loadMutableVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getResourcesFile
import state.getSettingsDirectory
import java.awt.Rectangle
import java.io.File
import javax.swing.JOptionPane

/** 记忆单词的数据类 */
@ExperimentalSerializationApi
@Serializable
data class WordScreenData(
    val wordVisible: Boolean = true,
    val phoneticVisible: Boolean = true,
    val morphologyVisible: Boolean = true,
    val definitionVisible: Boolean = true,
    val translationVisible: Boolean = true,
    val subtitlesVisible: Boolean = true,
    val sentencesVisible: Boolean = true,
    val isPlaySoundTips: Boolean = true,
    val soundTipsVolume: Float = 0.6F,
    val pronunciation: String = "us",
    val playTimes:Int = 1,
    val isAuto: Boolean = false,
    val repeatTimes: Int = 1,
    val index: Int = 0,
    val hardVocabularyIndex: Int = 0,
    val familiarVocabularyIndex: Int = 0,
    var vocabularyName: String = "",
    var vocabularyPath: String = "",
    var externalSubtitlesVisible: Boolean = true,
    var isWriteSubtitles: Boolean = true,
    var isChangeVideoBounds: Boolean = false,
    var playerLocationX: Int = 0,
    var playerLocationY: Int = 0,
    var playerWidth: Int = 1005,
    var playerHeight: Int = 502,
)

/** 记忆单词的可观察状态 */
@OptIn(ExperimentalSerializationApi::class)
class WordScreenState(wordScreenData: WordScreenData) {

    // 可持久化的状态 开始
    /**
     * 单词组件的可见性
     */
    var wordVisible by mutableStateOf(wordScreenData.wordVisible)

    /**
     * 音标组件的可见性
     */
    var phoneticVisible by mutableStateOf(wordScreenData.phoneticVisible)

    /**
     * 词型组件的可见性
     */
    var morphologyVisible by mutableStateOf(wordScreenData.morphologyVisible)

    /**
     * 定义组件的可见性
     */
    var definitionVisible by mutableStateOf(wordScreenData.definitionVisible)

    /**
     * 翻译组件的可见性
     */
    var translationVisible by mutableStateOf(wordScreenData.translationVisible)

    /**
     * 字幕组件的可见性
     */
    var subtitlesVisible by mutableStateOf(wordScreenData.subtitlesVisible)

    /**
     * 例句组件的可见性
     */
    var sentencesVisible by mutableStateOf(wordScreenData.sentencesVisible)

    /**
     * 是否播放提示音
     */
    var isPlaySoundTips by mutableStateOf(wordScreenData.isPlaySoundTips)

    /**
     * 提示音音量
     */
    var soundTipsVolume by mutableStateOf(wordScreenData.soundTipsVolume)

    /**
     * 选择发音，有英音、美音、日语
     */
    var pronunciation by mutableStateOf(wordScreenData.pronunciation)

    /**
     * 单词发音的播放次数
     */
    var playTimes by mutableStateOf(wordScreenData.playTimes)

    /**
     * 是否是自动切换
     */
    var isAuto by mutableStateOf(wordScreenData.isAuto)

    /**
     * 单词的重复次数
     */
    var repeatTimes by mutableStateOf(wordScreenData.repeatTimes)

    /**
     * 当前单词的索引，从0开始，在标题栏显示的时候 +1
     */
    var index by mutableStateOf(wordScreenData.index)

    /**
     * 困难词库的索引，从0开始，在标题栏显示的时候 +1
     */
    var hardVocabularyIndex by mutableStateOf(wordScreenData.hardVocabularyIndex)

    /**
     * 熟悉词库的索引，从0开始，在标题栏显示的时候 +1
     */
    var familiarVocabularyIndex by mutableStateOf(wordScreenData.hardVocabularyIndex)

    /**
     * 当前单词的章节，从1开始
     */
    var chapter by mutableStateOf((wordScreenData.index / 20) + 1)

    /**
     * 词库的名称
     */
    var vocabularyName by mutableStateOf(wordScreenData.vocabularyName)

    /**
     * 当前正在学习的词库的路径
     */
    var vocabularyPath by mutableStateOf(wordScreenData.vocabularyPath)

    /** 外部字幕的可见性 */
    var externalSubtitlesVisible by mutableStateOf(wordScreenData.externalSubtitlesVisible)

    /** 抄写字幕，打开后播放了某条字幕后，光标就切换到字幕，就可以抄写字幕了 */
    var isWriteSubtitles by mutableStateOf(wordScreenData.isWriteSubtitles)

    var isChangeVideoBounds by mutableStateOf(wordScreenData.isChangeVideoBounds)

    var playerLocationX by mutableStateOf(wordScreenData.playerLocationX)

    var playerLocationY by mutableStateOf(wordScreenData.playerLocationY)

    var playerWidth by mutableStateOf(wordScreenData.playerWidth)

    var playerHeight by mutableStateOf(wordScreenData.playerHeight)

    // 可持久化的状态 结束

    /** 单词输入框输入的结果*/
    val wordTypingResult =  mutableStateListOf<Pair<Char, Boolean>>()

    /** 单词输入框里的字符串*/
    var wordTextFieldValue by  mutableStateOf("")

    /** 当前单词的正确次数 */
    var wordCorrectTime by mutableStateOf(0)

    /** 当前单词的错误次数 */
    var wordWrongTime by mutableStateOf(0)

    /** 第一条字幕的输入字符串*/
    var captionsTextFieldValue1 by  mutableStateOf("")

    /** 第二条字幕的输入字符串*/
    var captionsTextFieldValue2 by  mutableStateOf("")

    /** 第三条字幕的输入字符串*/
    var captionsTextFieldValue3 by mutableStateOf("")

    /** 字幕输入框的结果 */
    val captionsTypingResultMap =
        mutableStateMapOf<Int, MutableList<Pair<Char, Boolean>>>()

    /** 当前正在学习的词库 */
    var vocabulary = loadMutableVocabulary(vocabularyPath)

    /** 记忆单词界面的记忆策略 */
    var memoryStrategy by mutableStateOf(MemoryStrategy.Normal)

    /** 要听写的单词 */
    val dictationWords = mutableStateListOf<Word>()

    /** 听写单词时的索引 */
    var dictationIndex by mutableStateOf(0)

    /** 要单独听写测试的单词 */
    val reviewWords = mutableStateListOf<Word>()

    /** 听写错误的单词 */
    val wrongWords = mutableStateListOf<Word>()

    /** 进入听写模式之前需要保存变量 `typing` 的一些状态,退出听写模式后恢复 */
    private val visibleMap = mutableStateMapOf<String, Boolean>()
    // visible
    /** 获得当前单词 */
    fun getCurrentWord(): Word {

        return when (memoryStrategy){

            MemoryStrategy.Normal -> getWord(index)

            MemoryStrategy.Dictation -> dictationWords[dictationIndex]

            MemoryStrategy.DictationTest -> reviewWords[dictationIndex]

            MemoryStrategy.NormalReviewWrong -> wrongWords[dictationIndex]

            MemoryStrategy.DictationTestReviewWrong -> wrongWords[dictationIndex]
        }

    }

    fun getVocabularyDir():File{
        return File(vocabularyPath).parentFile
    }

    /** 根据索引返回单词 */
    private fun getWord(index: Int): Word {
        val size = vocabulary.wordList.size
        return if (index in 0 until size) {
            vocabulary.wordList[index]
        } else {
            // 如果用户使用编辑器修改了索引，并且不在单词列表的范围以内，就把索引改成0。
            this.index = 0
            saveWordScreenState()
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
    fun hiddenInfo(
        dictationState: DictationState
    ) {
        // 先保存状态
        visibleMap["isAuto"] = isAuto
        visibleMap["wordVisible"] = wordVisible
        visibleMap["phoneticVisible"] = phoneticVisible
        visibleMap["definitionVisible"] = definitionVisible
        visibleMap["morphologyVisible"] = morphologyVisible
        visibleMap["translationVisible"] = translationVisible
        visibleMap["subtitlesVisible"] = subtitlesVisible
        // 再改变状态
        isAuto = true
        wordVisible = false
        phoneticVisible = dictationState.phoneticVisible
        definitionVisible = dictationState.definitionVisible
        morphologyVisible = dictationState.morphologyVisible
        translationVisible = dictationState.translationVisible
        subtitlesVisible = dictationState.subtitlesVisible

    }

    /** 退出听写模式，恢复应用状态 */
    fun showInfo(clear:Boolean = true) {
        // 恢复状态
        isAuto = visibleMap["isAuto"]!!
        wordVisible = visibleMap["wordVisible"]!!
        phoneticVisible = visibleMap["phoneticVisible"]!!
        definitionVisible = visibleMap["definitionVisible"]!!
        morphologyVisible = visibleMap["morphologyVisible"]!!
        translationVisible = visibleMap["translationVisible"]!!
        subtitlesVisible = visibleMap["subtitlesVisible"]!!

        if(clear){
            dictationWords.clear()
        }

    }

    /** 清除当前单词的状态 */
    val clearInputtedState:() -> Unit = {
        wordTypingResult.clear()
        wordTextFieldValue = ""
        captionsTypingResultMap.clear()
        captionsTextFieldValue1 = ""
        captionsTextFieldValue2 = ""
        captionsTextFieldValue3 = ""
        wordCorrectTime = 0
        wordWrongTime = 0
    }


    /** 保存当前的词库 */
    fun saveCurrentVocabulary() {

        runBlocking {
            launch (Dispatchers.IO){
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(vocabulary.serializeVocabulary)
                val file = getResourcesFile(vocabularyPath)
                file.writeText(json)
            }
        }
    }


    /** 保存记忆单词的设置信息 */
    fun saveWordScreenState() {
        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        // 只有在正常记忆单词和复习错误单词时的状态改变才需要持久化
        if (memoryStrategy != MemoryStrategy.Dictation && memoryStrategy != MemoryStrategy.DictationTest) {
            runBlocking {
                launch {
                    val wordScreenData = WordScreenData(
                        wordVisible,
                        phoneticVisible,
                        morphologyVisible,
                        definitionVisible,
                        translationVisible,
                        subtitlesVisible,
                        sentencesVisible,
                        isPlaySoundTips,
                        soundTipsVolume,
                        pronunciation,
                        playTimes,
                        isAuto,
                        repeatTimes,
                        index,
                        hardVocabularyIndex,
                        familiarVocabularyIndex,
                        vocabularyName,
                        vocabularyPath,
                        externalSubtitlesVisible,
                        isWriteSubtitles,
                        isChangeVideoBounds,
                        playerLocationX,
                        playerLocationY,
                        playerWidth,
                        playerHeight
                    )

                    val json = encodeBuilder.encodeToString(wordScreenData)
                    val settings = getWordSettingsFile()
                    settings.writeText(json)
                }
            }
        }

    }

    fun changePlayerBounds(rectangle: Rectangle){
        playerLocationX = rectangle.x
        playerLocationY = rectangle.y
        playerWidth = rectangle.width
        playerHeight = rectangle.height
        saveWordScreenState()
    }
}

@Composable
fun rememberWordState(): WordScreenState = remember{
    loadWordState()
}
@Composable
fun rememberPronunciation():String = remember{
    val wordState = loadWordState()
    wordState.pronunciation
}

/** 加载应用记忆单词界面的设置信息 */
@OptIn(ExperimentalSerializationApi::class)
private fun loadWordState(): WordScreenState {
    val wordScreenSettings = getWordSettingsFile()
    return if (wordScreenSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys = true }
            val wordScreenData = decodeFormat.decodeFromString<WordScreenData>(wordScreenSettings.readText())
            val wordScreenState = WordScreenState(wordScreenData)
            // 主要是为了避免再次重启是出现”找不到词库"对话框
            if(wordScreenState.vocabulary.name.isEmpty() &&
                wordScreenState.vocabulary.relateVideoPath.isEmpty() &&
                wordScreenState.vocabulary.wordList.isEmpty()){
                wordScreenState.vocabularyName = ""
                wordScreenState.vocabularyPath = ""
                wordScreenState.saveWordScreenState()
            }
            wordScreenState
        } catch (exception: Exception) {
            FlatLightLaf.setup()
            JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$wordScreenSettings")
            WordScreenState(WordScreenData())
        }

    } else {
        WordScreenState(WordScreenData())
    }
}

/** 获取记忆单词的配置文件 */
private fun getWordSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "TypingWordSettings.json")
}

package player

import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import data.ExternalCaption
import data.MutableVocabulary
import data.VocabularyType
import data.Word
import data.deepCopy
import data.getFamiliarVocabularyFile
import data.loadMutableVocabulary
import data.loadVocabulary
import data.saveVocabulary
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import ui.wordscreen.WordScreenState
import java.io.File
import java.time.LocalDateTime
import javax.swing.JOptionPane
import kotlinx.coroutines.*

@OptIn(ExperimentalSerializationApi::class)
class PlayerState(playerData: PlayerData) {

    /** 显示视频播放器 */
    var visible by  mutableStateOf(false)
    /** 播放器 > 视频地址 */
    var videoPath by mutableStateOf("")
    var startTime by mutableStateOf("00:00:00")
    /** 记忆单词界面调用的查看语境功能时，显示哪个字幕轨道的字幕 */
    var showContextTrackId by mutableStateOf(0)

    /** 与视频关联的词库，用于生成弹幕 */
    var vocabulary by  mutableStateOf<MutableVocabulary?>(null)
    /** 与视频关联的词库地址，用于保存词库，因为看视频时可以查看单词详情，如果觉得太简单了可以删除或加入到熟悉词库 */
    var vocabularyPath by mutableStateOf("")

    /** 记忆单词界面正在记忆的词库 */
    var wordScreenVocabulary by mutableStateOf<MutableVocabulary?>(null)
    var wordScreenVocabularyPath by mutableStateOf("")


    var showSequence by mutableStateOf(playerData.showSequence)
    var danmakuVisible by mutableStateOf(playerData.danmakuVisible)
    var autoCopy by mutableStateOf(playerData.autoCopy)
    var autoSpeak by mutableStateOf(playerData.autoSpeak)
    var preferredChinese by mutableStateOf(playerData.preferredChinese)
    var autoPause by mutableStateOf(playerData.autoPause)

    var showCaptionList by mutableStateOf(false)
    var recentList = readRecentList()
    
    /** 通知消息 */
    var notificationMessage by mutableStateOf("")
    var notificationType by mutableStateOf(NotificationType.INFO)
    var showNotification by mutableStateOf(false)
    private var notificationJob: Job? = null

    /** 从记忆单词界面调用的查看语境功能 */
    val showContext :(MediaInfo) -> Unit = { mediaInfo ->
        // 显示视频播放器窗口
        visible = true
        showCaptionList = true
        // 设置视频路径和开始时间
        videoPath = mediaInfo.mediaPath
        startTime = mediaInfo.caption.start
        if(mediaInfo.trackId != -1){
            showContextTrackId = mediaInfo.trackId
        }
    }

    /** 从工具栏打开视频播放器 */
    val showPlayer :(WordScreenState) -> Unit = { wordScreenState ->
        // 显示视频播放器窗口
        visible = true
        // 设置记忆单词界面的词库
        wordScreenVocabulary = wordScreenState.vocabulary
        wordScreenVocabularyPath = wordScreenState.vocabularyPath
    }

    /** 设置视频地址的函数，放到这里是因为记忆单词窗口可以接受拖放的视频，然后打开视频播放器 */
    val videoPathChanged:(String) -> Unit = {
        // 已经打开了一个视频再打开一个新的视频，重置与旧视频相关联的词库。
        if(videoPath.isNotEmpty() && vocabulary != null){
            vocabularyPath = ""
            vocabulary = null
        }
        videoPath = it
    }
    /** 设置词库地址的函数，放到这里是因为记忆单词可以接受拖放的视频，然后把当前词库关联到打开的视频播放器。*/
    val vocabularyPathChanged:(String) -> Unit = {
        if(videoPath.isNotEmpty()){
            vocabularyPath = it
            val newVocabulary = loadMutableVocabulary(it)
            vocabulary = newVocabulary
        }else{
            JOptionPane.showMessageDialog(null,"先打开视频，再拖放词库。")
        }
    }

    /** 在记忆单词界面拖放一个视频，当前的词库会被当成弹幕显示 */
    val openVideo:(String, String) -> Unit = { videoPath, danmakuPath ->
        // 打开视频播放器窗口
        visible = true
        // 设置视频路径和开始时间
        this.videoPath = videoPath
        startTime = "00:00:00"
        // 如果视频的路径和词库对应的视频路径不一致怎么办？
        // 这里不处理，VideoPlayer 加载弹幕的函数会处理。
        // 设置词库路径
        vocabularyPath = danmakuPath
        // 加载词库
        vocabulary = loadMutableVocabulary(danmakuPath)
    }


    fun savePlayerState() {
        runBlocking {
            launch (Dispatchers.IO){
                val playerData = PlayerData(
                    showSequence, danmakuVisible, autoCopy, autoSpeak, preferredChinese, autoPause
                )
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(playerData)
                val playerSettings = getPlayerSettingsFile()
                playerSettings.writeText(json)
            }
        }
    }




    /**
     * 读取最近播放视频的列表。
     *
     * 从存储的文件中加载最近播放的视频列表，并按时间倒序排序。
     * 如果文件不存在或解析失败，则返回一个空列表。
     *
     * @return 一个包含最近播放视频的可观察状态列表（SnapshotStateList）。
     */
    private fun readRecentList(): SnapshotStateList<RecentVideo> {
        val recentListFile = getRecentVideoFile()
        var list = if (recentListFile.exists()) {
            try {
                Json.decodeFromString<List<RecentVideo>>(recentListFile.readText())
            } catch (exception: Exception) {
                exception.printStack()
                listOf()
            }

        } else {
            listOf()
        }
        list = list.sortedByDescending { it.dateTime }
        return list.toMutableStateList()
    }

    fun updateLastPlayedTime(newTime: String) {
        runBlocking {
            launch(Dispatchers.IO) {
                if (recentList.isNotEmpty()) {
                    val firstItem = recentList.first()
                    firstItem.lastPlayedTime =newTime
                    val encodeBuilder = Json {
                        prettyPrint = true
                        encodeDefaults = true
                    }
                    val json = encodeBuilder.encodeToString(recentList.toList())
                    getRecentVideoFile().writeText(json)
                }
            }
        }
    }

    /**
     * 将视频保存到最近播放列表。
     *
     * 如果视频已存在于最近播放列表中，则将其移到列表顶部。
     * 如果列表已满（最多20个），则移除最旧的条目。
     * 保存更新后的列表到存储文件中。
     *
     * @param recentVideo 最近播放的视频条目，包含视频的名称和路径。
     */
    fun saveToRecentList(recentVideo: RecentVideo) {
        runBlocking {
            launch(Dispatchers.IO) {
                if (recentVideo.name.isNotEmpty()) {
                    val existingItem = recentList.find { it.name == recentVideo.name && it.path == recentVideo.path }
                    if (existingItem != null) {
                        recentList.remove(existingItem)
                    }
                    // 更新播放日期
                    val newItem = recentVideo.copy(dateTime = LocalDateTime.now().toString())
                    recentList.add(0, newItem)
                    if (recentList.size > 20) {
                        recentList.removeAt(20) // 保持最近列表最多20个
                    }
                    val encodeBuilder = Json {
                        prettyPrint = true
                        encodeDefaults = true
                    }

                    val json = encodeBuilder.encodeToString(recentList.toList())
                    getRecentVideoFile().writeText(json)
                }
            }
        }
    }

    fun clearRecentList() {
        runBlocking {
            launch(Dispatchers.IO) {
                recentList.clear()
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(recentList.toList())
                getRecentVideoFile().writeText(json)
            }
        }
    }

    /** 显示通知消息 */
    fun showNotification(
        message: String,
        type: NotificationType = NotificationType.INFO
    ) {
        notificationJob?.cancel()
        notificationMessage = message
        notificationType = type
        showNotification = true

        // 消息显示时间，信息类消息显示3秒，操作类消息显示1.5秒
        val delay = if(type == NotificationType.INFO) 3000L else 1500L
        notificationJob = CoroutineScope(Dispatchers.Main).launch {
            delay(delay) // 3秒后自动隐藏
            showNotification = false
        }
    }

    /**
     * 从最近播放列表中移除无效视频条目。
     *
     * 从列表中删除指定的视频条目，并将更新后的列表保存到存储文件中。
     *
     * @param invalidItem 要移除的最近播放视频条目。
     */
    fun removeRecentItem(invalidItem: RecentVideo) {
        runBlocking {
            launch (Dispatchers.IO){
                recentList.remove(invalidItem)
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                val json = encodeBuilder.encodeToString(recentList.toList())
                val recentListFile = getRecentVideoFile()
                recentListFile.writeText(json)
            }
        }
    }



    /** 删除单词 */
    val deleteWord: (Word) -> Unit = { word ->

        // 先从视频词库中删除单词
        vocabulary!!.wordList.remove(word)
        vocabulary!!.size = vocabulary!!.wordList.size
        // 再从记忆单词界面的词库中删除单词
        wordScreenVocabulary?.let {
            wordScreenVocabulary!!.wordList.remove(word)
            wordScreenVocabulary!!.size = wordScreenVocabulary!!.wordList.size
        }
        // 最后保存词库
        try{

           if(wordScreenVocabularyPath.isNotEmpty()){
               saveVocabulary(vocabulary!!.serializeVocabulary,vocabularyPath)
               wordScreenVocabulary?.let {
                   saveVocabulary(wordScreenVocabulary!!.serializeVocabulary,wordScreenVocabularyPath)
                   showNotification("已删除单词: ${word.value}")
               }
           }else{
                showNotification("记忆单词界面还没有设置词库，无法保存。")
           }


        }catch (e:Exception){
            // 回滚
            vocabulary!!.wordList.add(word)
            vocabulary!!.size = vocabulary!!.wordList.size
            wordScreenVocabulary?.let {
                wordScreenVocabulary!!.wordList.add(word)
                wordScreenVocabulary!!.size = wordScreenVocabulary!!.wordList.size
            }
            e.printStackTrace()
            showNotification("删除单词失败: ${word.value}\n错误信息:${e.message}")
        }
    }

    /**
     * 将单词添加到当前正在记忆的词库。
     *
     * 根据当前词库的类型和状态，执行以下操作：
     * 1. 如果当前词库为空，初始化词库的基础信息并添加单词。
     * 2. 如果当前词库是熟悉词库或困难词库，不允许添加单词。
     * 3. 如果当前词库是视频词库或字幕词库：
     *    - 如果词库对应的视频路径与当前播放的视频路径不一致，将词库转换为文档类型并添加单词。
     *    - 如果路径一致，直接添加单词。
     * 4. 如果当前词库是文档类型，将单词的内部字幕转换为外部字幕后添加。
     *
     * 添加完成后，会更新词库的大小并尝试保存词库到磁盘。
     * 如果保存失败，会回滚操作并提示错误信息。
     *
     * @param word 要添加的单词。
     */
    fun addWord(word : Word) {

        if(wordScreenVocabularyPath.isEmpty()){
            // 先这样写，后面再优化
            showNotification("添加失败，\n要先在记忆单词界面选择一个词库，\n否则无法使用这个功能。\n")
            return
        }

        val newWord = word.deepCopy()
        if(wordScreenVocabulary != null && wordScreenVocabularyPath.isNotEmpty()){
            // wordScreenVocabulary 正常情况下不会为 NULL，最少也是一个空的词库，而且没有持久化到磁盘。
            if(wordScreenVocabulary?.wordList?.isEmpty() == true){
                // 记忆单词界面打开了，但是词库是空的，什么有效信息都没有，需用重置词库的基础信息
                // 这里还有一个问题 wordScreenVocabularyPath 还未设置，所以这段代码不会被执行
                wordScreenVocabulary!!.wordList.add(newWord)
                wordScreenVocabulary!!.name = File(videoPath).name
                wordScreenVocabulary!!.type == VocabularyType.SUBTITLES
                wordScreenVocabulary!!.relateVideoPath = videoPath
                wordScreenVocabulary!!.subtitlesTrackId = -1
                wordScreenVocabulary!!.size = wordScreenVocabulary!!.wordList.size

            }
            if( wordScreenVocabulary!!.wordList.isNotEmpty()){

                // 记忆单词界面打开的词库是熟悉词库或困难词库，不添加
                if(wordScreenVocabulary!!.name == "FamiliarVocabulary" ||
                    wordScreenVocabulary!!.name == "HardVocabulary"){
                    showNotification("单词记忆界面的词库是 ${wordScreenVocabulary!!.name}, 无法添加单词。")
                    return
                }

                // 如果正在记忆的是视频词库或字幕词库
                if(wordScreenVocabulary!!.type == VocabularyType.MKV ||
                    wordScreenVocabulary!!.type == VocabularyType.SUBTITLES){
                    // 判断这个词库对应的视频路径和当前播放的视频路径是否一致
                    // 如果不一致，把词库转换成 DOCUMENT 类型，因为这个词库不再对应一个视频了，而是多个视频。
                    if(wordScreenVocabulary!!.relateVideoPath != videoPath){
                        wordScreenVocabulary!!.type = VocabularyType.DOCUMENT
                        wordScreenVocabulary!!.relateVideoPath = ""
                        wordScreenVocabulary!!.subtitlesTrackId = -1
                        wordScreenVocabulary!!.wordList.forEach { word ->
                            // 把内部字幕转换成外部字幕
                            word.captions.forEach { caption ->
                                val externalCaption = ExternalCaption(
                                    relateVideoPath = wordScreenVocabulary!!.relateVideoPath,
                                    subtitlesTrackId = -1,
                                    subtitlesName = wordScreenVocabulary!!.name,
                                    start = caption.start,
                                    end = caption.end,
                                    content = caption.content
                                )
                                word.externalCaptions.add(externalCaption)
                            }
                            word.captions.clear()
                        }
                        // 把内置字幕转换成外部字幕
                        newWord.captions.forEach { caption ->
                            val externalCaption = ExternalCaption(
                                relateVideoPath = videoPath,
                                subtitlesTrackId = -1,
                                subtitlesName = wordScreenVocabulary!!.name,// 视频的文件名
                                start = caption.start,
                                end = caption.end,
                                content = caption.content
                            )
                            newWord.externalCaptions.add(externalCaption)
                        }
                        newWord.captions.clear()

                    }

                }else{
                    // 正在记忆的词库是 DOCUMENT 词库，需要把单词的内部字幕转换成外部字幕
                    newWord.captions.forEach { caption ->
                        val externalCaption = ExternalCaption(
                            relateVideoPath = videoPath,
                            subtitlesTrackId = -1,
                            subtitlesName = wordScreenVocabulary!!.name,// 视频的文件名
                            start = caption.start,
                            end = caption.end,
                            content = caption.content
                        )
                        newWord.externalCaptions.add(externalCaption)
                    }
                    newWord.captions.clear()
                }

                if (!wordScreenVocabulary!!.wordList.contains(newWord)) {
                    wordScreenVocabulary!!.wordList.add(newWord)
                }else{
                    showNotification("单词: ${newWord.value} 已经存在于词库:\n${wordScreenVocabulary!!.name} 中。")
                    return
                }

                // 更新词库大小
                wordScreenVocabulary!!.size = wordScreenVocabulary!!.wordList.size

                // 保存词库
                try{
                    saveVocabulary(wordScreenVocabulary!!.serializeVocabulary,wordScreenVocabularyPath)
                    showNotification("已添加单词: ${newWord.value} 到正在记忆的词库")
                }catch (e:Exception){
                    // 回滚
                    wordScreenVocabulary!!.wordList.remove(newWord)
                    wordScreenVocabulary!!.size = wordScreenVocabulary!!.wordList.size
                    e.printStackTrace()
                    showNotification("添加单词失败: ${newWord.value}\n错误信息:${e.message}")
                }
            }

        }

    }

    /** 把单词加入到熟悉词库 */
    val addToFamiliar: (Word) -> Unit = addToFamiliar@{ word ->
        if(vocabulary == null){
            // 先这样写，后面再优化
            showNotification("添加失败，\n要先在记忆单词界面选择一个词库，\n否则无法使用这个功能。\n")
            return@addToFamiliar
        }

        val familiarWord = word.deepCopy()
        val file = getFamiliarVocabularyFile()
        val familiar = loadVocabulary(file.absolutePath)
        // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
        familiarWord.captions.forEach { caption ->
            val externalCaption = ExternalCaption(
                relateVideoPath = vocabulary!!.relateVideoPath,
                subtitlesTrackId = vocabulary!!.subtitlesTrackId,
                subtitlesName = vocabulary!!.name,
                start = caption.start,
                end = caption.end,
                content = caption.content
            )
            println("转换内置字幕为外部字幕: $externalCaption")
            familiarWord.externalCaptions.add(externalCaption)
        }
        familiarWord.captions.clear()


        if (!familiar.wordList.contains(familiarWord)) {
            familiar.wordList.add(familiarWord)
            familiar.size = familiar.wordList.size

            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }

            try{
                saveVocabulary(familiar, file.absolutePath)
                vocabulary?.let{
                    deleteWord(word)
                }

                showNotification("已添加到熟悉词库: ${word.value}")
            }catch (e:Exception){
                // 回滚
                familiar.wordList.remove(familiarWord)
                familiar.size = familiar.wordList.size
                e.printStackTrace()
                showNotification("保存到熟悉词库失败: ${word.value}")
            }
        }else{
            showNotification("熟悉词库已经存在单词: ${familiarWord.value}")
        }


    }

}

private fun getPlayerSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "/VideoPlayer/PlayerSettings.json")
}

private fun getRecentVideoFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "/VideoPlayer/RecentVideo.json")
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberPlayerState() = remember {
    val playerSettings = getPlayerSettingsFile()
    if (playerSettings.exists()) {
        try {
            val decodeFormat = Json { ignoreUnknownKeys }
            val playerData = decodeFormat.decodeFromString<PlayerData>(playerSettings.readText())
            PlayerState(playerData)
        } catch (exception: Exception) {
            exception.printStack()
            println("解析视频播放器的设置失败，将使用默认值")
            val playerState = PlayerState(PlayerData())
            playerState
        }
    } else {
        val playerState = PlayerState(PlayerData())
        playerState
    }
}

@ExperimentalSerializationApi
@Serializable
data class PlayerData(
    var showSequence: Boolean = false,
    var danmakuVisible: Boolean = false,
    var autoCopy: Boolean = false,
    var autoSpeak: Boolean = true,
    var preferredChinese: Boolean = true,
    var autoPause: Boolean = false
)

@Serializable
data class RecentVideo(
    val dateTime: String,
    val name: String,
    val path: String,
    var lastPlayedTime: String = "00:00:00"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecentVideo) return false
        return this.name == other.name && this.path == other.path
    }

    override fun hashCode(): Int {
        return name.hashCode() + path.hashCode()
    }
}

enum class NotificationType {
    INFO,
    ACTION
}
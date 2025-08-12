package player

import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import data.MutableVocabulary
import data.loadMutableVocabulary
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import java.io.File
import java.time.LocalDateTime
import javax.swing.JOptionPane

@OptIn(ExperimentalSerializationApi::class)
class PlayerState(playerData: PlayerData) {

    /** 显示视频播放器 */
    var showPlayerWindow by  mutableStateOf(false)
    /** 播放器 > 视频地址 */
    var videoPath by mutableStateOf("")
    var startTime by mutableStateOf("00:00:00")

    /** 与视频关联的词库，用于生成弹幕 */
    var vocabulary by  mutableStateOf<MutableVocabulary?>(null)
    /** 与视频关联的词库地址，用于保存词库，因为看视频时可以查看单词详情，如果觉得太简单了可以删除或加入到熟悉词库 */
    var vocabularyPath by mutableStateOf("")



    var showSequence by mutableStateOf(playerData.showSequence)
    var danmakuVisible by mutableStateOf(playerData.danmakuVisible)
    var autoCopy by mutableStateOf(playerData.autoCopy)
    var autoSpeak by mutableStateOf(playerData.autoSpeak)
    var preferredChinese by mutableStateOf(playerData.preferredChinese)
    var autoPause by mutableStateOf(playerData.autoPause)

    var showCaptionList by mutableStateOf(false)
    var recentList = readRecentList()

    val showContext :(MediaInfo) -> Unit = { mediaInfo ->
        // 显示视频播放器窗口
        showPlayerWindow = true
        // 设置视频路径和开始时间
        videoPath = mediaInfo.mediaPath
        startTime = mediaInfo.caption.start
        showCaptionList = true
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
        showPlayerWindow = true
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
        list = list.sortedByDescending { it.time }
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
//                    val newItem = RecentVideo(LocalDateTime.now().toString(), name, path)
                    val newItem = recentVideo.copy(time = LocalDateTime.now().toString())
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

}

enum class SkipMode {
    TIME,     // 时间模式
    SUBTITLE  // 字幕模式
}
private fun getPlayerSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "PlayerSettings.json")
}

private fun getRecentVideoFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "RecentVideo.json")
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
    val time: String,
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
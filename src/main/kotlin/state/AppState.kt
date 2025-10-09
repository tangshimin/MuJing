/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package state

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import com.formdev.flatlaf.FlatLightLaf
import data.RecentItem
import data.getHardVocabularyFile
import data.loadMutableVocabulary
import data.loadMutableVocabularyByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import player.isLinux
import player.isMacOS
import player.isWindows
import theme.createColors
import ui.wordscreen.MemoryStrategy
import ui.wordscreen.WordScreenState
import java.io.File
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane

/** 所有界面共享的状态 */
@ExperimentalSerializationApi
class AppState {

    /** 全局状态里需要持久化的状态 */
    var global: GlobalState = loadGlobalState()

    /** Material 颜色 */
    var colors by mutableStateOf(createColors(global))

    /** 一个后台窗口，用于放置 VLC 组件在后台解析字幕列表  */
    var videoPlayerWindow = createVideoPlayerWindow()

    /** 困难词库 */
    var hardVocabulary = loadMutableVocabularyByName("HardVocabulary")

    /** 最近生成的词库列表 */
    var recentList = readRecentList()

    /** 打开侧边栏 */
    var openSidebar by mutableStateOf(false)

    /** 打开设置*/
    var openSettings by mutableStateOf(false)

    /** 是否显示等待窗口 */
    var loadingFileChooserVisible by mutableStateOf(false)

    /** 是否显示【新建词库】窗口 */
    var newVocabulary by  mutableStateOf(false)
    /** 是否显示【编辑词库】窗口 */
    var editVocabulary by  mutableStateOf(false)

    /** 是否显示【合并词库】窗口 */
    var mergeVocabulary by mutableStateOf(false)

    /** 是否显示【过滤词库】窗口 */
    var filterVocabulary by mutableStateOf(false)

    /** 是否显示【导入词库到熟悉词库】窗口 */
    var importFamiliarVocabulary by mutableStateOf(false)

    /** 是否显示【用文档生成词库】窗口 */
    var generateVocabularyFromDocument by mutableStateOf(false)

    /** 是否显示【用字幕文件生成词库】窗口 */
    var generateVocabularyFromSubtitles by mutableStateOf(false)

    /** 是否显示【用视频生成词库】 窗口 */
    var generateVocabularyFromVideo by mutableStateOf(false)

    /** 显示软件更新对话框 */
    var showUpdateDialog by mutableStateOf(false)

    /** 软件的最新版本 */
    var latestVersion by mutableStateOf("")

    /** 版本说明 **/
    var releaseNote by mutableStateOf("")

    /** 本地缓存的单词发音列表 */
    var localAudioSet = loadAudioSet()

    var vocabularyChanged by mutableStateOf(false)

    /** 加载全局的设置信息 */
    private fun loadGlobalState(): GlobalState {
        val globalSettings = getGlobalSettingsFile()
        return if (globalSettings.exists()) {
            try {
                val decodeFormat = Json { ignoreUnknownKeys = true }
                val globalData = decodeFormat.decodeFromString<GlobalData>(globalSettings.readText())
                GlobalState(globalData)
            } catch (exception: Exception) {
                FlatLightLaf.setup()
                JOptionPane.showMessageDialog(null, "设置信息解析错误，将使用默认设置。\n地址：$globalSettings")
                GlobalState(GlobalData())
            }
        } else {
            GlobalState(GlobalData())
        }
    }


    /** 初始化视频播放窗口 */
    @OptIn(ExperimentalComposeUiApi::class)
    private fun createVideoPlayerWindow(): JFrame {
        val window = JFrame()
        window.title = "视频播放窗口"
        javaClass.getResourceAsStream("/logo/logo.png")?.use { inputStream ->
            val image = ImageIO.read(inputStream)
            window.iconImage = image
        }
        window.isUndecorated = true
        window.isAlwaysOnTop = true
        return window
    }

    /** 保存全局的设置信息 */
    fun saveGlobalState() {
        runBlocking {
            launch (Dispatchers.IO){
                val globalData = GlobalData(
                    global.type,
                    global.isDarkTheme,
                    global.isFollowSystemTheme,
                    global.audioVolume,
                    global.videoVolume,
                    global.keystrokeVolume,
                    global.isPlayKeystrokeSound,
                    global.primaryColor.value,
                    global.backgroundColor.value,
                    global.onBackgroundColor.value,
                    global.wordTextStyle,
                    global.detailTextStyle,
                    global.letterSpacing.value,
                    global.position.x.value,
                    global.position.y.value,
                    global.size.width.value,
                    global.size.height.value,
                    global.placement,
                    global.autoUpdate,
                    global.ignoreVersion,
                    global.bncNum,
                    global.frqNum,
                    global.maxSentenceLength,
                    global.showInputCount
                )
                val json = encodeBuilder.encodeToString(globalData)
                val settings = getGlobalSettingsFile()
                settings.writeText(json)
            }
        }
    }

    /** 改变词库 */
    fun changeVocabulary(
        vocabularyFile: File,
        wordScreenState: WordScreenState,
        index: Int
    ):Boolean {
        val newVocabulary = loadMutableVocabulary(vocabularyFile.absolutePath)
        if(newVocabulary.wordList.size>0){

            wordScreenState.clearInputtedState()
            if(wordScreenState.memoryStrategy == MemoryStrategy.Dictation || wordScreenState.memoryStrategy == MemoryStrategy.DictationTest){
                wordScreenState.memoryStrategy = MemoryStrategy.Normal
                wordScreenState.showInfo()
            }
            // 把困难词库和熟悉词库的索引保存在 wordScreenState.
            when (wordScreenState.vocabulary.name) {
                "HardVocabulary" -> {
                    wordScreenState.hardVocabularyIndex = wordScreenState.index
                }
                "FamiliarVocabulary" -> {
                    wordScreenState.familiarVocabularyIndex = wordScreenState.index
                }
                else -> {
                    // 保存当前词库的索引到最近列表,
                    if(wordScreenState.vocabularyPath.isNotEmpty()){
                        saveToRecentList(wordScreenState.vocabulary.name, wordScreenState.vocabularyPath,wordScreenState.index)
                    }
                }
            }

            wordScreenState.vocabulary = newVocabulary
            wordScreenState.vocabularyName = vocabularyFile.nameWithoutExtension
            wordScreenState.vocabularyPath = vocabularyFile.absolutePath
            wordScreenState.unit = (index / 20) + 1
            wordScreenState.index = index
            vocabularyChanged = true
            wordScreenState.saveWordScreenState()
            return true
        }
        return false
    }

    fun findVocabularyIndex(file:File):Int{
        var index = 0
        for (recentItem in recentList) {
            if(file.absolutePath == recentItem.path){
                index = recentItem.index
            }
        }
        return index
    }

    /** 保存困难词库 */
    fun saveHardVocabulary(){
        runBlocking {
            launch (Dispatchers.IO){
                val json = encodeBuilder.encodeToString(hardVocabulary.serializeVocabulary)
                val file = getHardVocabularyFile()
                file.writeText(json)
            }
        }
    }

    /** 读取最近生成的词库列表 */
    private fun readRecentList(): SnapshotStateList<RecentItem> {
        val recentListFile = getRecentListFile()
        var list = if (recentListFile.exists()) {
            try {
                Json.decodeFromString<List<RecentItem>>(recentListFile.readText())
            } catch (exception: Exception) {
                listOf()
            }

        } else {
            listOf()
        }
        list = list.sortedByDescending { it.time }
        return list.toMutableStateList()
    }

    private fun getRecentListFile(): File {
        val settingsDir = getSettingsDirectory()
        return File(settingsDir, "recentList.json")
    }

    fun saveToRecentList(name: String, path: String,index: Int) {
        runBlocking {
            launch (Dispatchers.IO){
                if(name.isNotEmpty()){
                    val item = RecentItem(LocalDateTime.now().toString(), name, path,index)
                    if (!recentList.contains(item)) {
                        if (recentList.size == 1000) {
                            recentList.removeAt(999)
                        }
                        recentList.add(0, item)
                    } else {
                        recentList.remove(item)
                        recentList.add(0, item)
                    }
                    val serializeList = mutableListOf<RecentItem>()
                    serializeList.addAll(recentList)

                    val json = encodeBuilder.encodeToString(serializeList)
                    val recentListFile = getRecentListFile()
                    recentListFile.writeText(json)
                }

            }
        }

    }
    fun clearRecentList() {
        runBlocking {
            launch (Dispatchers.IO){
                recentList.clear()
                val recentListFile = getRecentListFile()
                if (recentListFile.exists()) {
                    recentListFile.delete()
                }
            }
        }
    }
    fun removeRecentItem(recentItem: RecentItem) {
        runBlocking {
            launch (Dispatchers.IO){
                recentList.remove(recentItem)
                val serializeList = mutableListOf<RecentItem>()
                serializeList.addAll(recentList)
                val json = encodeBuilder.encodeToString(serializeList)
                val recentListFile = getRecentListFile()
                recentListFile.writeText(json)
            }
        }
    }

    private fun loadAudioSet(): MutableSet<String> {
        val audioDir = getAudioDirectory()
        if (!audioDir.exists()) {
            audioDir.mkdir()
        }
        val set = mutableSetOf<String>()
        audioDir.list()?.let { set.addAll(it) }
        return set
    }



    /** 搜索 */
    var searching by  mutableStateOf(false)
    /** 打开搜索 **/
    val openSearch:() -> Unit = {
        searching = true
    }

    val openLoadingDialog:() -> Unit = {
        if(isWindows()) {
            loadingFileChooserVisible = true
        }
    }

}


/** 序列化配置 */
private val encodeBuilder = Json {
    prettyPrint = true
    encodeDefaults = true
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun rememberAppState() = remember {
    AppState()
}

/**
 * 载入资源，资源在打包之前和打包之后的路径是不一样的
- 相关链接：#938 https://github.com/JetBrains/compose-jb/issues/938
- #938 的测试代码的地址
- https://github.com/JetBrains/compose-jb/blob/3070856954d4c653ea13a73aa77adb86a2788c66/gradle-plugins/compose/src/test/test-projects/application/resources/src/main/kotlin/main.kt
- 如果 System.getProperty("compose.application.resources.dir") 为 null,说明还没有打包
 */
fun composeAppResource(path: String): File {
    val property = "compose.application.resources.dir"
    val dir = System.getProperty(property)
    return if (dir != null) {
        //打包之后的环境
        File(dir).resolve(path)
    } else {// 开发环境
        // 通用资源
        var commonPath = File("resources/common/$path")
        // window 操作系统专用资源
        if (!commonPath.exists() && isWindows()) {
            commonPath = File("resources/windows/$path")
        }
        // macOS 操作系统专用资源
        if (!commonPath.exists() && isMacOS()) {
            val arch = System.getProperty("os.arch").lowercase()
            commonPath = if (arch == "arm" || arch == "aarch64") {
                File("resources/macos-arm64/$path")
            }else {
                File("resources/macos-x64/$path")
            }
        }
        // Linux 操作系统专用资源
        if (!commonPath.exists() && isLinux()) {
            commonPath = File("resources/linux/$path")
        }
        commonPath
    }
}

fun getAudioDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val audioDir = File(homeDir, ".MuJing/audio")
    if (!audioDir.exists()) {
        audioDir.mkdir()
    }
    return audioDir
}

/** 获取应用程序的配置文件的目录 */
fun getSettingsDirectory(): File {
    val homeDir = File(System.getProperty("user.home"))
    val applicationDir = File(homeDir, ".MuJing")
    if (!applicationDir.exists()) {
        applicationDir.mkdir()
    }
    return applicationDir
}

/** 获取全局的配置文件 */
private fun getGlobalSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "AppSettings.json")
}


/**
 * 获得资源文件
 * @param path 文件路径
 */
fun getResourcesFile(path: String): File {
    val file = if (File(path).isAbsolute) {
        File(path)
    } else {
        composeAppResource(path)
    }
    return file
}

/**
 * 从内置词库选择词库后，打开词库文件
 * @param path 词库文件路径
 * @param appState 全局状态
 * @param wordScreenState 单词界面状态
 */
@OptIn(ExperimentalSerializationApi::class)
fun openVocabularyFile(
    path: String,
    appState: AppState,
    wordScreenState: WordScreenState,
) {
    val file = File(path)
    val index = appState.findVocabularyIndex(file)
    val changed = appState.changeVocabulary(
        vocabularyFile = file,
        wordScreenState = wordScreenState,
        index = index
    )
    if (changed) {
        appState.global.type = ScreenType.WORD
        appState.saveGlobalState()
    }
}
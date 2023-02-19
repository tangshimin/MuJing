package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import data.MutableVocabulary
import data.VocabularyType
import data.getHardVocabularyFile
import data.loadMutableVocabulary
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import state.*
import ui.dialog.*
import ui.flatlaf.setupFileChooser
import ui.flatlaf.updateFlatLaf
import java.awt.Rectangle
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileSystemView
import kotlin.concurrent.schedule


// build.gradle.kts 的版本也需要更改
const val version = "v2.0.0-beta02"


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun App() {

    /** 显示视频播放器 */
    var showPlayerWindow by remember { mutableStateOf(false) }
    /** 播放器 > 视频地址 */
    var videoPath by remember{ mutableStateOf("") }
    /** 与视频关联的词库，用于生成弹幕 */
    var vocabulary by remember{ mutableStateOf<MutableVocabulary?>(null) }
    /** 与视频关联的词库地址，用于保存词库，因为看视频时可以查看单词详情，如果觉得太简单了可以删除或加入到熟悉词库 */
    var vocabularyPath by remember{ mutableStateOf("") }
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
        vocabularyPath = it
        val newVocabulary = loadMutableVocabulary(it)
        vocabulary = newVocabulary
    }


    val appState = rememberAppState()

    var showMainWindow by remember { mutableStateOf(true) }
    if (showMainWindow) {
        CompositionLocalProvider(
            LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
            LocalCtrl provides rememberCtrl(),
            LocalTextSelectionColors provides textSelectionColors()
        ) {
            val audioPlayerComponent = LocalAudioPlayerComponent.current

            val close: () -> Unit = {
                showMainWindow = false
                audioPlayerComponent.mediaPlayer().release()
                appState.videoPlayerComponent.mediaPlayer().release()
            }

            val windowState = rememberWindowState(
                position = appState.global.position,
                placement = appState.global.placement,
                size = appState.global.size,
            )

            var title by remember{ mutableStateOf("") }
            Window(
                title = title,
                icon = painterResource("logo/logo.png"),
                state = windowState,
                onCloseRequest = {close() },
            ) {
                MaterialTheme(colors = appState.colors) {
                    appState.global.wordFontSize = computeFontSize(appState.global.wordTextStyle)
                    appState.global.detailFontSize = computeFontSize(appState.global.detailTextStyle)
                    val wordState = rememberWordState()
                    WindowMenuBar(
                        appState = appState,
                        typingState = wordState,
                        close = {close()}
                    )
                    MenuDialogs(appState)
                    if(appState.searching){
                        Search(appState = appState,typingWordState = wordState)
                    }
                    when (appState.global.type) {
                        TypingType.WORD -> {
                            title = computeTitle(wordState.vocabularyName,wordState.vocabulary.wordList.isNotEmpty())

                            // 显示器缩放
                            val density = LocalDensity.current.density
                            // 视频播放器的位置，大小
                            val videoBounds = computeVideoBounds(windowState, appState.openSettings,density)

                            val resetVideoBounds :() -> Rectangle ={
                                appState.isChangeVideoBounds = false
                                computeVideoBounds(windowState, appState.openSettings,density)
                            }

                            TypingWord(
                                window = window,
                                title = title,
                                appState = appState,
                                typingWord = wordState,
                                videoBounds = videoBounds,
                                resetVideoBounds = resetVideoBounds,
                                showPlayer = { showPlayerWindow = it },
                                setVideoPath = videoPathChanged,
                                vocabularyPathChanged = vocabularyPathChanged
                            )
                        }
                        TypingType.SUBTITLES -> {
                            val subtitlesState = rememberSubtitlesState()
                            title = computeTitle(subtitlesState)
                            TypingSubtitles(
                                subtitlesState = subtitlesState,
                                globalState = appState.global,
                                saveSubtitlesState = { subtitlesState.saveTypingSubtitlesState() },
                                saveGlobalState = { appState.saveGlobalState() },
                                isOpenSettings = appState.openSettings,
                                setIsOpenSettings = { appState.openSettings = it },
                                window = window,
                                title = title,
                                playerWindow = appState.videoPlayerWindow,
                                videoVolume = appState.global.videoVolume,
                                mediaPlayerComponent = appState.videoPlayerComponent,
                                futureFileChooser = appState.futureFileChooser,
                                openLoadingDialog = { appState.openLoadingDialog()},
                                closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                openSearch = {appState.openSearch()},
                                showPlayer = { showPlayerWindow = it },
                            )
                        }

                        TypingType.TEXT -> {
                            val textState = rememberTextState()
                            title = computeTitle(textState)
                            TypingText(
                                title = title,
                                window = window,
                                globalState = appState.global,
                                saveGlobalState = { appState.saveGlobalState() },
                                textState = textState,
                                saveTextState = { textState.saveTypingTextState() },
                                isOpenSettings = appState.openSettings,
                                setIsOpenSettings = {appState.openSettings = it},
                                futureFileChooser = appState.futureFileChooser,
                                openLoadingDialog = { appState.openLoadingDialog()},
                                closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                openSearch = {appState.openSearch()},
                                showVideoPlayer = { showPlayerWindow = it },
                                setVideoPath = videoPathChanged,
                            )
                        }
                    }
                }

                //移动，或改变窗口后保存状态到磁盘
                LaunchedEffect(windowState) {
                    snapshotFlow { windowState.size }
                        .onEach{onWindowResize(windowState.size,appState)}
                        .launchIn(this)

                    snapshotFlow { windowState.placement }
                        .onEach {  onWindowPlacement(windowState.placement,appState)}
                        .launchIn(this)

                    snapshotFlow { windowState.position }
                        .onEach { onWindowRelocate(windowState.position,appState) }
                        .launchIn(this)
                }

                /** 启动应用后，自动检查更新 */
                LaunchedEffect(Unit){
                    if( appState.global.autoUpdate){
                        Timer("update",false).schedule(5000){
                            val result = autoDetectingUpdates(version)
                            if(result.first && result.second != appState.global.ignoreVersion){
                                appState.showUpdateDialog = true
                                appState.latestVersion = result.second
                                appState.releaseNote = result.third
                            }
                        }
                    }
                }
            }

        }
    }


    if(showPlayerWindow){
        CompositionLocalProvider(
            LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
            LocalCtrl provides rememberCtrl(),
            LocalTextSelectionColors provides textSelectionColors()
        ) {
            MaterialTheme(colors = appState.colors) {
                val closePlayerWindow:() -> Unit = {
                    showPlayerWindow = false
                    videoPath = ""
                    vocabularyPath = ""
                    vocabulary = null
                }
                val pronunciation = rememberPronunciation()
                Player(
                    close = {closePlayerWindow()},
                    videoPath = videoPath,
                    videoPathChanged = videoPathChanged,
                    vocabulary = vocabulary,
                    vocabularyPath = vocabularyPath,
                    vocabularyPathChanged = vocabularyPathChanged,
                    audioSet = appState.localAudioSet,
                    pronunciation = pronunciation,
                    audioVolume = appState.global.audioVolume,
                    videoVolume = appState.global.videoVolume,
                    videoVolumeChanged = {
                        appState.global.videoVolume = it
                        appState.saveGlobalState()
                    },
                    futureFileChooser = appState.futureFileChooser,
                )

            }

        }
    }

    // 改变主题后，更新菜单栏、标题栏的样式
    LaunchedEffect(appState.global.isDarkTheme){
        updateFlatLaf(appState.global.isDarkTheme)
        appState.futureFileChooser = setupFileChooser()
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun onWindowResize(size: DpSize, state: AppState) {
    state.global.size = size
    state.saveGlobalState()
}

@OptIn(ExperimentalSerializationApi::class)
private fun onWindowRelocate(position: WindowPosition, state: AppState) {
    state.global.position = position as WindowPosition.Absolute
    state.saveGlobalState()
}

@OptIn(ExperimentalSerializationApi::class)
private fun onWindowPlacement(placement: WindowPlacement, state: AppState){
    state.global.placement = placement
    state.saveGlobalState()
}


private fun computeTitle(
    name:String,
    isNotEmpty:Boolean
) :String{
    return if (isNotEmpty) {
        when (name) {
            "FamiliarVocabulary" -> {
                "熟悉词库"
            }
            "HardVocabulary" -> {
                "困难词库"
            }
            else -> name
        }
    } else {
        "请选择词库"
    }
}
private fun computeTitle(subtitlesState:SubtitlesState) :String{
    val mediaPath = subtitlesState.mediaPath
    return if(mediaPath.isNotEmpty()){
        try{
            val fileName = File(mediaPath).nameWithoutExtension
            fileName + " - " + subtitlesState.trackDescription
        }catch (exception:Exception){
            "字幕浏览器"
        }

    }else{
        "字幕浏览器"
    }
}

private fun computeTitle(textState: TextState) :String{
    val textPath = textState.textPath
    return if(textPath.isNotEmpty()){
        try{
            val fileName = File(textPath).nameWithoutExtension
            fileName
        }catch (exception :Exception){
            "抄写文本"
        }

    }else {
        "抄写文本"
    }
}

/**
 * 菜单栏
 */
@OptIn(ExperimentalSerializationApi::class)
@Composable
private fun FrameWindowScope.WindowMenuBar(
    appState: AppState,
    typingState:WordState,
    close: () -> Unit,
) = MenuBar {
    Menu("词库(V)", mnemonic = 'V') {
        var showFilePicker by remember {mutableStateOf(false)}
        Item("打开词库(O)", mnemonic = 'O') {

            if (isWindows()) {
                showFilePicker = true
            } else if (isMacOS()) {
                Thread {
                    val fileChooser = appState.futureFileChooser.get()
                    fileChooser.dialogTitle = "选择词库"
                    fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    fileChooser.selectedFile = null
                    if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                        val file = fileChooser.selectedFile
                        val index = appState.findVocabularyIndex(file)
                        appState.changeVocabulary(
                            vocabularyFile = file,
                            typingState,
                            index
                        )
                        appState.global.type = TypingType.WORD
                        appState.saveGlobalState()
                    }
                }.start()
            }

        }
        FilePicker(
            show = showFilePicker,
            fileExtension = "json",
            initialDirectory = ""){path ->
            if(!path.isNullOrEmpty()){
                val file = File(path)
                val index = appState.findVocabularyIndex(file)
                appState.changeVocabulary(
                    vocabularyFile = file,
                    typingState,
                    index
                )
                appState.global.type = TypingType.WORD
                appState.saveGlobalState()
            }
            showFilePicker = false
        }
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        Item("选择内置词库(B)", mnemonic = 'B', onClick = {showBuiltInVocabulary = true})
        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            futureFileChooser = appState.futureFileChooser
        )
        Item("困难词库(K)", enabled = appState.hardVocabulary.wordList.isNotEmpty(), mnemonic = 'K',onClick = {
            val file = getHardVocabularyFile()
            appState.changeVocabulary(file, typingState,typingState.hardVocabularyIndex)
            appState.global.type = TypingType.WORD
            appState.saveGlobalState()
        })

        Menu("打开最近词库(R)",enabled = appState.recentList.isNotEmpty(), mnemonic = 'R') {
            for (i in 0 until appState.recentList.size){
                val recentItem = appState.recentList.getOrNull(i)
                if(recentItem!= null){
                    Item(text = recentItem.name, onClick = {
                        val recentFile = File(recentItem.path)
                        if (recentFile.exists()) {
                            appState.changeVocabulary(recentFile,typingState, recentItem.index)
                            appState.global.type = TypingType.WORD
                            appState.saveGlobalState()
                            appState.loadingFileChooserVisible = false
                        } else {
                            appState.removeRecentItem(recentItem)
                            JOptionPane.showMessageDialog(null, "文件地址错误：\n${recentItem.path}")
                        }

                    })

                }
            }
        }

        Separator()
        Item("合并词库(M)", mnemonic = 'M', onClick = {
            appState.mergeVocabulary = true
        })
        Item("过滤词库(F)", mnemonic = 'F', onClick = {
            appState.filterVocabulary = true
        })
        Item("导入词库到熟悉词库(I)", mnemonic = 'F', onClick = {
            appState.importFamiliarVocabulary = true
        })
        Separator()
        var showWordFrequency by remember { mutableStateOf(false) }
        Item("根据词频生成词库(C)", mnemonic = 'C', onClick = {showWordFrequency = true })
        if(showWordFrequency){
            WordFrequencyDialog(
                futureFileChooser = appState.futureFileChooser,
                saveToRecentList = { name, path ->
                    appState.saveToRecentList(name, path,0)
                },
                close = {showWordFrequency = false}
            )
        }
        Item("用文档生成词库(D)", mnemonic = 'D', onClick = {
            appState.generateVocabularyFromDocument = true
        })
        Item("用字幕生成词库(Z)", mnemonic = 'Z', onClick = {
            appState.generateVocabularyFromSubtitles = true
        })
        Item("用 MKV 视频生成词库(V)", mnemonic = 'V', onClick = {
            appState.generateVocabularyFromMKV = true
        })
        Separator()
        var showSettingsDialog by remember { mutableStateOf(false) }
        Item("设置(S)", mnemonic = 'S', onClick = { showSettingsDialog = true })
        if(showSettingsDialog){
            SettingsDialog(
                close = {showSettingsDialog = false},
                state = appState,
                typingWordState = typingState
            )
        }
        if(isWindows()){
            Separator()
            Item("退出(X)", mnemonic = 'X', onClick = { close() })
        }

    }
    Menu("字幕(S)", mnemonic = 'S') {
        val enableTypingSubtitles = (appState.global.type != TypingType.SUBTITLES)
        Item(
            "字幕浏览器(T)", mnemonic = 'T',
            enabled = enableTypingSubtitles,
            onClick = {
                appState.global.type = TypingType.SUBTITLES
                appState.saveGlobalState()
            },
        )
        var showLinkVocabulary by remember { mutableStateOf(false) }
        if (showLinkVocabulary) {
            LinkVocabularyDialog(
                state = appState,
                close = {
                    showLinkVocabulary = false
                }
            )
        }

        Item(
            "链接字幕词库(L)", mnemonic = 'L',
            onClick = { showLinkVocabulary = true },
        )
        var showLyricDialog by remember{ mutableStateOf(false) }
        if(showLyricDialog){
            LyricToSubtitlesDialog(
                close = {showLyricDialog = false},
                futureFileChooser = appState.futureFileChooser,
                openLoadingDialog = {appState.loadingFileChooserVisible = true},
                closeLoadingDialog = {appState.loadingFileChooserVisible = false}
            )
        }
        Item(
            "歌词转字幕(C)",mnemonic = 'C',
            enabled = true,
            onClick = {showLyricDialog = true}
        )
    }
    Menu("文本(T)", mnemonic = 'T') {
        val enable = appState.global.type != TypingType.TEXT
        Item(
            "抄写文本(T)", mnemonic = 'T',
            enabled = enable,
            onClick = {
                appState.global.type = TypingType.TEXT
                appState.saveGlobalState()
            },
        )
        var showTextFormatDialog by remember { mutableStateOf(false) }
        if(showTextFormatDialog){
            TextFormatDialog(
                close = {showTextFormatDialog = false},
                futureFileChooser= appState.futureFileChooser,
                openLoadingDialog = {appState.loadingFileChooserVisible = true},
                closeLoadingDialog = {appState.loadingFileChooserVisible = false},
            )
        }
        Item(
            "文本格式化(F)", mnemonic = 'F',
            onClick = { showTextFormatDialog = true },
        )
    }
    Menu("帮助(H)", mnemonic = 'H') {
        var documentDialogVisible by remember { mutableStateOf(false) }
        Item("文档D)", mnemonic = 'D', onClick = { documentDialogVisible = true})
        if(documentDialogVisible){
            DocumentDialog(
                close = {documentDialogVisible = false}
            )
        }
        var shortcutKeyDialogVisible by remember { mutableStateOf(false) }
        Item("快捷键(K)", mnemonic = 'K', onClick = {shortcutKeyDialogVisible = true})
        if(shortcutKeyDialogVisible){
            ShortcutKeyDialog(close ={shortcutKeyDialogVisible = false} )
        }
        var directoryDialogVisible by remember { mutableStateOf(false) }
        Item("特殊文件夹(F)",mnemonic = 'F', onClick = {directoryDialogVisible = true})
        if(directoryDialogVisible){
            SpecialDirectoryDialog(close ={directoryDialogVisible = false})
        }
        var donateDialogVisible by remember { mutableStateOf(false) }
        Item("捐赠", onClick = { donateDialogVisible = true })
        if(donateDialogVisible){
            DonateDialog (
                close = {donateDialogVisible = false}
            )
        }
        Item("检查更新(U)", mnemonic = 'U', onClick = {
            appState.showUpdateDialog = true
            appState.latestVersion = ""
        })
        var aboutDialogVisible by remember { mutableStateOf(false) }
        Item("关于(A)", mnemonic = 'A', onClick = { aboutDialogVisible = true })
        if (aboutDialogVisible) {
            AboutDialog(
                version = version,
                close = { aboutDialogVisible = false }
            )
        }

    }
}

/**
 * 工具栏
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
fun Toolbar(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier,
    globalState: GlobalState,
    saveGlobalState:() -> Unit,
    showPlayer :(Boolean) -> Unit
) {

    Row (modifier = modifier.padding(top = if (isMacOS()) 30.dp else 0.dp)){
        val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
        val scope = rememberCoroutineScope()
        Settings(
            isOpen = isOpen,
            setIsOpen = setIsOpen,
            modifier = Modifier
        )

        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "记忆单词", modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 50,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.BottomCenter,
                alignment = Alignment.BottomCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(onClick = {
                scope.launch {
                    globalState.type = TypingType.WORD
                    saveGlobalState()
                }
            }) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (globalState.type == TypingType.WORD) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

        }

        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "字幕浏览器", modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 50,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.BottomCenter,
                alignment = Alignment.BottomCenter,
                offset = DpOffset.Zero
            )
        ) {

            IconButton(onClick = {
                scope.launch {
                    globalState.type = TypingType.SUBTITLES
                    saveGlobalState()
                }
            }) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == TypingType.SUBTITLES) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }
        }





        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "抄写文本", modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 50,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.BottomCenter,
                alignment = Alignment.BottomCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(onClick = {
                scope.launch {
                    globalState.type = TypingType.TEXT
                    saveGlobalState()
                }
            }) {
                Icon(
                    Icons.Filled.Title,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == TypingType.TEXT) MaterialTheme.colors.primary else tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

        }



        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "视频播放器", modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 50,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.BottomCenter,
                alignment = Alignment.BottomCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(onClick = {showPlayer(true)}) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

        }
    }
}
/**
 * 设置
 */
@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun Settings(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(if (isOpen) 217.dp else 48.dp)
                    .shadow(
                        elevation = 0.dp,
                        shape = if (isOpen) RectangleShape else RoundedCornerShape(50)
                    )
                    .background(MaterialTheme.colors.background)
                    .clickable { setIsOpen(!isOpen) }) {

                TooltipArea(
                    tooltip = {
                        Surface(
                            elevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            shape = RectangleShape
                        ) {
                            val ctrl = LocalCtrl.current
                            Text(text = "设置 $ctrl+1", modifier = Modifier.padding(10.dp))
                        }
                    },
                    delayMillis = 100,
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,
                        alignment = Alignment.BottomCenter,
                        offset = DpOffset(30.dp,0.dp)
                    )
                ) {
                    val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                    Icon(
                        if (isOpen) Icons.Filled.ArrowBack else Icons.Filled.Tune,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.clickable { setIsOpen(!isOpen) }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )

                }

                if (isOpen) {
                    Divider(Modifier.height(48.dp).width(1.dp))
                }
            }
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
        }
    }
}

val LocalCtrl = staticCompositionLocalOf<String> {
    error("LocalCtrl isn't provided")
}

/** 本地的 Ctrl 键 */
@Composable
fun rememberCtrl(): String = remember {
    if (isMacOS()) "⌃" else "Ctrl"
}

/** 选择字符时的背景颜色 */
fun textSelectionColors(): TextSelectionColors {
    val defaultSelectionColor = Color(0xFF4286F4)
    val backgroundColor = defaultSelectionColor.copy(alpha = 0.4f)
    return TextSelectionColors(handleColor = defaultSelectionColor, backgroundColor = backgroundColor)
}

/**
 * 对话框
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalSerializationApi::class)
@ExperimentalComposeUiApi
@Composable
fun MenuDialogs(state: AppState) {

    if (state.loadingFileChooserVisible) {
        LoadingDialog()
    }
    if (state.mergeVocabulary) {
        MergeVocabularyDialog(
            futureFileChooser = state.futureFileChooser,
            saveToRecentList = { name, path ->
                state.saveToRecentList(name, path,0)
            },
            close = { state.mergeVocabulary = false })
    }
    if (state.filterVocabulary) {
        GenerateVocabularyDialog(
            state = state,
            title = "过滤词库",
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.importFamiliarVocabulary) {
        FamiliarDialog(
            futureFileChooser = state.futureFileChooser,
            close = { state.importFamiliarVocabulary = false }
        )
    }
    if (state.generateVocabularyFromDocument) {
        GenerateVocabularyDialog(
            state = state,
            title = "用文档生成词库",
            type = VocabularyType.DOCUMENT
        )
    }
    if (state.generateVocabularyFromSubtitles) {
        GenerateVocabularyDialog(
            state = state,
            title = "用字幕生成词库",
            type = VocabularyType.SUBTITLES
        )
    }

    if (state.generateVocabularyFromMKV) {
        GenerateVocabularyDialog(
            state = state,
            title = "用 MKV 视频生成词库",
            type = VocabularyType.MKV
        )
    }

    if(state.showUpdateDialog){
        UpdateDialog(
            close = {state.showUpdateDialog = false},
            version = version,
            autoUpdate = state.global.autoUpdate,
            setAutoUpdate = {
                state.global.autoUpdate = it
                state.saveGlobalState()
            },
            latestVersion = state.latestVersion,
            releaseNote = state.releaseNote,
            ignore = {
                state.global.ignoreVersion = it
                state.saveGlobalState()
            }
        )
    }
}


/**
 * 等待窗口
 */
@Composable
fun LoadingDialog() {
    Dialog(
        title = "正在加载文件选择器",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = {},
        undecorated = true,
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(300.dp, 300.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(Modifier.width(300.dp).height(300.dp)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

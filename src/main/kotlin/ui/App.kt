package ui

import theme.CustomLocalProvider
import theme.LocalCtrl
import theme.PlayerLocalProvider
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.movcontext.MuJing.BuildConfig
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import player.*
import theme.scrollbarStyle
import state.*
import theme.toAwt
import ui.dialog.*
import ui.edit.ChooseEditVocabulary
import ui.edit.EditVocabulary
import ui.edit.checkVocabulary
import ui.flatlaf.setupFileChooser
import ui.flatlaf.updateFlatLaf
import ui.search.Search
import ui.subtitlescreen.SubtitleScreen
import ui.subtitlescreen.SubtitlesState
import ui.subtitlescreen.rememberSubtitlesState
import ui.textscreen.TextScreen
import ui.textscreen.TextState
import ui.textscreen.rememberTextState
import ui.wordscreen.WordScreen
import ui.wordscreen.WordScreenState
import ui.wordscreen.rememberPronunciation
import ui.wordscreen.rememberWordState
import util.computeVideoBounds
import java.awt.Rectangle
import java.io.File
import javax.swing.JOptionPane


@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalSerializationApi::class
)
@Composable
fun App(
    appState: AppState = rememberAppState(),
    playerState: PlayerState = rememberPlayerState(),
    wordState: WordScreenState = rememberWordState(),
    subtitlesState: SubtitlesState = rememberSubtitlesState(),
    textState: TextState = rememberTextState()
) {

    var showMainWindow by remember { mutableStateOf(true) }
    if (showMainWindow) {
        CustomLocalProvider{
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
                onKeyEvent = { it ->
                    val isCtrlPressed = if(isMacOS()) it.isMetaPressed else  it.isCtrlPressed
                    if (isCtrlPressed && it.key == Key.Comma && it.type == KeyEventType.KeyUp) {
                        appState.openSettings = true
                        true
                    }else{
                        false // 返回 false 让 Compose 继续处理事件
                    }

                }
            ) {

                MaterialTheme(colors = appState.colors) {
                    // 和 Compose UI 有关的 LocalProvider 需要放在 MaterialTheme 里面,不然无效。
                    CompositionLocalProvider(
                        LocalScrollbarStyle provides scrollbarStyle(),
                    ){
                        appState.global.wordFontSize = computeFontSize(appState.global.wordTextStyle)
                        appState.global.detailFontSize = computeFontSize(appState.global.detailTextStyle)
                        WindowMenuBar(
                            window = window,
                            appState = appState,
                            wordScreenState = wordState,
                            close = {close()}
                        )
                        MenuDialogs(appState)
                        if(appState.searching){
                            Search(
                                appState = appState,
                                wordScreenState = wordState,
                                vocabulary = wordState.vocabulary,
                            )
                        }
                        when (appState.global.type) {
                            ScreenType.WORD -> {
                                title = computeTitle(wordState.vocabularyName,wordState.vocabulary.wordList.isNotEmpty())

                                // 显示器缩放
                                val density = LocalDensity.current.density
                                // 视频播放器的位置，大小
                                val videoBounds by remember (windowState,appState.openSidebar,density){
                                    derivedStateOf {
                                        if(wordState.isChangeVideoBounds){
                                            Rectangle(wordState.playerLocationX,wordState.playerLocationY,wordState.playerWidth,wordState.playerHeight)
                                        }else{
                                            computeVideoBounds(windowState, appState.openSidebar,density)
                                        }
                                    }
                                }

                                val resetVideoBounds :() -> Rectangle ={
                                    val bounds = computeVideoBounds(windowState, appState.openSidebar,density)
                                    wordState.isChangeVideoBounds = false
                                    appState.videoPlayerWindow.size =bounds.size
                                    appState.videoPlayerWindow.location = bounds.location
                                    appState.videoPlayerComponent.size = bounds.size
                                    videoBounds.location = bounds.location
                                    videoBounds.size = bounds.size
                                    wordState.changePlayerBounds(bounds)
                                    bounds
                                }
                                WordScreen(
                                    window = window,
                                    title = title,
                                    appState = appState,
                                    wordScreenState = wordState,
                                    videoBounds = videoBounds,
                                    resetVideoBounds = resetVideoBounds,
                                    showPlayer = { playerState.showPlayerWindow = it },
                                    setVideoPath = playerState.videoPathChanged,
                                    setVideoVocabulary = playerState.vocabularyPathChanged
                                )
                            }
                            ScreenType.SUBTITLES -> {
                                title = computeTitle(subtitlesState)
                                SubtitleScreen(
                                    subtitlesState = subtitlesState,
                                    globalState = appState.global,
                                    saveSubtitlesState = { subtitlesState.saveTypingSubtitlesState() },
                                    saveGlobalState = { appState.saveGlobalState() },
                                    isOpenSettings = appState.openSidebar,
                                    setIsOpenSettings = { appState.openSidebar = it },
                                    window = window,
                                    title = title,
                                    playerWindow = appState.videoPlayerWindow,
                                    videoVolume = appState.global.videoVolume,
                                    mediaPlayerComponent = appState.videoPlayerComponent,
                                    futureFileChooser = appState.futureFileChooser,
                                    openLoadingDialog = { appState.openLoadingDialog()},
                                    closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                    openSearch = {appState.openSearch()},
                                    showPlayer = { playerState.showPlayerWindow = it },
                                )
                            }

                            ScreenType.TEXT -> {
                                title = computeTitle(textState)
                                TextScreen(
                                    title = title,
                                    window = window,
                                    globalState = appState.global,
                                    saveGlobalState = { appState.saveGlobalState() },
                                    textState = textState,
                                    saveTextState = { textState.saveTypingTextState() },
                                    isOpenSettings = appState.openSidebar,
                                    setIsOpenSettings = {appState.openSidebar = it},
                                    futureFileChooser = appState.futureFileChooser,
                                    openLoadingDialog = { appState.openLoadingDialog()},
                                    closeLoadingDialog = { appState.loadingFileChooserVisible = false },
                                    openSearch = {appState.openSearch()},
                                    showVideoPlayer = { playerState.showPlayerWindow = it },
                                    setVideoPath = playerState.videoPathChanged,
                                )
                            }
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
                val scope = rememberCoroutineScope()
                /** 启动应用后，自动检查更新 */
                LaunchedEffect(Unit) {
                    if (appState.global.autoUpdate) {
                        scope.launch(Dispatchers.IO) {
                            delay(5000)
                            val result = autoDetectingUpdates(BuildConfig.APP_VERSION)
                            if (result.first && result.second != appState.global.ignoreVersion) {
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

    if(playerState.showPlayerWindow){

        MaterialTheme(colors = appState.colors) {
            // 和 Compose UI 有关的 LocalProvider 需要放在 MaterialTheme 里面,不然无效。
            PlayerLocalProvider {
                val pronunciation = rememberPronunciation()
                Player(
                    playerState = playerState,
                    audioSet = appState.localAudioSet,
                    pronunciation = pronunciation,
                    audioVolume = appState.global.audioVolume,
                    videoVolume = appState.global.videoVolume,
                    videoVolumeChanged = {
                        appState.global.videoVolume = it
                        appState.saveGlobalState()
                    },
                )
            }


        }


    }

    var showEditVocabulary by remember { mutableStateOf(false) }
    var chosenPath by remember { mutableStateOf("") }
    if(appState.editVocabulary){
        ChooseEditVocabulary(
            close = {appState.editVocabulary = false},
            recentList = appState.recentList,
            removeRecentItem = {appState.removeRecentItem(it)},
            openEditVocabulary = {
                chosenPath = it
                showEditVocabulary = true
                appState.editVocabulary = false
                },
            colors = appState.colors,
        )
    }
    if (appState.newVocabulary) {
        NewVocabularyDialog(
            close = { appState.newVocabulary = false },
            setEditPath = {
                chosenPath = it
                showEditVocabulary = true
            },
            colors = appState.colors,
        )
    }
    if(showEditVocabulary){
        val valid by remember { mutableStateOf(checkVocabulary(chosenPath)) }
        if(valid){
            EditVocabulary(
                close = {showEditVocabulary = false},
                vocabularyPath = chosenPath,
                isDarkTheme = appState.global.isDarkTheme,
                updateFlatLaf = {
                    updateFlatLaf(
                        darkTheme = appState.global.isDarkTheme,
                        isFollowSystemTheme = appState.global.isFollowSystemTheme,
                        background = appState.global.backgroundColor.toAwt(),
                        onBackground = appState.global.onBackgroundColor.toAwt()
                    )
                }
            )
        }else{
            showEditVocabulary = false
        }

    }

    // 改变主题后，更新菜单栏、标题栏的样式
    LaunchedEffect(appState.global.isDarkTheme,appState.global.isFollowSystemTheme){
        updateFlatLaf(
            darkTheme = appState.global.isDarkTheme,
            isFollowSystemTheme = appState.global.isFollowSystemTheme,
            background = appState.global.backgroundColor.toAwt(),
            onBackground = appState.global.onBackgroundColor.toAwt()
        )
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
private fun computeTitle(subtitlesState: SubtitlesState) :String{
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
    window: ComposeWindow,
    appState: AppState,
    wordScreenState: WordScreenState,
    close: () -> Unit,
) = MenuBar {

    val isWindows = isWindows()
    Menu("词库${ if (isWindows) "(V)" else ""}", mnemonic = 'V') {
        var showFilePicker by remember {mutableStateOf(false)}
        Item("打开词库 ${if(isWindows) "(O)" else ""}", mnemonic = 'O') { showFilePicker = true }
        val extensions = if(isMacOS()) listOf("public.json") else listOf("json")
        FilePicker(
            show = showFilePicker,
            fileExtensions = extensions,
            initialDirectory = ""){pickFile ->
            if(pickFile != null){
                if(pickFile.path.isNotEmpty()){
                    val file = File(pickFile.path)
                    val index = appState.findVocabularyIndex(file)
                    val changed = appState.changeVocabulary(
                        vocabularyFile = file,
                        wordScreenState,
                        index
                    )
                    if(changed){
                        appState.global.type = ScreenType.WORD
                        appState.saveGlobalState()
                    }

                }
            }

            showFilePicker = false
        }
        Menu("打开最近词库${if(isWindows) "(R)" else ""}",enabled = appState.recentList.isNotEmpty(), mnemonic = 'R') {
            for (i in 0 until appState.recentList.size){
                val recentItem = appState.recentList.getOrNull(i)
                if(recentItem!= null){
                    Item(text = recentItem.name, onClick = {
                        val recentFile = File(recentItem.path)
                        if (recentFile.exists()) {
                            val changed = appState.changeVocabulary(recentFile,wordScreenState, recentItem.index)
                            if(changed){
                                appState.global.type = ScreenType.WORD
                                appState.saveGlobalState()
                            }else{
                                appState.removeRecentItem(recentItem)
                            }

                        } else {
                            appState.removeRecentItem(recentItem)
                            JOptionPane.showMessageDialog(window, "文件地址错误：\n${recentItem.path}")
                        }

                        appState.loadingFileChooserVisible = false

                    })

                }
            }
        }
        Item("新建词库${if(isWindows) "(N)" else ""}", mnemonic = 'N', onClick = {
            appState.newVocabulary = true
        })
        Item("编辑词库${if(isWindows) "(E)" else ""}", mnemonic = 'E', onClick = {
            appState.editVocabulary = true
        })
        Separator()
        var showBuiltInVocabulary by remember{mutableStateOf(false)}
        Item("选择内置词库${if(isWindows) "(B)" else ""}", mnemonic = 'B', onClick = {showBuiltInVocabulary = true})
        BuiltInVocabularyDialog(
            show = showBuiltInVocabulary,
            close = {showBuiltInVocabulary = false},
            futureFileChooser = appState.futureFileChooser
        )
        Item("熟悉词库${if(isWindows) "(F)" else ""}", mnemonic = 'F',onClick = {
            val file = getFamiliarVocabularyFile()
            if(file.exists()){
                val vocabulary =loadVocabulary(file.absolutePath)
                if(vocabulary.wordList.isEmpty()){
                    JOptionPane.showMessageDialog(window,"熟悉词库现在还没有单词")
                }else{
                    val changed = appState.changeVocabulary(file, wordScreenState,wordScreenState.familiarVocabularyIndex)
                    if(changed){
                        appState.global.type = ScreenType.WORD
                        appState.saveGlobalState()
                    }
                }

            }else{
                JOptionPane.showMessageDialog(window,"熟悉词库现在还没有单词")
            }
        })
        Item("困难词库${if(isWindows) "(K)" else ""}", enabled = appState.hardVocabulary.wordList.isNotEmpty(), mnemonic = 'K',onClick = {
            val file = getHardVocabularyFile()
            val changed = appState.changeVocabulary(file, wordScreenState,wordScreenState.hardVocabularyIndex)
            if(changed){
                appState.global.type = ScreenType.WORD
                appState.saveGlobalState()
            }

        })

        Separator()
        Item("合并词库${if(isWindows) "(M)" else ""}", mnemonic = 'M', onClick = {
            appState.mergeVocabulary = true
        })
        Item("过滤词库${if(isWindows) "(G)" else ""}", mnemonic = 'G', onClick = {
            appState.filterVocabulary = true
        })
        var matchVocabulary by remember{ mutableStateOf(false) }
        Item("匹配词库${if(isWindows) "(P)" else ""}", mnemonic = 'P', onClick = {
            matchVocabulary = true
        })
        if(matchVocabulary){
            MatchVocabularyDialog(
                futureFileChooser = appState.futureFileChooser,
                close = {matchVocabulary = false}
            )
        }

        var showLinkVocabulary by remember { mutableStateOf(false) }
        if (showLinkVocabulary) {
            LinkVocabularyDialog(
                appState = appState,
                close = {
                    showLinkVocabulary = false
                }
            )
        }

        Item(
            "链接字幕词库${if(isWindows) "(L)" else ""}", mnemonic = 'L',
            onClick = { showLinkVocabulary = true },
        )
        Item("导入词库到熟悉词库${if(isWindows) "(I)" else ""}", mnemonic = 'I', onClick = {
            appState.importFamiliarVocabulary = true
        })

        Separator()
        var showWordFrequency by remember { mutableStateOf(false) }
        Item("根据词频生成词库${if(isWindows) "(C)" else ""}", mnemonic = 'C', onClick = {showWordFrequency = true })
        if(showWordFrequency){
            WordFrequencyDialog(
                futureFileChooser = appState.futureFileChooser,
                saveToRecentList = { name, path ->
                    appState.saveToRecentList(name, path,0)
                },
                close = {showWordFrequency = false}
            )
        }
        Item("用文档生成词库${if(isWindows) "(D)" else ""}", mnemonic = 'D', onClick = {
            appState.generateVocabularyFromDocument = true
        })
        Item("用字幕生成词库${if(isWindows) "(Z)" else ""}", mnemonic = 'Z', onClick = {
            appState.generateVocabularyFromSubtitles = true
        })
        Item("用视频生成词库${if(isWindows) "(V)" else ""}", mnemonic = 'V', onClick = {
            appState.generateVocabularyFromVideo = true
        })
        Separator()
        val shortcut = if(isMacOS()) KeyShortcut(Key.Comma, meta = true) else KeyShortcut(Key.Comma, ctrl = true)
        Item("设置${if(isWindows) "(S)" else ""}", mnemonic = 'S', shortcut = shortcut, onClick = { appState.openSettings = true })
        if(appState.openSettings){
            SettingsDialog(
                close = {appState.openSettings = false},
                state = appState,
                wordScreenState = wordScreenState
            )
        }
        if(isWindows()){
            Separator()
            Item("退出${if(isWindows) "(X)" else ""}", mnemonic = 'X', onClick = { close() })
        }

    }
    Menu("字幕${if(isWindows) "(S)" else ""}", mnemonic = 'S') {
        val enableTypingSubtitles = (appState.global.type != ScreenType.SUBTITLES)
        Item(
            "字幕浏览器${if(isWindows) "(T)" else ""}", mnemonic = 'T',
            enabled = enableTypingSubtitles,
            onClick = {
                appState.global.type = ScreenType.SUBTITLES
                appState.saveGlobalState()
            },
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
            "歌词转字幕${if(isWindows) "(C)" else ""}",mnemonic = 'C',
            enabled = true,
            onClick = {showLyricDialog = true}
        )
    }
    Menu("文本${if(isWindows) "(T)" else ""}", mnemonic = 'T') {
        val enable = appState.global.type != ScreenType.TEXT
        Item(
            "抄写文本${if(isWindows) "(T)" else ""}", mnemonic = 'T',
            enabled = enable,
            onClick = {
                appState.global.type = ScreenType.TEXT
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
            "文本格式化${if(isWindows) "(F)" else ""}", mnemonic = 'F',
            onClick = { showTextFormatDialog = true },
        )
    }
    Menu("帮助${if(isWindows) "(H)" else ""}", mnemonic = 'H') {
        var documentWindowVisible by remember { mutableStateOf(false) }
        var currentPage by remember { mutableStateOf("features") }
        Item("使用手册${if(isWindows) "(D)" else ""}", mnemonic = 'D', onClick = { documentWindowVisible = true})
        if(documentWindowVisible){
            DocumentWindow(
                close = {documentWindowVisible = false},
                currentPage = currentPage,
                setCurrentPage = {currentPage = it}
            )
        }
        var shortcutKeyDialogVisible by remember { mutableStateOf(false) }
        Item("快捷键${if(isWindows) "(K)" else ""}", mnemonic = 'K', onClick = {shortcutKeyDialogVisible = true})
        if(shortcutKeyDialogVisible){
            ShortcutKeyDialog(close ={shortcutKeyDialogVisible = false} )
        }
        var directoryDialogVisible by remember { mutableStateOf(false) }
        Item("特殊文件夹${if(isWindows) "(F)" else ""}",mnemonic = 'F', onClick = {directoryDialogVisible = true})
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
        Item("检查更新${if(isWindows) "(U)" else ""}", mnemonic = 'U', onClick = {
            appState.showUpdateDialog = true
            appState.latestVersion = ""
        })
        var aboutDialogVisible by remember { mutableStateOf(false) }
        Item("关于${if(isWindows) "(A)" else ""}", mnemonic = 'A', onClick = { aboutDialogVisible = true })
        if (aboutDialogVisible) {
            AboutDialog(
                version = BuildConfig.APP_VERSION,
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
    showPlayer :(Boolean) -> Unit,
    openSearch :() -> Unit
) {

    Row (modifier = modifier.padding(top = if (isMacOS()) 30.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically){
        val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
        val scope = rememberCoroutineScope()
        SidebarButton(
            isOpen = isOpen,
            setIsOpen = setIsOpen,
            modifier = Modifier
        )
        if(!isOpen)Divider(Modifier.width(1.dp).height(20.dp))
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
            IconButton(
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.WORD
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("WordButton")
            ) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (globalState.type == ScreenType.WORD) MaterialTheme.colors.primary else tint,
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

            IconButton(
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.SUBTITLES
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("SubtitlesButton")
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == ScreenType.SUBTITLES) MaterialTheme.colors.primary else tint,
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
            IconButton(
                onClick = {
                    scope.launch {
                        globalState.type = ScreenType.TEXT
                        saveGlobalState()
                    }
                },
                modifier = Modifier.testTag("TextButton")
            ) {
                Icon(
                    Icons.Filled.Title,
                    contentDescription = "Localized description",
                    tint = if (globalState.type == ScreenType.TEXT) MaterialTheme.colors.primary else tint,
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
            IconButton(
                onClick = { showPlayer(true) },
                modifier = Modifier.testTag("PlayerButton")
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Localized description",
                    tint = tint,
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
                    Text(text = "搜索", modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 50,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.BottomCenter,
                alignment = Alignment.BottomCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(
                onClick = openSearch,
                modifier = Modifier.testTag("PlayerButton")
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Localized description",
                    tint = tint,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
            }

        }
        Divider(Modifier.width(1.dp).height(20.dp))
    }
}
/**
 * 侧边栏
 */
@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun SidebarButton(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Column(Modifier.width(IntrinsicSize.Max)) {

            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val text = if (isMacOS()) "$ctrl ⌃ S" else "$ctrl + Alt + S"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "侧边栏  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = text)
                            }
                        }
                    }
                },
                delayMillis = 100,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(5.dp,0.dp)
                )
            ) {
                if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
                val width by animateDpAsState(targetValue = if (isOpen) 217.dp else 48.dp)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag("SettingsButton")
                        .width(width)
                        .shadow(
                            elevation = 0.dp,
                            shape = if (isOpen) RectangleShape else RoundedCornerShape(50)
                        )
                        .background(MaterialTheme.colors.background)
                        .clickable { setIsOpen(!isOpen) }) {



                    val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                    Icon(
                        if (isOpen) Icons.Filled.ArrowBack else icons.DockToRight,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.clickable { setIsOpen(!isOpen) }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )
                    if (isOpen) {
                        Divider(Modifier.height(48.dp).width(1.dp))
                    }
                }
            }
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
        }
    }
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

    if (state.generateVocabularyFromVideo) {
        GenerateVocabularyDialog(
            state = state,
            title = "用视频生成词库",
            type = VocabularyType.MKV
        )
    }

    if(state.showUpdateDialog){
        UpdateDialog(
            close = {state.showUpdateDialog = false},
            version =BuildConfig.APP_VERSION,
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
    DialogWindow(
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

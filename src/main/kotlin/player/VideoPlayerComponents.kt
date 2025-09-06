package player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import icons.Autopause
import icons.AutopauseDisabled
import icons.Block
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import java.awt.Component
import java.awt.Cursor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeControl(
    videoVolume: Float,
    videoVolumeChanged: (Float) -> Unit,
    videoPlayerComponent: Component,
    audioSliderPress: Boolean,
    onAudioSliderPressChanged: (Boolean) -> Unit
) {
    // 音量
    var volumeOff by remember { mutableStateOf(false) }
    var volumeSliderVisible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { volumeSliderVisible = true }
            .onPointerEvent(PointerEventType.Exit) { if(!audioSliderPress) volumeSliderVisible = false }
    ) {
        IconButton(onClick = {
            volumeOff = !volumeOff
            if(volumeOff){
                videoPlayerComponent.mediaPlayer().audio()
                    .setVolume(0)
            }
        }) {
            Icon(
                if (volumeOff) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = "Localized description",
                tint = Color.White,
            )
        }
        AnimatedVisibility (visible = volumeSliderVisible) {
            Slider(
                value = videoVolume,
                valueRange = 1f..100f,
                onValueChange = {
                    videoVolumeChanged (it)
                    if(it > 1f){
                        volumeOff = false
                        videoPlayerComponent.mediaPlayer().audio()
                            .setVolume(videoVolume.toInt())
                    }else{
                        volumeOff = true
                        videoPlayerComponent.mediaPlayer().audio()
                            .setVolume(0)
                    }
                },
                modifier = Modifier
                    .width(100.dp)
                    .padding(end = 16.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        volumeSliderVisible = true
                    }
                    .onPointerEvent(PointerEventType.Press){ onAudioSliderPressChanged(true) }
                    .onPointerEvent(PointerEventType.Release){ onAudioSliderPressChanged(false) }
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ListButton(
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {
                Text(
                    text = "字幕列表/播放列表",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "字幕列表",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CaptionAndVideoList(
    show: Boolean,
    timedCaption: VideoPlayerTimedCaption,
    play: (Long) -> Unit,
    replayCaption:() -> Unit = {},
    previousCaption:() -> Unit = {},
    nextCaption:() -> Unit = {},
    playerState: PlayerState,
    listSelectedTab: Int,
    onListSelectedTabChanged: (Int) -> Unit,
    filePickerLauncher: PickerResultLauncher,
    directoryPickerLauncher: PickerResultLauncher
){
    AnimatedVisibility(
        visible = show,
        enter =fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        /** 字幕列表宽度状态 */
        var subtitleListWidth by remember { mutableStateOf(400.dp) }

        /** 当前选中的播放列表项索引 */
        var selectedPlaylistIndex by remember { mutableStateOf(-1) }

        Column(Modifier
            .width(subtitleListWidth)
            .background(Color(0xFF1E1E1E))
        ) {
            val focusManager = LocalFocusManager.current

            /** 显示添加视频选项菜单 */
            var showAddOptionsMenu by remember { mutableStateOf(false) }

            // Tab标签页
            TabRow(
                selectedTabIndex = listSelectedTab,
                backgroundColor = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxWidth(),
            ) {

                Tab(
                    text = { Text("字幕列表", color = if (listSelectedTab == 0) MaterialTheme.colors.primary else Color.White) },
                    selected = listSelectedTab == 0,
                    onClick = {
                        onListSelectedTabChanged(0)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.height(38.dp)
                )
                Tab(
                    text = { Text("播放列表", color = if (listSelectedTab == 1) MaterialTheme.colors.primary else Color.White) },
                    selected = listSelectedTab == 1,
                    onClick = {
                        onListSelectedTabChanged(1)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.height(38.dp)

                )
            }
            Divider()
            Row (Modifier){
                val densityValue = LocalDensity.current.density
                // 拖拽分隔条
                VerticalSplitter(
                    onResize = { delta ->
                        val newWidth = subtitleListWidth - (delta / densityValue).dp
                        // 限制最小宽度为300dp，最大宽度为800dp
                        subtitleListWidth = newWidth.coerceIn(300.dp, 800.dp)
                    }
                )
                Box{
                    // 为不同标签页创建状态
                    val captionListState = rememberLazyListState(0)
                    val playlistState = rememberLazyListState(0)
                    var lazyColumnHeight by remember{mutableStateOf(0)}

                    when (listSelectedTab) {
                        0 -> {
                            // 字幕列表内容
                            Column(
                                modifier = Modifier
                                    .width(subtitleListWidth)
                                    .fillMaxHeight()
                            ){
                                if(timedCaption.isNotEmpty()){
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onGloballyPositioned { coordinates ->
                                                lazyColumnHeight =coordinates.size.height
                                            },
                                        state = captionListState,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        itemsIndexed(timedCaption.captionList) { index, caption ->

                                            var background by remember { mutableStateOf(Color.Transparent) }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(background)
                                                    .onClick(
                                                        onClick = {
                                                            play(caption.start)
                                                            focusManager.clearFocus() // 释放焦点
                                                        }
                                                    )
                                                    .onPointerEvent(PointerEventType.Enter) {
                                                        background = Color.White.copy(alpha = 0.08f) // 柔和的悬停色

                                                    }
                                                    .onPointerEvent(PointerEventType.Exit){
                                                        background = Color.Transparent // 鼠标移出时还原
                                                    }

                                            ){
                                                SelectionContainer {
                                                    Text(
                                                        text = caption.content.removeSuffix("\n"),
                                                        style = MaterialTheme.typography.subtitle1,
                                                        color = if(index == timedCaption.currentIndex) MaterialTheme.colors.primary else  Color.LightGray,
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                                    )
                                                }

                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(timedCaption.currentIndex){
                                    // 确保当前字幕在可见范围内
                                    if(timedCaption.currentIndex >= 0 && timedCaption.currentIndex < captionListState.layoutInfo.totalItemsCount) {
                                        val firstVisibleItem = captionListState.layoutInfo.visibleItemsInfo.firstOrNull()
                                        val height = firstVisibleItem?.size ?: 0
                                        val offset = -(lazyColumnHeight/2) + height
                                        captionListState.scrollToItem(timedCaption.currentIndex,offset)
                                    }
                                }
                                LaunchedEffect(Unit){
                                    if(timedCaption.currentIndex == 0){
                                        // 初始加载时滚动到顶部
                                        captionListState.scrollToItem(0)
                                    }

                                }
                            }
                        }
                        1 -> {
                            // 播放列表内容
                            LaunchedEffect(playerState.videoPath, listSelectedTab) {
                                // 当切换到播放列表标签页且视频路径不为空时，检查是否需要加载播放列表
                                if (listSelectedTab == 1 && playerState.videoPath.isNotEmpty()) {
                                    // 只有当播放列表为空或者当前视频不在播放列表中时才重新加载
                                    val shouldReload = playerState.playlist.isEmpty() ||
                                            playerState.playlist.none { it.path == playerState.videoPath }

                                    if (shouldReload) {
                                        playerState.loadPlaylist(playerState.videoPath)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .width(subtitleListWidth)
                                    .fillMaxHeight()
                            ) {
                                if (playerState.playlist.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 60.dp), // 底部留白防止工具栏遮挡
                                        state = playlistState,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        itemsIndexed(playerState.playlist) { index, playlistItem ->
                                            val focusManager = LocalFocusManager.current
                                            var background by remember { mutableStateOf(Color.Transparent) }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (selectedPlaylistIndex == index)
                                                            Color.White.copy(alpha = 0.12f)
                                                        else
                                                            background
                                                    )
                                                    .onPointerEvent(PointerEventType.Enter) {
                                                        background = Color.White.copy(alpha = 0.08f)
                                                    }
                                                    .onPointerEvent(PointerEventType.Exit) {
                                                        background = Color.Transparent
                                                    }
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                // 单击选择
                                                                selectedPlaylistIndex = index
                                                                focusManager.clearFocus()
                                                            },
                                                            onDoubleTap = {
                                                                // 双击播放
                                                                playerState.playPlaylistItem(index)
                                                                selectedPlaylistIndex = index
                                                                focusManager.clearFocus()
                                                            }
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        // 视频名称
                                                        Text(
                                                            text = playlistItem.name,
                                                            style = MaterialTheme.typography.subtitle1,
                                                            color = if (playlistItem.isCurrentlyPlaying)
                                                                MaterialTheme.colors.primary
                                                            else Color.LightGray,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }

                                                    // 上次播放时间
                                                    if (playlistItem.lastPlayedTime != "00:00:00") {
                                                        Text(
                                                            text = "上次播放: ${playlistItem.lastPlayedTime}",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 空播放列表提示
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "播放列表为空",
                                                style = MaterialTheme.typography.body1,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "打开视频后会自动加载播放列表",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(
                            scrollState = if (listSelectedTab == 0) captionListState else playlistState
                        )
                    )
                    // 底部工具栏
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFF1E1E1E))
                            .fillMaxWidth()
                            .onPointerEvent(PointerEventType.Enter){}// 阻止鼠标穿透
                            .padding(bottom = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Divider(Modifier.padding(bottom = 10.dp))
                        if (listSelectedTab == 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()){
                                TooltipArea(
                                    tooltip = {
                                        Surface(
                                            elevation = 4.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ){
                                                Text(text = "上一条字幕 ",color = MaterialTheme.colors.onSurface)
                                                Text(text ="A",color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                            }
                                        }
                                    },
                                    delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                                    tooltipPlacement = TooltipPlacement.ComponentRect(
                                        anchor = Alignment.TopCenter,
                                        alignment = Alignment.TopCenter,
                                        offset = DpOffset.Zero
                                    )

                                ) {
                                    IconButton(onClick = previousCaption){
                                        Icon(
                                            imageVector = Icons.Filled.ArrowBackIos,
                                            contentDescription = "Previous Caption",
                                            tint = Color.White
                                        )
                                    }
                                }
                                TooltipArea(
                                    tooltip = {
                                        Surface(
                                            elevation = 4.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ){
                                                Text(text = "重复播放 ",color = MaterialTheme.colors.onSurface)
                                                Text(text ="S",color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                            }
                                        }
                                    },
                                    delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                                    tooltipPlacement = TooltipPlacement.ComponentRect(
                                        anchor = Alignment.TopCenter,
                                        alignment = Alignment.TopCenter,
                                        offset = DpOffset.Zero
                                    )

                                ) {
                                    IconButton(onClick = replayCaption){
                                        Icon(
                                            imageVector = Icons.Filled.Replay,
                                            contentDescription = "Replay Caption",
                                            tint = Color.White
                                        )
                                    }

                                }

                                TooltipArea(
                                    tooltip = {
                                        Surface(
                                            elevation = 4.dp,
                                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ){
                                                Text(text = "下一条字幕 ",color = MaterialTheme.colors.onSurface)
                                                Text(text ="D",color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                            }
                                        }
                                    },
                                    delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
                                    tooltipPlacement = TooltipPlacement.ComponentRect(
                                        anchor = Alignment.TopCenter,
                                        alignment = Alignment.TopCenter,
                                        offset = DpOffset.Zero
                                    )

                                ) {
                                    IconButton(onClick = nextCaption){
                                        Icon(
                                            imageVector = Icons.Filled.ArrowForwardIos,
                                            contentDescription = "Next Caption",
                                            tint = Color.White
                                        )
                                    }
                                }


                            }
                        }else{
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()){
                                Box {
                                    IconButton(onClick = {
                                        showAddOptionsMenu = true
                                    }){
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "添加视频",
                                            tint = Color.White
                                        )
                                    }

                                    // 添加视频选项下拉菜单
                                    DropdownMenu(
                                        expanded = showAddOptionsMenu,
                                        onDismissRequest = { showAddOptionsMenu = false }
                                    ) {
                                        DropdownMenuItem(onClick = {
                                            showAddOptionsMenu = false
                                            filePickerLauncher.launch()
                                        }) {
                                            Text("添加文件", color = MaterialTheme.colors.onSurface)
                                        }
                                        DropdownMenuItem(onClick = {
                                            showAddOptionsMenu = false
                                            directoryPickerLauncher.launch()
                                        }) {
                                            Text("添加文件夹", color = MaterialTheme.colors.onSurface)
                                        }
                                    }
                                }

                                IconButton(onClick = {
                                    // 移除选择的视频
                                    if (selectedPlaylistIndex >= 0 && selectedPlaylistIndex < playerState.playlist.size) {
                                        val removedItem = playerState.playlist[selectedPlaylistIndex]
                                        playerState.playlist.removeAt(selectedPlaylistIndex)
                                        playerState.showNotification("已移除: ${removedItem.name}")
                                        selectedPlaylistIndex = -1
                                    }
                                }){
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "移除视频",
                                        tint = Color.White
                                    )
                                }



                                OutlinedButton(onClick = {
                                    // 清空播放列表
                                    if (playerState.playlist.isNotEmpty()) {
                                        playerState.playlist.clear()
                                        selectedPlaylistIndex = -1
                                        playerState.showNotification("已清空播放列表")
                                    } else {
                                        playerState.showNotification("播放列表已为空", NotificationType.ACTION)
                                    }
                                },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color.Transparent
                                    )
                                ){
                                    Text(text = "清空", color = Color.White)
                                }


                            }
                        }

                    }

                }

            }
        }

    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationMessage(
    message: String,
    type: NotificationType = NotificationType.INFO,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colors.surface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        ) {
            if(type == NotificationType.INFO){
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭通知",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

            }else{
                // 显示刚刚执行的操作
                Box(
                    modifier = Modifier
                        .padding(20.dp),
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

        }

    }
}

@Composable
fun VerticalSplitter(
    onResize: (delta: Float) -> Unit,
){
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    onResize(delta)
                },
                startDragImmediately = true,
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenButton(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(
                        text =if (isFullscreen) "退出全屏 " else "进入全屏 ",
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(text ="F",color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                }
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (isFullscreen) "退出全屏" else "进入全屏",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
){


    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                val text = if(isPlaying) "暂停（空格）" else "播放（空格）"
                Text(
                    text = text,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        // 播放按钮
        IconButton(onClick =onClick) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Localized description",
                tint = Color.White,
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "停止播放",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconButton(onClick = onClick,enabled = enabled) {
            Icon(
                imageVector =  Icons.Filled.Stop,
                contentDescription = "停止播放",
                tint = Color.White
            )
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerSettingsButton(
    settingsExpanded: Boolean,
    onSettingsExpandedChanged: (Boolean) -> Unit,
    playerState: PlayerState,
    onKeepControlBoxVisible: () -> Unit
) {
    val settingsEnabled = isMacOS()
    if(settingsEnabled){
        Box {
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "设置",
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                },
                delayMillis = 100,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.TopCenter,
                    alignment = Alignment.TopCenter,
                    offset = DpOffset.Zero
                )
            ) {
                IconButton(onClick = { onSettingsExpandedChanged(true) }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Localized description",
                        tint = Color.White,
                    )
                }
            }

            val offsetX = if(playerState.showCaptionList) (-95).dp else 80.dp
            DropdownMenu(
                expanded = settingsExpanded,
                offset = DpOffset(x =offsetX, y = 0.dp),
                onDismissRequest = {
                    onSettingsExpandedChanged(false)
                    onKeepControlBoxVisible()
                },
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Enter) {
                        onKeepControlBoxVisible()
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        onKeepControlBoxVisible()
                    }
            ) {
                DropdownMenuItem(onClick = { }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "全屏时显示关闭按钮",
                            color =MaterialTheme.colors.onBackground,
                        )
                        Switch(checked = playerState.showClose,
                            onCheckedChange = {
                                playerState.showClose = it
                                playerState.savePlayerState()
                            })
                    }
                }
            }
        }

    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SubtitleAndAudioSelector(
    videoPath: String,
    subtitleTrackList: List<Pair<Int,String>>,
    extSubList: List<Pair<String,File>> = emptyList(),
    audioTrackList: List<Pair<Int,String>>,
    currentSubtitleTrack: Int,
    currentAudioTrack: Int,
    showSubtitleMenu: Boolean,
    onShowSubtitleMenuChanged: (Boolean) -> Unit,
    onShowSubtitlePicker: () -> Unit,
    onSubTrackChanged: (Int,String) -> Unit,
    extSubIndex: Int,
    setExternalSubtitle: (Int,File,String) -> Unit,
    onAudioTrackChanged: (Int) -> Unit,
    onKeepControlBoxVisible: () -> Unit
) {
    Box{
        // 字幕和声音轨道选择按钮
        TooltipArea(
            tooltip = {
                Surface(
                    elevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RectangleShape
                ) {
                    Text(text = "字幕和声音轨道",
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(10.dp))
                }
            },
            delayMillis = 100,
            tooltipPlacement = TooltipPlacement.ComponentRect(
                anchor = Alignment.TopCenter,
                alignment = Alignment.TopCenter,
                offset = DpOffset.Zero
            )
        ) {
            IconButton(
                onClick = { onShowSubtitleMenuChanged(!showSubtitleMenu) },
                enabled = videoPath.isNotEmpty()
            ) {
                Icon(
                    Icons.Filled.Subtitles,
                    contentDescription = "Localized description",
                    tint = if(videoPath.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }

        var height = ((subtitleTrackList.size + extSubList.size) * 40 + 100).dp
        if(height > 740.dp) height = 740.dp

        DropdownMenu(
            expanded = showSubtitleMenu,
            onDismissRequest = { onShowSubtitleMenuChanged(false) },
            modifier = Modifier.width(282.dp).height(height)
                .onPointerEvent(PointerEventType.Enter) {
                    onKeepControlBoxVisible()
                }
                .onPointerEvent(PointerEventType.Exit) {
                    onKeepControlBoxVisible()
                },
            offset = DpOffset(x = 141.dp, y = (-20).dp),
        ) {
            var state by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = state,
                backgroundColor = Color.Transparent,
                modifier = Modifier.width(282.dp).height(40.dp)
            ) {
                Tab(
                    text = { Text(text = "字幕",color = MaterialTheme.colors.onSurface) },
                    selected = state == 0,
                    onClick = { state = 0 }
                )
                Tab(
                    text = { Text("声音",color = MaterialTheme.colors.onSurface) },
                    selected = state == 1,
                    onClick = { state = 1 }
                )
            }
            when (state) {
                0 -> {
                    Column(Modifier.width(282.dp).height(height)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = onShowSubtitlePicker)
                                .width(282.dp).height(40.dp)
                        ){
                            Text(
                                text = "添加字幕",
                                color = MaterialTheme.colors.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = {
                                    onShowSubtitleMenuChanged(false)
                                    onSubTrackChanged(-1,"关闭字幕")
                                })
                                .width(282.dp).height(40.dp)
                        ){
                            val color = if(currentSubtitleTrack == -1)
                                MaterialTheme.colors.primary else Color.Transparent
                            Spacer(Modifier
                                .background(color)
                                .height(16.dp)
                                .width(2.dp)
                            )
                            Text(
                                text = "关闭字幕",
                                color = if(currentSubtitleTrack == -1)
                                    MaterialTheme.colors.primary else  MaterialTheme.colors.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Divider()
                        Box(Modifier.width(282.dp).height(height)) {
                            val scrollState = rememberLazyListState()
                            LazyColumn(Modifier.fillMaxSize(), scrollState) {
                                items(subtitleTrackList) { (trackId, description) ->

                                    if(description != "Disable" && trackId != -1){
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable(onClick = {
                                                    onShowSubtitleMenuChanged(false)
                                                    onSubTrackChanged(trackId,description)
                                                })
                                                .width(282.dp).height(40.dp)
                                        ) {
                                            val color = if(currentSubtitleTrack == trackId)
                                                MaterialTheme.colors.primary else Color.Transparent
                                            Spacer(Modifier
                                                .background(color)
                                                .height(16.dp)
                                                .width(2.dp)
                                            )
                                            Text(
                                                text = description,
                                                color = if(currentSubtitleTrack == trackId)
                                                    MaterialTheme.colors.primary else  MaterialTheme.colors.onSurface,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )


                                        }
                                    }
                                }


                                itemsIndexed(extSubList) { index,(lang,file) ->
                                    if(subtitleTrackList.isNotEmpty() && index == 0){
                                        Divider()
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable(onClick = {
                                                setExternalSubtitle(index,file,lang)
                                                onShowSubtitleMenuChanged(false)
                                            })
                                            .width(282.dp).height(40.dp)
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if(extSubIndex == index) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState = scrollState),
                            )
                        }
                    }
                }
                1 -> {
                    Box(Modifier.width(282.dp).height(height)) {
                        val scrollState = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize(), scrollState) {
                            items(audioTrackList) { (track, description) ->
                                DropdownMenuItem(
                                    onClick = {
                                        onShowSubtitleMenuChanged(false)
                                        onAudioTrackChanged(track)
                                    },
                                    modifier = Modifier.width(282.dp).height(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val color = if(currentAudioTrack == track)
                                            MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                        Spacer(Modifier
                                            .background(color)
                                            .height(16.dp)
                                            .width(2.dp)
                                        )
                                        Text(
                                            text = description,
                                            color = if(currentAudioTrack == track)
                                                MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(scrollState = scrollState),
                        )
                    }
                }
            }
        }
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DanmakuButton(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "单词弹幕",
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconToggleButton(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        ) {
            Box{
                Text(
                    text = "弹",
                    style = MaterialTheme.typography.h6.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color =  if(isEnabled) Color.White else  Color.Gray,
                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                )
                if(!isEnabled){
                    Icon(
                        imageVector = Block,
                        contentDescription = "弹幕已禁用",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .size(12.dp).offset((-12).dp,(4).dp)
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoPauseButton(
    isEnabled: Boolean,
    active: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    Text(text = "播放完每条字幕后自动暂停 ",color = MaterialTheme.colors.onSurface)
                    Text(text ="P",color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                }
            }
        },
        delayMillis = 100, // 延迟 100 毫秒显示 Tooltip
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset.Zero
        )

    ) {
        IconToggleButton(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        ){
            val icon = if(isEnabled) Autopause else AutopauseDisabled

            val tint = if(isEnabled && active) {
                MaterialTheme.colors.primary
            }else{
                Color.White
            }
            Icon(
                imageVector = icon,
                contentDescription = "自动暂停",
                tint = tint
            )
        }
    }
}

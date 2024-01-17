package player

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import data.Caption
import state.getResourcesFile
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.*
import java.awt.event.*
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * @param window 视频播放窗口,  使用 JFrame 的一个原因是 swingPanel 重组的时候会产生闪光,
 * 相关 Issue: https://github.com/JetBrains/compose-jb/issues/1800,
 * 等Jetbrains 把 bug 修复了再重构。
 * @param setIsPlaying 设置是否正在播放视频
 * @param volume 音量
 * @param playTriple 视频播放参数，Caption 表示要播放的字幕，String 表示视频的地址，Int 表示字幕的轨道 ID。
 * @param videoPlayerComponent 视频播放组件
 * @param bounds 视频播放窗口的位置和大小
 * @param externalSubtitlesVisible 是否加载外部字幕
 */
fun play(
    window: JFrame,
    setIsPlaying: (Boolean) -> Unit,
    volume: Float,
    playTriple: Triple<Caption, String, Int>,
    videoPlayerComponent: Component,
    bounds: Rectangle,
    externalSubtitlesVisible:Boolean = false,
    resetVideoBounds :() ->  Rectangle = {Rectangle(0,0,540,330)},
    isVideoBoundsChanged:Boolean = false,
    setIsVideoBoundsChanged:(Boolean) -> Unit = {}
) {

    val playIcon = FlatSVGIcon(getResourcesFile("icon/play_arrow_white_24dp.svg"))
    val pauseIcon = FlatSVGIcon(getResourcesFile("icon/pause_white_24dp.svg"))
    val stopIcon = FlatSVGIcon(getResourcesFile("icon/stop_white_24dp.svg"))
    val backIcon = FlatSVGIcon(getResourcesFile("icon/flip_to_back_white_24dp.svg"))
    if(FlatLaf.isLafDark()){
        playIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
        pauseIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
        stopIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
        backIcon.colorFilter = FlatSVGIcon.ColorFilter { Color.LIGHT_GRAY }
    }

    val controlPanel = JPanel()
    controlPanel.isOpaque = false
    controlPanel.bounds = Rectangle(bounds.size.width/2 - 75,bounds.size.height - 50 ,150,50)
    controlPanel.isVisible = false


    val restoreButton = FlatButton()
    restoreButton.isVisible = isVideoBoundsChanged
    restoreButton.toolTipText = "还原"
    restoreButton.isContentAreaFilled = false
    restoreButton.buttonType = FlatButton.ButtonType.roundRect
    restoreButton.icon = backIcon
    restoreButton.addActionListener {
        val newRectangle =  resetVideoBounds()
        controlPanel.bounds = Rectangle(newRectangle.size.width/2 - 75,newRectangle.size.height - 50 ,150,50)
        restoreButton.isVisible = false
    }

    val playButton = FlatButton()
    playButton.isContentAreaFilled = false
    playButton.buttonType = FlatButton.ButtonType.roundRect
    playButton.icon = pauseIcon

    val stopButton = FlatButton()
    stopButton.isContentAreaFilled = false
    stopButton.buttonType = FlatButton.ButtonType.roundRect
    stopButton.icon = stopIcon

    controlPanel.add(restoreButton)
    controlPanel.add(playButton)
    controlPanel.add(stopButton)


    val embeddedMediaPlayerComponent:Component = if(isMacOS()){
        videoPlayerComponent as CallbackMediaPlayerComponent
    }else{
        videoPlayerComponent as EmbeddedMediaPlayerComponent
    }
    val playAction :() -> Unit = {
        if(videoPlayerComponent.mediaPlayer().status().isPlaying){
            videoPlayerComponent.mediaPlayer().controls().pause()
            playButton.icon = playIcon
            controlPanel.isVisible = true
        }else{
            videoPlayerComponent.mediaPlayer().controls().play()
            playButton.icon = pauseIcon
        }
        videoPlayerComponent.requestFocusInWindow()
    }
    playButton.addActionListener { playAction() }
    val keyListener = object: KeyAdapter() {
        override fun keyPressed(keyeEvent: KeyEvent) {
            if(keyeEvent.keyCode == 32){
                playAction()
            }
        }
    }
    fun getScreenLocation(e: MouseEvent, frame: JFrame): Point {
        val cursor = e.point
        val viewLocation = frame.locationOnScreen
        return Point((viewLocation.getX() + cursor.getX()).toInt(), (viewLocation.getY() + cursor.getY()).toInt())
    }


    // 鼠标相对于 window 坐标的坐标
    var xx = 0
    var yy = 0

    var startDrag = Point(0,0)
    val mouseListener = object: MouseAdapter(){
        override fun mousePressed(e: MouseEvent?) {
            if(e != null){
                xx = e.x
                yy = e.y
                startDrag = e.locationOnScreen
            }
        }

        override fun mouseClicked(e: MouseEvent?) {
           if(e?.button == 1){
               playAction()
           }
        }

        override fun mouseExited(e: MouseEvent?) {
            if(e != null ){
                val point = e.point
                if(point.x !in controlPanel.bounds.x..(controlPanel.bounds.x + controlPanel.bounds.width) ||
                    point.y !in controlPanel.bounds.y .. (controlPanel.bounds.y + controlPanel.bounds.height)
                ){
                    controlPanel.isVisible = false
                }

            }
        }

        override fun mouseEntered(e: MouseEvent?) {
            if(!controlPanel.isVisible){
                controlPanel.isVisible = true
            }
        }

    }
    val controlPanelMouseListener = object : MouseAdapter() {
        override fun mouseExited(e: MouseEvent?) {
            if(e != null){
                val point = e.point
                if(
                    point.y !in controlPanel.bounds.y .. (controlPanel.bounds.y + controlPanel.bounds.height)
                ){
                    controlPanel.isVisible = false
                }
            }
        }


    }
    controlPanel.addMouseListener(controlPanelMouseListener)
    val mouseMotionListener = object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent?) {
            if (e != null) {
                val cursorLocation = e.point
                val xPos = cursorLocation.x
                val yPos = cursorLocation.y
                val cursorArea =10
                when {
                    xPos >= cursorArea && xPos <= window.width - cursorArea && yPos >= window.height-cursorArea -> {
                        // 光标在窗口的底部边界
                        window.cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)
                    }
                    xPos >= window.width-cursorArea && yPos >= cursorArea && yPos <= window.height - cursorArea -> {
                        // 光标在窗口的右边界
                        window.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                    }
                    xPos >= window.width - cursorArea && yPos < cursorArea -> {
                        // 光标在窗口的右上角
                        window.cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)
                    }
                    xPos <= cursorArea && yPos>=cursorArea && yPos <= window.height - cursorArea -> {
                        // 光标在窗口的左边界
                        window.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
                    }
                    xPos >= cursorArea && xPos <= window.width - cursorArea && yPos <= cursorArea -> {
                        // 光标在窗口的顶部边界
                        window.cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
                    }
                    xPos <= cursorArea && yPos <= cursorArea -> {
                        // 光标在窗口的左上角
                        window.cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)
                    }
                    xPos > window.width - cursorArea && yPos >window.height - cursorArea -> {
                        // 光标在窗口的右下角
                        window.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
                    }
                    xPos <= cursorArea && yPos >= window.height- cursorArea -> {
                        // 光标在窗口的左下角
                        window.cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)
                    }

                    else -> {
                        // 在窗口内
                        window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    }
                }
            }
        }
        val toolkit = Toolkit.getDefaultToolkit()
        override fun mouseDragged(e: MouseEvent?) {

            // 移动窗口
            if (e != null && window.cursor.equals(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))) {
                val x = e.xOnScreen
                val y = e.yOnScreen
                window.location = Point(x-xx,y-yy)
                bounds.location = window.location
                restoreButton.isVisible = true
                setIsVideoBoundsChanged(true)
            // 调整窗口大小
            }else if(e != null && !window.cursor.equals(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))){

                val current = getScreenLocation(e, window)
                val offset = Point(current.x - startDrag.x,current.y - startDrag.y)
                val oldLocationX = window.location.x
                val oldLocationY = window.location.y

                var newLocationX = startDrag.x + offset.x
                var newLocationY = startDrag.y + offset.y
                var setLocation = false
                var newWidth = e.x
                var newHeight = e.y
                val minWidth = 300
                val minHeight = 300

                val cursor = window.cursor
                when(cursor){

                    Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) ->{
                        newLocationX = window.location.x
                        newWidth = window.width
                        newHeight = window.height - (newLocationY - oldLocationY)
                        setLocation = true
                    }
                    Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) ->{
                        newHeight = window.height
                    }
                    Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) ->{
                        newWidth = window.width
                    }
                    Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) ->{
                        newWidth = window.width - (newLocationX - oldLocationX)
                        newHeight = window.height
                        newLocationY = window.location.y
                        setLocation = true
                    }
                    Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR) ->{
                        newHeight = window.height - (newLocationY - oldLocationY)
                        newLocationX = window.location.x
                        setLocation = true
                    }
                    Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR) ->{
                        newWidth = window.width - (newLocationX - oldLocationX)
                        newLocationY = window.location.y
                        setLocation = true
                    }
                    Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR) ->{
                        newWidth = window.width - (newLocationX - oldLocationX)
                        newHeight = window.height - (newLocationY - oldLocationY)
                        setLocation = true
                    }

                }

                // 处理宽度边界
                if(newWidth >= toolkit.screenSize.width || newWidth <= minWidth){
                    newLocationX = oldLocationX
                    newWidth = window.width
                }
                // 处理长度边界
                if(newHeight >= toolkit.screenSize.height - 30 || newHeight <= minHeight){
                    newLocationY = oldLocationY
                    newHeight = window.height
                }

                // Cursor.SE_RESIZE_CURSOR
                if (newWidth != window.width || newHeight != window.height) {

                    window.size = Dimension(newWidth,newHeight)
                    embeddedMediaPlayerComponent.size = Dimension(newWidth,newHeight)

                    bounds.size =  embeddedMediaPlayerComponent.size
                    controlPanel.location = Point(bounds.size.width/2 - 75,bounds.size.height - 50 )
                    if(setLocation){
                        window.location = Point(newLocationX,newLocationY)
                        bounds.location = window.location
                        restoreButton.isVisible = true
                        setIsVideoBoundsChanged(true)
                    }
                }

            }
        }

    }

    // 关闭操作的公共函数，一次用于播放完毕，自动关闭，一次用于停止按钮的 clicked 动作。
    val closeFunc:() -> Unit = {
        if(videoPlayerComponent.mediaPlayer().status().isPlaying){
        videoPlayerComponent.mediaPlayer().controls().pause()
        }
        setIsPlaying(false)

        videoPlayerComponent.removeKeyListener(keyListener)
        embeddedMediaPlayerComponent.videoSurfaceComponent().removeMouseListener(mouseListener)
        embeddedMediaPlayerComponent.videoSurfaceComponent().removeMouseMotionListener(mouseMotionListener)
        EventQueue.invokeLater {
            window.remove(videoPlayerComponent)
            window.remove(controlPanel)
        }

        window.isVisible = false

    }

    val mediaPlayerEventListener = object:MediaPlayerEventAdapter(){
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            videoPlayerComponent.requestFocusInWindow()
            mediaPlayer.audio().setVolume((volume).toInt())
        }
        override fun finished(mediaPlayer: MediaPlayer) {
            closeFunc()
            videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    }

    val closeAction:() -> Unit = {
        closeFunc()
        videoPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(mediaPlayerEventListener)
    }
    stopButton.addActionListener { closeAction() }
    embeddedMediaPlayerComponent.videoSurfaceComponent().addMouseMotionListener(mouseMotionListener)
    embeddedMediaPlayerComponent.videoSurfaceComponent().addMouseListener(mouseListener)
    videoPlayerComponent.addKeyListener(keyListener)
    videoPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(mediaPlayerEventListener)
    videoPlayerComponent.bounds = Rectangle(0, 0, bounds.size.width, bounds.size.height)

    window.size = bounds.size
    window.location = bounds.location
    window.layout = null
    window.contentPane.add(controlPanel)
    window.contentPane.add(videoPlayerComponent)
    window.isVisible = true

    val caption = playTriple.first
    val relativeVideoPath = playTriple.second
    val trackId = playTriple.third
    val start = convertTimeToSeconds(caption.start)
    val end = convertTimeToSeconds(caption.end)
    // 使用内部字幕轨道,通常是从 MKV 生成的词库
    if(trackId != -1){
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":sub-track=$trackId", ":start-time=$start", ":stop-time=$end")
    // 自动加载外部字幕
    }else if(externalSubtitlesVisible){
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":sub-autodetect-file",":start-time=$start", ":stop-time=$end")
    }else{
        // 视频有硬字幕，加载了外部字幕会发生重叠。
        videoPlayerComponent.mediaPlayer().media()
            .play(relativeVideoPath, ":no-sub-autodetect-file",":start-time=$start", ":stop-time=$end")
    }
}



/**
 * 播放音频
 */
fun play(
    setIsPlaying: (Boolean) -> Unit,
    audioPlayerComponent: AudioPlayerComponent,
    volume: Float,
    caption: Caption,
    videoPath:String,
){


    audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume((volume * 100).toInt())
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            setIsPlaying(false)
            audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
        }
    })
    val start = convertTimeToSeconds(caption.start)
    val end = convertTimeToSeconds(caption.end)
    audioPlayerComponent.mediaPlayer().media()
        .play(videoPath,  ":start-time=$start",  ":stop-time=$end")
}

/**
 * 转换时间为秒
 */
fun convertTimeToSeconds(time:String):Double{
    val parts = time.split(":")
    val hours = parts[0].toLong()
    val minutes = parts[1].toLong()
    val seconds = parts[2].toDouble()

    val totalSeconds = hours * 3600 + minutes * 60 + seconds
    return totalSeconds
}

/**
 * 转换时间为毫秒
 */
fun convertTimeToMilliseconds(time:String):Long{
    val parts = time.split(":")
    val hours = parts[0].toLong()
    val minutes = parts[1].toLong()
    val seconds = parts[2].substringBefore(".").toLong()
    val milliseconds = parts[2].substringAfter(".").toLong()

    val totalMilliseconds = ((hours * 3600 + minutes * 60 + seconds) * 1000) + milliseconds
    return totalMilliseconds
}


fun Component.videoSurfaceComponent(): Component {
    return when (this) {
        is CallbackMediaPlayerComponent -> videoSurfaceComponent()
        is EmbeddedMediaPlayerComponent -> videoSurfaceComponent()
        else -> throw IllegalArgumentException("You can only call videoSurfaceComponent() on vlcj player component")
    }
}
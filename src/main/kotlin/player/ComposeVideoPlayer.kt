package player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.Component
import java.nio.ByteBuffer

@Composable
fun ComposeVideoPlayer(
    url:String,
    width:Int,
    height:Int
) {
    var imageBitmap by remember{ mutableStateOf(ImageBitmap(width, height)) }
    Image(modifier = Modifier.width(width.dp).height(height.dp), bitmap = imageBitmap, contentDescription = "Video")
    val mediaPlayerComponent by remember { mutableStateOf(createMediaPlayerComponent()) }
    LaunchedEffect(Unit){
        var byteArray:ByteArray? = null
        var imageInfo :ImageInfo? = null
        val mediaPlayer = mediaPlayerComponent.embeddedMediaPlayer()
        val callbackVideoSurface = CallbackVideoSurface(
            object :BufferFormatCallback{
                override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                    imageInfo = ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
                    return RV32BufferFormat(sourceWidth, sourceHeight)
                }

                override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
                    byteArray = ByteArray(buffers[0].limit())
                }
            },
            object : RenderCallback {

                override fun display(
                    mediaPlayer: MediaPlayer,
                    nativeBuffers: Array<out ByteBuffer>,
                    bufferFormat: BufferFormat
                ) {
                    imageInfo?.let {
                        val byteBuffer = nativeBuffers[0]
                        byteBuffer.get(byteArray)
                        byteBuffer.rewind()
                        imageBitmap = Bitmap().apply {
                            allocPixels(it)
                            installPixels(byteArray)
                        }.asComposeImageBitmap()
                    }
                }
            },
            true,
            VideoSurfaceAdapters.getVideoSurfaceAdapter(),
        )
        mediaPlayer.videoSurface().set(callbackVideoSurface)
        mediaPlayer.media().play(url)
    }
}

fun Component.embeddedMediaPlayer(): EmbeddedMediaPlayer {
    return when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
    }
}

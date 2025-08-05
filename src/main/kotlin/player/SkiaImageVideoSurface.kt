package player

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import io.ktor.utils.io.*
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer

/**
 * 使用 Skia Pixmap 渲染视频帧到 Image 的 VideoSurface。
 * copied from https://github.com/caprica/vlcj/issues/1234#issuecomment-2143293403
 */
class SkiaImageVideoSurface : VideoSurface(VideoSurfaceAdapters.getVideoSurfaceAdapter()) {

    private val videoSurface = SkiaImageCallbackVideoSurface()
    private lateinit var pixmap: Pixmap
    private val skiaImage = mutableStateOf<Image?>(null)

    val image: State<Image?> = skiaImage

    fun release() {
        try{
            // 释放当前的 Skia Image
            skiaImage.value?.close()
            skiaImage.value = null

            // 释放 Pixmap 资源
            if (::pixmap.isInitialized) {
                pixmap.close()
            }
        }catch (ex: Exception) {
            // 捕获并忽略异常，确保资源释放不会导致崩溃
            ex.printStack()
        }

    }
    private inner class SkiaImageBufferFormatCallback : BufferFormatCallback {
        private var sourceWidth: Int = 0
        private var sourceHeight: Int = 0

        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            this.sourceWidth = sourceWidth
            this.sourceHeight = sourceHeight
            return RV32BufferFormat(sourceWidth, sourceHeight)
        }

        override fun newFormatSize(
            bufferWidth: Int,
            bufferHeight: Int,
            displayWidth: Int,
            displayHeight: Int
        ) = Unit

        override fun allocatedBuffers(buffers: Array<ByteBuffer>) {
            val buffer = buffers[0]
            val pointer = ByteBufferFactory.getAddress(buffer)
            val imageInfo = ImageInfo.makeN32Premul(sourceWidth, sourceHeight, ColorSpace.sRGB)

            pixmap = Pixmap.make(imageInfo, pointer, sourceWidth * 4)
        }
    }

    private inner class SkiaImageRenderCallback : RenderCallback {
        override fun display(
            mediaPlayer: MediaPlayer,
            nativeBuffers: Array<ByteBuffer>,
            bufferFormat: BufferFormat,
            displayWidth: Int,
            displayHeight: Int
        ) {
            if (!mediaPlayer.status().isPlaying) {
                return
            }

            // 清理 Skia 资源
            skiaImage.value?.close()
            skiaImage.value = Image.makeFromPixmap(pixmap)
        }

        override fun lock(mediaPlayer: MediaPlayer?) = Unit
        override fun unlock(mediaPlayer: MediaPlayer?) = Unit
    }

    private inner class SkiaImageCallbackVideoSurface : CallbackVideoSurface(
        SkiaImageBufferFormatCallback(),
        SkiaImageRenderCallback(),
        true,
        videoSurfaceAdapter,
    )

    override fun attach(mediaPlayer: MediaPlayer) {
        videoSurface.attach(mediaPlayer)
    }

}
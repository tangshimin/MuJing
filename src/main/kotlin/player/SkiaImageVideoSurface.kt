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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 使用 Skia Pixmap 渲染视频帧到 Image 的 VideoSurface。
 * copied from https://github.com/caprica/vlcj/issues/1234#issuecomment-2143293403

 * ## 线程安全机制详解
 *
 * `SkiaImageVideoSurface` 被设计为在多线程环境中安全运行，主要涉及两个关键线程：
 * 1.  **VLCJ 渲染回调线程**: 该线程在 `SkiaImageRenderCallback.display` 方法中运行，负责从视频解码器接收新帧，并更新 `skiaImage` 状态。这是一个写操作。
 * 2.  **Compose UI 绘制线程**: 该线程在 `CustomCanvas` 的 `onDraw` 块中运行，通过调用 `withImage` 方法来读取 `skiaImage` 并将其绘制到屏幕上。这是一个读操作。
 *
 * ### 问题
 *
 * 如果没有同步机制，当 `display` 线程正在关闭旧 `Image` 并创建新 `Image` 的过程中（`skiaImage.value?.close()` 和 `skiaImage.value = ...` 之间），UI 线程可能会尝试读取 `skiaImage`。这会导致 UI 线程访问到一个已经被释放的、无效的或处于不一致状态的 `Image` 对象，从而引发原生代码崩溃（如日志中所示的 `Image_nGetImageInfo` 错误）。
 *
 * ### 解决方案：ReentrantLock
 *
 * 为了解决这个竞态条件，我们引入了一个 `java.util.concurrent.locks.ReentrantLock` 实例（名为 `lock`）。所有对 `skiaImage` 的读写操作都被包裹在 `lock.withLock { ... }` 代码块中。
 *
 * `lock.withLock` 保证了在任何时刻，只有一个线程能够进入被其保护的代码块。它就像一个通行证，确保了“更新图像”和“读取图像进行绘制”这两个操作是**互斥**的，不会同时发生。
 *
 * ### 工作流程
 *
 * - **当 `display` 线程先获得锁**:
 *   1. `display` 线程进入 `lock.withLock` 块，开始更新图像。
 *   2. 如果此时 UI 线程调用 `withImage`，它会因为无法获得锁而**阻塞等待**。
 *   3. `display` 线程安全地关闭旧图像，创建并赋值新图像。
 *   4. `display` 线程执行完毕，释放锁。
 *   5. UI 线程获得锁，继续执行，此时它读取到的是一个全新的、完整的 `Image` 对象。
 *
 * - **当 UI 绘制线程先获得锁**:
 *   1. UI 线程通过 `withImage` 进入 `lock.withLock` 块，读取当前的 `Image` 对象准备绘制。
 *   2. 如果此时 `display` 线程想要更新图像，它会因为无法获得锁而**阻塞等待**。
 *   3. UI 线程使用当前有效的 `Image` 完成所有绘制操作。
 *   4. UI 线程执行完毕，释放锁。
 *   5. `display` 线程获得锁，开始执行图像的更新流程。
 *
 * ### 结论
 *
 * 通过这种方式，`ReentrantLock` 确保了 `skiaImage` 的原子性更新。UI 线程要么绘制旧的、完整的图像，要么等待更新完成后，绘制新的、完整的图像。它永远不会访问到一个被中途修改或已失效的 `Image` 对象，从而根除了崩溃的风险。
 */


class SkiaImageVideoSurface : VideoSurface(VideoSurfaceAdapters.getVideoSurfaceAdapter()) {

    private val videoSurface = SkiaImageCallbackVideoSurface()
    private lateinit var pixmap: Pixmap
    private val skiaImage = mutableStateOf<Image?>(null)
    private val lock = ReentrantLock()
    private val _isRenderingEnabled = AtomicBoolean(true)


    /**
     * 安全地访问 Skia Image 对象。
     * @param block 一个代码块，它接收一个可空的 Image 对象作为参数。
     */
    fun <R> withImage(block: (Image?) -> R): R = lock.withLock {
        block(skiaImage.value)
    }
    /**
     * 设置是否启用视频渲染
     * @param enabled true 表示启用渲染，false 表示暂停渲染（如最小化时）
     */
    fun setRenderingEnabled(enabled: Boolean) {
        _isRenderingEnabled.set(enabled)
    }

    fun release() {
        lock.withLock {
            try{

                // 停止渲染
                _isRenderingEnabled.set(false)

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


            lock.withLock {

                if (!_isRenderingEnabled.get()) {
                    return
                }
                // 清理 Skia 资源
                skiaImage.value?.close()
                skiaImage.value = Image.makeFromPixmap(pixmap)
            }
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


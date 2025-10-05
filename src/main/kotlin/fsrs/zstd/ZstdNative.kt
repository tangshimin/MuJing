/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 *
 * This file contains code based on FSRS-Kotlin (https://github.com/open-spaced-repetition/FSRS-Kotlin)
 * Original work Copyright (c) 2025 khordady
 * Original work licensed under MIT License
 *
 * The original MIT License text:
 *
 * MIT License
 *
 * Copyright (c) 2025 khordady
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fsrs.zstd

class ZstdNative {
    external fun compress(input: ByteArray, compressionLevel: Int): ByteArray
    external fun decompress(input: ByteArray): ByteArray
    external fun compressStream(input: ByteArray, compressionLevel: Int): ByteArray
    external fun getVersion(): String
    
    companion object {
        init {
            var loaded = false
            try {
                System.loadLibrary("rust_zstd_jni")
                loaded = true
            } catch (e: Throwable) {
                // 尝试从工程内置路径加载
                val userDir = System.getProperty("user.dir") ?: "."
                val candidates = mutableListOf<String>()
                val base = "$userDir/rust-zstd-jni/target"
                val names = when (detectOs()) {
                    Os.Mac -> listOf("release/librust_zstd_jni.dylib", "debug/librust_zstd_jni.dylib")
                    Os.Linux -> listOf("release/librust_zstd_jni.so", "debug/librust_zstd_jni.so")
                    Os.Windows -> listOf("release/rust_zstd_jni.dll", "debug/rust_zstd_jni.dll")
                }
                names.forEach { candidates.add("$base/$it") }
                var lastError: Throwable = e
                for (p in candidates) {
                    try {
                        java.nio.file.Files.exists(java.nio.file.Paths.get(p)).also {
                            if (it) {
                                System.load(p)
                                loaded = true
                                break
                            }
                        }
                    } catch (t: Throwable) {
                        lastError = t
                    }
                }
                if (!loaded) throw lastError
            }
        }
        private enum class Os { Mac, Linux, Windows }
        private fun detectOs(): Os {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("mac") || os.contains("darwin") -> Os.Mac
                os.contains("win") -> Os.Windows
                else -> Os.Linux
            }
        }
    }
}
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

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZstdNativeTest {
    @Test
    fun compress_decompress_roundtrip_and_magic() {
        val native = ZstdNative()
        val version = native.getVersion()
        println("zstd version: $version")

        val input = ByteArray(64 * 1024) { (it % 251).toByte() }
        val compressed = native.compress(input, 0)
        assertTrue(compressed.size >= 5, "compressed too short")
        // magic: 28 B5 2F FD
        assertTrue(compressed[0] == 0x28.toByte())
        assertTrue(compressed[1] == 0xB5.toByte())
        assertTrue(compressed[2] == 0x2F.toByte())
        assertTrue(compressed[3] == 0xFD.toByte())
        val fd = compressed[4].toInt() and 0xFF
        assertTrue(fd == 0x24 || fd == 0x04, "unexpected frame descriptor: 0x${fd.toString(16)}")

        val decompressed = native.decompress(compressed)
        assertArrayEquals(input, decompressed)
    }
}

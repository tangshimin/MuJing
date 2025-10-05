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

package fsrs.apkg

import java.io.InputStream

/**
 * APKG 元数据类
 * 处理 meta 文件中的版本信息
 */
data class ApkgMeta(
    val version: Int
) {
    
    fun toByteArray(): ByteArray {
        val versionBytes = encodeVarint(version.toLong())
        return byteArrayOf(0x08) + versionBytes
    }
    companion object {
        /**
         * 从输入流解析 meta 文件
         */
        fun fromInputStream(input: InputStream): ApkgMeta {
            val bytes = input.readBytes()
            // 简单的 protobuf 解析：第一个字节是字段标签，后续是 varint 编码的版本号
            if (bytes.isNotEmpty() && bytes[0] == 0x08.toByte()) {
                val version = decodeVarint(bytes, 1)
                return ApkgMeta(version.toInt())
            }
            throw IllegalArgumentException("Invalid meta file format")
        }
        
        /**
         * 解码 varint 值
         */
        private fun decodeVarint(bytes: ByteArray, startIndex: Int): Long {
            var result = 0L
            var shift = 0
            var index = startIndex
            
            while (index < bytes.size) {
                val b = bytes[index].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) {
                    return result
                }
                shift += 7
                index++
            }
            throw IllegalArgumentException("Invalid varint encoding")
        }
        
        /**
         * 创建新格式的 meta 数据
         */
        fun createLatest(): ByteArray {
            val versionBytes = encodeVarint(3L) // LATEST 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 创建过渡格式的 meta 数据
         */
        fun createTransitional(): ByteArray {
            val versionBytes = encodeVarint(2L) // LEGACY2 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 创建旧格式的 meta 数据
         */
        fun createLegacy(): ByteArray {
            val versionBytes = encodeVarint(1L) // LEGACY1 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 编码 varint 值
         */
        private fun encodeVarint(value: Long): ByteArray {
            val result = mutableListOf<Byte>()
            var v = value
            
            while (v >= 0x80) {
                result.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            result.add(v.toByte())
            
            return result.toByteArray()
        }
    }
}